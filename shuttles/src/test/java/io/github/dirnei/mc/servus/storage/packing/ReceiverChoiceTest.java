package io.github.dirnei.mc.servus.storage.packing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReceiverChoiceTest {

    private record Station(String name, long pos) {
    }

    @Test
    void twoIdleStationsPickTheLower() {
        Station a = new Station("a", 20);
        Station b = new Station("b", 10);
        assertEquals(b, ReceiverChoice.choose(List.of(a, b), station -> 0, Station::pos).orElseThrow());
    }

    @Test
    void busierStationIsSkipped() {
        Station a = new Station("a", 10);
        Station b = new Station("b", 20);
        Map<Station, Integer> incoming = Map.of(a, 2, b, 1);
        assertEquals(b, ReceiverChoice.choose(List.of(a, b), incoming::get, Station::pos).orElseThrow());
    }

    @Test
    void tieGoesByPosition() {
        Station a = new Station("a", 30);
        Station b = new Station("b", -5);
        Station c = new Station("c", 7);
        assertEquals(b, ReceiverChoice.choose(List.of(a, b, c), station -> 1, Station::pos).orElseThrow());
    }

    @Test
    void noStationGivesNone() {
        assertTrue(ReceiverChoice.choose(List.<Station>of(), station -> 0, Station::pos).isEmpty());
    }
}
