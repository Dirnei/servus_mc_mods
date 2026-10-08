package io.github.dirnei.mc.servus.foundation.data;

import net.minecraft.network.chat.Component;

/** The one line summary shown when a player clicks a data duct with an empty hand. */
public final class DataSummary {

    public static final String KEY = "message.servus_foundation.data_cable.summary";

    private DataSummary() {
    }

    public static Component of(DataNetwork network) {
        return Component.translatable(KEY, network.cables(),
                network.dataMembers(DataRole.PRODUCER).size(), network.dataMembers(DataRole.STOCK).size());
    }
}
