package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The elevator item. Like scaffolding, a plain click on any face of a segment
 * places the new segment on top of the shaft; a sneak click keeps the
 * placement assist, which also builds up. Used on a Warehouse Rail, it puts the
 * segment in the rail's place and the segment remembers the rail.
 */
public class ElevatorItem extends AssistedBlockItem {

    public ElevatorItem(Block block, Properties properties) {
        super(block, ElevatorAssist.INSTANCE, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ServusShuttlesBlocks.RAIL.get())) {
            return super.useOn(context);
        }
        BlockState segment = Block.updateFromNeighbourShapes(getBlock().defaultBlockState(), level, pos);
        if (!segment.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Identifier wood = level.getBlockEntity(pos) instanceof RailBlockEntity rail ? rail.woodSlot().raw() : null;
        level.setBlock(pos, segment, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ElevatorBlockEntity elevator) {
            elevator.rememberRail(wood);
            elevator.woodSlot().set(wood);
        }
        SoundType sound = segment.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.servus_foundation.wood", Wood.block(Wood.of(stack)).getName())
                .withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (context.isSecondaryUseActive()) {
            return context;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        if (!(level.getBlockState(clicked).getBlock() instanceof ElevatorBlock)) {
            return context;
        }
        OptionalInt top = ShaftTop.firstFree(clicked.getY(), level.getMaxY() + 1,
                ElevatorBlock.isSegment(level, clicked));
        if (top.isEmpty()) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("build.tooHigh", level.getMaxY())
                        .withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        BlockPos free = clicked.atY(top.getAsInt());
        BlockPlaceContext above = BlockPlaceContext.at(context, free, Direction.UP);
        return level.getBlockState(free).canBeReplaced(above) ? above : null;
    }
}
