package io.github.dirnei.mc.servus.foundation.logistics.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelListTest {

    private static final UUID TEAM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static Channel create(ChannelList list, UUID team, String name) {
        return list.create(team, name, 14, null).orElseThrow();
    }

    private static List<String> names(List<Channel> channels) {
        return channels.stream().map(Channel::name).toList();
    }

    @Test
    void createAllocatesNewIds() {
        ChannelList list = new ChannelList();
        Channel iron = create(list, TEAM, "Iron ore");
        Channel gold = create(list, TEAM, "Gold");
        assertNotEquals(ChannelList.DEFAULT_ID, iron.id());
        assertNotEquals(iron.id(), gold.id());
        assertEquals(iron, list.get(iron.id()));
        assertEquals(TEAM, iron.team());
        assertEquals(14, iron.colour());
    }

    @Test
    void idsNeverReused() {
        ChannelList list = new ChannelList();
        Channel first = create(list, TEAM, "Old");
        assertTrue(list.delete(first.id()));
        Channel second = create(list, TEAM, "New");
        assertNotEquals(first.id(), second.id());
        ChannelList reloaded = new ChannelList(list.all(), list.nextId());
        assertNotEquals(first.id(), create(reloaded, TEAM, "Newer").id());
        assertFalse(reloaded.exists(first.id()));
        assertEquals(ChannelList.DEFAULT_ID, reloaded.resolve(first.id()));
    }

    @Test
    void renameKeepsId() {
        ChannelList list = new ChannelList();
        Channel iron = create(list, TEAM, "Iron");
        Channel renamed = list.edit(iron.id(), "Iron ore", 11, "minecraft:iron_ore").orElseThrow();
        assertEquals(iron.id(), renamed.id());
        assertEquals(TEAM, renamed.team());
        assertEquals("Iron ore", list.get(iron.id()).name());
        assertEquals(11, list.get(iron.id()).colour());
        assertEquals("minecraft:iron_ore", list.get(iron.id()).icon());
        assertTrue(list.edit(iron.id(), "IRON ORE", 11, null).isPresent(), "a channel may keep its own name in another case");
    }

    @Test
    void emptyNameRefused() {
        ChannelList list = new ChannelList();
        assertEquals(Optional.of(ChannelList.NameProblem.EMPTY), list.nameProblem(TEAM, "   ", 0));
        assertTrue(list.create(TEAM, "", 0, null).isEmpty());
        assertTrue(list.all().isEmpty());
    }

    @Test
    void longNameRefused() {
        ChannelList list = new ChannelList();
        String max = "a".repeat(ChannelList.MAX_NAME);
        assertEquals(Optional.empty(), list.nameProblem(TEAM, max, 0));
        assertEquals(Optional.of(ChannelList.NameProblem.TOO_LONG), list.nameProblem(TEAM, max + "a", 0));
        assertTrue(list.create(TEAM, max + "a", 0, null).isEmpty());
    }

    @Test
    void takenNameIgnoresCase() {
        ChannelList list = new ChannelList();
        create(list, TEAM, "Iron ore");
        assertEquals(Optional.of(ChannelList.NameProblem.TAKEN), list.nameProblem(TEAM, "iron ORE", 0));
        assertEquals(Optional.of(ChannelList.NameProblem.TAKEN), list.nameProblem(TEAM, " Iron ore ", 0));
        assertTrue(list.create(TEAM, "iron ORE", 0, null).isEmpty());
        assertEquals(1, list.forTeam(TEAM).size());
    }

    @Test
    void sameNameInOtherTeam() {
        ChannelList list = new ChannelList();
        create(list, TEAM, "Iron ore");
        assertTrue(list.create(OTHER, "Iron ore", 0, null).isPresent());
        assertEquals(1, list.forTeam(TEAM).size());
        assertEquals(1, list.forTeam(OTHER).size());
    }

    @Test
    void deletableWhenUnused() {
        assertTrue(ChannelList.deletable(5, 0, 0));
    }

    @Test
    void deletableWhenOnlyOpenSide() {
        assertTrue(ChannelList.deletable(5, 1, 1));
        assertFalse(ChannelList.deletable(5, 1, 0), "one use elsewhere keeps the channel");
    }

    @Test
    void notDeletableWithOtherUses() {
        assertFalse(ChannelList.deletable(5, 2, 1));
        assertFalse(ChannelList.deletable(5, 3, 0));
    }

    @Test
    void defaultNeverEditedOrDeleted() {
        ChannelList list = new ChannelList();
        assertFalse(ChannelList.deletable(ChannelList.DEFAULT_ID, 0, 0));
        assertFalse(list.delete(ChannelList.DEFAULT_ID));
        assertTrue(list.edit(ChannelList.DEFAULT_ID, "Main", 3, null).isEmpty());
        assertEquals(Channel.DEFAULT, list.get(ChannelList.DEFAULT_ID));
        assertTrue(list.exists(ChannelList.DEFAULT_ID));
    }

    @Test
    void matchingByText() {
        ChannelList list = new ChannelList();
        create(list, TEAM, "Iron ore");
        create(list, TEAM, "Main grid");
        create(list, TEAM, "Gold ore");
        assertEquals(List.of("", "Gold ore", "Iron ore"), names(ChannelList.matching(list.all(), "ORE", null)));
        assertEquals(List.of("", "Gold ore", "Iron ore", "Main grid"), names(ChannelList.matching(list.all(), " ", null)));
    }

    @Test
    void matchingByUsedIds() {
        ChannelList list = new ChannelList();
        Channel iron = create(list, TEAM, "Iron ore");
        create(list, TEAM, "Main grid");
        Channel gold = create(list, TEAM, "Gold");
        List<Channel> used = ChannelList.matching(list.all(), "", Set.of(iron.id(), gold.id(), 99));
        assertEquals(List.of("", "Gold", "Iron ore"), names(used));
        assertEquals(List.of(""), names(ChannelList.matching(list.all(), "", Set.of())));
    }

    @Test
    void defaultListedFirst() {
        ChannelList list = new ChannelList();
        create(list, TEAM, "Aaa");
        List<Channel> found = ChannelList.matching(list.all(), "zzz", null);
        assertEquals(List.of(Channel.DEFAULT), found, "Default stays listed for any search");
        assertTrue(ChannelList.matching(list.all(), "", null).get(0).isDefault());
    }
}
