package io.github.dirnei.mc.servus.foundation.logistics.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.logistics.network.EndpointTally.Side;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndpointTallyTest {

    private static final int IRON = 3;
    private static final int GOLD = 4;

    @Test
    void inAndOutCountsBoth() {
        assertEquals(new EndpointTally(2, 2), EndpointTally.of(IRON,
                List.of(new Side(IRON, SideMode.BOTH), new Side(IRON, SideMode.BOTH))));
    }

    @Test
    void pullInCountsIn() {
        assertEquals(new EndpointTally(1, 0), EndpointTally.of(IRON, List.of(new Side(IRON, SideMode.INPUT))));
    }

    @Test
    void pushOutCountsOut() {
        assertEquals(new EndpointTally(0, 1), EndpointTally.of(IRON, List.of(new Side(IRON, SideMode.OUTPUT))));
    }

    @Test
    void disabledIgnored() {
        assertEquals(new EndpointTally(0, 0), EndpointTally.of(IRON, List.of(new Side(IRON, SideMode.DISABLED))));
    }

    @Test
    void otherChannelIgnored() {
        List<Side> sides = List.of(new Side(GOLD, SideMode.BOTH), new Side(GOLD, SideMode.INPUT),
                new Side(IRON, SideMode.INPUT), new Side(IRON, SideMode.OUTPUT), new Side(IRON, SideMode.OUTPUT));
        assertEquals(new EndpointTally(1, 2), EndpointTally.of(IRON, sides));
        assertEquals(new EndpointTally(2, 1), EndpointTally.of(GOLD, sides));
    }
}
