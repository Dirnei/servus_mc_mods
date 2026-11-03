package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.model.EditResult;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Refusal;
import io.github.dirnei.mc.servus.notes.model.Scope;
import io.github.dirnei.mc.servus.notes.server.NotesData;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

/** Server lists are seen by everyone and edited by ops, guests come from the known players, and transfer moves lists. */
final class NotesSharingGameTests {

    private NotesSharingGameTests() {
    }

    static void serverListForEveryone(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        Person sepp = player(server, "Sepp");
        Person admin = player(server, "Admin");
        UUID listId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Spawn shop"));
        applied(helper, server, anna, new NoteEdit.AddTask(listId, taskId, "Counter", Optional.empty()));
        helper.assertFalse(sees(server, sepp, listId), "A private list should be hidden from Sepp");
        applied(helper, server, anna, new NoteEdit.SetScope(listId, Scope.SERVER));

        helper.assertTrue(sees(server, sepp, listId), "Sepp should see the Server list");
        refused(helper, server, sepp, new NoteEdit.EditText(listId, taskId, "Mine"), Refusal.NOT_ALLOWED);
        applied(helper, server, sepp, new NoteEdit.Tick(listId, taskId, true));
        helper.assertValueEqual(NotesData.get(server).book().get(listId).orElseThrow().task(taskId).orElseThrow()
                .done().orElseThrow().by(), sepp, "ticked by");

        NameAndId op = new NameAndId(admin.id(), admin.name());
        server.getPlayerList().op(op);
        try {
            applied(helper, server, admin, new NoteEdit.EditText(listId, taskId, "Big counter"));
            refused(helper, server, admin, new NoteEdit.DeleteList(listId), Refusal.NOT_ALLOWED);
            refused(helper, server, admin, new NoteEdit.SetScope(listId, Scope.PRIVATE), Refusal.NOT_ALLOWED);
        } finally {
            server.getPlayerList().deop(op);
        }
        refused(helper, server, admin, new NoteEdit.EditText(listId, taskId, "Small counter"), Refusal.NOT_ALLOWED);

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.succeed();
    }

    static void guestsAreKnownPlayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        Person max = new Person(UUID.randomUUID(), "Max");
        UUID listId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Farm"));
        refused(helper, server, anna, new NoteEdit.AddGuest(listId, max, Guest.Right.READ), Refusal.NOT_FOUND);

        NotesData.get(server).remember(max);
        applied(helper, server, anna, new NoteEdit.AddGuest(listId, new Person(max.id(), "Forged"), Guest.Right.READ));
        helper.assertValueEqual(NotesData.get(server).book().get(listId).orElseThrow().guest(max.id()),
                Optional.of(new Guest(max, Guest.Right.READ)), "guest with the known name");
        helper.assertTrue(sees(server, max, listId), "A read guest should see the list");
        refused(helper, server, max, new NoteEdit.RenameList(listId, "Mine"), Refusal.NOT_ALLOWED);

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.succeed();
    }

    static void transferMovesEveryList(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person max = player(server, "Max");
        Person anna = player(server, "Anna");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        applied(helper, server, max, new NoteEdit.CreateList(first, "Base"));
        applied(helper, server, max, new NoteEdit.CreateList(second, "Mine"));

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "notes transfer " + max.name() + " " + anna.name());

        NotesData data = NotesData.get(server);
        helper.assertTrue(data.book().ownedBy(max.id()).isEmpty(), "Max should own no list");
        helper.assertValueEqual(data.book().ownedBy(anna.id()).size(), 2, "lists Anna owns");
        helper.assertValueEqual(data.book().get(first).orElseThrow().ownerName(), anna.name(), "owner name");
        applied(helper, server, anna, new NoteEdit.DeleteList(first));
        applied(helper, server, anna, new NoteEdit.DeleteList(second));
        helper.succeed();
    }

    /** A known player with a name unique to this run, so tests never meet each other's players. */
    private static Person player(MinecraftServer server, String name) {
        UUID id = UUID.randomUUID();
        Person person = new Person(id, name + id.toString().substring(0, 6));
        NotesData.get(server).remember(person);
        return person;
    }

    private static boolean sees(MinecraftServer server, Person player, UUID listId) {
        return NotesService.visibleTo(server, player.id()).stream().anyMatch(view -> view.list().id().equals(listId));
    }

    private static void applied(GameTestHelper helper, MinecraftServer server, Person actor, NoteEdit edit) {
        EditResult result = NotesService.apply(server, actor, edit);
        helper.assertTrue(result instanceof EditResult.Applied, edit + " by " + actor.name() + " should be applied, was " + result);
    }

    private static void refused(GameTestHelper helper, MinecraftServer server, Person actor, NoteEdit edit, Refusal reason) {
        helper.assertValueEqual(NotesService.apply(server, actor, edit), new EditResult.Refused(reason), edit + " by " + actor.name());
    }
}
