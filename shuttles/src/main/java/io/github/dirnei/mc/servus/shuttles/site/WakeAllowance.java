package io.github.dirnei.mc.servus.shuttles.site;

/** Whether a site may wake now, and if not, the first reason why. */
public final class WakeAllowance {

    public enum Reason {
        ALLOWED,
        NO_UPGRADE,
        OVER_CAPACITY,
        NO_POWER,
        NO_OWNER,
        FTB_CHUNKS,
        TEAM_LIMIT;

        /** The hard limit: these never let a site wake and release an awake one. */
        public boolean hardLimit() {
            return this == NO_UPGRADE || this == OVER_CAPACITY;
        }
    }

    private WakeAllowance() {
    }

    /**
     * @param hasTeam    whether the site has an owner team
     * @param ftbBlocks  whether FTB Chunks is installed and Servus loaders respect it
     * @param capacity   the site's capacity state
     * @param registered whether the site's chunks already count in its team's load
     * @param fits       whether the site's span fits into its team's chunk cap, counted once with the
     *                   chunks the team's anchors and other sites already hold
     */
    public static Reason check(boolean hasTeam, boolean ftbBlocks, SiteCapacity.State capacity, boolean registered,
                               boolean fits) {
        return check(hasTeam, ftbBlocks, capacity, registered, fits, 1, 0);
    }

    /**
     * As above, with the site's power: a site whose controller holds less
     * than the FE to start a wake does not wake.
     *
     * @param stored  FE the site's controller holds, live or from its record
     * @param toStart FE one second awake costs
     */
    public static Reason check(boolean hasTeam, boolean ftbBlocks, SiteCapacity.State capacity, boolean registered,
                               boolean fits, long stored, long toStart) {
        if (capacity == SiteCapacity.State.NO_UPGRADE) {
            return Reason.NO_UPGRADE;
        }
        if (capacity == SiteCapacity.State.OVER) {
            return Reason.OVER_CAPACITY;
        }
        if (stored < toStart) {
            return Reason.NO_POWER;
        }
        if (!hasTeam) {
            return Reason.NO_OWNER;
        }
        if (ftbBlocks) {
            return Reason.FTB_CHUNKS;
        }
        if (!registered && !fits) {
            return Reason.TEAM_LIMIT;
        }
        return Reason.ALLOWED;
    }
}
