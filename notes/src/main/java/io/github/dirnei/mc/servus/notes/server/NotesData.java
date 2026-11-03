package io.github.dirnei.mc.servus.notes.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NotesBook;
import io.github.dirnei.mc.servus.notes.model.Person;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every list on the server and every player who has joined since Notes was installed, one store
 * for the whole world in {@code data/servus_notes/notes.dat}. Server thread only.
 */
public final class NotesData extends SavedData {

    private static final Codec<NotesData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NotesCodecs.LIST.listOf().optionalFieldOf("Lists", List.of()).forGetter(data -> List.copyOf(data.book.all())),
            NotesCodecs.PERSON.listOf().optionalFieldOf("KnownPlayers", List.of()).forGetter(data -> data.knownPlayers()),
            NotesCodecs.PINS.optionalFieldOf("Pins", Map.of()).forGetter(data -> Map.copyOf(data.pins)))
            .apply(i, NotesData::new));

    private static final SavedDataType<NotesData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notes"), NotesData::new, CODEC);

    private NotesBook book;
    private final Map<UUID, String> known = new LinkedHashMap<>();
    private final Map<UUID, UUID> pins = new LinkedHashMap<>();

    public NotesData() {
        this(List.of(), List.of(), Map.of());
    }

    private NotesData(List<NoteList> lists, List<Person> knownPlayers, Map<UUID, UUID> savedPins) {
        this.book = new NotesBook(lists);
        knownPlayers.forEach(person -> known.put(person.id(), person.name()));
        pins.putAll(savedPins);
    }

    public static NotesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public NotesBook book() {
        return book;
    }

    public void setBook(NotesBook newBook) {
        if (!newBook.equals(book)) {
            book = newBook;
            setDirty();
        }
    }

    /** Every player who has joined since Notes was installed, with the name they had last. */
    public List<Person> knownPlayers() {
        List<Person> players = new ArrayList<>();
        known.forEach((id, name) -> players.add(new Person(id, name)));
        return Collections.unmodifiableList(players);
    }

    /** Remembers the player's name; true when they are new or renamed. */
    public boolean remember(Person player) {
        String before = known.put(player.id(), player.name());
        if (player.name().equals(before)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Optional<Person> known(UUID id) {
        return Optional.ofNullable(known.get(id)).map(name -> new Person(id, name));
    }

    /** The known player with this name, ignoring case. */
    public Optional<Person> knownByName(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        return knownPlayers().stream().filter(person -> person.name().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    public Collection<String> knownNames() {
        return Collections.unmodifiableCollection(known.values());
    }

    /** The list {@code player} pinned to their HUD, if any. */
    public Optional<UUID> pin(UUID player) {
        return Optional.ofNullable(pins.get(player));
    }

    /** Every pin: player id to list id. */
    public Map<UUID, UUID> pins() {
        return Collections.unmodifiableMap(pins);
    }

    /** Pins {@code list} for {@code player}, or clears their pin when empty; true when it changed. */
    public boolean setPin(UUID player, Optional<UUID> list) {
        UUID before = list.isPresent() ? pins.put(player, list.get()) : pins.remove(player);
        if (Objects.equals(before, list.orElse(null))) {
            return false;
        }
        setDirty();
        return true;
    }
}
