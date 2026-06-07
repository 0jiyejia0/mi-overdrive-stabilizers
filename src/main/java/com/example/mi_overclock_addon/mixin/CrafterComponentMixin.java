package com.example.mi_overclock_addon.mixin;

import java.util.ArrayList;
import java.util.List;

import com.example.mi_overclock_addon.logic.OverclockModuleType;

import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import aztech.modern_industrialization.util.Simulation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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

    /** Module type resolved once at the start of each tick and reused for the rest of the tick. */
    @Unique
    private OverclockModuleType mi_overclock_addon$tickModuleType = OverclockModuleType.NONE;

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

        // Holding warm-up without an active recipe makes vanilla return singletonList(null), which
        // crashes when iterated. Guard that case, and also let modules rescan recipes while overclocked.
        boolean needsNullRecipeGuard = activeRecipe == null;
        boolean moduleAllowsSwitching = OverclockModuleType.from(behavior).allowsRecipeSwitching();

        if (needsNullRecipeGuard || moduleAllowsSwitching) {
            cir.setReturnValue(mi_overclock_addon$getMatchingRecipes());
        }
    }

    @Unique
    private Iterable<RecipeHolder<MachineRecipe>> mi_overclock_addon$getMatchingRecipes() {
        ServerLevel serverWorld = (ServerLevel) behavior.getCrafterWorld();
        MachineRecipeType recipeType = behavior.recipeType();
        List<RecipeHolder<MachineRecipe>> recipes = new ArrayList<>(recipeType.getFluidOnlyRecipes(serverWorld));
        for (ConfigurableItemStack stack : inventory.getItemInputs()) {
            if (!stack.isEmpty()) {
                recipes.addAll(recipeType.getMatchingRecipes(serverWorld, stack.getResource().getItem()));
            }
        }
        return recipes;
    }

    /**
     * While a module holds warm-up, never let MI clear the active recipe. This keeps the overclock
     * value stable instead of flickering to 0 when efficiency dips to the 0/1 boundary between crafts.
     * The {@code getRecipes} guard above still protects the null-recipe case as a safety net.
     */
    @Inject(method = "clearActiveRecipeIfPossible", at = @At("HEAD"), cancellable = true)
    private void mi_overclock_addon$keepRecipeWhileHoldingWarmUp(CallbackInfo ci) {
        if (mi_overclock_addon$tickStartEfficiency > 0 && mi_overclock_addon$tickModuleType.allowsRecipeSwitching()) {
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
        OverclockModuleType moduleType = mi_overclock_addon$tickModuleType;
        if (!moduleType.allowsRecipeSwitching() || !cir.getReturnValueZ() || activeRecipe == null) {
            return;
        }

        // Digital jumps straight to max the instant it has any recipe, so the overclock reads full
        // immediately on insertion rather than only after the first craft.
        if (moduleType.isDigital()) {
            efficiencyTicks = maxEfficiencyTicks;
            mi_overclock_addon$justSwitchedRecipe = true;
            return;
        }

        // Memory / Persistent only react to an actual switch to a different recipe.
        if (mi_overclock_addon$previousRecipe == null || activeRecipe == mi_overclock_addon$previousRecipe) {
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
        mi_overclock_addon$tickModuleType = OverclockModuleType.from(behavior);

        // Without a module the banked warm-up is forgotten, so removing and re-inserting a module
        // starts a fresh bank instead of resurrecting an old peak. tickRecipe runs every tick
        // (even while idle), so this triggers promptly when the module is pulled.
        if (mi_overclock_addon$tickModuleType == OverclockModuleType.NONE) {
            mi_overclock_addon$bankedEfficiency = 0;
            return;
        }

        // Digital pins to max at the start of the tick so this tick already runs at full speed.
        // Only do so when configured to hold while idle; otherwise it reaches max through actual
        // crafting (handled at RETURN) and recipe switches, and merely holds its value while idle.
        if (activeRecipe != null && mi_overclock_addon$tickModuleType.holdsMaxWhenIdle()) {
            efficiencyTicks = maxEfficiencyTicks;
        }
    }

    @Inject(method = "tickRecipe", at = @At("RETURN"))
    private void mi_overclock_addon$holdOverclock(CallbackInfoReturnable<Boolean> cir) {
        OverclockModuleType moduleType = mi_overclock_addon$tickModuleType;
        if (moduleType == OverclockModuleType.NONE) {
            return;
        }

        boolean active = cir.getReturnValueZ();

        // Digital stays pinned at maximum overclock. While actively crafting this is free; while idle
        // it only keeps forcing max (and spends EU to do so) if configured to hold when idle.
        if (moduleType.isDigital() && activeRecipe != null && maxEfficiencyTicks > 0) {
            if (active) {
                efficiencyTicks = maxEfficiencyTicks;
                mi_overclock_addon$bankEfficiency();
                return;
            }
            if (moduleType.holdsMaxWhenIdle()) {
                long euToMaintain = Math.max(1, getCurrentRecipeEu());
                if (behavior.consumeEu(euToMaintain, Simulation.ACT) > 0) {
                    efficiencyTicks = maxEfficiencyTicks;
                    mi_overclock_addon$bankEfficiency();
                    return;
                }
            }
            // Idle without idle-hold (or out of power): fall through to the freeze so the overclock
            // is retained for free, just like the Persistent module.
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
