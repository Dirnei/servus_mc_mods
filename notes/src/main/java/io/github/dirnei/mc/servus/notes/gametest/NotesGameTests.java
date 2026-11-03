package io.github.dirnei.mc.servus.notes.gametest;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the Notes GameTests. Notes cannot use Foundation's helper, so every test is listed
 * in {@link #TESTS} and becomes a test function plus a function test instance named
 * {@code servus_notes:<name>}, run in the empty structure of its batch.
 */
public final class NotesGameTests {

    /** The 12 by 6 by 12 empty structure the tests run in. */
    public static final String EMPTY = "empty_12x6x12";

    /** The batch, and test environment, of every test that names no other. */
    public static final String DEFAULT_BATCH = "default";

    /** One GameTest: its name, batch, timeout and body. */
    public record NotesTest(String name, String batch, int timeoutTicks, Consumer<GameTestHelper> body) {

        /** A test in the default batch that fails after 100 ticks. */
        public static NotesTest of(String name, Consumer<GameTestHelper> body) {
            return new NotesTest(name, DEFAULT_BATCH, 100, body);
        }
    }

    /** Every Notes GameTest. Add new tests here. */
    public static final List<NotesTest> TESTS = List.of(
            NotesTest.of("setup.loads_alone", NotesSetupGameTests::loadsAlone),
            NotesTest.of("lists.edit_is_saved", NotesListsGameTests::editIsSaved),
            NotesTest.of("lists.stranger_is_refused", NotesListsGameTests::strangerIsRefused),
            NotesTest.of("sharing.server_list_for_everyone", NotesSharingGameTests::serverListForEveryone),
            NotesTest.of("sharing.guests_are_known_players", NotesSharingGameTests::guestsAreKnownPlayers),
            NotesTest.of("sharing.transfer_moves_every_list", NotesSharingGameTests::transferMovesEveryList),
            NotesTest.of("goals.read_guest_types_a_count", NotesGoalsGameTests::readGuestTypesACount),
            NotesTest.of("goals.unknown_item_is_refused", NotesGoalsGameTests::unknownItemIsRefused),
            NotesTest.of("hud.pin_refused_for_invisible_list", NotesHudGameTests::pinRefusedForInvisibleList),
            NotesTest.of("hud.pin_cleared_when_guest_removed", NotesHudGameTests::pinClearedWhenGuestRemoved));

    private NotesGameTests() {
    }

    /** Registers {@link #TESTS}. Call from the mod constructor. */
    public static void register(IEventBus modBus) {
        DeferredRegister<Consumer<GameTestHelper>> functions =
                DeferredRegister.create(Registries.TEST_FUNCTION, ServusNotesIds.MOD_ID);
        for (NotesTest test : TESTS) {
            functions.register(test.name(), test::body);
        }
        functions.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, NotesGameTests::registerInstances);
    }

    private static void registerInstances(RegisterGameTestsEvent event) {
        Map<String, Holder<TestEnvironmentDefinition<?>>> batches = new HashMap<>();
        for (NotesTest test : TESTS) {
            Holder<TestEnvironmentDefinition<?>> environment = batches.computeIfAbsent(test.batch(),
                    batch -> event.registerEnvironment(id(batch)));
            TestData<Holder<TestEnvironmentDefinition<?>>> data =
                    new TestData<>(environment, id(EMPTY), test.timeoutTicks(), 0, true);
            event.registerTest(id(test.name()),
                    new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())), data));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, path);
    }
}
