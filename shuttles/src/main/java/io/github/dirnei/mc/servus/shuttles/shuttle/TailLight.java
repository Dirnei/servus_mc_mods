package io.github.dirnei.mc.servus.shuttles.shuttle;

/**
 * What a shuttle's tail lights show for why it drives or stands: dim red
 * while it drives, bright red while it brakes or is held behind another
 * shuttle, blinking amber while it waits for something else, off while it
 * stands for its work or rides the car. Pure Java.
 */
public enum TailLight {
    OFF, DIM_RED, RED, AMBER;

    /** Ticks the amber blink is lit, then the same ticks dark. */
    public static final int BLINK_TICKS = 10;

    /**
     * @param braking  whether the shuttle drives slower than a tick before
     * @param gameTime the client's game time, for the blink
     */
    public static TailLight of(StandReason.Kind kind, boolean braking, long gameTime) {
        return switch (kind) {
            case DRIVING, RING -> braking ? RED : DIM_RED;
            case FOLLOWING -> RED;
            case LIFT, DECK, POWER, CHUNK -> Math.floorMod(gameTime, 2L * BLINK_TICKS) < BLINK_TICKS ? AMBER : OFF;
            case STOP, FIELD_WORK, DOOR, HANDOVER, RIDING -> OFF;
        };
    }
}
