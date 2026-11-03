package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.model.EditResult;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NotesBook;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Task;
import io.github.dirnei.mc.servus.notes.server.NotesCodecs;
import io.github.dirnei.mc.servus.notes.server.NotesData;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

/** Edits go through the server's service into the world's saved lists, and only the owner may make them. */
final class NotesListsGameTests {

    private NotesListsGameTests() {
    }

    static void editIsSaved(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = new Person(UUID.randomUUID(), "Anna");
        UUID listId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Nether"));
        applied(helper, server, anna, new NoteEdit.AddTask(listId, taskId, "Portal at spawn", Optional.empty()));
        applied(helper, server, anna, new NoteEdit.Tick(listId, taskId, true));

        NotesData data = NotesData.get(server);
        NoteList saved = data.book().get(listId).orElse(null);
        helper.assertTrue(saved != null, "The new list should be in the saved data");
        helper.assertTrue(data.isDirty(), "The saved data should be marked for saving");
        Task task = saved.task(taskId).orElseThrow();
        helper.assertValueEqual(task.done().map(done -> done.by()), Optional.of(anna), "ticked by");
        helper.assertValueEqual(task.done().orElseThrow().day(), server.overworld().getOverworldClockTime() / 24000L, "tick day");

        Tag tag = NotesCodecs.BOOK.encodeStart(NbtOps.INSTANCE, data.book()).getOrThrow();
        NotesBook read = NotesCodecs.BOOK.parse(NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertValueEqual(read.get(listId), Optional.of(saved), "list after a codec round trip");
        helper.assertTrue(NotesService.visibleTo(server, anna.id()).stream().anyMatch(view -> view.list().id().equals(listId)),
                "The owner should see the list");

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.assertTrue(NotesData.get(server).book().get(listId).isEmpty(), "The deleted list should be gone");
        helper.succeed();
    }

    static void strangerIsRefused(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = new Person(UUID.randomUUID(), "Anna");
        Person sepp = new Person(UUID.randomUUID(), "Sepp");
        UUID listId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Private"));
        applied(helper, server, anna, new NoteEdit.AddTask(listId, taskId, "Secret base", Optional.empty()));
        NoteList before = NotesData.get(server).book().get(listId).orElseThrow();

        List<NoteEdit> forged = List.of(
                new NoteEdit.RenameList(listId, "Mine now"),
                new NoteEdit.AddTask(listId, UUID.randomUUID(), "Sneaky", Optional.empty()),
                new NoteEdit.Tick(listId, taskId, true),
                new NoteEdit.Claim(listId, taskId),
                new NoteEdit.DeleteList(listId));
        for (NoteEdit edit : forged) {
            EditResult result = NotesService.apply(server, sepp, edit);
            helper.assertTrue(result instanceof EditResult.Refused, "A stranger's " + edit + " should be refused");
        }
        helper.assertValueEqual(NotesData.get(server).book().get(listId), Optional.of(before), "list after forged edits");
        helper.assertTrue(NotesService.visibleTo(server, sepp.id()).isEmpty(), "A stranger should see no list");

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.succeed();
    }

    private static void applied(GameTestHelper helper, MinecraftServer server, Person actor, NoteEdit edit) {
        EditResult result = NotesService.apply(server, actor, edit);
        helper.assertTrue(result instanceof EditResult.Applied, edit + " should be applied, was " + result);
    }
}
