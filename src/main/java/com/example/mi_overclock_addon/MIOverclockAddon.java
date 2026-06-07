package com.example.mi_overclock_addon;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.example.mi_overclock_addon.config.OverclockConfig;
import com.example.mi_overclock_addon.registry.ModCreativeTabs;
import com.example.mi_overclock_addon.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MIOverclockAddon.MOD_ID)
public final class MIOverclockAddon {
    public static final String MOD_ID = "mi_overclock_addon";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MIOverclockAddon(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, OverclockConfig.SPEC);
        LOGGER.info("Loaded MI Overdrive Stabilizers");
    }
}
