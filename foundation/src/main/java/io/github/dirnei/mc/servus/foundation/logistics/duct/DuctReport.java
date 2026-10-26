package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.data.CableTier;
import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.DuctChannelData;
import io.github.dirnei.mc.servus.foundation.logistics.network.DuctEvent;
import io.github.dirnei.mc.servus.foundation.logistics.network.EndpointTally;
import io.github.dirnei.mc.servus.foundation.logistics.network.LineNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The chat report of the Data Potato on a duct: per present line the clicked
 * side's mode, Control, channel, priority and filter entries, run size and in and out sides, or for the
 * centre whether the duct is powered and the run sizes; the data line always names tier, bandwidth and
 * load. Server thread only; counts cover the loaded part of a run.
 */
public final class DuctReport {

    private static final String KEY = "message.servus_foundation.potato.";

    private DuctReport() {
    }

    /** The report for a click at {@code location} (absolute) on {@code duct}. */
    public static List<Component> at(UtilityDuctBlockEntity duct, Vec3 location) {
        return of(duct, UtilityDuctBlock.hitArm(duct, location, duct.getBlockPos()));
    }

    /** The report of the arm on {@code side}, or of the centre when {@code side} is null. */
    public static List<Component> of(UtilityDuctBlockEntity duct, Direction side) {
        List<Component> lines = new ArrayList<>();
        lines.add(side == null
                ? Component.translatable(KEY + "duct_centre", Component.translatable(KEY + (duct.isPowered() ? "powered" : "unpowered")))
                : Component.translatable(KEY + "duct_side", Component.translatable("gui.servus_foundation.side." + side.getSerializedName())));
        for (Line line : Line.VALUES) {
            if (!duct.has(line)) {
                continue;
            }
            lines.add(line == Line.DATA ? dataLine(duct) : side == null ? runLine(duct, line) : sideLine(duct, line, side));
        }
        return lines;
    }

    private static Component runLine(UtilityDuctBlockEntity duct, Line line) {
        return Component.translatable(KEY + "run", lineName(line), runSize(duct, line));
    }

    private static Component sideLine(UtilityDuctBlockEntity duct, Line line, Direction side) {
        int channel = duct.channel(line, side);
        EndpointTally tally = EndpointTally.of(channel, duct.<LineNetwork<?>>network(line).map(LineNetwork::blockSides).orElse(List.of()));
        Component mode = Component.translatable("gui.servus_foundation.mode." + duct.mode(line, side).name().toLowerCase(Locale.ROOT));
        Component control = control(duct, line, side);
        if (line == Line.ITEM) {
            return Component.translatable(KEY + "filtered_side", lineName(line), mode, control, channelName(duct, channel), duct.priority(side),
                    itemEntries(duct, side), runSize(duct, line), tally.in(), tally.out());
        }
        if (line == Line.FLUID) {
            return Component.translatable(KEY + "filtered_side", lineName(line), mode, control, channelName(duct, channel), duct.fluidPriority(side),
                    duct.fluidFilter(side).entries().size(), runSize(duct, line), tally.in(), tally.out());
        }
        return Component.translatable(KEY + "side", lineName(line), mode, control, channelName(duct, channel), runSize(duct, line),
                tally.in(), tally.out());
    }

    private static int itemEntries(UtilityDuctBlockEntity duct, Direction side) {
        int entries = 0;
        for (int slot = 0; slot < UtilityDuctBlockEntity.FILTER_SLOTS; slot++) {
            if (!duct.filter(side).getStackInSlot(slot).isEmpty()) {
                entries++;
            }
        }
        return entries;
    }

    /** The side's Control by name, marked paused while it keeps the side from moving anything. */
    private static Component control(UtilityDuctBlockEntity duct, Line line, Direction side) {
        Component name = Component.translatable("gui.servus_foundation.control." + duct.control(line, side).key());
        return duct.isActive(line, side) ? name : Component.translatable(KEY + "paused", name);
    }

    private static Component dataLine(UtilityDuctBlockEntity duct) {
        CableTier tier = duct.dataTier();
        int load = duct.dataNetwork().map(network -> network.load(Conversions.toNodePos(duct.getBlockPos()))).orElse(0);
        return Component.translatable(KEY + "data", lineName(Line.DATA), duct.lineItem(Line.DATA).getDefaultInstance().getHoverName(),
                (int) Math.round(tier.itemsPerSecond()), load);
    }

    private static Component lineName(Line line) {
        return ServusLogisticsItems.lineItem(line).getDefaultInstance().getHoverName();
    }

    private static int runSize(UtilityDuctBlockEntity duct, Line line) {
        return duct.<ServusNetwork<UtilityDuctBlockEntity, DuctEvent>>network(line).map(ServusNetwork::size).orElse(0);
    }

    /** Default by its lang name, a team channel by its name in quotes, both in the channel's ring colour. */
    private static Component channelName(UtilityDuctBlockEntity duct, int id) {
        Channel found = duct.getLevel() instanceof ServerLevel server ? DuctChannelData.get(server.getServer()).list().get(id) : null;
        Channel channel = found == null ? Channel.DEFAULT : found;
        MutableComponent name = channel.isDefault()
                ? Component.translatable("gui.servus_foundation.channel.default")
                : Component.literal("\"" + channel.name() + "\"");
        return name.withStyle(style -> style.withColor(TextColor.fromRgb(channel.argb() & 0xFFFFFF)));
    }
}
