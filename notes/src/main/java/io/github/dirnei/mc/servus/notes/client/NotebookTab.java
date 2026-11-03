package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Scope;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** The paper tabs on the top edge of the notebook's right page, in their order from left to right. */
public enum NotebookTab {
    MINE("red"),
    SHARED("green"),
    TEAM("blue"),
    SERVER("gold");

    private final Identifier sprite;
    private final Identifier activeSprite;

    NotebookTab(String colour) {
        this.sprite = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/tab_" + colour);
        this.activeSprite = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/tab_" + colour + "_active");
    }

    /** The tab's sprite, standing up and in full colour while {@code active}. */
    public Identifier sprite(boolean active) {
        return active ? activeSprite : sprite;
    }

    /** Whether the notebook shows this tab: Team only with FTB Teams and while the player is in a party. */
    public boolean available(boolean teamsInstalled, Optional<UUID> party) {
        return this != TEAM || teamsInstalled && party.isPresent();
    }

    /**
     * Whether the tab lists {@code view} for the player {@code self} in {@code party}: Mine the lists they
     * own, Shared those they are a guest of, Team the Team lists of their party, Server every Server list.
     */
    public boolean shows(ListView view, UUID self, Optional<UUID> party) {
        NoteList list = view.list();
        return switch (this) {
            case MINE -> list.owner().equals(self);
            case SHARED -> list.guest(self).isPresent();
            case TEAM -> list.scope() == Scope.TEAM && party.isPresent() && list.team().equals(party);
            case SERVER -> list.scope() == Scope.SERVER;
        };
    }

    public Component label() {
        return Component.translatable("gui.servus_notes.notebook.tab." + key());
    }

    /** The full name, shown as the tab's tooltip. */
    public Component tooltip() {
        return Component.translatable("gui.servus_notes.notebook.tab." + key() + ".tooltip");
    }

    private String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
