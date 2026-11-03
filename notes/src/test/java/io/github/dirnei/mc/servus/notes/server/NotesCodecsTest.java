package io.github.dirnei.mc.servus.notes.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.notes.model.DoneRecord;
import io.github.dirnei.mc.servus.notes.model.Goal;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.Spot;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NotesBook;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Scope;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.junit.jupiter.api.Test;

class NotesCodecsTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");
    private static final Person SEPP = new Person(new UUID(0, 2), "Sepp");

    @Test
    void bookSurvivesARoundTrip() {
        Task open = Task.open(new UUID(2, 1), "Portal at spawn");
        Task done = new Task(new UUID(2, 2), "Obsidian", "ten blocks\nfrom the lava pool", true,
                Optional.of(SEPP), Optional.of(new DoneRecord(ANNA, 409, 1_800_000_000_000L)));
        NotesBook book = new NotesBook(List.of(
                new NoteList(new UUID(1, 1), "Nether", ANNA.id(), ANNA.name(), List.of(open, done)),
                NoteList.create(new UUID(1, 2), "Empty", SEPP)));

        JsonElement json = NotesCodecs.BOOK.encodeStart(JsonOps.INSTANCE, book).getOrThrow();
        NotesBook read = NotesCodecs.BOOK.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(book, read);
        assertEquals(List.of(new UUID(1, 1), new UUID(1, 2)), read.all().stream().map(NoteList::id).toList());
    }

    @Test
    void missingOptionalFieldsReadAsDefaults() {
        JsonObject task = new JsonObject();
        task.add("Id", NotesCodecs.PERSON.encodeStart(JsonOps.INSTANCE, ANNA).getOrThrow().getAsJsonObject().get("Id"));
        task.addProperty("Text", "Walls");

        Task read = NotesCodecs.TASK.parse(JsonOps.INSTANCE, task).getOrThrow();

        assertEquals(Task.open(ANNA.id(), "Walls"), read);
    }

    @Test
    void sharedListSurvivesARoundTrip() {
        NoteList list = new NoteList(new UUID(1, 1), "Nether", ANNA.id(), ANNA.name(), List.of(), Scope.TEAM,
                Optional.of(new UUID(9, 1)), List.of(new Guest(SEPP, Guest.Right.READ)));

        JsonElement json = NotesCodecs.LIST.encodeStart(JsonOps.INSTANCE, list).getOrThrow();

        assertEquals(list, NotesCodecs.LIST.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void listSavedBeforeSharingReadsAsPrivateWithoutGuests() {
        JsonObject old = new JsonObject();
        old.add("Id", uuid(new UUID(1, 1)));
        old.addProperty("Name", "Nether");
        old.add("Owner", uuid(ANNA.id()));
        old.addProperty("OwnerName", "Anna");

        NoteList read = NotesCodecs.LIST.parse(JsonOps.INSTANCE, old).getOrThrow();

        assertEquals(NoteList.create(new UUID(1, 1), "Nether", ANNA), read);
        assertEquals(Scope.PRIVATE, read.scope());
        assertTrue(read.team().isEmpty());
        assertTrue(read.guests().isEmpty());
    }

    @Test
    void goalsAndSpotSurviveARoundTrip() {
        Task task = Task.open(new UUID(2, 1), "Materials")
                .withGoals(List.of(new Goal(new UUID(5, 1), "minecraft:blackstone", 4000, 1840, ANNA),
                        new Goal(new UUID(5, 2), "gone_mod:widget", 64, 3, SEPP)))
                .withSpot(Optional.of(new Spot(-120, 64, 340, "minecraft:the_nether", "Tunnel")));

        JsonElement json = NotesCodecs.TASK.encodeStart(JsonOps.INSTANCE, task).getOrThrow();

        assertEquals(task, NotesCodecs.TASK.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void taskSavedBeforeGoalsReadsWithoutGoalsAndSpot() {
        JsonObject old = new JsonObject();
        old.add("Id", uuid(new UUID(2, 1)));
        old.addProperty("Text", "Walls");
        old.addProperty("Sub", true);

        Task read = NotesCodecs.TASK.parse(JsonOps.INSTANCE, old).getOrThrow();

        assertTrue(read.goals().isEmpty());
        assertTrue(read.spot().isEmpty());
        assertTrue(read.sub());
    }

    @Test
    void spotSavedWithoutNameReadsAsUnnamed() {
        JsonObject old = new JsonObject();
        old.addProperty("X", -120);
        old.addProperty("Y", 64);
        old.addProperty("Z", 340);
        old.addProperty("Dimension", "minecraft:the_nether");

        assertEquals(new Spot(-120, 64, 340, "minecraft:the_nether"), NotesCodecs.SPOT.parse(JsonOps.INSTANCE, old).getOrThrow());
    }

    @Test
    void pinsSurviveARoundTrip() {
        Map<UUID, UUID> pins = Map.of(ANNA.id(), new UUID(1, 1), SEPP.id(), new UUID(1, 2));

        JsonElement json = NotesCodecs.PINS.encodeStart(JsonOps.INSTANCE, pins).getOrThrow();

        assertEquals(pins, NotesCodecs.PINS.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    private static JsonElement uuid(UUID id) {
        return UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, id).getOrThrow();
    }
}
