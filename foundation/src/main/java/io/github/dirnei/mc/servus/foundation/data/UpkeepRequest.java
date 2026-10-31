package io.github.dirnei.mc.servus.foundation.data;

/**
 * An upkeep request on its way to a member.
 *
 * @param supplier  position of the supplier that took it, as {@code BlockPos.asLong}
 * @param ticket    the supplier's ticket for it
 * @param remaining how many items are still to arrive
 * @param lastCount the member's count at the last check
 * @param until     game time after which the request is given up
 */
public record UpkeepRequest(long supplier, long ticket, int remaining, int lastCount, long until) {

    /** The request after the member's count went to {@code count}: every rise is credited as an arrival. */
    public UpkeepRequest counted(int count) {
        int arrived = Math.max(0, count - lastCount);
        return new UpkeepRequest(supplier, ticket, remaining - arrived, count, until);
    }
}
