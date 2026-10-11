package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.foundation.placement.AssistedBlockItem;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The elevator item. Like scaffolding, a plain click on any face of a segment
 * places the new segment on top of the shaft; a sneak click keeps the
 * placement assist, which also builds up.
 */
public class ElevatorItem extends AssistedBlockItem {

    public ElevatorItem(Block block, Properties properties) {
        super(block, ElevatorAssist.INSTANCE, properties);
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
                y -> level.getBlockState(clicked.atY(y)).getBlock() instanceof ElevatorBlock);
        if (top.isEmpty()) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("build.tooHigh", level.getMaxY() + 1 - 1)
                        .withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        BlockPos free = clicked.atY(top.getAsInt());
        BlockPlaceContext above = BlockPlaceContext.at(context, free, Direction.UP);
        return level.getBlockState(free).canBeReplaced(above) ? above : null;
    }
}
