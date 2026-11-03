package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;

/**
 * A block position in a dimension, kept on a task to find the work again.
 *
 * @param dimension the dimension's id as text, for example {@code minecraft:the_nether}
 * @param name      what the players call the place, empty while it has no name
 */
public record Spot(int x, int y, int z, String dimension, String name) {

    public Spot {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(name, "name");
    }

    /** A place without a name. */
    public Spot(int x, int y, int z, String dimension) {
        this(x, y, z, dimension, "");
    }

    public Spot withName(String newName) {
        return new Spot(x, y, z, dimension, newName);
    }

    /** The same block without the name, for comparing places by where they are. */
    public Spot position() {
        return withName("");
    }
}
