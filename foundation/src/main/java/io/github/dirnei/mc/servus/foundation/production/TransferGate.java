package io.github.dirnei.mc.servus.foundation.production;

/**
 * A data member that limits how fast items enter or leave it over the data
 * line, such as a Packing Station. A delivery asks the gate of its source for
 * {@link Flow#OUT} and the gate of its target for {@link Flow#IN} each tick and
 * moves no more than both allow. Members without a gate do not limit a
 * delivery. Exposed as the {@code servus_foundation:transfer_gate} block
 * capability. Server thread only.
 */
public interface TransferGate {

    /** The direction of items relative to the gate. */
    enum Flow {
        /** Items entering the gate's block. */
        IN,
        /** Items leaving the gate's block. */
        OUT
    }

    /** How many items may pass in that direction this tick. */
    int allowance(Flow flow);

    /** Records that {@code items} passed in that direction. */
    void consume(Flow flow, int items);
}
