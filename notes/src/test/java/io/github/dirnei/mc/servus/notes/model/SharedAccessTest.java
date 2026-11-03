package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The whole rights matrix: every kind of player against every scope, with and without FTB Teams. */
class SharedAccessTest {

    private static final UUID PARTY = new UUID(9, 1);
    private static final UUID OTHER_PARTY = new UUID(9, 2);

    private static final Person OWNER = new Person(new UUID(0, 1), "Dirnei");
    private static final Person MEMBER = new Person(new UUID(0, 2), "Anna");
    private static final Person READER = new Person(new UUID(0, 3), "Sepp");
    private static final Person EDITOR = new Person(new UUID(0, 4), "Max");
    private static final Person STRANGER = new Person(new UUID(0, 5), "Vroni");
    private static final Person OP = new Person(new UUID(0, 6), "Admin");

    /** see, edit, tick, manage as letters: "SETM" is everything, "-" is nothing. */
    private static Rights rights(String letters) {
        return new Rights(letters.contains("S"), letters.contains("E"), letters.contains("T"), letters.contains("M"));
    }

    private static NoteList list(Scope scope) {
        return new NoteList(new UUID(1, 1), "Nether highway", OWNER.id(), OWNER.name(), List.of(), scope,
                Optional.of(PARTY), List.of(new Guest(READER, Guest.Right.READ), new Guest(EDITOR, Guest.Right.EDIT)));
    }

    private static SharedAccess access(boolean teamsInstalled) {
        Map<UUID, Viewer> viewers = Map.of(
                OWNER.id(), new Viewer(OWNER.id(), false, Optional.of(PARTY), teamsInstalled),
                MEMBER.id(), new Viewer(MEMBER.id(), false, Optional.of(PARTY), teamsInstalled),
                READER.id(), new Viewer(READER.id(), false, Optional.of(OTHER_PARTY), teamsInstalled),
                EDITOR.id(), new Viewer(EDITOR.id(), false, Optional.empty(), teamsInstalled),
                STRANGER.id(), new Viewer(STRANGER.id(), false, Optional.of(OTHER_PARTY), teamsInstalled),
                OP.id(), new Viewer(OP.id(), true, Optional.empty(), teamsInstalled));
        return new SharedAccess(viewers::get);
    }

    static List<Arguments> matrix() {
        return List.of(
                // scope, FTB Teams, owner, team member, read guest, edit guest, stranger, op
                Arguments.of(Scope.PRIVATE, true, "SETM", "-", "ST", "SET", "-", "-"),
                Arguments.of(Scope.TEAM, true, "SETM", "SET", "ST", "SET", "-", "-"),
                Arguments.of(Scope.SERVER, true, "SETM", "ST", "ST", "SET", "ST", "SET"),
                Arguments.of(Scope.PRIVATE, false, "SETM", "-", "ST", "SET", "-", "-"),
                Arguments.of(Scope.TEAM, false, "SETM", "-", "ST", "SET", "-", "-"),
                Arguments.of(Scope.SERVER, false, "SETM", "ST", "ST", "SET", "ST", "SET"));
    }

    @ParameterizedTest(name = "{0}, FTB Teams {1}")
    @MethodSource("matrix")
    void rightsFollowTheMatrix(Scope scope, boolean teamsInstalled, String owner, String member, String reader,
                               String editor, String stranger, String op) {
        SharedAccess access = access(teamsInstalled);
        NoteList list = list(scope);
        assertEquals(rights(owner), access.rights(OWNER.id(), list), "owner");
        assertEquals(rights(member), access.rights(MEMBER.id(), list), "team member");
        assertEquals(rights(reader), access.rights(READER.id(), list), "read guest");
        assertEquals(rights(editor), access.rights(EDITOR.id(), list), "edit guest");
        assertEquals(rights(stranger), access.rights(STRANGER.id(), list), "stranger");
        assertEquals(rights(op), access.rights(OP.id(), list), "op");
    }

    @ParameterizedTest(name = "FTB Teams {0}")
    @MethodSource("teams")
    void partyCountsOnlyWithFtbTeams(boolean teamsInstalled) {
        assertEquals(teamsInstalled ? Optional.of(PARTY) : Optional.empty(), access(teamsInstalled).party(MEMBER.id()));
        assertEquals(Optional.empty(), access(teamsInstalled).party(EDITOR.id()));
    }

    static List<Boolean> teams() {
        return List.of(true, false);
    }

    @ParameterizedTest(name = "FTB Teams {0}")
    @MethodSource("teams")
    void memberOfAnotherPartyIsAStranger(boolean teamsInstalled) {
        NoteList list = list(Scope.TEAM).withScope(Scope.TEAM, Optional.of(OTHER_PARTY));
        assertEquals(Rights.NONE, access(teamsInstalled).rights(MEMBER.id(), list));
    }
}
