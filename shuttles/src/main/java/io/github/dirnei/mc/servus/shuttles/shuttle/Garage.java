package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * A garage of the fleet: its position, home rail and shuttles, sorted by
 * slot. Shuttles inside stand in the garage's own cell and reach the home
 * rail through the doorway.
 */
final class Garage {

    final BlockPos pos;
    /** The garage's own cell, where its shuttles stand while they are inside. */
    final NodePos inside;
    NodePos home;
    boolean active = true;
    /** Whether the garage holds a Muffler, sent to clients with its shuttles. */
    boolean muffled;
    final List<Shuttle> shuttles = new ArrayList<>();

    Garage(BlockPos pos) {
        this.pos = pos;
        this.inside = Conversions.toNodePos(pos);
    }

    /** A shuttle standing inside, or null while the garage has no home rail and its shuttles cannot leave. */
    ShuttleMotion parked() {
        return home == null ? null : ShuttleMotion.parked(inside);
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
