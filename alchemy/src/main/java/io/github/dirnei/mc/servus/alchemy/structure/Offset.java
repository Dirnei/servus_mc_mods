package io.github.dirnei.mc.servus.alchemy.structure;

/** A world offset in blocks. Pure Java, shared by every alchemy build. */
public record Offset(int x, int y, int z) {

    public Offset negate() {
        return new Offset(-x, -y, -z);
    }
}
