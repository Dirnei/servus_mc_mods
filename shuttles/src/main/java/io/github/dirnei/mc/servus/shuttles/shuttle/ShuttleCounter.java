package io.github.dirnei.mc.servus.shuttles.shuttle;

/** Hands out numbers counting up from 1, such as shuttle numbers and job ids; never gives a number twice. */
public final class ShuttleCounter {

    private int last;

    /** A counter that has given numbers up to {@code last}; 0 for a fresh world. */
    public ShuttleCounter(int last) {
        this.last = Math.max(0, last);
    }

    /** The next number, remembered as given. */
    public int take() {
        return ++last;
    }

    /** The last number given, 0 when none was. */
    public int last() {
        return last;
    }
}
