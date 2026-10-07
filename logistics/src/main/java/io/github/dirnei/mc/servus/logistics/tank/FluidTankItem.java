package io.github.dirnei.mc.servus.logistics.tank;

import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * The Fluid Tank as an item. Carries the fluid of the block it was broken
 * from and refuses to be placed where it would join tanks holding different
 * fluids.
 */
public class FluidTankItem extends BlockItem {

    public FluidTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The fluid the item carries, empty when none. */
    public static FluidStack fluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(ServusLogisticsComponents.FLUID.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && mixesFluids(level, context.getClickedPos(), fluid(context.getItemInHand()))) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.servus_logistics.fluid_tank.mixed")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    /**
     * True when the item's fluid and the fluids of the tanks touching
     * {@code pos} are more than one distinct fluid.
     */
    public static boolean mixesFluids(Level level, BlockPos pos, FluidStack carried) {
        List<FluidStack> fluids = new ArrayList<>();
        add(fluids, carried);
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof FluidTankBlockEntity tank) {
                add(fluids, tank.network().map(TankNetwork::groupFluid).orElseGet(tank::share));
            }
        }
        return fluids.size() > 1;
    }

    private static void add(List<FluidStack> fluids, FluidStack fluid) {
        if (fluid.isEmpty()) {
            return;
        }
        for (FluidStack known : fluids) {
            if (FluidStack.isSameFluidSameComponents(known, fluid)) {
                return;
            }
        }
        fluids.add(fluid);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluidStack fluid = fluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.servus_logistics.fluid_tank.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.servus_logistics.fluid_tank.fluid", fluid.getHoverName(), fluid.getAmount())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
