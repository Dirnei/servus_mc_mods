package io.github.dirnei.mc.servus.foundation.material;

import io.github.dirnei.mc.servus.foundation.block.TooltipBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a material block; its tooltip names the material it was crafted from. */
public class MaterialBlockItem extends BlockItem {

    public MaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof TooltipBlock block) {
            block.appendTooltip(stack, context, display, tooltip, flag);
        }
        tooltip.accept(line(stack, Material.kindOf(stack)));
    }

    /** The gray tooltip line naming the material a stack carries, after the kind's name. */
    public static Component line(ItemStack stack, MaterialKind kind) {
        Identifier material = Material.or(stack.get(MaterialContent.MATERIAL.get()), kind);
        return Component.translatable(kind.tooltipKey(), Material.block(material, kind).getName()).withStyle(ChatFormatting.GRAY);
    }
}
