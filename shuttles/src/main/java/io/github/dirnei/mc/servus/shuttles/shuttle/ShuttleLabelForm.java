package io.github.dirnei.mc.servus.shuttles.shuttle;

/** Which parts a shuttle's label shows, from whether it has a name plate and a number. */
public enum ShuttleLabelForm {
    /** "Berta (#12)" */
    NUMBERED_NAMED,
    /** "Shuttle #12" */
    NUMBERED,
    /** "Berta (Shuttle Mk2)" */
    NAMED,
    /** "Shuttle Mk1" */
    PLAIN;

    public static ShuttleLabelForm of(boolean plate, boolean number) {
        if (number) {
            return plate ? NUMBERED_NAMED : NUMBERED;
        }
        return plate ? NAMED : PLAIN;
    }
}
