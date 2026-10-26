package io.github.dirnei.mc.servus.foundation.naming;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/** A machine GUI whose title row renames the machine it was opened on. */
public interface MachineNameMenu {

    /** The named machine the GUI shows, on both sides; empty for a machine without a name. */
    Optional<NamedMachine> namedMachine();

    boolean stillValid(Player player);

    /** Server side: the name typed in the GUI, cleaned by the machine; ignored once the player may no longer use the GUI. */
    default void renameMachine(Player player, String typed) {
        if (stillValid(player)) {
            namedMachine().ifPresent(machine -> machine.rename(typed));
        }
    }
}
