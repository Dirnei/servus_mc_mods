package io.github.dirnei.mc.servus.shuttles.gantry;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Gantry item. Used on a straight Warehouse Rail it builds the five
 * parts around it when they all have room and both legs stand on a solid
 * top, and hands the master what the item kept. Anywhere else, or when a
 * check fails, nothing is placed and the player is told why.
 */
public class FluidGantryItem extends BlockItem {

    public FluidGantryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos rail = context.getClickedPos();
        BlockState railState = level.getBlockState(rail);
        Player player = context.getPlayer();
        if (!railState.is(ServusShuttlesBlocks.RAIL.get())) {
            return refuse(level, player, "needs_rail");
        }
        Direction.Axis axis = runAxis(railState, context.getHorizontalDirection().getAxis());
        if (axis == null) {
            return refuse(level, player, "needs_straight_rail");
        }
        Map<GantryLayout.Part, GantryLayout.Vec> parts = FluidGantryBlock.parts(rail, axis);
        BlockPlaceContext placing = new BlockPlaceContext(context);
        for (GantryLayout.Vec vec : parts.values()) {
            BlockPos pos = FluidGantryBlock.pos(vec);
            if (!level.isInsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced(placing)) {
                return refuse(level, player, "no_room");
            }
        }
        for (GantryLayout.Part leg : new GantryLayout.Part[] {GantryLayout.Part.LEFT_LOW, GantryLayout.Part.RIGHT_LOW}) {
            BlockPos below = FluidGantryBlock.pos(parts.get(leg)).below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return refuse(level, player, "no_footing");
            }
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        place(level, parts, axis, stack.getOrDefault(ServusShuttlesComponents.GANTRY_CONTENTS.get(), GantryContents.EMPTY));
        SoundType sound = getBlock().defaultBlockState().getSoundType();
        level.playSound(null, rail, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /** Places the master first, then the other parts, and hands the master the item's contents. */
    private void place(Level level, Map<GantryLayout.Part, GantryLayout.Vec> parts, Direction.Axis axis, GantryContents contents) {
        BlockState base = getBlock().defaultBlockState().setValue(FluidGantryBlock.AXIS, axis).setValue(FluidGantryBlock.MODE, contents.mode());
        BlockPos master = FluidGantryBlock.pos(parts.get(GantryLayout.Part.LEFT_LOW));
        level.setBlock(master, base.setValue(FluidGantryBlock.PART, GantryPart.LEFT_LOW), Block.UPDATE_ALL);
        if (level.getBlockEntity(master) instanceof FluidGantryBlockEntity gantry) {
            gantry.applyContents(contents);
        }
        for (Map.Entry<GantryLayout.Part, GantryLayout.Vec> entry : parts.entrySet()) {
            if (entry.getKey() != GantryLayout.Part.LEFT_LOW) {
                level.setBlock(FluidGantryBlock.pos(entry.getValue()), base.setValue(FluidGantryBlock.PART, GantryPart.of(entry.getKey())), Block.UPDATE_ALL);
            }
        }
        FluidGantryBlock.invalidateCapabilities(level, master, level.getBlockState(master));
    }

    /**
     * The axis a rail runs along, from the sides it links to: one axis only, or
     * {@code fallback} for a rail linked to nothing. Null for a curve, a cross
     * or a rail with a dock across its run.
     */
    static Direction.@Nullable Axis runAxis(BlockState rail, Direction.Axis fallback) {
        boolean alongZ = rail.getValue(ConnectableBlock.NORTH) || rail.getValue(ConnectableBlock.SOUTH);
        boolean alongX = rail.getValue(ConnectableBlock.EAST) || rail.getValue(ConnectableBlock.WEST);
        if (alongX && alongZ) {
            return null;
        }
        if (alongX) {
            return Direction.Axis.X;
        }
        return alongZ ? Direction.Axis.Z : fallback;
    }

    private static InteractionResult refuse(Level level, @Nullable Player player, String reason) {
        if (!level.isClientSide() && player != null) {
            player.sendOverlayMessage(Component.translatable("message.servus_shuttles.fluid_gantry." + reason));
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        GantryContents contents = stack.get(ServusShuttlesComponents.GANTRY_CONTENTS.get());
        if (contents == null) {
            return;
        }
        for (int tank = 0; tank < 2; tank++) {
            FluidStack fluid = contents.tank(tank);
            Component what = fluid.isEmpty() ? Component.translatable("tooltip.servus_shuttles.fluid_gantry.empty")
                    : Component.translatable("tooltip.servus_shuttles.fluid_gantry.amount", fluid.getHoverName(), fluid.getAmount());
            tooltip.accept(Component.translatable("tooltip.servus_shuttles.fluid_gantry.tank_" + tank, what).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.servus_shuttles.fluid_gantry.mode",
                Component.translatable("gui.servus_shuttles.fluid_gantry.mode." + contents.mode().getSerializedName())).withStyle(ChatFormatting.GRAY));
    }
}
