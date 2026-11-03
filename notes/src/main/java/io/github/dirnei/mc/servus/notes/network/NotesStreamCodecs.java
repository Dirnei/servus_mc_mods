package io.github.dirnei.mc.servus.notes.network;

import io.github.dirnei.mc.servus.notes.model.DoneRecord;
import io.github.dirnei.mc.servus.notes.model.Goal;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.Limits;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Rights;
import io.github.dirnei.mc.servus.notes.model.Scope;
import io.github.dirnei.mc.servus.notes.model.Spot;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Network codecs of the notes model. */
public final class NotesStreamCodecs {

    /** The longest string a notes payload carries; the server's limits are far below it. */
    private static final int MAX_STRING = 8192;

    public static final StreamCodec<FriendlyByteBuf, Person> PERSON = StreamCodec.of(
            (buf, person) -> {
                buf.writeUUID(person.id());
                buf.writeUtf(person.name(), MAX_STRING);
            },
            buf -> new Person(buf.readUUID(), buf.readUtf(MAX_STRING)));

    public static final StreamCodec<FriendlyByteBuf, DoneRecord> DONE = StreamCodec.of(
            (buf, done) -> {
                PERSON.encode(buf, done.by());
                buf.writeVarLong(done.day());
                buf.writeLong(done.epochMillis());
            },
            buf -> new DoneRecord(PERSON.decode(buf), buf.readVarLong(), buf.readLong()));

    public static final StreamCodec<FriendlyByteBuf, Goal> GOAL = StreamCodec.of(
            (buf, goal) -> {
                buf.writeUUID(goal.id());
                buf.writeUtf(goal.itemId(), MAX_STRING);
                buf.writeVarInt(goal.target());
                buf.writeVarInt(goal.count());
                PERSON.encode(buf, goal.lastBy());
            },
            buf -> new Goal(buf.readUUID(), buf.readUtf(MAX_STRING), buf.readVarInt(), buf.readVarInt(), PERSON.decode(buf)));

    public static final StreamCodec<FriendlyByteBuf, Spot> SPOT = StreamCodec.of(
            (buf, spot) -> {
                buf.writeVarInt(spot.x());
                buf.writeVarInt(spot.y());
                buf.writeVarInt(spot.z());
                buf.writeUtf(spot.dimension(), MAX_STRING);
                buf.writeUtf(spot.name(), MAX_STRING);
            },
            buf -> new Spot(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(MAX_STRING), buf.readUtf(MAX_STRING)));

    public static final StreamCodec<FriendlyByteBuf, Task> TASK = StreamCodec.of(
            (buf, task) -> {
                buf.writeUUID(task.id());
                buf.writeUtf(task.text(), MAX_STRING);
                buf.writeUtf(task.notes(), MAX_STRING);
                buf.writeBoolean(task.sub());
                buf.writeOptional(task.assignee(), PERSON);
                buf.writeOptional(task.done(), DONE);
                writeList(buf, task.goals(), GOAL);
                buf.writeOptional(task.spot(), SPOT);
            },
            buf -> new Task(buf.readUUID(), buf.readUtf(MAX_STRING), buf.readUtf(MAX_STRING), buf.readBoolean(),
                    buf.readOptional(PERSON), buf.readOptional(DONE), readList(buf, GOAL), buf.readOptional(SPOT)));

    public static final StreamCodec<FriendlyByteBuf, Guest> GUEST = StreamCodec.of(
            (buf, guest) -> {
                PERSON.encode(buf, guest.who());
                buf.writeEnum(guest.right());
            },
            buf -> new Guest(PERSON.decode(buf), buf.readEnum(Guest.Right.class)));

    public static final StreamCodec<FriendlyByteBuf, NoteList> LIST = StreamCodec.of(
            (buf, list) -> {
                buf.writeUUID(list.id());
                buf.writeUtf(list.name(), MAX_STRING);
                buf.writeUUID(list.owner());
                buf.writeUtf(list.ownerName(), MAX_STRING);
                writeList(buf, list.tasks(), TASK);
                buf.writeEnum(list.scope());
                buf.writeOptional(list.team(), UUIDUtil.STREAM_CODEC);
                writeList(buf, list.guests(), GUEST);
            },
            buf -> {
                UUID id = buf.readUUID();
                String name = buf.readUtf(MAX_STRING);
                UUID owner = buf.readUUID();
                String ownerName = buf.readUtf(MAX_STRING);
                List<Task> tasks = readList(buf, TASK);
                Scope scope = buf.readEnum(Scope.class);
                Optional<UUID> team = buf.readOptional(UUIDUtil.STREAM_CODEC);
                return new NoteList(id, name, owner, ownerName, tasks, scope, team, readList(buf, GUEST));
            });

    public static final StreamCodec<FriendlyByteBuf, Rights> RIGHTS = StreamCodec.of(
            (buf, rights) -> buf.writeByte((rights.see() ? 1 : 0) | (rights.edit() ? 2 : 0)
                    | (rights.tick() ? 4 : 0) | (rights.manage() ? 8 : 0)),
            buf -> {
                byte bits = buf.readByte();
                return new Rights((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
            });

    public static final StreamCodec<FriendlyByteBuf, ListView> VIEW = StreamCodec.of(
            (buf, view) -> {
                LIST.encode(buf, view.list());
                RIGHTS.encode(buf, view.rights());
            },
            buf -> new ListView(LIST.decode(buf), RIGHTS.decode(buf)));

    public static final StreamCodec<FriendlyByteBuf, Limits> LIMITS = StreamCodec.of(
            (buf, limits) -> {
                buf.writeVarInt(limits.maxListsPerPlayer());
                buf.writeVarInt(limits.maxTasksPerList());
                buf.writeVarInt(limits.maxNameLength());
                buf.writeVarInt(limits.maxTaskLength());
                buf.writeVarInt(limits.maxNotesLength());
                buf.writeVarInt(limits.maxGoalsPerTask());
            },
            buf -> new Limits(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    /** Dispatches on a byte tag per edit record; the tags are the order of the cases below and must stay stable within a version. */
    public static final StreamCodec<FriendlyByteBuf, NoteEdit> EDIT = StreamCodec.of(NotesStreamCodecs::writeEdit, NotesStreamCodecs::readEdit);

    private NotesStreamCodecs() {
    }

    private static void writeEdit(FriendlyByteBuf buf, NoteEdit edit) {
        switch (edit) {
            case NoteEdit.CreateList e -> head(buf, 0, e).writeUtf(e.name(), MAX_STRING);
            case NoteEdit.RenameList e -> head(buf, 1, e).writeUtf(e.name(), MAX_STRING);
            case NoteEdit.DeleteList e -> head(buf, 2, e);
            case NoteEdit.AddTask e -> {
                head(buf, 3, e).writeUUID(e.taskId()).writeUtf(e.text(), MAX_STRING);
                buf.writeOptional(e.afterId(), UUIDUtil.STREAM_CODEC);
                buf.writeOptional(e.spot(), SPOT);
            }
            case NoteEdit.EditText e -> head(buf, 4, e).writeUUID(e.taskId()).writeUtf(e.text(), MAX_STRING);
            case NoteEdit.EditNotes e -> head(buf, 5, e).writeUUID(e.taskId()).writeUtf(e.notes(), MAX_STRING);
            case NoteEdit.DeleteTask e -> head(buf, 6, e).writeUUID(e.taskId());
            case NoteEdit.PlaceTask e -> {
                head(buf, 23, e).writeUUID(e.taskId());
                buf.writeOptional(e.afterId(), UUIDUtil.STREAM_CODEC);
                buf.writeBoolean(e.sub());
            }
            case NoteEdit.Tick e -> head(buf, 10, e).writeUUID(e.taskId()).writeBoolean(e.done());
            case NoteEdit.SetAssignee e -> {
                head(buf, 11, e).writeUUID(e.taskId());
                buf.writeOptional(e.assignee(), PERSON);
            }
            case NoteEdit.Claim e -> head(buf, 12, e).writeUUID(e.taskId());
            case NoteEdit.SetScope e -> head(buf, 13, e).writeEnum(e.scope());
            case NoteEdit.AddGuest e -> {
                head(buf, 14, e);
                PERSON.encode(buf, e.guest());
                buf.writeEnum(e.right());
            }
            case NoteEdit.SetGuestRight e -> head(buf, 15, e).writeUUID(e.guestId()).writeEnum(e.right());
            case NoteEdit.RemoveGuest e -> head(buf, 16, e).writeUUID(e.guestId());
            case NoteEdit.AddGoal e -> head(buf, 17, e).writeUUID(e.taskId()).writeUUID(e.goalId()).writeUtf(e.itemId(), MAX_STRING);
            case NoteEdit.SetGoalItem e -> head(buf, 18, e).writeUUID(e.taskId()).writeUUID(e.goalId()).writeUtf(e.itemId(), MAX_STRING);
            case NoteEdit.SetGoalTarget e -> head(buf, 19, e).writeUUID(e.taskId()).writeUUID(e.goalId()).writeVarInt(e.target());
            case NoteEdit.SetGoalCount e -> head(buf, 20, e).writeUUID(e.taskId()).writeUUID(e.goalId()).writeVarInt(e.count());
            case NoteEdit.RemoveGoal e -> head(buf, 21, e).writeUUID(e.taskId()).writeUUID(e.goalId());
            case NoteEdit.SetSpot e -> {
                head(buf, 22, e).writeUUID(e.taskId());
                buf.writeOptional(e.spot(), SPOT);
            }
        }
    }

    private static FriendlyByteBuf head(FriendlyByteBuf buf, int tag, NoteEdit edit) {
        buf.writeByte(tag);
        buf.writeUUID(edit.listId());
        return buf;
    }

    private static NoteEdit readEdit(FriendlyByteBuf buf) {
        int tag = buf.readByte();
        UUID list = buf.readUUID();
        return switch (tag) {
            case 0 -> new NoteEdit.CreateList(list, buf.readUtf(MAX_STRING));
            case 1 -> new NoteEdit.RenameList(list, buf.readUtf(MAX_STRING));
            case 2 -> new NoteEdit.DeleteList(list);
            case 3 -> new NoteEdit.AddTask(list, buf.readUUID(), buf.readUtf(MAX_STRING), buf.readOptional(UUIDUtil.STREAM_CODEC),
                    buf.readOptional(SPOT));
            case 4 -> new NoteEdit.EditText(list, buf.readUUID(), buf.readUtf(MAX_STRING));
            case 5 -> new NoteEdit.EditNotes(list, buf.readUUID(), buf.readUtf(MAX_STRING));
            case 6 -> new NoteEdit.DeleteTask(list, buf.readUUID());
            case 10 -> new NoteEdit.Tick(list, buf.readUUID(), buf.readBoolean());
            case 11 -> new NoteEdit.SetAssignee(list, buf.readUUID(), buf.readOptional(PERSON));
            case 12 -> new NoteEdit.Claim(list, buf.readUUID());
            case 13 -> new NoteEdit.SetScope(list, buf.readEnum(Scope.class));
            case 14 -> new NoteEdit.AddGuest(list, PERSON.decode(buf), buf.readEnum(Guest.Right.class));
            case 15 -> new NoteEdit.SetGuestRight(list, buf.readUUID(), buf.readEnum(Guest.Right.class));
            case 16 -> new NoteEdit.RemoveGuest(list, buf.readUUID());
            case 17 -> new NoteEdit.AddGoal(list, buf.readUUID(), buf.readUUID(), buf.readUtf(MAX_STRING));
            case 18 -> new NoteEdit.SetGoalItem(list, buf.readUUID(), buf.readUUID(), buf.readUtf(MAX_STRING));
            case 19 -> new NoteEdit.SetGoalTarget(list, buf.readUUID(), buf.readUUID(), buf.readVarInt());
            case 20 -> new NoteEdit.SetGoalCount(list, buf.readUUID(), buf.readUUID(), buf.readVarInt());
            case 21 -> new NoteEdit.RemoveGoal(list, buf.readUUID(), buf.readUUID());
            case 22 -> new NoteEdit.SetSpot(list, buf.readUUID(), buf.readOptional(SPOT));
            case 23 -> new NoteEdit.PlaceTask(list, buf.readUUID(), buf.readOptional(UUIDUtil.STREAM_CODEC), buf.readBoolean());
            default -> throw new IllegalArgumentException("Unknown notes edit tag " + tag);
        };
    }

    /** Reads a list of {@code codec} values written by {@link #writeList}. */
    static <T> List<T> readList(FriendlyByteBuf buf, StreamCodec<FriendlyByteBuf, T> codec) {
        int size = buf.readVarInt();
        List<T> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(codec.decode(buf));
        }
        return values;
    }

    static <T> void writeList(FriendlyByteBuf buf, List<T> values, StreamCodec<FriendlyByteBuf, T> codec) {
        buf.writeVarInt(values.size());
        values.forEach(value -> codec.encode(buf, value));
    }
}
