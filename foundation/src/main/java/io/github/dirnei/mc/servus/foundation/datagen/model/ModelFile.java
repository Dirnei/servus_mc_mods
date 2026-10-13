package io.github.dirnei.mc.servus.foundation.datagen.model;

import net.minecraft.resources.Identifier;

/**
 * A model by location. The builders in this package write plain vanilla block
 * and item model JSON, since vanilla's model generators have no free form
 * element builder.
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

    /** A model that exists as a resource; not checked, the datagen has no file helper for it. */
    public static class ExistingModelFile extends UncheckedModelFile {

        public ExistingModelFile(Identifier location) {
            super(location);
        }
    }
}
