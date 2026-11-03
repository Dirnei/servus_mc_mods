package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.model.EditResult;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.server.NotesData;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/** The HUD pin is kept by the server: only lists the player sees, and gone once they lose the list. */
final class NotesHudGameTests {

    private NotesHudGameTests() {
    }

    static void pinRefusedForInvisibleList(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        Person sepp = player(server, "Sepp");
        UUID listId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Vault"));

        helper.assertFalse(NotesService.pin(server, sepp.id(), Optional.of(listId)), "Sepp should not pin a list he does not see");
        helper.assertValueEqual(NotesData.get(server).pin(sepp.id()), Optional.empty(), "Sepp's pin");
        helper.assertTrue(NotesService.pin(server, anna.id(), Optional.of(listId)), "Anna should pin her own list");
        helper.assertValueEqual(NotesData.get(server).pin(anna.id()), Optional.of(listId), "Anna's pin");

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.assertValueEqual(NotesData.get(server).pin(anna.id()), Optional.empty(), "Anna's pin after deleting the list");
        helper.succeed();
    }

    static void pinClearedWhenGuestRemoved(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        Person max = player(server, "Max");
        UUID listId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Nether highway"));
        applied(helper, server, anna, new NoteEdit.CreateList(otherId, "Farm"));
        applied(helper, server, anna, new NoteEdit.AddGuest(listId, max, Guest.Right.READ));
        helper.assertTrue(NotesService.pin(server, max.id(), Optional.of(listId)), "A guest should pin the list");
        NotesService.pin(server, anna.id(), Optional.of(listId));

        applied(helper, server, anna, new NoteEdit.RenameList(otherId, "Wheat farm"));
        helper.assertValueEqual(NotesData.get(server).pin(max.id()), Optional.of(listId), "Max's pin after an unrelated edit");

        applied(helper, server, anna, new NoteEdit.RemoveGuest(listId, max.id()));
        helper.assertValueEqual(NotesData.get(server).pin(max.id()), Optional.empty(), "Max's pin after losing the list");
        helper.assertValueEqual(NotesData.get(server).pin(anna.id()), Optional.of(listId), "Anna keeps her pin");

        NotesService.pin(server, anna.id(), Optional.empty());
        helper.assertValueEqual(NotesData.get(server).pin(anna.id()), Optional.empty(), "Anna's pin after unpinning");
        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        applied(helper, server, anna, new NoteEdit.DeleteList(otherId));
        helper.succeed();
    }

    private static Person player(MinecraftServer server, String name) {
        UUID id = UUID.randomUUID();
        Person person = new Person(id, name + id.toString().substring(0, 6));
        NotesData.get(server).remember(person);
        return person;
    }

    private static void applied(GameTestHelper helper, MinecraftServer server, Person actor, NoteEdit edit) {
        EditResult result = NotesService.apply(server, actor, edit);
        helper.assertTrue(result instanceof EditResult.Applied, edit + " by " + actor.name() + " should be applied, was " + result);
    }
}
