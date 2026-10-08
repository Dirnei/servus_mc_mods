package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The one carrot gesture for blocks with sides: a click cycles the clicked
 * face, sneaking dismantles and keeps the configuration in the dropped item.
 */
public final class SideConfigWrench {

    private SideConfigWrench() {
    }

    /**
     * @param dropContents what to spill before the block is saved to its item, so nothing is duplicated
     */
    public static <T extends BlockEntity & SidedBlockEntity> InteractionResult use(UseOnContext context, Block block, T blockEntity,
                                                                                Runnable dropContents) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            dropContents.run();
            ItemStack drop = new ItemStack(block);
            blockEntity.saveToItem(drop, level.registryAccess());
            Block.popResource(level, pos, drop);
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        RelativeSide side = blockEntity.relativeSide(face);
        SideMode mode = blockEntity.cycleSide(side);
        if (player != null) {
            player.displayClientMessage(message(side, mode), true);
        }
        return InteractionResult.SUCCESS;
    }

    public static Component sideName(RelativeSide side) {
        return Component.translatable("gui.servus_lib.side." + side.name().toLowerCase(Locale.ROOT));
    }

    public static Component modeName(SideMode mode) {
        return Component.translatable("gui.servus_lib.side_mode." + mode.key());
    }

    /** "Left: Input" for the action bar and the panel tooltip. */
    public static Component message(RelativeSide side, SideMode mode) {
        return Component.translatable("gui.servus_lib.side_line", sideName(side), modeName(mode));
    }
}
