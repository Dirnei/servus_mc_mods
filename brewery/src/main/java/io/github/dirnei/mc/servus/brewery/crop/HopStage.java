package io.github.dirnei.mc.servus.brewery.crop;

/** What grows on one Hop Pole part, in growth order. */
public enum HopStage {
    /** The pole only. */
    BARE,
    /** Small shoots at the bottom of the part. */
    YOUNG,
    /** Leaves wrap the whole part. */
    GROWN,
    /** Leaves with ripe Hop cones. */
    CONES;

    /** Whether a vine covers the part. */
    public boolean hasVine() {
        return this != BARE;
    }
}
