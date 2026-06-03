package com.example.mi_overclock_addon.registry;

import com.example.mi_overclock_addon.MIOverclockAddon;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MIOverclockAddon.MOD_ID);

    public static final DeferredItem<Item> OVERCLOCK_MEMORY_MODULE = ITEMS.registerSimpleItem(
            "overclock_memory_module",
            new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> PERSISTENT_OVERCLOCK_MODULE = ITEMS.registerSimpleItem(
            "persistent_overclock_module",
            new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> QUANTUM_OVERCLOCK_CONTROLLER = ITEMS.registerSimpleItem(
            "quantum_overclock_controller",
            new Item.Properties().stacksTo(16));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
