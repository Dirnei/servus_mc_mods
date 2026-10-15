package io.github.dirnei.mc.servus.alchemy.hint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeLedger.State;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OldGnomeLedgerTest {

    private static final UUID TEAM = UUID.randomUUID();
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void wakesOnlyOnce() {
        OldGnomeLedger ledger = new OldGnomeLedger();
        assertTrue(ledger.mayWake(TEAM));
        assertTrue(ledger.wake(TEAM, 42, OVERWORLD));
        assertFalse(ledger.mayWake(TEAM));
        assertFalse(ledger.wake(TEAM, 7, OVERWORLD));
        assertEquals(42, ledger.entry(TEAM).pos());
        assertTrue(ledger.awake(TEAM).isPresent());
        assertTrue(ledger.mayWake(UUID.randomUUID()), "other teams have their own gnome");
    }

    @Test
    void lostBeforeFoundMayWakeAgain() {
        OldGnomeLedger ledger = new OldGnomeLedger();
        ledger.wake(TEAM, 42, OVERWORLD);
        assertTrue(ledger.lost(TEAM, 42));
        assertTrue(ledger.mayWake(TEAM));
        assertEquals(State.NONE, ledger.entry(TEAM).state());
    }

    @Test
    void foundNeverWakesAgain() {
        OldGnomeLedger ledger = new OldGnomeLedger();
        ledger.wake(TEAM, 42, OVERWORLD);
        assertTrue(ledger.found(TEAM));
        assertFalse(ledger.lost(TEAM, 42));
        assertEquals(State.FOUND, ledger.entry(TEAM).state());
        assertFalse(ledger.mayWake(TEAM));
        assertTrue(ledger.awake(TEAM).isEmpty());
    }

    @Test
    void lostAtOtherPosIsIgnored() {
        OldGnomeLedger ledger = new OldGnomeLedger();
        ledger.wake(TEAM, 42, OVERWORLD);
        assertFalse(ledger.lost(TEAM, 43));
        assertEquals(State.AWAKE, ledger.entry(TEAM).state());
    }

    @Test
    void foundOnlyFromAwake() {
        OldGnomeLedger ledger = new OldGnomeLedger();
        assertFalse(ledger.found(TEAM));
        assertEquals(State.NONE, ledger.entry(TEAM).state());
        ledger.restore(TEAM, new OldGnomeLedger.Entry(State.FOUND, 5, OVERWORLD));
        assertEquals(State.FOUND, ledger.entry(TEAM).state());
        assertEquals(1, ledger.entries().size());
    }
}
