package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.model.EditResult;
import io.github.dirnei.mc.servus.notes.model.Goal;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Refusal;
import io.github.dirnei.mc.servus.notes.server.NotesData;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/** Item goals through the server: a read guest types counts, and goals only name items the server has. */
final class NotesGoalsGameTests {

    private NotesGoalsGameTests() {
    }

    static void readGuestTypesACount(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        Person sepp = player(server, "Sepp");
        UUID listId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Nether highway"));
        applied(helper, server, anna, new NoteEdit.AddTask(listId, taskId, "Materials", Optional.empty()));
        applied(helper, server, anna, new NoteEdit.AddGoal(listId, taskId, goalId, "minecraft:packed_ice"));
        applied(helper, server, anna, new NoteEdit.SetGoalCount(listId, taskId, goalId, 210));
        applied(helper, server, anna, new NoteEdit.AddGuest(listId, sepp, Guest.Right.READ));

        applied(helper, server, sepp, new NoteEdit.SetGoalCount(listId, taskId, goalId, 300));
        Goal goal = NotesData.get(server).book().get(listId).orElseThrow().task(taskId).orElseThrow().goal(goalId).orElseThrow();
        helper.assertValueEqual(goal.count(), 300, "count");
        helper.assertValueEqual(goal.lastBy(), sepp, "last count by");
        refused(helper, server, sepp, new NoteEdit.SetGoalTarget(listId, taskId, goalId, 1), Refusal.NOT_ALLOWED);

        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.succeed();
    }

    static void unknownItemIsRefused(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Person anna = player(server, "Anna");
        UUID listId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        applied(helper, server, anna, new NoteEdit.CreateList(listId, "Farm"));
        applied(helper, server, anna, new NoteEdit.AddTask(listId, taskId, "Glass", Optional.empty()));
        refused(helper, server, anna, new NoteEdit.AddGoal(listId, taskId, UUID.randomUUID(), "gone_mod:widget"), Refusal.NOT_FOUND);
        refused(helper, server, anna, new NoteEdit.AddGoal(listId, taskId, UUID.randomUUID(), "minecraft:air"), Refusal.NOT_FOUND);
        applied(helper, server, anna, new NoteEdit.DeleteList(listId));
        helper.succeed();
    }

    /** A known player with a name unique to this run, so tests never meet each other's players. */
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

    private static void refused(GameTestHelper helper, MinecraftServer server, Person actor, NoteEdit edit, Refusal reason) {
        helper.assertValueEqual(NotesService.apply(server, actor, edit), new EditResult.Refused(reason), edit + " by " + actor.name());
    }
}
