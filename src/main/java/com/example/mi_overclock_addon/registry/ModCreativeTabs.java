package com.example.mi_overclock_addon.registry;

import com.example.mi_overclock_addon.MIOverclockAddon;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            MIOverclockAddon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mi_overclock_addon"))
                    .icon(() -> new ItemStack(ModItems.QUANTUM_OVERCLOCK_CONTROLLER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OVERCLOCK_MEMORY_MODULE.get());
                        output.accept(ModItems.PERSISTENT_OVERCLOCK_MODULE.get());
                        output.accept(ModItems.QUANTUM_OVERCLOCK_CONTROLLER.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
