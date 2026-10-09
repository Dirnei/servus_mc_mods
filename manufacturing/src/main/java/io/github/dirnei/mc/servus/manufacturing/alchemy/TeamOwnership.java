package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The team an alchemy block works for: journal progress is counted for it and its chapters gate
 * what the block may do. Held by each alchemy block entity, saved under {@code Team}.
 */
public final class TeamOwnership {

    private static final String TAG = "Team";

    private final Runnable changed;
    private UUID team;

    public TeamOwnership(Runnable changed) {
        this.changed = changed;
    }

    /** The owning team, or {@code null} for a block nobody placed. */
    public UUID team() {
        return team;
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(team);
    }

    /** Takes the placing player's id and stores their team. */
    public void setOwner(Level level, UUID player) {
        team = resolveTeam(level, player);
        changed.run();
    }

    /** Sets the owning team directly, for tests and commands. */
    public void setTeam(UUID team) {
        this.team = team;
        changed.run();
    }

    /** The team of a player who is online, else the player's own id. */
    public static UUID resolveTeam(Level level, UUID player) {
        if (level instanceof ServerLevel server) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(player);
            if (online != null) {
                return Teams.teamOf(online);
            }
        }
        return player;
    }

    public void save(CompoundTag tag) {
        if (team != null) {
            tag.putUUID(TAG, team);
        }
    }

    public void load(CompoundTag tag) {
        team = tag.hasUUID(TAG) ? tag.getUUID(TAG) : null;
    }
}
