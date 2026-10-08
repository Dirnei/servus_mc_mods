package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * The Fluid Tank as an item. Carries the fluid of the block it was broken
 * from and refuses to be placed where it would join tanks holding different
 * fluids. On top of a tank it can place a whole layer at once.
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
        if (!context.getLevel().isClientSide() && placeLayer(context)) {
            return InteractionResult.CONSUME;
        }
        return placeOne(context);
    }

    private InteractionResult placeOne(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && mixesFluids(level, context.getClickedPos(), fluid(context.getItemInHand()))) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.servus_foundation.fluid_tank.mixed")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    /**
     * A click on the top of a tank's top layer places a whole new layer over
     * the tank's footprint when the player does not sneak, the held stack is
     * empty tank items, holds enough of them (or the player is in creative)
     * and every spot is free. Server side only; the client predicts one block.
     */
    private boolean placeLayer(BlockPlaceContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSecondaryUseActive() || context.getClickedFace() != Direction.UP
                || !fluid(stack).isEmpty()) {
            return false;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos().below();
        if (!(level.getBlockEntity(clicked) instanceof FluidTankBlockEntity tank)) {
            return false;
        }
        TankNetwork network = tank.network().orElse(null);
        if (network == null) {
            return false;
        }
        TankShape box = network.boxAt(Conversions.toNodePos(clicked));
        if (box.blocksPerLayer() < 2 || clicked.getY() != box.maxY()) {
            return false;
        }
        int needed = box.blocksPerLayer();
        if (!player.getAbilities().instabuild && stack.getCount() < needed) {
            return false;
        }
        BlockState state = getBlock().defaultBlockState();
        List<BlockPlaceContext> spots = new ArrayList<>(needed);
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, box.maxY() + 1, z);
                BlockPlaceContext spot = BlockPlaceContext.at(context, pos, Direction.UP);
                // A taken spot moves the context to the block above it, so the spot must stay where it was asked.
                if (!spot.getClickedPos().equals(pos) || !spot.canPlace() || !level.isUnobstructed(state, pos, CollisionContext.of(player))
                        || mixesFluids(level, pos, FluidStack.EMPTY)) {
                    return false;
                }
                spots.add(spot);
            }
        }
        for (BlockPlaceContext spot : spots) {
            super.place(spot);
        }
        return true;
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
            tooltip.add(Component.translatable("tooltip.servus_foundation.fluid_tank.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.servus_foundation.fluid_tank.fluid", fluid.getHoverName(), fluid.getAmount())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
