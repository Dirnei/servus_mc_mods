package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Limits;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.network.EditPayload;
import io.github.dirnei.mc.servus.notes.network.PinPayload;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The client's copy of every list the player sees, kept current by the server's snapshots, plus
 * the player's party, the known players, the list selected in the notebook, the list pinned to the
 * HUD and the last refused edit. Client thread only.
 */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class ClientNotes {

    /** Hears about changes to the client's lists. */
    public interface Listener {

        /** One list changed: {@code before} is null for a new list, {@code after} null for a removed one. */
        void listChanged(@Nullable ListView before, @Nullable ListView after);

        /** Every list was replaced, as on joining a server. */
        default void allReplaced() {
        }
    }

    /** A refused edit: the reason key and when it arrived ({@link Util#getMillis()}). */
    public record Refusal(String reasonKey, long at) {
    }

    private static final Map<UUID, ListView> LISTS = new LinkedHashMap<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static Limits limits = Limits.DEFAULT;
    private static @Nullable UUID selected;
    private static @Nullable Refusal refusal;
    private static boolean teamsInstalled;
    private static Optional<UUID> party = Optional.empty();
    private static List<Person> knownPlayers = List.of();
    private static Optional<UUID> pinned = Optional.empty();

    private ClientNotes() {
    }

    public static Collection<ListView> lists() {
        return Collections.unmodifiableCollection(LISTS.values());
    }

    public static Optional<ListView> get(@Nullable UUID listId) {
        return listId == null ? Optional.empty() : Optional.ofNullable(LISTS.get(listId));
    }

    public static Limits limits() {
        return limits;
    }

    /** The list the notebook shows on its right page, kept while the notebook is closed. */
    public static @Nullable UUID selected() {
        return selected;
    }

    public static void select(@Nullable UUID listId) {
        selected = listId;
    }

    /** Whether the server runs FTB Teams. */
    public static boolean teamsInstalled() {
        return teamsInstalled;
    }

    /** The FTB Teams party the player is in, empty when in none. */
    public static Optional<UUID> party() {
        return party;
    }

    /** Every player who has been on the server, to pick guests from. */
    public static List<Person> knownPlayers() {
        return knownPlayers;
    }

    /** The local player's id, or the zero id while there is none. */
    public static UUID self() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? new UUID(0, 0) : player.getUUID();
    }

    /** The list pinned to the HUD, as the server last said. */
    public static Optional<UUID> pinned() {
        return pinned;
    }

    /** Whether {@code listId} is the list pinned to the HUD. */
    public static boolean isPinned(UUID listId) {
        return pinned.filter(listId::equals).isPresent();
    }

    /** Asks the server to pin a list to the HUD, or to clear the pin when empty. Ignored while not connected. */
    public static void pin(Optional<UUID> listId) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new PinPayload(listId));
        }
    }

    public static void pinned(Optional<UUID> listId) {
        pinned = listId;
    }

    public static Optional<Refusal> refusal() {
        return Optional.ofNullable(refusal);
    }

    /** Asks the server to apply an edit; the result comes back as a snapshot or a refusal. Ignored while not connected. */
    public static void send(NoteEdit edit) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new EditPayload(edit));
        }
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void replaceAll(List<ListView> views, Limits newLimits) {
        LISTS.clear();
        views.forEach(view -> LISTS.put(view.list().id(), view));
        limits = newLimits;
        LISTENERS.forEach(Listener::allReplaced);
    }

    public static void update(ListView view) {
        ListView before = LISTS.put(view.list().id(), view);
        if (before != null) {
            TickToasts.show(before.list(), view.list());
        }
        LISTENERS.forEach(listener -> listener.listChanged(before, view));
    }

    public static void remove(UUID listId) {
        ListView before = LISTS.remove(listId);
        if (before != null) {
            LISTENERS.forEach(listener -> listener.listChanged(before, null));
        }
    }

    public static void viewer(boolean installed, Optional<UUID> newParty) {
        teamsInstalled = installed;
        party = newParty;
    }

    public static void knownPlayers(List<Person> players) {
        knownPlayers = List.copyOf(players);
    }

    public static void refused(String reasonKey) {
        refusal = new Refusal(reasonKey, Util.getMillis());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LISTS.clear();
        limits = Limits.DEFAULT;
        selected = null;
        refusal = null;
        teamsInstalled = false;
        party = Optional.empty();
        knownPlayers = List.of();
        pinned = Optional.empty();
    }
}
