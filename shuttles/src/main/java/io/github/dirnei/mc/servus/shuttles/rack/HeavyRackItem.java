package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** The rack item; its tooltip names the planks it was crafted from. A top click places a layer, a sneak click extends the row. */
public class HeavyRackItem extends AssistedBlockItem {

    public HeavyRackItem(Block block, Properties properties) {
        super(block, RackAssist.HEAVY, properties);
    }

    /** A layer over the clicked shelf, or over the column's top shelf on a sneak click up (see {@link RackLayer}), else one rack through the assist. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide()
                && (RackLayer.place(context, getBlock().defaultBlockState(), HeavyRackBlock::shelf, this::placeWithoutAssist)
                || RackLayer.placeUpColumn(context, placementAssist(), getBlock().defaultBlockState(), HeavyRackBlock::shelf,
                this::placeWithoutAssist))) {
            return InteractionResult.CONSUME;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        Block planks = BuiltInRegistries.BLOCK.getValue(HeavyRackBlockEntity.woodOf(stack));
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.heavy_rack.wood", planks.getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
