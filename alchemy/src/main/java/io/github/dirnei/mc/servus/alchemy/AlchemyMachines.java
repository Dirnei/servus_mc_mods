package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;

/** What the alchemy machines share: the synced data slots of their principle tanks and their closed sides. */
public final class AlchemyMachines {

    /** Container data after the machine's own: Salt, Sulfur, Mercury amounts, then the tank capacity. */
    public static final int DATA_PRINCIPLE_FIRST = MachineBlockEntity.DATA_COUNT;
    public static final int DATA_TANK_CAPACITY = DATA_PRINCIPLE_FIRST + 3;
    public static final int DATA_COUNT = DATA_PRINCIPLE_FIRST + 4;

    private AlchemyMachines() {
    }

    /** The data value at {@code index} if it is one of the tank entries, else -1. */
    public static int tankData(PrincipleTanks tanks, int index) {
        if (index >= DATA_PRINCIPLE_FIRST && index < DATA_TANK_CAPACITY) {
            return tanks.amount(Principle.values()[index - DATA_PRINCIPLE_FIRST]);
        }
        if (index == DATA_TANK_CAPACITY) {
            return tanks.capacity();
        }
        return -1;
    }

    /** Locks every side off, so items move only through the build's own ports. */
    public static void closeAllSides(MachineBlockEntity machine) {
        for (RelativeSide side : RelativeSide.values()) {
            machine.sideConfig().lock(side, SideMode.DISABLED);
        }
    }
}
