package io.github.dirnei.mc.servus.foundation.data.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PacketFlightTest {

    /** Source member, six ducts, target member: seven blocks. */
    private static List<NodePos> route() {
        List<NodePos> route = new ArrayList<>();
        for (int x = 0; x <= 7; x++) {
            route.add(new NodePos(x, 0, 0));
        }
        return route;
    }

    @Test
    void sevenBlocksAtFourPerSecondTake35Ticks() {
        PacketLimits limits = new PacketLimits(8, 4.0, 20);
        PacketFlight flight = new PacketFlight(route());
        assertEquals(7, flight.length());
        int ticks = 0;
        while (!flight.arrived()) {
            flight.advance(limits.blocksPerTick());
            ticks++;
        }
        assertEquals(35, ticks);
        assertEquals(35, PacketFlight.ticksFor(7, 4.0));
    }

    @Test
    void currentNodeFollowsProgress() {
        PacketFlight flight = new PacketFlight(route());
        assertEquals(new NodePos(0, 0, 0), flight.current());
        flight.advance(2.5);
        assertEquals(new NodePos(2, 0, 0), flight.current());
        assertTrue(flight.passes(new NodePos(5, 0, 0)));
        assertFalse(flight.passes(new NodePos(1, 0, 0)));
        assertEquals(new NodePos(6, 0, 0), flight.lastNode());
    }

    @Test
    void rerouteKeepsPosition() {
        PacketFlight flight = new PacketFlight(route());
        flight.advance(3.2);
        NodePos at = flight.current();
        List<NodePos> detour = List.of(at, new NodePos(3, 0, 1), new NodePos(4, 0, 1), new NodePos(5, 0, 1), new NodePos(5, 0, 0));
        PacketFlight rerouted = new PacketFlight(detour);
        assertEquals(at, rerouted.source());
        assertEquals(at, rerouted.current());
        assertEquals(4, rerouted.length());
    }
}
