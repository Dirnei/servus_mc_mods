package io.github.dirnei.mc.servus.shuttles.gantry;

/**
 * What a fluid store is for on a tanker run, whatever block holds it.
 * Import: a put-away source, an Import gantry tank or a Fluid Inbound tank.
 * Storage: the warehouse's fluid stock, a Storage gantry tank or a Fluid
 * Rack section. Export: a kept tank that restocks fill, an Export gantry tank.
 */
public enum FluidRole {
    IMPORT, STORAGE, EXPORT;

    /** The role of a gantry's tanks in that mode. */
    public static FluidRole of(GantryMode mode) {
        return switch (mode) {
            case IMPORT -> IMPORT;
            case STORAGE -> STORAGE;
            case EXPORT -> EXPORT;
        };
    }
}
