package io.github.dirnei.mc.servus.storage.site;

/**
 * Whether a Warehouse Controller is the site's antenna: it holds a Wireless
 * Upgrade and enough FE for one item at the smallest cost. Pure, no Minecraft
 * imports.
 */
public final class WirelessReach {

    private WirelessReach() {
    }

    /** Whether a controller with or without the upgrade and holding this much FE is wireless capable. */
    public static boolean capable(boolean upgrade, long energy, double fePerItem) {
        return upgrade && WirelessCost.powered(energy, fePerItem);
    }

    /** The FE a controller must hold to be powered for wireless: one item at the smallest cost, rounded up, at least 1. */
    public static long needs(double fePerItem) {
        return WirelessCost.minimumCharge(fePerItem);
    }
}
