package com.example.mi_overclock_addon.registry;

import com.example.mi_overclock_addon.MIOverclockAddon;
import com.example.mi_overclock_addon.item.OverclockModuleItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MIOverclockAddon.MOD_ID);

    public static final DeferredItem<OverclockModuleItem> OVERCLOCK_MEMORY_MODULE = ITEMS.registerItem(
            "overclock_memory_module",
            properties -> new OverclockModuleItem(properties, 2),
            new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));

    public static final DeferredItem<OverclockModuleItem> PERSISTENT_OVERCLOCK_MODULE = ITEMS.registerItem(
            "persistent_overclock_module",
            properties -> new OverclockModuleItem(properties, 2),
            new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<OverclockModuleItem> DIGITAL_OVERCLOCK_STABILIZER = ITEMS.registerItem(
            "quantum_overclock_controller",
            properties -> new OverclockModuleItem(properties, 2),
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
