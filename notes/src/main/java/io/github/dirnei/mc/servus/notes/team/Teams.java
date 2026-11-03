package io.github.dirnei.mc.servus.notes.team;

/** The team lookup in use: {@link NoTeams} unless a team mod integration installed its own. */
public final class Teams {

    private static TeamLookup lookup = NoTeams.INSTANCE;

    private Teams() {
    }

    public static TeamLookup lookup() {
        return lookup;
    }

    /** Called once while the mod loads. */
    public static void install(TeamLookup newLookup) {
        lookup = newLookup;
    }
}
