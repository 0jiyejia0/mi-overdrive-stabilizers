package com.example.mi_overclock_addon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An overclock module item. The detailed tooltip is hidden behind the same Shift prompt used by
 * Modern Industrialization's own item tooltips.
 */
public class OverclockModuleItem extends Item {
    private static final String SHIFT_TOOLTIP_KEY = "text.modern_industrialization.TooltipsShiftRequired";

    private final int tooltipLineCount;

    public OverclockModuleItem(Properties properties, int tooltipLineCount) {
        super(properties);
        this.tooltipLineCount = tooltipLineCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String base = getDescriptionId(stack);

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(SHIFT_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }

        for (int line = 0; line < tooltipLineCount; line++) {
            tooltip.add(Component.translatable(base + ".line_" + line).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(base + ".flavor").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
