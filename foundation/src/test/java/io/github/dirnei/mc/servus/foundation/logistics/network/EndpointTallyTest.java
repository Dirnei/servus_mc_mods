package io.github.dirnei.mc.servus.foundation.logistics.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.network.EndpointTally.Side;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndpointTallyTest {

    private static final int IRON = 3;
    private static final int GOLD = 4;

    @Test
    void inAndOutCountsBoth() {
        assertEquals(new EndpointTally(2, 2), EndpointTally.of(IRON,
                List.of(new Side(IRON, DuctMode.IN_OUT), new Side(IRON, DuctMode.IN_OUT))));
    }

    @Test
    void inCountsIn() {
        assertEquals(new EndpointTally(1, 0), EndpointTally.of(IRON, List.of(new Side(IRON, DuctMode.IN))));
        assertEquals(new EndpointTally(1, 0), EndpointTally.of(IRON, List.of(new Side(IRON, DuctMode.IN_PULL))));
    }

    @Test
    void outCountsOut() {
        assertEquals(new EndpointTally(0, 1), EndpointTally.of(IRON, List.of(new Side(IRON, DuctMode.OUT))));
    }

    @Test
    void unconnectedIgnored() {
        assertEquals(new EndpointTally(0, 0), EndpointTally.of(IRON, List.of(new Side(IRON, DuctMode.NONE))));
    }

    @Test
    void otherChannelIgnored() {
        List<Side> sides = List.of(new Side(GOLD, DuctMode.IN_OUT), new Side(GOLD, DuctMode.IN_PULL),
                new Side(IRON, DuctMode.IN_PULL), new Side(IRON, DuctMode.OUT), new Side(IRON, DuctMode.OUT));
        assertEquals(new EndpointTally(1, 2), EndpointTally.of(IRON, sides));
        assertEquals(new EndpointTally(2, 1), EndpointTally.of(GOLD, sides));
    }
}
