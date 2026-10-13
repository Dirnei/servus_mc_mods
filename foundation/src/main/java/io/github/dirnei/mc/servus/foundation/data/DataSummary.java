package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.network.Conversions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** The one line summary shown when a player clicks a data duct with an empty hand. */
public final class DataSummary {

    /** The summary of a network: cables, producers and stock members. */
    public static final String KEY = "message.servus_foundation.data_cable.summary";

    /** The summary with how many items per second pass the clicked duct out of its bandwidth. */
    public static final String LOAD_KEY = "message.servus_foundation.data_cable.summary_load";

    private DataSummary() {
    }

    /** The summary of a duct: the network's counts plus "N of M items/s" for that duct over the last second. */
    public static Component of(DataNetwork network, BlockPos duct, CableTier tier) {
        int bandwidth = (int) Math.round(tier.itemsPerSecond());
        return Component.translatable(LOAD_KEY, network.cables(), network.dataMembers(DataRole.PRODUCER).size(),
                network.dataMembers(DataRole.STOCK).size(), network.load(Conversions.toNodePos(duct)), bandwidth);
    }

    /** The summary of a network without a duct's load. */
    public static Component of(DataNetwork network) {
        return Component.translatable(KEY, network.cables(),
                network.dataMembers(DataRole.PRODUCER).size(), network.dataMembers(DataRole.STOCK).size());
    }
}
