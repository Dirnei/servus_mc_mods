package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleDrive;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Where a synced shuttle is now, extrapolated from its view the way the server drives it. */
final class ShuttleGlide {

    private ShuttleGlide() {
    }

    /** Where on its path the shuttle is after driving the elapsed ticks since the sync: {segment, progress, speed}. */
    static double[] at(ShuttleView view, double elapsed) {
        List<BlockPos> path = view.path();
        ShuttleView.Drive drive = view.drive();
        double leadAt = view.lead().map(lead -> (double) lead.at()).orElse(Double.POSITIVE_INFINITY);
        double leadSpeed = view.lead().map(lead -> (double) lead.speed()).orElse(0.0);
        return ShuttleDrive.glide(path.size(), view.segment(), view.progress(), drive.speed(), drive.top(), drive.accel(), elapsed,
                i -> !horizontal(path, i), i -> corner(path, i), drive.corner(), leadAt, leadSpeed);
    }

    private static boolean corner(List<BlockPos> path, int i) {
        if (i <= 0 || i >= path.size() - 1) {
            return false;
        }
        BlockPos a = path.get(i - 1);
        BlockPos b = path.get(i);
        BlockPos c = path.get(i + 1);
        return ShuttleDrive.corner(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), c.getX(), c.getY(), c.getZ());
    }

    static boolean horizontal(List<BlockPos> path, int segment) {
        BlockPos from = path.get(segment);
        BlockPos to = path.get(segment + 1);
        return from.getX() != to.getX() || from.getZ() != to.getZ();
    }
}
