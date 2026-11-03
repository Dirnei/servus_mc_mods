package io.github.dirnei.mc.servus.notes.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.notes.model.DoneRecord;
import io.github.dirnei.mc.servus.notes.model.Goal;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NotesBook;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Scope;
import io.github.dirnei.mc.servus.notes.model.Spot;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;

/** Save codecs of the notes model. Optional fields default so older saves keep loading when fields are added. */
public final class NotesCodecs {

    public static final Codec<Person> PERSON = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("Id").forGetter(Person::id),
            Codec.STRING.fieldOf("Name").forGetter(Person::name))
            .apply(i, Person::new));

    public static final Codec<DoneRecord> DONE = RecordCodecBuilder.create(i -> i.group(
            PERSON.fieldOf("By").forGetter(DoneRecord::by),
            Codec.LONG.fieldOf("Day").forGetter(DoneRecord::day),
            Codec.LONG.fieldOf("Time").forGetter(DoneRecord::epochMillis))
            .apply(i, DoneRecord::new));

    public static final Codec<Goal> GOAL = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("Id").forGetter(Goal::id),
            Codec.STRING.fieldOf("Item").forGetter(Goal::itemId),
            Codec.INT.fieldOf("Target").forGetter(Goal::target),
            Codec.INT.optionalFieldOf("Count", 0).forGetter(Goal::count),
            PERSON.fieldOf("LastBy").forGetter(Goal::lastBy))
            .apply(i, Goal::new));

    public static final Codec<Spot> SPOT = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("X").forGetter(Spot::x),
            Codec.INT.fieldOf("Y").forGetter(Spot::y),
            Codec.INT.fieldOf("Z").forGetter(Spot::z),
            Codec.STRING.fieldOf("Dimension").forGetter(Spot::dimension),
            Codec.STRING.optionalFieldOf("Name", "").forGetter(Spot::name))
            .apply(i, Spot::new));

    public static final Codec<Task> TASK = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("Id").forGetter(Task::id),
            Codec.STRING.fieldOf("Text").forGetter(Task::text),
            Codec.STRING.optionalFieldOf("Notes", "").forGetter(Task::notes),
            Codec.BOOL.optionalFieldOf("Sub", false).forGetter(Task::sub),
            PERSON.optionalFieldOf("Assignee").forGetter(Task::assignee),
            DONE.optionalFieldOf("Done").forGetter(Task::done),
            GOAL.listOf().optionalFieldOf("Goals", List.of()).forGetter(Task::goals),
            SPOT.optionalFieldOf("Spot").forGetter(Task::spot))
            .apply(i, Task::new));

    public static final Codec<Scope> SCOPE = enumCodec(Scope.class, Scope::key);

    public static final Codec<Guest.Right> RIGHT = enumCodec(Guest.Right.class, Guest.Right::key);

    public static final Codec<Guest> GUEST = RecordCodecBuilder.create(i -> i.group(
            PERSON.fieldOf("Who").forGetter(Guest::who),
            RIGHT.fieldOf("Right").forGetter(Guest::right))
            .apply(i, Guest::new));

    public static final Codec<NoteList> LIST = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("Id").forGetter(NoteList::id),
            Codec.STRING.fieldOf("Name").forGetter(NoteList::name),
            UUIDUtil.CODEC.fieldOf("Owner").forGetter(NoteList::owner),
            Codec.STRING.fieldOf("OwnerName").forGetter(NoteList::ownerName),
            TASK.listOf().optionalFieldOf("Tasks", List.of()).forGetter(NoteList::tasks),
            SCOPE.optionalFieldOf("Scope", Scope.PRIVATE).forGetter(NoteList::scope),
            UUIDUtil.CODEC.optionalFieldOf("Team").forGetter(NoteList::team),
            GUEST.listOf().optionalFieldOf("Guests", List.of()).forGetter(NoteList::guests))
            .apply(i, NoteList::new));

    public static final Codec<NotesBook> BOOK = RecordCodecBuilder.create(i -> i.group(
            LIST.listOf().optionalFieldOf("Lists", List.of()).forGetter(book -> List.copyOf(book.all())))
            .apply(i, NotesBook::new));

    /** Pinned lists by player: player id to list id. */
    public static final Codec<Map<UUID, UUID>> PINS = Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC);

    private NotesCodecs() {
    }

    /** Reads and writes an enum by its lower case key. */
    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type, Function<E, String> key) {
        return Codec.STRING.comapFlatMap(name -> {
            for (E value : type.getEnumConstants()) {
                if (key.apply(value).equals(name)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> "Unknown " + type.getSimpleName() + " " + name);
        }, key);
    }
}
