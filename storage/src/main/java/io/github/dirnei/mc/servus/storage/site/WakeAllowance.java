package io.github.dirnei.mc.servus.storage.site;

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
     * @param registered whether the site already holds a slot of its team
     * @param teamActive active loaders of the team, this site's slot included when registered
     * @param teamCap    the team's cap of active loaders
     */
    public static Reason check(boolean hasTeam, boolean ftbBlocks, SiteCapacity.State capacity, boolean registered,
                               int teamActive, int teamCap) {
        return check(hasTeam, ftbBlocks, capacity, registered, teamActive, teamCap, 1, 0);
    }

    /**
     * As above, with the site's power: a site whose controller holds less
     * than the FE to start a wake does not wake.
     *
     * @param stored  FE the site's controller holds, live or from its record
     * @param toStart FE one second awake costs
     */
    public static Reason check(boolean hasTeam, boolean ftbBlocks, SiteCapacity.State capacity, boolean registered,
                               int teamActive, int teamCap, long stored, long toStart) {
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
        if (!registered && teamActive >= teamCap) {
            return Reason.TEAM_LIMIT;
        }
        return Reason.ALLOWED;
    }
}
