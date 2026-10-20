package io.github.dirnei.mc.servus.foundation.drive;

/**
 * The angle of a turning part on the client, with a flywheel's momentum: while
 * driven it spins up to full speed within a few ticks and then turns at a
 * constant rate; once the drive stops it coasts, slowing down evenly to a stop.
 * Advanced once per client tick, read every frame. Never saved or synced. Pure Java.
 */
public final class Flywheel {

    /** Ticks from standing to full speed. */
    public static final int SPIN_UP_TICKS = 4;
    /** Ticks from full speed to standing once the drive stops. */
    public static final int COAST_TICKS = 20;
    private static final float STOPPED = 1.0E-4F;

    private final float degreesPerTick;
    private final float spinUpStep;
    private final float coastStep;
    private float speed;
    private float angle;
    private float previousAngle;

    public Flywheel(float degreesPerTick) {
        this(degreesPerTick, SPIN_UP_TICKS, COAST_TICKS);
    }

    public Flywheel(float degreesPerTick, int spinUpTicks, int coastTicks) {
        this.degreesPerTick = degreesPerTick;
        this.spinUpStep = 1.0F / Math.max(1, spinUpTicks);
        this.coastStep = 1.0F / Math.max(1, coastTicks);
    }

    /** One client tick: speeds up while {@code driven}, coasts down while not, then turns by the speed. */
    public void tick(boolean driven) {
        speed = driven ? Math.min(1, speed + spinUpStep) : Math.max(0, speed - coastStep);
        if (speed < STOPPED) {
            speed = 0;
        }
        previousAngle = angle;
        angle += speed * degreesPerTick;
        if (angle >= 360) {
            angle -= 360;
            previousAngle -= 360;
        }
    }

    /** The speed as a share of full speed, 0 to 1. */
    public float speed() {
        return speed;
    }

    /** Whether the part still moves. */
    public boolean turning() {
        return speed > 0;
    }

    /** The angle in degrees for a frame, between the last two ticks. */
    public float angle(float partialTick) {
        return previousAngle + (angle - previousAngle) * partialTick;
    }
}
