package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;

/** A list as one player receives it: the list and what that player may do with it. */
public record ListView(NoteList list, Rights rights) {

    public ListView {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(rights, "rights");
    }
}
