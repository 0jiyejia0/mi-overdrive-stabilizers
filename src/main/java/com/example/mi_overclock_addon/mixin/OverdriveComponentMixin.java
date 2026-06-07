package com.example.mi_overclock_addon.mixin;

import com.example.mi_overclock_addon.logic.OverclockModuleType;

import aztech.modern_industrialization.machines.components.OverdriveComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OverdriveComponent.class)
public abstract class OverdriveComponentMixin {
    /**
     * Our overclock modules share the overdrive slot, but they must not trigger MI's vanilla
     * overdrive speed boost. Only their own overclock-retention effect should apply.
     */
    @Inject(method = "shouldOverdrive", at = @At("HEAD"), cancellable = true)
    private void mi_overclock_addon$suppressVanillaOverdrive(CallbackInfoReturnable<Boolean> cir) {
        if (OverclockModuleType.isOverclockModule(((OverdriveComponentAccessor) this).mi_overclock_addon$getOverdriveModule())) {
            cir.setReturnValue(false);
        }
    }
}
