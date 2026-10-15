package io.github.dirnei.mc.servus.shuttles.menu;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends a menu's content payload to its player: every few broadcasts, and
 * only when its signature changed since the last send.
 */
final class PayloadSync<P extends CustomPacketPayload> {

    private final int interval;
    private final Supplier<P> payload;
    private final ToLongFunction<P> signature;
    private int timer;
    /** The signature last sent; null before the first send or after {@link #force()}. */
    private Long lastSignature;

    /** Checks every {@code interval} broadcasts; {@code immediate} checks on the first one already. */
    PayloadSync(int interval, boolean immediate, Supplier<P> payload, ToLongFunction<P> signature) {
        this.interval = interval;
        this.payload = payload;
        this.signature = signature;
        this.timer = immediate ? interval : 0;
    }

    /** Uses the payload's hash code as its signature. */
    PayloadSync(int interval, boolean immediate, Supplier<P> payload) {
        this(interval, immediate, payload, P::hashCode);
    }

    /** Call from {@code broadcastChanges}; does nothing on the client. */
    void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (++timer < interval) {
            return;
        }
        timer = 0;
        P current = payload.get();
        long now = signature.applyAsLong(current);
        if (lastSignature == null || now != lastSignature) {
            lastSignature = now;
            PacketDistributor.sendToPlayer(serverPlayer, current);
        }
    }

    /** Sends the payload at once, changed or not, and starts the interval over. */
    void sendNow(ServerPlayer player) {
        P current = payload.get();
        lastSignature = signature.applyAsLong(current);
        timer = 0;
        PacketDistributor.sendToPlayer(player, current);
    }

    /** Makes the next broadcast check for a change. */
    void soon() {
        timer = interval;
    }

    /** Makes the next broadcast send, changed or not. */
    void force() {
        lastSignature = null;
        soon();
    }
}
