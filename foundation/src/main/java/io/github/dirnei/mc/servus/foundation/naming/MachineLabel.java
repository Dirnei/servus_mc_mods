package io.github.dirnei.mc.servus.foundation.naming;

/**
 * How a named machine reads for players, from its number (0 when it has none)
 * and its name: "Outbound #3", "Compost Dock (Outbound #3)", "Compost Dock" or
 * "Outbound Station".
 */
public record MachineLabel(Form form, int number, String name) {

    /** Which parts the label shows. */
    public enum Form {
        /** "Compost Dock (Outbound #3)" */
        NUMBERED_NAMED,
        /** "Outbound #3" */
        NUMBERED,
        /** "Compost Dock" */
        NAMED,
        /** "Outbound Station" */
        PLAIN
    }

    /** The label of a machine; the name is cleaned first. */
    public static MachineLabel of(int number, String name) {
        String cleaned = NameText.clean(name);
        boolean named = !cleaned.isEmpty();
        Form form = number > 0
                ? (named ? Form.NUMBERED_NAMED : Form.NUMBERED)
                : (named ? Form.NAMED : Form.PLAIN);
        return new MachineLabel(form, Math.max(0, number), cleaned);
    }

    /**
     * The label as text, with the kind word ("Outbound") and the plain block
     * name ("Outbound Station") supplied by the caller in the player's language.
     */
    public String text(String kindWord, String plainName) {
        return switch (form) {
            case NUMBERED_NAMED -> name + " (" + numbered(kindWord) + ")";
            case NUMBERED -> numbered(kindWord);
            case NAMED -> name;
            case PLAIN -> plainName;
        };
    }

    private String numbered(String kindWord) {
        return kindWord + " #" + number;
    }
}
