package io.github.dirnei.mc.servus.shuttles.fluid;

/**
 * What a fluid store is for on a tanker run, whatever block holds it.
 * Import: a put-away source, a Fluid Inbound tank. Storage: the warehouse's
 * fluid stock, a Fluid Rack shelf. Export: a kept tank that restocks
 * fill, a Fluid Outbound tank.
 */
public enum FluidRole {
    IMPORT, STORAGE, EXPORT
}
