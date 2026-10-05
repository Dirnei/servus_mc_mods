package io.github.dirnei.mc.servus.manufacturing.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResearchStateTest {

    private static final String RED = "servus_manufacturing:red_pack";
    private static final TechNode ROOT = new TechNode("root", List.of(), Map.of(RED, 2));
    private static final TechNode CHILD = new TechNode("child", List.of("root"), Map.of(RED, 1));
    private static final TechNode OTHER = new TechNode("other", List.of(), Map.of(RED, 3));

    @Test
    void rootIsAvailableAndChildIsLocked() {
        ResearchState state = new ResearchState();

        assertTrue(state.isAvailable(ROOT));
        assertFalse(state.isAvailable(CHILD));
        assertFalse(state.start(CHILD));
        assertTrue(state.current().isEmpty());
    }

    @Test
    void consumeUntilComplete() {
        ResearchState state = new ResearchState();
        assertTrue(state.start(ROOT));
        assertEquals(2, state.remaining(ROOT, RED));

        assertFalse(state.consume(ROOT, RED));
        assertEquals(1, state.remaining(ROOT, RED));
        assertTrue(state.consume(ROOT, RED));

        assertTrue(state.isCompleted("root"));
        assertTrue(state.current().isEmpty());
        assertFalse(state.isAvailable(ROOT));
        assertTrue(state.isAvailable(CHILD));
    }

    @Test
    void unneededPackIsRefused() {
        ResearchState state = new ResearchState();
        state.start(ROOT);

        assertEquals(0, state.remaining(ROOT, "other_pack"));
        assertThrows(IllegalStateException.class, () -> state.consume(ROOT, "other_pack"));
        assertEquals(0, state.remaining(OTHER, RED));
        assertThrows(IllegalStateException.class, () -> state.consume(OTHER, RED));
    }

    @Test
    void switchingKeepsProgress() {
        ResearchState state = new ResearchState();
        state.start(ROOT);
        state.consume(ROOT, RED);

        assertTrue(state.start(OTHER));
        assertEquals(3, state.remaining(OTHER, RED));
        assertEquals(0, state.remaining(ROOT, RED));

        assertTrue(state.start(ROOT));
        assertEquals(1, state.remaining(ROOT, RED));
        assertEquals(1, state.consumed("root", RED));
    }

    @Test
    void restoreRoundTrip() {
        ResearchState state = new ResearchState();
        state.start(ROOT);
        state.consume(ROOT, RED);

        ResearchState copy = new ResearchState();
        copy.restore(state.completed(), state.current().orElse(null), state.progress());

        assertEquals(1, copy.remaining(ROOT, RED));
        assertEquals(state.current(), copy.current());
        assertTrue(copy.consume(ROOT, RED));
        assertTrue(copy.isCompleted("root"));
    }
}
