package io.github.dirnei.mc.servus.shuttles.fluidstation;

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
 * The item of a two blocks high fluid station. It places only when both
 * spaces are free and the block below has a solid top, and tells the player
 * why not otherwise. The placed station takes over the tanks and sides the
 * item kept as block entity data.
 */
public class FluidStationItem extends BlockItem {

    public FluidStationItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The lang key suffix of why a station cannot stand where the context places it, or null when it can. */
    static @Nullable String refusal(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos lower = context.getClickedPos();
        BlockPos upper = lower.above();
        if (!level.isInsideBuildHeight(upper) || !level.getBlockState(upper).canBeReplaced(context)) {
            return "no_room";
        }
        BlockPos below = lower.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return "no_footing";
        }
        return null;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.canPlace()) {
            String refusal = refusal(context);
            if (refusal != null) {
                Player player = context.getPlayer();
                if (!context.getLevel().isClientSide() && player != null) {
                    player.sendOverlayMessage(Component.translatable("message.servus_shuttles.fluid_station." + refusal));
                }
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }
}
