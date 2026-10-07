package io.github.dirnei.mc.servus.storage.shuttle;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What a client needs to draw one shuttle: its path, where on it the shuttle
 * is, how fast it moves and what it carries. A parked shuttle has a one node
 * path. While the shuttle rides a lift car, lift holds that shaft's lowest
 * segment and the shuttle is drawn on the car.
 */
public record ShuttleView(List<BlockPos> path, int segment, float progress, float speed, ItemStack cargo, Optional<BlockPos> lift) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleView> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ShuttleView::path,
            ByteBufCodecs.VAR_INT, ShuttleView::segment,
            ByteBufCodecs.FLOAT, ShuttleView::progress,
            ByteBufCodecs.FLOAT, ShuttleView::speed,
            ItemStack.OPTIONAL_STREAM_CODEC, ShuttleView::cargo,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ShuttleView::lift,
            ShuttleView::new);

    public boolean moving() {
        return path.size() > 1 && segment < path.size() - 1;
    }
}
