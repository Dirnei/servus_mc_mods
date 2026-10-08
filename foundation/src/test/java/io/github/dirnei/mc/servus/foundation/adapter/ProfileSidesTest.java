package io.github.dirnei.mc.servus.foundation.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfileSidesTest {

    private static final MachineProfile FURNACE = new MachineProfile(List.of("minecraft:furnace"), "minecraft:smelting",
            Map.of(RelativeSide.TOP, MachineProfile.Role.INGREDIENTS, RelativeSide.BOTTOM, MachineProfile.Role.OUTPUT,
                    RelativeSide.LEFT, MachineProfile.Role.UPKEEP, RelativeSide.RIGHT, MachineProfile.Role.UPKEEP),
            "minecraft:coal", 16);

    @Test
    void roleToAbsoluteForEachFacing() {
        assertEquals(Optional.of(Side.UP), ProfileSides.firstOf(FURNACE, MachineProfile.Role.INGREDIENTS, Side.NORTH));
        assertEquals(Optional.of(Side.DOWN), ProfileSides.firstOf(FURNACE, MachineProfile.Role.OUTPUT, Side.EAST));
        assertEquals(Optional.of(Side.EAST), ProfileSides.firstOf(FURNACE, MachineProfile.Role.UPKEEP, Side.NORTH));
        assertEquals(Optional.of(Side.SOUTH), ProfileSides.firstOf(FURNACE, MachineProfile.Role.UPKEEP, Side.EAST));
        assertEquals(Optional.of(Side.WEST), ProfileSides.firstOf(FURNACE, MachineProfile.Role.UPKEEP, Side.SOUTH));
    }

    @Test
    void firstSideWinsInFixedOrder() {
        MachineProfile backAndLeft = new MachineProfile(List.of("x:y"), "x:z",
                Map.of(RelativeSide.LEFT, MachineProfile.Role.UPKEEP, RelativeSide.BACK, MachineProfile.Role.UPKEEP), "", 16);
        assertEquals(Optional.of(Side.SOUTH), ProfileSides.firstOf(backAndLeft, MachineProfile.Role.UPKEEP, Side.NORTH));
    }

    @Test
    void missingRoleIsEmpty() {
        MachineProfile none = new MachineProfile(List.of("x:y"), "x:z", Map.of(), "", 16);
        assertTrue(ProfileSides.firstOf(none, MachineProfile.Role.INGREDIENTS, Side.NORTH).isEmpty());
    }
}
