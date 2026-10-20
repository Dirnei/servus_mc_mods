package io.github.dirnei.mc.servus.shuttles.shuttle;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a client needs to draw one shuttle: its path, where on it the shuttle
 * is, how fast it moves and what it carries. A parked shuttle has a one node
 * path. While the shuttle rides a lift car, lift holds that shaft's lowest
 * segment and the shuttle is drawn on the car. The shuttle item tells its
 * tier, upgrades and tool; a tanker carrying fluid shows it in its sight glass.
 */
public record ShuttleView(List<BlockPos> path, int segment, float progress, Drive drive, ItemStack cargo, Optional<BlockPos> lift,
                          ItemStack shuttle, Optional<SightGlass> glass) {

    /** The fluid a tanker carries and how full it is, from 0 to 1. */
    public record SightGlass(FluidStack fluid, float share) {

        public static final StreamCodec<RegistryFriendlyByteBuf, SightGlass> STREAM_CODEC = StreamCodec.composite(
                FluidStack.STREAM_CODEC, SightGlass::fluid,
                ByteBufCodecs.FLOAT, SightGlass::share,
                SightGlass::new);
    }

    /**
     * How the shuttle drives on from here: its speed now, its top speed (0
     * while it waits, so it stands still), the speed it gains per tick and
     * its corner speed (0 for no corner limit).
     */
    public record Drive(float speed, float top, float accel, float corner) {

        public static final Drive STILL = new Drive(0, 0, 0, 0);

        public static final StreamCodec<RegistryFriendlyByteBuf, Drive> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Drive::speed,
                ByteBufCodecs.FLOAT, Drive::top,
                ByteBufCodecs.FLOAT, Drive::accel,
                ByteBufCodecs.FLOAT, Drive::corner,
                Drive::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleView> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ShuttleView::path,
            ByteBufCodecs.VAR_INT, ShuttleView::segment,
            ByteBufCodecs.FLOAT, ShuttleView::progress,
            Drive.STREAM_CODEC, ShuttleView::drive,
            ItemStack.OPTIONAL_STREAM_CODEC, ShuttleView::cargo,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ShuttleView::lift,
            ItemStack.OPTIONAL_STREAM_CODEC, ShuttleView::shuttle,
            ByteBufCodecs.optional(SightGlass.STREAM_CODEC), ShuttleView::glass,
            ShuttleView::new);

    public boolean moving() {
        return path.size() > 1 && segment < path.size() - 1;
    }
}
