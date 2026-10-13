package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Looks at the data line in GameTests. Positions are relative to the test. */
final class DataLines {

    private DataLines() {
    }

    /** The data line of the block at {@code pos}; fails when there is none. */
    static DataNetwork at(GameTestHelper helper, BlockPos pos) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(pos)).orElseThrow();
    }

    /** Whether the data line of the block at {@code pos} has {@code producers} producers and one CPU, so orders can be planned. */
    static boolean ready(GameTestHelper helper, BlockPos pos, int producers) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(pos))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == producers
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }
}
