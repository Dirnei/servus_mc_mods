package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** A garage of the fleet: its position, home rail and shuttles, sorted by slot. */
final class Garage {

    final BlockPos pos;
    NodePos home;
    boolean active = true;
    final List<Shuttle> shuttles = new ArrayList<>();

    Garage(BlockPos pos) {
        this.pos = pos;
    }

    Shuttle inSlot(int slot) {
        for (Shuttle shuttle : shuttles) {
            if (shuttle.slot == slot) {
                return shuttle;
            }
        }
        return null;
    }

    boolean fits(JobNeeds needs) {
        for (Shuttle shuttle : shuttles) {
            if (needs.fits(shuttle.profile)) {
                return true;
            }
        }
        return false;
    }
}
