package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import java.util.Optional;
import java.util.UUID;

/** An alchemy block entity that works for the team held in its {@link TeamOwnership}. */
public interface TeamOwned extends OwnedMachine {

    TeamOwnership ownership();

    @Override
    default Optional<UUID> owner() {
        return ownership().owner();
    }

    @Override
    default void setOwner(UUID player) {
        ownership().setOwner(player);
    }

    /** Sets the owning team directly, for tests and commands. */
    default void setTeam(UUID team) {
        ownership().setTeam(team);
    }

    /** The owning team, or {@code null} for a block nobody placed. */
    default UUID team() {
        return ownership().team();
    }
}
