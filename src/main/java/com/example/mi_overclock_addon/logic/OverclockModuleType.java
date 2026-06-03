package com.example.mi_overclock_addon.logic;

import com.example.mi_overclock_addon.mixin.OverdriveComponentAccessor;
import com.example.mi_overclock_addon.registry.ModItems;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.components.OverdriveComponent;
import net.minecraft.world.item.ItemStack;

public enum OverclockModuleType {
    NONE,
    MEMORY,
    PERSISTENT,
    QUANTUM;

    public boolean allowsRecipeSwitching() {
        return this != NONE;
    }

    public boolean isQuantum() {
        return this == QUANTUM;
    }

    /**
     * Efficiency to restore right after the machine switches to a different recipe.
     *
     * <p>The value is restored from the machine's banked peak warm-up (the highest warm-up it
     * ever reached), capped to the new recipe's maximum. Because the bank stores raw warm-up
     * ticks, it is exploit-resistant: a low-cap recipe can never inflate the bank beyond its own
     * low cap, so you cannot "launder" a quick warm-up into a high-cap recipe. At the same time,
     * detouring through a low-cap recipe no longer erases the peak you genuinely earned.
     *
     * @param bankedEfficiency peak warm-up ticks the machine has ever reached
     * @param newMaxEfficiency maximum efficiency ticks of the new recipe
     */
    public int switchEfficiency(int bankedEfficiency, int newMaxEfficiency) {
        return switch (this) {
            // Restores half of the banked warm-up when changing recipes.
            case MEMORY -> Math.min(newMaxEfficiency, bankedEfficiency / 2);
            // Restores the full banked warm-up when changing recipes.
            case PERSISTENT -> Math.min(newMaxEfficiency, bankedEfficiency);
            // Always pinned to the maximum overclock.
            case QUANTUM -> newMaxEfficiency;
            case NONE -> 0;
        };
    }

    public static boolean isOverclockModule(ItemStack stack) {
        return fromStack(stack) != NONE;
    }

    public static OverclockModuleType fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return NONE;
        }
        if (stack.getItem() == ModItems.QUANTUM_OVERCLOCK_CONTROLLER.get()) {
            return QUANTUM;
        }
        if (stack.getItem() == ModItems.PERSISTENT_OVERCLOCK_MODULE.get()) {
            return PERSISTENT;
        }
        if (stack.getItem() == ModItems.OVERCLOCK_MEMORY_MODULE.get()) {
            return MEMORY;
        }
        return NONE;
    }

    public static OverclockModuleType from(CrafterComponent.Behavior behavior) {
        if (!(behavior instanceof MachineBlockEntity machine)) {
            return NONE;
        }

        OverdriveComponent overdrive = machine.components.getNullable(OverdriveComponent.class);
        if (overdrive == null) {
            return NONE;
        }

        ItemStack stack = ((OverdriveComponentAccessor) overdrive).mi_overclock_addon$getOverdriveModule();
        return fromStack(stack);
    }
}
