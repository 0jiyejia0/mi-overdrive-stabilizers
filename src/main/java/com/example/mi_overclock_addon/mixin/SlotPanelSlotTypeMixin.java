package com.example.mi_overclock_addon.mixin;

import com.example.mi_overclock_addon.logic.OverclockModuleType;

import aztech.modern_industrialization.machines.guicomponents.SlotPanel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlotPanel.SlotType.class)
public abstract class SlotPanelSlotTypeMixin {
    /**
     * Allow our overclock modules to be inserted into the vanilla overdrive ("overclock") slot,
     * which otherwise only accepts MI's own overdrive module.
     */
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void mi_overclock_addon$allowOverclockModules(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) == SlotPanel.SlotType.OVERDRIVE_MODULE && OverclockModuleType.isOverclockModule(stack)) {
            cir.setReturnValue(true);
        }
    }
}
