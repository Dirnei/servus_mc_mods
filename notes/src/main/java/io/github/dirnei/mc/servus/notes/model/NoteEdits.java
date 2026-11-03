package io.github.dirnei.mc.servus.notes.model;

import io.github.dirnei.mc.servus.notes.model.EditResult.Applied;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Checks an edit against the actor's rights and the limits and applies it to a book. */
public final class NoteEdits {

    /** The longest player name an assignee may carry. */
    public static final int MAX_PLAYER_NAME = 16;

    /** The most guests one list may have. */
    public static final int MAX_GUESTS = 64;

    /** The highest count or target a goal takes. */
    public static final int MAX_AMOUNT = 1_000_000;

    /** The target a new goal starts with. */
    public static final int NEW_GOAL_TARGET = 64;

    /** The longest item or dimension id an edit may carry. */
    public static final int MAX_ID_LENGTH = 256;
    /** The longest name of a place. */
    public static final int MAX_PLACE_NAME = 32;

    private static final int MAX_COORDINATE = 30_000_000;

    private NoteEdits() {
    }

    /** Applies {@code edit} by {@code actor} at {@code now}, or says why not. */
    public static EditResult apply(NotesBook book, Person actor, Access access, Limits limits, Moment now, NoteEdit edit) {
        if (edit instanceof NoteEdit.CreateList create) {
            return createList(book, actor, limits, create);
        }
        Optional<NoteList> found = book.get(edit.listId());
        if (found.isEmpty()) {
            return refused(Refusal.NOT_FOUND);
        }
        NoteList list = found.get();
        if (!access.canSee(actor.id(), list)) {
            return refused(Refusal.NOT_FOUND);
        }
        UUID id = actor.id();
        return switch (edit) {
            case NoteEdit.CreateList create -> throw new IllegalStateException("handled above");
            case NoteEdit.RenameList rename -> allowed(access.canEdit(id, list), () -> ifText(rename.name(), limits.maxNameLength(),
                    () -> changed(book, list, list.withName(oneLine(rename.name())))));
            case NoteEdit.DeleteList delete -> allowed(access.canManage(id, list),
                    () -> new Applied(book.without(list.id()), Set.of(list.id())));
            case NoteEdit.AddTask add -> allowed(access.canEdit(id, list), () -> addTask(book, list, limits, add));
            case NoteEdit.EditText text -> allowed(access.canEdit(id, list), () -> ifText(text.text(), limits.maxTaskLength(),
                    () -> updateTask(book, list, text.taskId(), task -> task.withText(oneLine(text.text())))));
            case NoteEdit.EditNotes notes -> allowed(access.canEdit(id, list), () -> notes.notes().length() > limits.maxNotesLength()
                    ? refused(Refusal.TOO_LONG)
                    : updateTask(book, list, notes.taskId(), task -> task.withNotes(notes.notes().strip())));
            case NoteEdit.DeleteTask delete -> allowed(access.canEdit(id, list),
                    () -> reorder(book, list, delete.taskId(), NoteEdits::deleteTask));
            case NoteEdit.PlaceTask place -> allowed(access.canEdit(id, list), () -> placeRefusal(list.tasks(), place)
                    .map(NoteEdits::refused)
                    .orElseGet(() -> changed(book, list, list.withTasks(placed(list.tasks(), place)))));
            case NoteEdit.Tick tick -> allowed(access.canTick(id, list),
                    () -> updateTask(book, list, tick.taskId(), task -> ticked(task, tick.done(), actor, now)));
            case NoteEdit.SetAssignee assign -> allowed(access.canEdit(id, list), () -> setAssignee(book, list, assign));
            case NoteEdit.Claim claim -> allowed(access.canTick(id, list), () -> claim(book, list, actor, claim));
            case NoteEdit.SetScope scope -> allowed(access.canManage(id, list), () -> setScope(book, list, access.party(id), scope.scope()));
            case NoteEdit.AddGuest add -> allowed(access.canManage(id, list), () -> addGuest(book, list, add));
            case NoteEdit.SetGuestRight set -> allowed(access.canManage(id, list),
                    () -> updateGuest(book, list, set.guestId(), guest -> Optional.of(guest.withRight(set.right()))));
            case NoteEdit.RemoveGuest remove -> allowed(access.canManage(id, list),
                    () -> updateGuest(book, list, remove.guestId(), guest -> Optional.empty()));
            case NoteEdit.AddGoal add -> allowed(access.canEdit(id, list), () -> addGoal(book, list, actor, limits, add));
            case NoteEdit.SetGoalItem set -> allowed(access.canEdit(id, list), () -> ifItem(set.itemId(),
                    () -> updateGoal(book, list, set.taskId(), set.goalId(), goal -> goal.withItem(set.itemId()))));
            case NoteEdit.SetGoalTarget set -> allowed(access.canEdit(id, list),
                    () -> updateGoal(book, list, set.taskId(), set.goalId(), goal -> goal.withTarget(target(set.target()))));
            case NoteEdit.SetGoalCount set -> allowed(access.canTick(id, list),
                    () -> updateGoal(book, list, set.taskId(), set.goalId(), goal -> goal.withCount(amount(set.count()), actor)));
            case NoteEdit.RemoveGoal remove -> allowed(access.canEdit(id, list), () -> removeGoal(book, list, remove));
            case NoteEdit.SetSpot set -> allowed(access.canEdit(id, list), () -> spotRefusal(set.spot())
                    .map(NoteEdits::refused)
                    .orElseGet(() -> updateTask(book, list, set.taskId(), task -> task.withSpot(set.spot().map(NoteEdits::tidied)))));
        };
    }

    /** A count clamped to 0 to {@link #MAX_AMOUNT}. */
    public static int amount(int value) {
        return Math.clamp(value, 0, MAX_AMOUNT);
    }

    /** A target clamped to 1 to {@link #MAX_AMOUNT}, so a goal never counts as reached with nothing needed. */
    public static int target(int value) {
        return Math.clamp(value, 1, MAX_AMOUNT);
    }

    private static boolean validSpot(Spot spot) {
        return Math.abs(spot.x()) <= MAX_COORDINATE && Math.abs(spot.y()) <= MAX_COORDINATE && Math.abs(spot.z()) <= MAX_COORDINATE
                && !spot.dimension().isBlank() && spot.dimension().length() <= MAX_ID_LENGTH;
    }

    /** Why a spot is refused: out of the world or without a dimension, or a name that is too long. */
    private static Optional<Refusal> spotRefusal(Optional<Spot> spot) {
        if (spot.isEmpty()) {
            return Optional.empty();
        }
        if (!validSpot(spot.get())) {
            return Optional.of(Refusal.NOT_ALLOWED);
        }
        if (oneLine(spot.get().name()).length() > MAX_PLACE_NAME) {
            return Optional.of(Refusal.TOO_LONG);
        }
        return Optional.empty();
    }

    private static Spot tidied(Spot spot) {
        return spot.withName(oneLine(spot.name()));
    }

    private static EditResult ifItem(String itemId, Supplier<EditResult> edit) {
        if (itemId.isBlank()) {
            return refused(Refusal.EMPTY);
        }
        if (itemId.length() > MAX_ID_LENGTH) {
            return refused(Refusal.TOO_LONG);
        }
        return edit.get();
    }

    private static EditResult withTask(NoteList list, UUID taskId, Function<Task, EditResult> edit) {
        return list.task(taskId).map(edit).orElseGet(() -> refused(Refusal.NOT_FOUND));
    }

    private static EditResult addGoal(NotesBook book, NoteList list, Person actor, Limits limits, NoteEdit.AddGoal add) {
        return ifItem(add.itemId(), () -> withTask(list, add.taskId(), task -> {
            if (task.goal(add.goalId()).isPresent()) {
                return refused(Refusal.NOT_ALLOWED);
            }
            if (task.goals().size() >= limits.maxGoalsPerTask()) {
                return refused(Refusal.LIMIT_GOALS);
            }
            List<Goal> goals = new ArrayList<>(task.goals());
            goals.add(new Goal(add.goalId(), add.itemId(), NEW_GOAL_TARGET, 0, actor));
            return updateTask(book, list, task.id(), found -> found.withGoals(goals));
        }));
    }

    private static EditResult removeGoal(NotesBook book, NoteList list, NoteEdit.RemoveGoal remove) {
        return withTask(list, remove.taskId(), task -> task.goal(remove.goalId()).isEmpty() ? refused(Refusal.NOT_FOUND)
                : updateTask(book, list, task.id(), found -> found.withGoals(found.goals().stream()
                        .filter(goal -> !goal.id().equals(remove.goalId())).toList())));
    }

    private static EditResult updateGoal(NotesBook book, NoteList list, UUID taskId, UUID goalId, UnaryOperator<Goal> change) {
        return withTask(list, taskId, task -> task.goal(goalId).isEmpty() ? refused(Refusal.NOT_FOUND)
                : updateTask(book, list, taskId, found -> found.withGoals(found.goals().stream()
                        .map(goal -> goal.id().equals(goalId) ? change.apply(goal) : goal).toList())));
    }

    /** Hands every list {@code from} owns to {@code to}; a guest entry of {@code to} on a moved list is dropped. No limits apply. */
    public static Applied transfer(NotesBook book, UUID from, Person to) {
        NotesBook result = book;
        Set<UUID> changed = new HashSet<>();
        for (NoteList list : book.ownedBy(from)) {
            result = result.with(list.withOwner(to));
            changed.add(list.id());
        }
        return new Applied(result, changed);
    }

    /** Turns every Team list of the deleted party {@code team} into a private list of its owner, guests kept. */
    public static Applied teamDeleted(NotesBook book, UUID team) {
        NotesBook result = book;
        Set<UUID> changed = new HashSet<>();
        for (NoteList list : book.all()) {
            if (list.scope() == Scope.TEAM && list.team().equals(Optional.of(team))) {
                result = result.with(list.withScope(Scope.PRIVATE, Optional.empty()));
                changed.add(list.id());
            }
        }
        return new Applied(result, changed);
    }

    private static EditResult setScope(NotesBook book, NoteList list, Optional<UUID> party, Scope scope) {
        if (scope == Scope.TEAM && party.isEmpty()) {
            return refused(Refusal.NO_PARTY);
        }
        return changed(book, list, list.withScope(scope, scope == Scope.TEAM ? party : Optional.empty()));
    }

    private static EditResult addGuest(NotesBook book, NoteList list, NoteEdit.AddGuest add) {
        Person guest = add.guest();
        if (guest.id().equals(list.owner()) || list.guest(guest.id()).isPresent()) {
            return refused(Refusal.NOT_ALLOWED);
        }
        if (guest.name().isBlank()) {
            return refused(Refusal.EMPTY);
        }
        if (guest.name().length() > MAX_PLAYER_NAME) {
            return refused(Refusal.TOO_LONG);
        }
        if (list.guests().size() >= MAX_GUESTS) {
            return refused(Refusal.LIMIT_GUESTS);
        }
        List<Guest> guests = new ArrayList<>(list.guests());
        guests.add(new Guest(guest, add.right()));
        return changed(book, list, list.withGuests(guests));
    }

    /** Replaces the guest entry of {@code guestId} with what {@code change} returns, or removes it when that is empty. */
    private static EditResult updateGuest(NotesBook book, NoteList list, UUID guestId, Function<Guest, Optional<Guest>> change) {
        if (list.guest(guestId).isEmpty()) {
            return refused(Refusal.NOT_FOUND);
        }
        List<Guest> guests = new ArrayList<>();
        for (Guest guest : list.guests()) {
            if (guest.who().id().equals(guestId)) {
                change.apply(guest).ifPresent(guests::add);
            } else {
                guests.add(guest);
            }
        }
        return changed(book, list, list.withGuests(guests));
    }

    private static EditResult allowed(boolean right, Supplier<EditResult> edit) {
        return right ? edit.get() : refused(Refusal.NOT_ALLOWED);
    }

    private static EditResult ifText(String text, int maxLength, Supplier<EditResult> edit) {
        return checkedText(text, maxLength).map(NoteEdits::refused).orElseGet(edit);
    }

    private static EditResult createList(NotesBook book, Person actor, Limits limits, NoteEdit.CreateList create) {
        if (book.get(create.listId()).isPresent()) {
            return refused(Refusal.NOT_ALLOWED);
        }
        if (book.ownedBy(actor.id()).size() >= limits.maxListsPerPlayer()) {
            return refused(Refusal.LIMIT_LISTS);
        }
        Optional<Refusal> bad = checkedText(create.name(), limits.maxNameLength());
        if (bad.isPresent()) {
            return refused(bad.get());
        }
        NoteList list = NoteList.create(create.listId(), oneLine(create.name()), actor);
        return new Applied(book.with(list), Set.of(list.id()));
    }

    private static EditResult addTask(NotesBook book, NoteList list, Limits limits, NoteEdit.AddTask add) {
        if (list.indexOf(add.taskId()) >= 0) {
            return refused(Refusal.NOT_ALLOWED);
        }
        if (list.tasks().size() >= limits.maxTasksPerList()) {
            return refused(Refusal.LIMIT_TASKS);
        }
        Optional<Refusal> bad = checkedText(add.text(), limits.maxTaskLength());
        if (bad.isPresent()) {
            return refused(bad.get());
        }
        Optional<Refusal> badSpot = spotRefusal(add.spot());
        if (badSpot.isPresent()) {
            return refused(badSpot.get());
        }
        List<Task> tasks = new ArrayList<>(list.tasks());
        Task task = Task.open(add.taskId(), oneLine(add.text())).withSpot(add.spot().map(NoteEdits::tidied));
        int after = add.afterId().map(list::indexOf).orElse(-1);
        if (after < 0) {
            tasks.add(task);
        } else if (tasks.get(after).sub()) {
            tasks.add(after + 1, task.withSub(true));
        } else {
            tasks.add(blockEnd(tasks, after), task);
        }
        return changed(book, list, list.withTasks(tasks));
    }

    private static EditResult setAssignee(NotesBook book, NoteList list, NoteEdit.SetAssignee assign) {
        if (assign.assignee().map(person -> person.name().length() > MAX_PLAYER_NAME).orElse(false)) {
            return refused(Refusal.TOO_LONG);
        }
        return updateTask(book, list, assign.taskId(), task -> task.withAssignee(assign.assignee()));
    }

    private static EditResult claim(NotesBook book, NoteList list, Person actor, NoteEdit.Claim claim) {
        Optional<Task> task = list.task(claim.taskId());
        if (task.isEmpty()) {
            return refused(Refusal.NOT_FOUND);
        }
        if (task.get().isDone() || task.get().assignee().isPresent()) {
            return refused(Refusal.NOT_ALLOWED);
        }
        return updateTask(book, list, claim.taskId(), open -> open.withAssignee(Optional.of(actor)));
    }

    private static Task ticked(Task task, boolean done, Person actor, Moment now) {
        if (done == task.isDone()) {
            return task;
        }
        return task.withDone(done ? Optional.of(new DoneRecord(actor, now.day(), now.epochMillis())) : Optional.empty());
    }

    private static EditResult updateTask(NotesBook book, NoteList list, UUID taskId, UnaryOperator<Task> change) {
        return reorder(book, list, taskId, (tasks, index) -> {
            tasks.set(index, change.apply(tasks.get(index)));
            return tasks;
        });
    }

    @FunctionalInterface
    private interface TaskChange {
        List<Task> apply(List<Task> tasks, int index);
    }

    private static EditResult reorder(NotesBook book, NoteList list, UUID taskId, TaskChange change) {
        int index = list.indexOf(taskId);
        if (index < 0) {
            return refused(Refusal.NOT_FOUND);
        }
        List<Task> tasks = change.apply(new ArrayList<>(list.tasks()), index);
        return changed(book, list, list.withTasks(normalised(tasks)));
    }

    private static List<Task> deleteTask(List<Task> tasks, int index) {
        int end = tasks.get(index).sub() ? index + 1 : blockEnd(tasks, index);
        tasks.subList(index, end).clear();
        return tasks;
    }

    /** Why {@code place} cannot be done on {@code tasks}: an unknown task or anchor, an anchor inside the moved block, a subtask at the top. */
    public static Optional<Refusal> placeRefusal(List<Task> tasks, NoteEdit.PlaceTask place) {
        int index = indexOf(tasks, place.taskId());
        if (index < 0) {
            return Optional.of(Refusal.NOT_FOUND);
        }
        int end = movedEnd(tasks, index);
        if (place.afterId().isEmpty()) {
            return place.sub() && end - index == 1 ? Optional.of(Refusal.NOT_ALLOWED) : Optional.empty();
        }
        int after = indexOf(tasks, place.afterId().get());
        if (after < 0) {
            return Optional.of(Refusal.NOT_FOUND);
        }
        return after >= index && after < end ? Optional.of(Refusal.NOT_ALLOWED) : Optional.empty();
    }

    /**
     * The tasks after {@code place}, which {@link #placeRefusal} allows: the task and, when it is not a subtask, its
     * subtasks are taken out and put back after the anchor. A subtask goes right after the anchor; a task goes behind
     * the anchor's whole block, so it never takes over the anchor's later subtasks. A task with subtasks stays a task.
     */
    public static List<Task> placed(List<Task> tasks, NoteEdit.PlaceTask place) {
        int index = indexOf(tasks, place.taskId());
        int end = movedEnd(tasks, index);
        List<Task> block = new ArrayList<>(tasks.subList(index, end));
        List<Task> rest = new ArrayList<>(tasks.subList(0, index));
        rest.addAll(tasks.subList(end, tasks.size()));
        boolean sub = place.sub() && block.size() == 1 && place.afterId().isPresent();
        block.set(0, block.getFirst().withSub(sub));
        int at = 0;
        if (place.afterId().isPresent()) {
            int after = indexOf(rest, place.afterId().get());
            at = sub ? after + 1 : blockEnd(rest, blockStart(rest, after));
        }
        rest.addAll(at, block);
        return normalised(rest);
    }

    private static int indexOf(List<Task> tasks, UUID taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(taskId)) {
                return i;
            }
        }
        return -1;
    }

    /** The index just past what moves with the task at {@code index}: its subtasks unless it is one. */
    private static int movedEnd(List<Task> tasks, int index) {
        return tasks.get(index).sub() ? index + 1 : blockEnd(tasks, index);
    }

    /** The index of the task that heads the block holding {@code index}. */
    private static int blockStart(List<Task> tasks, int index) {
        int start = index;
        while (start > 0 && tasks.get(start).sub()) {
            start--;
        }
        return start;
    }

    /** The index just past the subtasks that follow {@code start}. */
    private static int blockEnd(List<Task> tasks, int start) {
        int end = start + 1;
        while (end < tasks.size() && tasks.get(end).sub()) {
            end++;
        }
        return end;
    }

    private static List<Task> normalised(List<Task> tasks) {
        if (!tasks.isEmpty() && tasks.getFirst().sub()) {
            tasks.set(0, tasks.getFirst().withSub(false));
        }
        return tasks;
    }

    private static EditResult changed(NotesBook book, NoteList before, NoteList after) {
        if (before.equals(after)) {
            return new Applied(book, Set.of());
        }
        return new Applied(book.with(after), Set.of(after.id()));
    }

    private static Optional<Refusal> checkedText(String text, int maxLength) {
        String clean = oneLine(text);
        if (clean.isEmpty()) {
            return Optional.of(Refusal.EMPTY);
        }
        if (clean.length() > maxLength) {
            return Optional.of(Refusal.TOO_LONG);
        }
        return Optional.empty();
    }

    /** The text on one line without surrounding blanks. */
    private static String oneLine(String text) {
        return text.replaceAll("[\\r\\n\\t]+", " ").strip();
    }

    private static EditResult refused(Refusal reason) {
        return new EditResult.Refused(reason);
    }
}
