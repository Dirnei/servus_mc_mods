package io.github.dirnei.mc.servus.alchemy.homunculus;

/**
 * A homunculus' life in its flask: it grows on fed ticks until it is grown, then works once every work interval of
 * fed ticks. A freed homunculus counts as fed. Pure Java, called once per server tick.
 */
public final class HomunculusLife {

    /** What one tick did. */
    public enum Step {
        NONE, GREW, GREW_UP, WORK
    }

    private int growth;
    private boolean grown;
    private boolean freed;
    private int workTimer;

    public int growth() {
        return growth;
    }

    public boolean grown() {
        return grown;
    }

    public boolean freed() {
        return freed;
    }

    public int workTimer() {
        return workTimer;
    }

    /** Whether the next tick needs Elixir: a freed homunculus lives without it. */
    public boolean needsElixir() {
        return !freed;
    }

    /** How far it has grown, from 0 to 1. */
    public float growthShare(HomunculusSettings settings) {
        return grown ? 1F : Math.min(1F, growth / (float) settings.growTicks());
    }

    /** One tick; {@code fed} tells whether Elixir fed it, ignored once it is freed. */
    public Step tick(HomunculusSettings settings, boolean fed) {
        if (!fed && !freed) {
            return Step.NONE;
        }
        if (!grown) {
            growth++;
            if (growth < settings.growTicks()) {
                return Step.GREW;
            }
            grown = true;
            growth = settings.growTicks();
            return Step.GREW_UP;
        }
        workTimer++;
        if (workTimer < settings.workInterval()) {
            return Step.NONE;
        }
        workTimer = 0;
        return Step.WORK;
    }

    /** Frees a grown homunculus; returns whether it was freed now. */
    public boolean free() {
        if (!grown || freed) {
            return false;
        }
        freed = true;
        return true;
    }

    /** Whether a gnome with {@code boostTicksLeft} of boost left should get a gift now. */
    public static boolean wantsGift(int boostTicksLeft, HomunculusSettings settings) {
        return boostTicksLeft < settings.giftBelowTicks();
    }

    /** Restores a saved life. */
    public void load(int growth, boolean grown, boolean freed, int workTimer) {
        this.growth = Math.max(0, growth);
        this.grown = grown;
        this.freed = grown && freed;
        this.workTimer = Math.max(0, workTimer);
    }
}
