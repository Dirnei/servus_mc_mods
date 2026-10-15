package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.shuttle.LiftSyncPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleDrive;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client side: the last known lift car per shaft bottom and when it arrived. */
public final class ClientLifts {

    /** @param lift the car as sent; @param receivedTick client game time at receipt */
    public record Lift(LiftSyncPayload lift, long receivedTick) {

        /** The car's y at the given client time, moved towards its target since receipt, speeding up and braking. */
        public double yAt(double time) {
            double elapsed = Math.min(Math.max(0, time - receivedTick), 400);
            double y = lift.y();
            ShuttleView.Drive drive = lift.drive();
            if (drive.top() <= 0 || lift.target() == y) {
                return y;
            }
            double sign = Math.signum(lift.target() - y);
            double v = drive.speed();
            double moved = 0;
            double gap = Math.abs(lift.target() - y);
            int whole = (int) Math.floor(elapsed);
            for (int t = 0; t < whole && moved < gap; t++) {
                v = ShuttleDrive.next(v, drive.top(), drive.accel(), gap - moved);
                moved += v;
            }
            double part = elapsed - whole;
            if (part > 0 && moved < gap) {
                moved += ShuttleDrive.next(v, drive.top(), drive.accel(), gap - moved) * part;
            }
            return y + sign * Math.min(gap, moved);
        }
    }

    private static final Map<BlockPos, Lift> LIFTS = new HashMap<>();

    private ClientLifts() {
    }

    public static void update(LiftSyncPayload payload) {
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        LIFTS.put(payload.bottom(), new Lift(payload, now));
    }

    public static Lift lift(BlockPos bottom) {
        return LIFTS.get(bottom);
    }

    public static void forget(BlockPos bottom) {
        LIFTS.remove(bottom);
    }

    public static void clear() {
        LIFTS.clear();
    }
}
