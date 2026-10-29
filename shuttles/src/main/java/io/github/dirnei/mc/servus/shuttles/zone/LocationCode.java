package io.github.dirnei.mc.servus.shuttles.zone;

/**
 * The address of a rack bay relative to its warehouse's controller, such as
 * {@code N12.E5+2}: north or south distance, east or west distance, then the
 * signed height. North is negative Z and east positive X; zero distances are
 * written {@code N0} and {@code E0}, zero height {@code +0}. Pure Java.
 */
public final class LocationCode {

    private LocationCode() {
    }

    /** The code of a bay {@code dx}, {@code dy}, {@code dz} blocks from the controller. */
    public static String of(int dx, int dy, int dz) {
        String northSouth = dz > 0 ? "S" + dz : "N" + -dz;
        String eastWest = dx < 0 ? "W" + -dx : "E" + dx;
        String height = dy < 0 ? "-" + -dy : "+" + dy;
        return northSouth + "." + eastWest + height;
    }
}
