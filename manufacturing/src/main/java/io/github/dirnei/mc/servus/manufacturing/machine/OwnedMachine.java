package io.github.dirnei.mc.servus.manufacturing.machine;

import java.util.Optional;
import java.util.UUID;

/** A machine that remembers who placed it, for team bound behavior. */
public interface OwnedMachine {

    Optional<UUID> owner();

    void setOwner(UUID owner);
}
