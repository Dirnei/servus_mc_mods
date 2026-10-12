package io.github.dirnei.mc.servus.foundation.machine.pump;

/** The lower half of a beam pump: keeps the client stroke phase its renderer and splash read. */
public interface BeamPumpEngine {

    /** Client only; the server never advances it. */
    StrokeClock strokeClock();
}
