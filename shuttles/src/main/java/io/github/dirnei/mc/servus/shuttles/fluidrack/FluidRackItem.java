package io.github.dirnei.mc.servus.shuttles.fluidrack;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import io.github.dirnei.mc.servus.shuttles.rack.RackAssist;
import io.github.dirnei.mc.servus.shuttles.rack.RackLayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The Fluid Rack item; its tooltip names the planks it was crafted from and the fluid it carries.
 * An empty item places a layer on a top click; a sneak click extends the row.
 */
public class FluidRackItem extends AssistedBlockItem {

    public FluidRackItem(Block block, Properties properties) {
        super(block, RackAssist.FLUID, properties);
    }

    /**
     * A layer over the clicked section first when the item carries no fluid
     * (see {@link RackLayer}), else one rack, sneak clicks through the row assist.
     */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide() && FluidRackBlockEntity.fluidOf(context.getItemInHand()).isEmpty()
                && (RackLayer.place(context, getBlock().defaultBlockState(), FluidRackBlock::section, this::placeWithoutAssist)
                || RackLayer.placeUpColumn(context, placementAssist(), getBlock().defaultBlockState(), FluidRackBlock::section,
                this::placeWithoutAssist))) {
            return InteractionResult.CONSUME;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        Block planks = BuiltInRegistries.BLOCK.getValue(FluidRackBlockEntity.woodOf(stack));
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.heavy_rack.wood", planks.getName())
                .withStyle(ChatFormatting.GRAY));
        FluidRackBlockEntity.fluidOf(stack).ifPresent(fluid -> tooltip.accept(
                Component.translatable("tooltip.servus_shuttles.fluid_rack.fluid", fluid.getHoverName(), fluid.getAmount())
                        .withStyle(ChatFormatting.GRAY)));
    }
}
