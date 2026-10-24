package io.github.dirnei.mc.servus.alchemy.calciner;

import java.util.Optional;

/** Where the matter of a calcination recipe comes from, in the order the journal lists them. Pure Java. */
public enum CalcinationDimension {
    OVERWORLD("overworld"),
    NETHER("nether"),
    END("end");

    private final String id;

    CalcinationDimension(String id) {
        this.id = id;
    }

    /** The name used in recipe JSON and lang keys. */
    public String id() {
        return id;
    }

    /** The dimension with that JSON name, empty for an unknown one. */
    public static Optional<CalcinationDimension> byId(String id) {
        for (CalcinationDimension dimension : values()) {
            if (dimension.id.equals(id)) {
                return Optional.of(dimension);
            }
        }
        return Optional.empty();
    }
}
