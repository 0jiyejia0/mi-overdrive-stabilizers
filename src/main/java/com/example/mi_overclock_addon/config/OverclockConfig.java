package com.example.mi_overclock_addon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the overclock addon. All values are read live during machine ticks, so changes
 * to the config file take effect without restarting once it is reloaded.
 */
public final class OverclockConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MEMORY_RETENTION_PERCENT;
    public static final ModConfigSpec.IntValue PERSISTENT_RETENTION_PERCENT;
    public static final ModConfigSpec.BooleanValue DIGITAL_HOLD_WHEN_IDLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ENABLED = builder
                .comment("Master switch. When false, overclock modules have no effect.")
                .define("enabled", true);

        builder.push("retention");

        MEMORY_RETENTION_PERCENT = builder
                .comment("Percent of the banked overclock the Analog Stabilizer restores when the recipe changes.")
                .defineInRange("memoryRetentionPercent", 50, 0, 100);

        PERSISTENT_RETENTION_PERCENT = builder
                .comment("Percent of the banked overclock the Electronic Stabilizer restores when the recipe changes.")
                .defineInRange("persistentRetentionPercent", 100, 0, 100);

        builder.pop();

        builder.push("digital");

        DIGITAL_HOLD_WHEN_IDLE = builder
                .comment(
                        "When true, the Digital Stabilizer drains EU to stay pinned at max overclock while idle.",
                        "When false, it behaves like the Electronic Stabilizer while idle (frozen, no extra EU drain).")
                .define("holdWhenIdle", true);

        builder.pop();

        SPEC = builder.build();
    }

    private OverclockConfig() {
    }
}
