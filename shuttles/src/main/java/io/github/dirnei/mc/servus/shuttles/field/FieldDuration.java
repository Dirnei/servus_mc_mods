package io.github.dirnei.mc.servus.shuttles.field;

/** A rough real time span for the GUI: "~3h 20m", "~45m", "<1m". Pure Java. */
public final class FieldDuration {

    private FieldDuration() {
    }

    public static String of(long seconds) {
        long minutes = Math.max(0, seconds) / 60;
        if (minutes < 1) {
            return "<1m";
        }
        if (minutes < 60) {
            return "~" + minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "~" + hours + "h " + minutes % 60 + "m";
        }
        return "~" + hours / 24 + "d " + hours % 24 + "h";
    }
}
