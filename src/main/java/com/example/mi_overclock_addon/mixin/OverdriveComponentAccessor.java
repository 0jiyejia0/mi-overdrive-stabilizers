package com.example.mi_overclock_addon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import aztech.modern_industrialization.machines.components.OverdriveComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(OverdriveComponent.class)
public interface OverdriveComponentAccessor {
    @Accessor("overdriveModule")
    ItemStack mi_overclock_addon$getOverdriveModule();
}
