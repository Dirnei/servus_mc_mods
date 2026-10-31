package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.shuttles.rail.RailFacings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The Parcel Locker item. The clicked space becomes the dock; the locker
 * stands only when all ten spaces are free and the five blocks below its
 * lower row have a solid top, and the item tells the player why not otherwise.
 */
public class ParcelLockerItem extends BlockItem {

    public ParcelLockerItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * The lang key suffix of why a locker whose front faces {@code facing}
     * cannot stand with its dock where the context places, or null when it can.
     */
    static @Nullable String refusal(BlockPlaceContext context, Direction facing) {
        Level level = context.getLevel();
        BlockPos dock = context.getClickedPos();
        for (LockerPart part : LockerPart.values()) {
            BlockPos pos = part.at(dock, facing);
            if (!level.isInsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced(context)) {
                return "no_room";
            }
        }
        for (LockerPart part : LockerPart.values()) {
            BlockPos below = part.at(dock, facing).below();
            if (part.row() == 0 && !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return "no_footing";
            }
        }
        return null;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.canPlace()) {
            String refusal = refusal(context, RailFacings.frontToRail(context, RailFacings.ANY_RAIL));
            if (refusal != null) {
                Player player = context.getPlayer();
                if (!context.getLevel().isClientSide() && player != null) {
                    player.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker." + refusal));
                }
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }
}
