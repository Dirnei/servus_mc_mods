package io.github.dirnei.mc.servus.lib.graph;

/**
 * An integer block position without any Minecraft dependency.
 */
public record NodePos(int x, int y, int z) {

    public NodePos relative(Side side) {
        return new NodePos(x + side.dx(), y + side.dy(), z + side.dz());
    }
}
