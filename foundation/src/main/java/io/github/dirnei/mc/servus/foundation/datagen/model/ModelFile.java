package io.github.dirnei.mc.servus.foundation.datagen.model;

import net.minecraft.resources.Identifier;

/**
 * A model by location, for the Servus model datagen. Vanilla's model
 * generators (26.1) have no free form element builder, so the Servus mods keep
 * their own small JSON model writers in this package; the output is the plain
 * vanilla block and item model format.
 */
public abstract class ModelFile {

    protected final Identifier location;

    protected ModelFile(Identifier location) {
        this.location = location;
    }

    public Identifier getLocation() {
        return location;
    }

    /** A model that is written elsewhere (vanilla or a hand written file); not checked. */
    public static class UncheckedModelFile extends ModelFile {

        public UncheckedModelFile(Identifier location) {
            super(location);
        }

        public UncheckedModelFile(String location) {
            this(Identifier.parse(location));
        }
    }

    /** A model that exists as a resource; the 26.1 datagen has no file helper to check it. */
    public static class ExistingModelFile extends UncheckedModelFile {

        public ExistingModelFile(Identifier location) {
            super(location);
        }
    }
}
