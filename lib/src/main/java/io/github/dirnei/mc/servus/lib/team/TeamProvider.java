package io.github.dirnei.mc.servus.lib.team;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Decides which team a player belongs to. */
@FunctionalInterface
public interface TeamProvider {

    UUID teamOf(ServerPlayer player);
}
