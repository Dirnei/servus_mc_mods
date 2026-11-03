package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;
import java.util.UUID;

/** A player as the notebook remembers them: the id plus the name they had, so no lookup is needed to show it. */
public record Person(UUID id, String name) {

    public Person {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
