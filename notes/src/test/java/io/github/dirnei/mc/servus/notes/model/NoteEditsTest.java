package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.notes.model.EditResult.Applied;
import io.github.dirnei.mc.servus.notes.model.EditResult.Refused;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.AddTask;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.Claim;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.CreateList;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.DeleteList;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.DeleteTask;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.EditNotes;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.EditText;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.PlaceTask;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.RenameList;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetAssignee;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.Tick;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.AddGuest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.RemoveGuest;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetGuestRight;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetScope;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.AddGoal;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.RemoveGoal;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetGoalCount;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetGoalItem;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetGoalTarget;
import io.github.dirnei.mc.servus.notes.model.NoteEdit.SetSpot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteEditsTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");
    private static final Person SEPP = new Person(new UUID(0, 2), "Sepp");
    private static final Moment NOW = new Moment(409, 1_800_000_000_000L);
    private static final Limits LIMITS = new Limits(2, 3, 10, 12, 20);
    private static final UUID LIST = new UUID(1, 1);

    private NotesBook book;

    @BeforeEach
    void setUp() {
        book = NotesBook.EMPTY;
        ok(ANNA, new CreateList(LIST, "Chores"));
    }

    private EditResult apply(Person actor, NoteEdit edit) {
        return NoteEdits.apply(book, actor, OwnerAccess.INSTANCE, LIMITS, NOW, edit);
    }

    private Applied ok(Person actor, NoteEdit edit) {
        Applied applied = assertInstanceOf(Applied.class, apply(actor, edit));
        book = applied.book();
        return applied;
    }

    private void refused(Refusal reason, Person actor, NoteEdit edit) {
        NotesBook before = book;
        assertEquals(new Refused(reason), apply(actor, edit));
        assertEquals(before, book);
    }

    private UUID task(String text) {
        UUID id = UUID.nameUUIDFromBytes(text.getBytes());
        ok(ANNA, new AddTask(LIST, id, text, Optional.empty()));
        return id;
    }

    private NoteList list() {
        return book.get(LIST).orElseThrow();
    }

    /** The tasks as text, subtasks marked with a leading "-". */
    private String shape() {
        return list().tasks().stream().map(t -> (t.sub() ? "-" : "") + t.text()).collect(Collectors.joining(" "));
    }

    private Limits roomyLimits() {
        return new Limits(2, 20, 10, 12, 20);
    }

    private void tasks(String... texts) {
        for (String text : texts) {
            UUID id = UUID.nameUUIDFromBytes(text.getBytes());
            book = ((Applied) NoteEdits.apply(book, ANNA, OwnerAccess.INSTANCE, roomyLimits(), NOW,
                    new AddTask(LIST, id, text, Optional.empty()))).book();
        }
    }

    private UUID id(String text) {
        return UUID.nameUUIDFromBytes(text.getBytes());
    }

    /** Makes the task a subtask of the task just above it. */
    private void indent(String text) {
        List<Task> tasks = list().tasks();
        int index = list().indexOf(id(text));
        ok(ANNA, new PlaceTask(LIST, id(text), Optional.of(tasks.get(index - 1).id()), true));
    }

    private Applied place(String text, String after, boolean sub) {
        return ok(ANNA, new PlaceTask(LIST, id(text), Optional.ofNullable(after).map(this::id), sub));
    }

    @Test
    void createMakesAnEmptyListOwnedByTheActor() {
        NoteList list = list();
        assertEquals("Chores", list.name());
        assertEquals(ANNA.id(), list.owner());
        assertEquals("Anna", list.ownerName());
        assertTrue(list.tasks().isEmpty());
    }

    @Test
    void createIsCappedPerPlayer() {
        ok(ANNA, new CreateList(new UUID(1, 2), "Second"));
        refused(Refusal.LIMIT_LISTS, ANNA, new CreateList(new UUID(1, 3), "Third"));
        assertEquals(Set.of(new UUID(1, 4)), ok(SEPP, new CreateList(new UUID(1, 4), "Sepps")).changed());
    }

    @Test
    void createRefusesATakenId() {
        refused(Refusal.NOT_ALLOWED, SEPP, new CreateList(LIST, "Mine now"));
    }

    @Test
    void renameChangesTheName() {
        assertEquals(Set.of(LIST), ok(ANNA, new RenameList(LIST, "  Errands ")).changed());
        assertEquals("Errands", list().name());
    }

    @Test
    void nameTooLongOrEmptyIsRefused() {
        refused(Refusal.TOO_LONG, ANNA, new RenameList(LIST, "Eleven char"));
        refused(Refusal.EMPTY, ANNA, new RenameList(LIST, "   "));
    }

    @Test
    void deleteRemovesTheList() {
        Applied applied = ok(ANNA, new DeleteList(LIST));
        assertEquals(Set.of(LIST), applied.changed());
        assertTrue(book.get(LIST).isEmpty());
    }

    @Test
    void addAppendsAnOpenTaskAndIsCapped() {
        task("Portal");
        task("Farm");
        task("Walls");
        assertEquals("Portal Farm Walls", shape());
        assertTrue(list().tasks().stream().noneMatch(Task::isDone));
        refused(Refusal.LIMIT_TASKS, ANNA, new AddTask(LIST, new UUID(2, 9), "More", Optional.empty()));
    }

    @Test
    void addAfterATaskLandsBehindItsSubtasks() {
        tasks("A", "a1", "B");
        indent("a1");
        book = ((Applied) NoteEdits.apply(book, ANNA, OwnerAccess.INSTANCE, roomyLimits(), NOW,
                new AddTask(LIST, id("C"), "C", Optional.of(id("A"))))).book();
        assertEquals("A -a1 C B", shape());
    }

    @Test
    void taskTextTooLongIsRefused() {
        refused(Refusal.TOO_LONG, ANNA, new AddTask(LIST, new UUID(2, 1), "Thirteen char", Optional.empty()));
        refused(Refusal.EMPTY, ANNA, new AddTask(LIST, new UUID(2, 1), " ", Optional.empty()));
    }

    @Test
    void editTextAndNotes() {
        UUID portal = task("Portal");
        ok(ANNA, new EditText(LIST, portal, "Nether gate"));
        ok(ANNA, new EditNotes(LIST, portal, "obsidian\nflint"));
        Task task = list().task(portal).orElseThrow();
        assertEquals("Nether gate", task.text());
        assertEquals("obsidian\nflint", task.notes());
        refused(Refusal.TOO_LONG, ANNA, new EditNotes(LIST, portal, "x".repeat(21)));
        refused(Refusal.NOT_FOUND, ANNA, new EditText(LIST, new UUID(9, 9), "Gone"));
    }

    @Test
    void deleteParentTakesItsSubtasks() {
        tasks("A", "a1", "a2", "B");
        indent("a1");
        indent("a2");
        ok(ANNA, new DeleteTask(LIST, id("A")));
        assertEquals("B", shape());
    }

    @Test
    void deleteSubtaskLeavesTheRest() {
        tasks("A", "a1", "a2");
        indent("a1");
        indent("a2");
        ok(ANNA, new DeleteTask(LIST, id("a1")));
        assertEquals("A -a2", shape());
    }

    @Test
    void placeMovesATaskDownAndUp() {
        tasks("A", "B", "C");
        place("A", "B", false);
        assertEquals("B A C", shape());
        place("C", null, false);
        assertEquals("C B A", shape());
    }

    @Test
    void placeIntoAndOutOfASubtask() {
        tasks("A", "B", "C");
        place("C", "A", true);
        assertEquals("A -C B", shape());
        place("B", "C", true);
        assertEquals("A -C -B", shape());
        place("C", "B", false);
        assertEquals("A -B C", shape());
    }

    @Test
    void parentCarriesItsSubtasksAndStaysATask() {
        tasks("A", "a1", "B", "b1", "C");
        indent("a1");
        indent("b1");
        place("A", "C", false);
        assertEquals("B -b1 C A -a1", shape());
        place("B", "A", true);
        assertEquals("C A -a1 B -b1", shape());
    }

    @Test
    void taskAfterASubtaskLandsBehindItsParentBlock() {
        tasks("A", "a1", "a2", "B");
        indent("a1");
        indent("a2");
        place("B", "a1", false);
        assertEquals("A -a1 -a2 B", shape());
        place("B", "a1", true);
        assertEquals("A -a1 -B -a2", shape());
    }

    @Test
    void placeInPlaceChangesNothing() {
        tasks("A", "B");
        assertTrue(place("B", "A", false).changed().isEmpty());
        assertTrue(place("A", null, false).changed().isEmpty());
        assertEquals("A B", shape());
    }

    @Test
    void subtaskMovedToTheTopBecomesATask() {
        tasks("A", "a1");
        indent("a1");
        place("a1", null, false);
        assertEquals("a1 A", shape());
    }

    @Test
    void placeRefusals() {
        tasks("A", "a1", "B");
        indent("a1");
        refused(Refusal.NOT_ALLOWED, ANNA, new PlaceTask(LIST, id("B"), Optional.empty(), true));
        refused(Refusal.NOT_FOUND, ANNA, new PlaceTask(LIST, id("B"), Optional.of(new UUID(9, 9)), false));
        refused(Refusal.NOT_FOUND, ANNA, new PlaceTask(LIST, new UUID(9, 9), Optional.empty(), false));
        refused(Refusal.NOT_ALLOWED, ANNA, new PlaceTask(LIST, id("A"), Optional.of(id("a1")), false));
        refused(Refusal.NOT_ALLOWED, ANNA, new PlaceTask(LIST, id("A"), Optional.of(id("A")), false));
    }

    @Test
    void tickRecordsWhoDayAndTime() {
        UUID portal = task("Portal");
        ok(ANNA, new Tick(LIST, portal, true));
        Task task = list().task(portal).orElseThrow();
        assertEquals(Optional.of(new DoneRecord(ANNA, 409, NOW.epochMillis())), task.done());
        assertEquals(0, list().openCount());
    }

    @Test
    void tickingADoneTaskKeepsTheFirstRecord() {
        UUID portal = task("Portal");
        ok(ANNA, new Tick(LIST, portal, true));
        Optional<DoneRecord> first = list().task(portal).orElseThrow().done();
        book = ((Applied) NoteEdits.apply(book, ANNA, OwnerAccess.INSTANCE, LIMITS, new Moment(500, 0), new Tick(LIST, portal, true))).book();
        assertEquals(first, list().task(portal).orElseThrow().done());
    }

    @Test
    void untickClearsTheRecord() {
        UUID portal = task("Portal");
        ok(ANNA, new Tick(LIST, portal, true));
        ok(ANNA, new Tick(LIST, portal, false));
        assertTrue(list().task(portal).orElseThrow().done().isEmpty());
        assertEquals(1, list().openCount());
    }

    @Test
    void claimOnlyOpenAndUnassigned() {
        UUID portal = task("Portal");
        UUID farm = task("Farm");
        ok(ANNA, new Claim(LIST, portal));
        assertEquals(Optional.of(ANNA), list().task(portal).orElseThrow().assignee());
        refused(Refusal.NOT_ALLOWED, ANNA, new Claim(LIST, portal));
        ok(ANNA, new Tick(LIST, farm, true));
        refused(Refusal.NOT_ALLOWED, ANNA, new Claim(LIST, farm));
    }

    @Test
    void setAndClearAssignee() {
        UUID portal = task("Portal");
        ok(ANNA, new SetAssignee(LIST, portal, Optional.of(SEPP)));
        assertEquals(Optional.of(SEPP), list().task(portal).orElseThrow().assignee());
        ok(ANNA, new SetAssignee(LIST, portal, Optional.empty()));
        assertTrue(list().task(portal).orElseThrow().assignee().isEmpty());
        refused(Refusal.TOO_LONG, ANNA, new SetAssignee(LIST, portal, Optional.of(new Person(SEPP.id(), "x".repeat(17)))));
    }

    @Test
    void everyEditOfANonOwnerIsRefused() {
        UUID portal = task("Portal");
        List<NoteEdit> edits = List.of(
                new RenameList(LIST, "Mine"),
                new DeleteList(LIST),
                new AddTask(LIST, new UUID(3, 3), "Sneaky", Optional.empty()),
                new EditText(LIST, portal, "Changed"),
                new EditNotes(LIST, portal, "Notes"),
                new DeleteTask(LIST, portal),
                new PlaceTask(LIST, portal, Optional.empty(), false),
                new Tick(LIST, portal, true),
                new SetAssignee(LIST, portal, Optional.of(SEPP)),
                new Claim(LIST, portal));
        for (NoteEdit edit : edits) {
            EditResult result = apply(SEPP, edit);
            assertInstanceOf(Refused.class, result, edit.toString());
        }
        assertFalse(list().task(portal).orElseThrow().isDone());
    }

    // ---------- sharing ----------

    private static final UUID PARTY = new UUID(9, 1);
    private static final Person MAX = new Person(new UUID(0, 3), "Max");
    private static final Person ADMIN = new Person(new UUID(0, 4), "Admin");

    /** Anna and Sepp share a party, Max is in none, Admin is an op. */
    private static SharedAccess shared() {
        Map<UUID, Viewer> viewers = Map.of(
                ANNA.id(), new Viewer(ANNA.id(), false, Optional.of(PARTY), true),
                SEPP.id(), new Viewer(SEPP.id(), false, Optional.of(PARTY), true),
                MAX.id(), new Viewer(MAX.id(), false, Optional.empty(), true),
                ADMIN.id(), new Viewer(ADMIN.id(), true, Optional.empty(), true));
        return new SharedAccess(viewers::get);
    }

    private EditResult applyShared(Person actor, NoteEdit edit) {
        EditResult result = NoteEdits.apply(book, actor, shared(), LIMITS, NOW, edit);
        if (result instanceof Applied applied) {
            book = applied.book();
        }
        return result;
    }

    @Test
    void onlyTheOwnerShares() {
        ok(ANNA, new SetScope(LIST, Scope.SERVER));
        List<NoteEdit> edits = List.of(
                new SetScope(LIST, Scope.PRIVATE),
                new AddGuest(LIST, MAX, Guest.Right.EDIT),
                new SetGuestRight(LIST, MAX.id(), Guest.Right.EDIT),
                new RemoveGuest(LIST, MAX.id()),
                new DeleteList(LIST));
        for (Person actor : List.of(SEPP, ADMIN)) {
            for (NoteEdit edit : edits) {
                assertEquals(new Refused(Refusal.NOT_ALLOWED), applyShared(actor, edit), actor.name() + " " + edit);
            }
        }
        assertEquals(Scope.SERVER, list().scope());
    }

    @Test
    void teamTakesTheOwnersPartyAndNeedsOne() {
        assertInstanceOf(Applied.class, applyShared(ANNA, new SetScope(LIST, Scope.TEAM)));
        assertEquals(Scope.TEAM, list().scope());
        assertEquals(Optional.of(PARTY), list().team());

        book = new NotesBook(List.of(list().withOwner(MAX).withScope(Scope.PRIVATE, Optional.empty())));
        assertEquals(new Refused(Refusal.NO_PARTY), applyShared(MAX, new SetScope(LIST, Scope.TEAM)));
        assertEquals(new Refused(Refusal.NO_PARTY), apply(MAX, new SetScope(LIST, Scope.TEAM)));
        assertEquals(Scope.PRIVATE, list().scope());
    }

    @Test
    void guestsStayThroughAScopeSwitch() {
        ok(ANNA, new AddGuest(LIST, SEPP, Guest.Right.READ));
        ok(ANNA, new SetScope(LIST, Scope.SERVER));
        applyShared(ANNA, new SetScope(LIST, Scope.TEAM));
        ok(ANNA, new SetScope(LIST, Scope.PRIVATE));
        assertEquals(List.of(new Guest(SEPP, Guest.Right.READ)), list().guests());
        assertTrue(list().team().isEmpty());
    }

    @Test
    void guestsAreAddedChangedAndRemoved() {
        assertEquals(Set.of(LIST), ok(ANNA, new AddGuest(LIST, SEPP, Guest.Right.READ)).changed());
        refused(Refusal.NOT_ALLOWED, ANNA, new AddGuest(LIST, SEPP, Guest.Right.EDIT));
        refused(Refusal.NOT_ALLOWED, ANNA, new AddGuest(LIST, ANNA, Guest.Right.EDIT));
        refused(Refusal.TOO_LONG, ANNA, new AddGuest(LIST, new Person(MAX.id(), "x".repeat(17)), Guest.Right.READ));
        ok(ANNA, new SetGuestRight(LIST, SEPP.id(), Guest.Right.EDIT));
        assertEquals(Optional.of(new Guest(SEPP, Guest.Right.EDIT)), list().guest(SEPP.id()));
        refused(Refusal.NOT_FOUND, ANNA, new SetGuestRight(LIST, MAX.id(), Guest.Right.EDIT));
        ok(ANNA, new RemoveGuest(LIST, SEPP.id()));
        assertTrue(list().guests().isEmpty());
        refused(Refusal.NOT_FOUND, ANNA, new RemoveGuest(LIST, SEPP.id()));
    }

    @Test
    void opEditsServerTasksButCannotDeleteTheList() {
        UUID portal = task("Portal");
        ok(ANNA, new SetScope(LIST, Scope.SERVER));
        assertInstanceOf(Applied.class, applyShared(ADMIN, new EditText(LIST, portal, "Big portal")));
        assertInstanceOf(Applied.class, applyShared(ADMIN, new DeleteTask(LIST, portal)));
        assertEquals(new Refused(Refusal.NOT_ALLOWED), applyShared(ADMIN, new DeleteList(LIST)));
        assertTrue(book.get(LIST).isPresent());
    }

    @Test
    void readerTicksAndClaimsButDoesNotEdit() {
        UUID portal = task("Portal");
        UUID walls = task("Walls");
        ok(ANNA, new SetScope(LIST, Scope.SERVER));
        assertEquals(new Refused(Refusal.NOT_ALLOWED), applyShared(MAX, new EditText(LIST, portal, "Mine")));
        assertEquals(new Refused(Refusal.NOT_ALLOWED), applyShared(MAX, new AddTask(LIST, new UUID(3, 3), "More", Optional.empty())));
        assertInstanceOf(Applied.class, applyShared(MAX, new Tick(LIST, portal, true)));
        assertEquals(MAX, list().task(portal).orElseThrow().done().orElseThrow().by());
        assertInstanceOf(Applied.class, applyShared(MAX, new Claim(LIST, walls)));
        assertEquals(Optional.of(MAX), list().task(walls).orElseThrow().assignee());
    }

    @Test
    void privateListIsInvisibleToAnOp() {
        assertEquals(new Refused(Refusal.NOT_FOUND), applyShared(ADMIN, new Tick(LIST, task("Portal"), true)));
    }

    @Test
    void transferMovesEveryListAndDropsTheNewOwnersGuestEntry() {
        ok(ANNA, new AddGuest(LIST, SEPP, Guest.Right.EDIT));
        ok(ANNA, new AddGuest(LIST, MAX, Guest.Right.READ));
        ok(ANNA, new CreateList(new UUID(1, 2), "Second"));
        ok(SEPP, new CreateList(new UUID(1, 3), "Sepps"));

        Applied moved = NoteEdits.transfer(book, ANNA.id(), SEPP);

        assertEquals(Set.of(LIST, new UUID(1, 2)), moved.changed());
        assertEquals(3, moved.book().ownedBy(SEPP.id()).size());
        assertEquals("Sepp", moved.book().get(LIST).orElseThrow().ownerName());
        assertEquals(List.of(new Guest(MAX, Guest.Right.READ)), moved.book().get(LIST).orElseThrow().guests());
        assertTrue(moved.book().ownedBy(ANNA.id()).isEmpty());
    }

    @Test
    void deletedPartyLeavesPrivateListsWithTheirGuests() {
        ok(ANNA, new AddGuest(LIST, MAX, Guest.Right.READ));
        applyShared(ANNA, new SetScope(LIST, Scope.TEAM));
        ok(ANNA, new CreateList(new UUID(1, 2), "Second"));

        Applied result = NoteEdits.teamDeleted(book, PARTY);

        assertEquals(Set.of(LIST), result.changed());
        NoteList list = result.book().get(LIST).orElseThrow();
        assertEquals(Scope.PRIVATE, list.scope());
        assertTrue(list.team().isEmpty());
        assertEquals(List.of(new Guest(MAX, Guest.Right.READ)), list.guests());
        assertTrue(NoteEdits.teamDeleted(book, new UUID(9, 9)).changed().isEmpty());
    }

    // ---------- goals and spots ----------

    private static final UUID GOAL = new UUID(5, 1);
    private static final Spot TUNNEL = new Spot(-120, 64, 340, "minecraft:the_nether");

    private Task taskOf(UUID taskId) {
        return list().task(taskId).orElseThrow();
    }

    private Goal goalOf(UUID taskId) {
        return taskOf(taskId).goal(GOAL).orElseThrow();
    }

    @Test
    void editorAddsAGoalWithCountZeroAndTheStartingTarget() {
        UUID materials = task("Materials");
        assertEquals(Set.of(LIST), ok(ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:blackstone")).changed());
        assertEquals(new Goal(GOAL, "minecraft:blackstone", NoteEdits.NEW_GOAL_TARGET, 0, ANNA), goalOf(materials));
        refused(Refusal.NOT_ALLOWED, ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:glass"));
        refused(Refusal.EMPTY, ANNA, new AddGoal(LIST, materials, new UUID(5, 2), " "));
        refused(Refusal.TOO_LONG, ANNA, new AddGoal(LIST, materials, new UUID(5, 2), "x".repeat(257)));
        refused(Refusal.NOT_FOUND, ANNA, new AddGoal(LIST, new UUID(7, 7), new UUID(5, 2), "minecraft:glass"));
    }

    @Test
    void editorChangesItemAndTargetAndRemovesTheGoal() {
        UUID materials = task("Materials");
        ok(ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:ice"));
        ok(ANNA, new SetGoalCount(LIST, materials, GOAL, 30));
        ok(ANNA, new SetGoalItem(LIST, materials, GOAL, "minecraft:packed_ice"));
        ok(ANNA, new SetGoalTarget(LIST, materials, GOAL, 512));
        assertEquals(new Goal(GOAL, "minecraft:packed_ice", 512, 30, ANNA), goalOf(materials));
        ok(ANNA, new RemoveGoal(LIST, materials, GOAL));
        assertTrue(taskOf(materials).goals().isEmpty());
        refused(Refusal.NOT_FOUND, ANNA, new RemoveGoal(LIST, materials, GOAL));
        refused(Refusal.NOT_FOUND, ANNA, new SetGoalTarget(LIST, materials, GOAL, 5));
    }

    @Test
    void countsAndTargetsAreClamped() {
        UUID materials = task("Materials");
        ok(ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:glass"));
        ok(ANNA, new SetGoalCount(LIST, materials, GOAL, -5));
        ok(ANNA, new SetGoalTarget(LIST, materials, GOAL, 0));
        assertEquals(0, goalOf(materials).count());
        assertEquals(1, goalOf(materials).target());
        ok(ANNA, new SetGoalCount(LIST, materials, GOAL, 2_000_000));
        ok(ANNA, new SetGoalTarget(LIST, materials, GOAL, Integer.MAX_VALUE));
        assertEquals(NoteEdits.MAX_AMOUNT, goalOf(materials).count());
        assertEquals(NoteEdits.MAX_AMOUNT, goalOf(materials).target());
        assertTrue(goalOf(materials).reached());
        assertFalse(taskOf(materials).isDone());
    }

    @Test
    void goalsAreCappedPerTask() {
        UUID materials = task("Materials");
        Limits two = new Limits(2, 3, 10, 12, 20, 2);
        for (int i = 0; i < 2; i++) {
            book = ((Applied) NoteEdits.apply(book, ANNA, OwnerAccess.INSTANCE, two, NOW,
                    new AddGoal(LIST, materials, new UUID(6, i), "minecraft:glass"))).book();
        }
        assertEquals(new Refused(Refusal.LIMIT_GOALS), NoteEdits.apply(book, ANNA, OwnerAccess.INSTANCE, two, NOW,
                new AddGoal(LIST, materials, new UUID(6, 9), "minecraft:glass")));
    }

    @Test
    void readerTypesCountsButDoesNotChangeGoals() {
        UUID materials = task("Materials");
        ok(ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:packed_ice"));
        ok(ANNA, new SetGoalCount(LIST, materials, GOAL, 210));
        ok(ANNA, new AddGuest(LIST, MAX, Guest.Right.READ));

        assertInstanceOf(Applied.class, applyShared(MAX, new SetGoalCount(LIST, materials, GOAL, 300)));
        assertEquals(300, goalOf(materials).count());
        assertEquals(MAX, goalOf(materials).lastBy());
        for (NoteEdit edit : List.of(
                new AddGoal(LIST, materials, new UUID(5, 2), "minecraft:glass"),
                new SetGoalItem(LIST, materials, GOAL, "minecraft:glass"),
                new SetGoalTarget(LIST, materials, GOAL, 1),
                new RemoveGoal(LIST, materials, GOAL),
                new SetSpot(LIST, materials, Optional.of(TUNNEL)))) {
            assertEquals(new Refused(Refusal.NOT_ALLOWED), applyShared(MAX, edit), edit.toString());
        }
    }

    @Test
    void strangerMayNotTypeACount() {
        UUID materials = task("Materials");
        ok(ANNA, new AddGoal(LIST, materials, GOAL, "minecraft:glass"));
        assertEquals(new Refused(Refusal.NOT_FOUND), applyShared(MAX, new SetGoalCount(LIST, materials, GOAL, 3)));
        refused(Refusal.NOT_FOUND, SEPP, new SetGoalCount(LIST, materials, GOAL, 3));
        assertEquals(0, goalOf(materials).count());
    }

    @Test
    void spotIsSetAndCleared() {
        UUID tunnel = task("Tunnel");
        ok(ANNA, new SetSpot(LIST, tunnel, Optional.of(TUNNEL)));
        assertEquals(Optional.of(TUNNEL), taskOf(tunnel).spot());
        ok(ANNA, new SetSpot(LIST, tunnel, Optional.empty()));
        assertTrue(taskOf(tunnel).spot().isEmpty());
        refused(Refusal.NOT_ALLOWED, ANNA, new SetSpot(LIST, tunnel, Optional.of(new Spot(0, 0, 0, ""))));
        refused(Refusal.NOT_ALLOWED, ANNA, new SetSpot(LIST, tunnel, Optional.of(new Spot(40_000_000, 0, 0, "minecraft:overworld"))));
    }

    @Test
    void placeNameIsKeptTidiedAndCapped() {
        UUID tunnel = task("Tunnel");
        ok(ANNA, new SetSpot(LIST, tunnel, Optional.of(TUNNEL.withName(" Portal room "))));
        assertEquals("Portal room", taskOf(tunnel).spot().orElseThrow().name());
        refused(Refusal.TOO_LONG, ANNA, new SetSpot(LIST, tunnel, Optional.of(TUNNEL.withName("x".repeat(NoteEdits.MAX_PLACE_NAME + 1)))));
        ok(ANNA, new SetSpot(LIST, tunnel, Optional.of(TUNNEL.withName("x".repeat(NoteEdits.MAX_PLACE_NAME)))));
    }

    @Test
    void taskIsAddedWithASpot() {
        UUID id = new UUID(3, 9);
        ok(ANNA, new AddTask(LIST, id, "Clear tunnel", Optional.empty(), Optional.of(TUNNEL)));
        assertEquals(Optional.of(TUNNEL), taskOf(id).spot());
        refused(Refusal.NOT_ALLOWED, ANNA, new AddTask(LIST, new UUID(3, 8), "Far", Optional.empty(),
                Optional.of(new Spot(0, 0, 0, " "))));
    }
}
