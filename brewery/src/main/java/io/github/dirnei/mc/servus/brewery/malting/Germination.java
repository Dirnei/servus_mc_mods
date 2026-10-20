package io.github.dirnei.mc.servus.brewery.malting;

import java.util.Optional;

/**
 * The timing of one batch of soaked grain on a Malting Floor, in game time ticks.
 *
 * @param startedAt game time the batch started
 * @param bonus     ticks gained by turning
 * @param lastTurn  game time of the last turn, or {@link #NEVER}
 */
public record Germination(long startedAt, long bonus, long lastTurn) {

    /** {@link #lastTurn} of a batch that was never turned. */
    public static final long NEVER = -1;

    /** A batch that starts now. */
    public static Germination start(long now) {
        return new Germination(now, 0, NEVER);
    }

    /** Ticks counted toward the germination time: time on the floor plus the turning bonus. */
    public long elapsed(long now) {
        return now - startedAt + bonus;
    }

    public boolean isDone(long now, long time) {
        return elapsed(now) >= time;
    }

    /** Ticks until the batch is done, never below 0. */
    public long remaining(long now, long time) {
        return Math.max(0, time - elapsed(now));
    }

    /** Whether the batch may be turned now: never turned, or the cooldown has passed. */
    public boolean canTurn(long now, long cooldown) {
        return lastTurn == NEVER || now - lastTurn >= cooldown;
    }

    /** The batch after a turn now, or empty while the cooldown runs. */
    public Optional<Germination> turn(long now, long cooldown, long turnBonus) {
        return canTurn(now, cooldown) ? Optional.of(new Germination(startedAt, bonus + turnBonus, now)) : Optional.empty();
    }
}
