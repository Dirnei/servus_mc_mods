package io.github.dirnei.mc.servus.lib.team;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * The team rule shared by every Servus mod. Without a team mod each player is
 * their own team. A team mod installs its provider once during startup.
 */
public final class Teams {

    private static volatile TeamProvider provider = ServerPlayer::getUUID;

    private Teams() {
    }

    public static TeamProvider provider() {
        return provider;
    }

    public static void install(TeamProvider teamProvider) {
        provider = teamProvider;
    }

    public static UUID teamOf(ServerPlayer player) {
        return provider.teamOf(player);
    }
}
