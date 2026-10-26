package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.shuttles.site.SiteName;

/**
 * How a station is named for players, from its number (0 when it has none) and
 * its name: "Outbound #3", "Compost Dock (#3)", "Compost Dock" or "Outbound Station".
 */
public record StationLabel(Form form, int number, String name) {

    /** Which parts the label shows. */
    public enum Form {
        /** "Compost Dock (#3)" */
        NUMBERED_NAMED,
        /** "Outbound #3" */
        NUMBERED,
        /** "Compost Dock" */
        NAMED,
        /** "Outbound Station" */
        PLAIN
    }

    /** The label of a station; the name is cleaned like a site name first. */
    public static StationLabel of(int number, String name) {
        String cleaned = SiteName.clean(name);
        boolean named = !cleaned.isEmpty();
        Form form = number > 0
                ? (named ? Form.NUMBERED_NAMED : Form.NUMBERED)
                : (named ? Form.NAMED : Form.PLAIN);
        return new StationLabel(form, Math.max(0, number), cleaned);
    }

    /**
     * The label as text, with the kind word ("Outbound") and the plain block
     * name ("Outbound Station") supplied by the caller in the player's language.
     */
    public String text(String kindWord, String plainName) {
        return switch (form) {
            case NUMBERED_NAMED -> name + " (#" + number + ")";
            case NUMBERED -> kindWord + " #" + number;
            case NAMED -> name;
            case PLAIN -> plainName;
        };
    }
}
