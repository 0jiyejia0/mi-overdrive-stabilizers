package com.example.mi_overclock_addon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An overclock module item. Adds a two-line tooltip (a gray functional description and an
 * italic flavour line) built from the item's own translation key, so every module shares the
 * same rendering logic and only the language files differ.
 */
public class OverclockModuleItem extends Item {
    public OverclockModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String base = getDescriptionId(stack);
        tooltip.add(Component.translatable(base + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(base + ".flavor").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
