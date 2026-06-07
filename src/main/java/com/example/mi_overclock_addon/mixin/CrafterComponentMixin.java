package com.example.mi_overclock_addon.mixin;

import com.example.mi_overclock_addon.logic.OverclockModuleType;

import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.util.Simulation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrafterComponent.class)
public abstract class CrafterComponentMixin {
    @Unique
    private static final String MI_OVERCLOCK_BANK_KEY = "MiOverclockBankedEfficiency";

    @Shadow
    private CrafterComponent.Inventory inventory;

    @Shadow
    private CrafterComponent.Behavior behavior;

    @Shadow
    private RecipeHolder<MachineRecipe> activeRecipe;

    @Shadow
    private int efficiencyTicks;

    @Shadow
    private int maxEfficiencyTicks;

    @Shadow
    public abstract long getCurrentRecipeEu();

    @Unique
    private int mi_overclock_addon$tickStartEfficiency;

    @Unique
    private RecipeHolder<MachineRecipe> mi_overclock_addon$previousRecipe;

    @Unique
    private boolean mi_overclock_addon$justSwitchedRecipe;

    /** Efficiency at the start of {@code updateActiveRecipe}, before MI may change the active recipe. */
    @Unique
    private int mi_overclock_addon$efficiencyBeforeRecipeUpdate;

    /**
     * Peak warm-up (efficiency ticks) the machine has ever reached while a module was installed.
     * This single value is the "banked warm-up": it lets the machine restore its overclock after
     * detouring through a low-cap recipe, while staying exploit-resistant (a low-cap recipe can
     * never raise it past its own low cap).
     */
    @Unique
    private int mi_overclock_addon$bankedEfficiency;

    /**
     * By default MI only looks at the current {@code activeRecipe} while {@code efficiencyTicks > 0}.
     * That makes the machine refuse to switch recipes until the overclock has fully decayed.
     * With a module installed, expose every recipe matching the current inputs so the machine can
     * move to a new recipe while still overclocked.
     */
    @Inject(method = "getRecipes()Ljava/lang/Iterable;", at = @At("HEAD"), cancellable = true)
    private void mi_overclock_addon$allowRecipeSwitching(CallbackInfoReturnable<Iterable<RecipeHolder<MachineRecipe>>> cir) {
        if (efficiencyTicks <= 0) {
            return;
        }

        // Holding warm-up without an active recipe (vanilla returns singletonList(null) and crashes).
        boolean needsNullRecipeGuard = activeRecipe == null;
        boolean moduleAllowsSwitching = OverclockModuleType.from(behavior).allowsRecipeSwitching();

        if (needsNullRecipeGuard || moduleAllowsSwitching) {
            cir.setReturnValue(CrafterComponent.getRecipes(behavior.getCrafterWorld(), behavior.recipeType(), inventory.getItemInputs()));
        }
    }

    /**
     * While a module is holding warm-up, do not clear the active recipe when efficiency briefly hits 0
     * before our end-of-tick freeze restores it.
     */
    @Inject(method = "clearActiveRecipeIfPossible", at = @At("HEAD"), cancellable = true)
    private void mi_overclock_addon$keepRecipeWhileHoldingWarmUp(CallbackInfo ci) {
        if (mi_overclock_addon$tickStartEfficiency > 0 && OverclockModuleType.from(behavior).allowsRecipeSwitching()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateActiveRecipe", at = @At("HEAD"))
    private void mi_overclock_addon$captureBeforeUpdate(CallbackInfoReturnable<Boolean> cir) {
        mi_overclock_addon$previousRecipe = activeRecipe;
        mi_overclock_addon$efficiencyBeforeRecipeUpdate = efficiencyTicks;
    }

    @Inject(method = "updateActiveRecipe", at = @At("RETURN"))
    private void mi_overclock_addon$retainEfficiencyOnSwitch(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                || mi_overclock_addon$previousRecipe == null
                || activeRecipe == mi_overclock_addon$previousRecipe) {
            return;
        }

        OverclockModuleType moduleType = OverclockModuleType.from(behavior);
        if (!moduleType.allowsRecipeSwitching()) {
            return;
        }

        int warmUpSource = Math.max(mi_overclock_addon$bankedEfficiency, mi_overclock_addon$efficiencyBeforeRecipeUpdate);
        if (warmUpSource <= 0) {
            return;
        }

        efficiencyTicks = moduleType.switchEfficiency(warmUpSource, maxEfficiencyTicks);
        mi_overclock_addon$justSwitchedRecipe = true;
    }

    @Inject(method = "tickRecipe", at = @At("HEAD"))
    private void mi_overclock_addon$captureTickStart(CallbackInfoReturnable<Boolean> cir) {
        mi_overclock_addon$tickStartEfficiency = efficiencyTicks;
        mi_overclock_addon$justSwitchedRecipe = false;

        OverclockModuleType moduleType = OverclockModuleType.from(behavior);

        // Without a module the banked warm-up is forgotten, so removing and re-inserting a module
        // starts a fresh bank instead of resurrecting an old peak. tickRecipe runs every tick
        // (even while idle), so this triggers promptly when the module is pulled.
        if (moduleType == OverclockModuleType.NONE) {
            mi_overclock_addon$bankedEfficiency = 0;
            return;
        }

        // Quantum keeps the machine pinned at maximum overclock as long as it has a recipe.
        if (activeRecipe != null && moduleType.isQuantum()) {
            efficiencyTicks = maxEfficiencyTicks;
        }
    }

    @Inject(method = "tickRecipe", at = @At("RETURN"))
    private void mi_overclock_addon$holdOverclock(CallbackInfoReturnable<Boolean> cir) {
        OverclockModuleType moduleType = OverclockModuleType.from(behavior);
        if (moduleType == OverclockModuleType.NONE) {
            return;
        }

        boolean active = cir.getReturnValueZ();

        // Quantum stays pinned at maximum overclock as long as the machine is powered.
        if (moduleType.isQuantum() && activeRecipe != null && maxEfficiencyTicks > 0) {
            if (!active) {
                long euToMaintain = Math.max(1, getCurrentRecipeEu());
                if (behavior.consumeEu(euToMaintain, Simulation.ACT) <= 0) {
                    return;
                }
            }
            efficiencyTicks = maxEfficiencyTicks;
            mi_overclock_addon$bankEfficiency();
            return;
        }

        // Memory / Persistent freeze decay while idle. MI drops 1 tick of efficiency when starved for
        // EU and clears the active recipe once efficiency hits 0, so we must freeze even if the recipe
        // was cleared this tick (e.g. holding at exactly 1 tick of warm-up).
        if (!mi_overclock_addon$justSwitchedRecipe
                && mi_overclock_addon$tickStartEfficiency > 0
                && efficiencyTicks < mi_overclock_addon$tickStartEfficiency) {
            int cap = maxEfficiencyTicks > 0 ? maxEfficiencyTicks : mi_overclock_addon$tickStartEfficiency;
            efficiencyTicks = Math.min(mi_overclock_addon$tickStartEfficiency, cap);
        }

        mi_overclock_addon$bankEfficiency();
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void mi_overclock_addon$writeBank(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putInt(MI_OVERCLOCK_BANK_KEY, mi_overclock_addon$bankedEfficiency);
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void mi_overclock_addon$readBank(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine, CallbackInfo ci) {
        mi_overclock_addon$bankedEfficiency = tag.getInt(MI_OVERCLOCK_BANK_KEY);
    }

    @Unique
    private void mi_overclock_addon$bankEfficiency() {
        if (efficiencyTicks > mi_overclock_addon$bankedEfficiency) {
            mi_overclock_addon$bankedEfficiency = efficiencyTicks;
        }
    }
}
