package com.example.mi_overclock_addon.logic;

import com.example.mi_overclock_addon.config.OverclockConfig;
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
    DIGITAL;

    public boolean allowsRecipeSwitching() {
        return this != NONE;
    }

    public boolean isDigital() {
        return this == DIGITAL;
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
            case MEMORY -> applyRetentionPercent(bankedEfficiency, OverclockConfig.MEMORY_RETENTION_PERCENT.get(), newMaxEfficiency);
            case PERSISTENT -> applyRetentionPercent(bankedEfficiency, OverclockConfig.PERSISTENT_RETENTION_PERCENT.get(), newMaxEfficiency);
            // Always pinned to the maximum overclock.
            case DIGITAL -> newMaxEfficiency;
            case NONE -> 0;
        };
    }

    /**
     * Restores {@code percent}% of the banked warm-up, rounding up so a non-zero bank never collapses
     * to 0, and clamping to the new recipe's cap.
     */
    private static int applyRetentionPercent(int bankedEfficiency, int percent, int newMaxEfficiency) {
        if (bankedEfficiency <= 0 || percent <= 0) {
            return 0;
        }
        int retained = (int) ((bankedEfficiency * (long) percent + 99) / 100);
        return Math.min(newMaxEfficiency, retained);
    }

    /** Whether this module keeps the machine pinned at max overclock while idle (drains EU to do so). */
    public boolean holdsMaxWhenIdle() {
        return this == DIGITAL && OverclockConfig.DIGITAL_HOLD_WHEN_IDLE.get();
    }

    public static boolean isOverclockModule(ItemStack stack) {
        return fromStack(stack) != NONE;
    }

    public static OverclockModuleType fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return NONE;
        }
        if (stack.getItem() == ModItems.DIGITAL_OVERCLOCK_STABILIZER.get()) {
            return DIGITAL;
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
        if (!OverclockConfig.ENABLED.get()) {
            return NONE;
        }
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
