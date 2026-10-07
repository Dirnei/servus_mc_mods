package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.shuttle.LiftSyncPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client side: the last known lift car per shaft bottom and when it arrived. */
public final class ClientLifts {

    /** @param lift the car as sent; @param receivedTick client game time at receipt */
    public record Lift(LiftSyncPayload lift, long receivedTick) {

        /** The car's y at the given client time, moved towards its target since receipt. */
        public double yAt(double time) {
            double elapsed = Math.max(0, time - receivedTick);
            double y = lift.y();
            if (lift.speed() <= 0 || lift.target() == y) {
                return y;
            }
            double step = lift.speed() * elapsed;
            return lift.target() > y ? Math.min(lift.target(), y + step) : Math.max(lift.target(), y - step);
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
