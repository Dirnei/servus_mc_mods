package io.github.dirnei.mc.servus.notes.server;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.config.NotesServerConfig;
import io.github.dirnei.mc.servus.notes.model.Access;
import io.github.dirnei.mc.servus.notes.model.EditResult;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.Moment;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.NoteEdits;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NotesBook;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Refusal;
import io.github.dirnei.mc.servus.notes.model.SharedAccess;
import io.github.dirnei.mc.servus.notes.model.Viewer;
import io.github.dirnei.mc.servus.notes.network.KnownPlayersPayload;
import io.github.dirnei.mc.servus.notes.network.ListGonePayload;
import io.github.dirnei.mc.servus.notes.network.ListPayload;
import io.github.dirnei.mc.servus.notes.network.ListsPayload;
import io.github.dirnei.mc.servus.notes.network.PinnedPayload;
import io.github.dirnei.mc.servus.notes.network.RefusedPayload;
import io.github.dirnei.mc.servus.notes.network.ViewerPayload;
import io.github.dirnei.mc.servus.notes.team.TeamLookup;
import io.github.dirnei.mc.servus.notes.team.Teams;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Applies edits to the server's lists and keeps every online player's copy current: after an
 * applied edit each changed list goes to every player who sees it, with that player's rights, and
 * a "gone" to everyone else. Party changes are queued and resent once per server tick. Server thread only.
 */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID)
public final class NotesService {

    private static final long TICKS_PER_DAY = 24000L;
    private static final Set<UUID> REFRESH = new LinkedHashSet<>();

    private NotesService() {
    }

    /** The sharing rules, asking the server who is op and the team lookup who is in which party. */
    public static Access access(MinecraftServer server) {
        return new SharedAccess(id -> viewer(server, id));
    }

    /** What the sharing rules know about {@code player}. */
    public static Viewer viewer(MinecraftServer server, UUID player) {
        TeamLookup teams = Teams.lookup();
        return new Viewer(player, isOp(server, player), teams.party(player), teams.installed());
    }

    /** Op rights for Notes: the permission level the family's op commands ask for (gamemaster). */
    private static boolean isOp(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(online.createCommandSourceStack());
        }
        String name = NotesData.get(server).known(player).map(Person::name).orElse("");
        return server.getPlayerList().isOp(new NameAndId(player, name));
    }

    /** The player as the notebook records them. */
    public static Person person(ServerPlayer player) {
        return new Person(player.getUUID(), player.getName().getString());
    }

    /** Applies an edit sent by {@code player}; a refusal is reported back to them. */
    public static EditResult apply(ServerPlayer player, NoteEdit edit) {
        EditResult result = apply(player.level().getServer(), person(player), edit);
        if (result instanceof EditResult.Refused refused) {
            PacketDistributor.sendToPlayer(player, new RefusedPayload(refused.reason().key()));
        }
        return result;
    }

    /** Applies an edit by {@code actor}, saves it and sends the changed lists to the online players. */
    public static EditResult apply(MinecraftServer server, Person actor, NoteEdit edit) {
        NotesData data = NotesData.get(server);
        if (edit instanceof NoteEdit.AddGuest add) {
            Optional<Person> known = data.known(add.guest().id());
            if (known.isEmpty()) {
                return new EditResult.Refused(Refusal.NOT_FOUND);
            }
            edit = new NoteEdit.AddGuest(add.listId(), known.get(), add.right());
        }
        if (namesUnknownItem(edit)) {
            return new EditResult.Refused(Refusal.NOT_FOUND);
        }
        EditResult result = NoteEdits.apply(data.book(), actor, access(server), NotesServerConfig.limits(), now(server), edit);
        if (result instanceof EditResult.Applied applied) {
            store(server, applied);
        }
        return result;
    }

    /** Whether the edit sets a goal to an item this server does not have; saved goals of removed items are kept, new ones are not made. */
    private static boolean namesUnknownItem(NoteEdit edit) {
        String itemId = switch (edit) {
            case NoteEdit.AddGoal add -> add.itemId();
            case NoteEdit.SetGoalItem set -> set.itemId();
            default -> null;
        };
        if (itemId == null) {
            return false;
        }
        Identifier id = Identifier.tryParse(itemId);
        return id == null || !BuiltInRegistries.ITEM.containsKey(id) || id.equals(BuiltInRegistries.ITEM.getDefaultKey());
    }

    /** Hands every list of {@code from} to {@code to}; returns how many lists moved. */
    public static int transfer(MinecraftServer server, UUID from, Person to) {
        EditResult.Applied applied = NoteEdits.transfer(NotesData.get(server).book(), from, to);
        store(server, applied);
        return applied.changed().size();
    }

    /** A party was disbanded: its Team lists become private lists of their owners. */
    public static void teamDeleted(MinecraftServer server, UUID team) {
        store(server, NoteEdits.teamDeleted(NotesData.get(server).book(), team));
    }

    private static void store(MinecraftServer server, EditResult.Applied applied) {
        if (!applied.changed().isEmpty()) {
            NotesData.get(server).setBook(applied.book());
            broadcast(server, applied.book(), applied.changed());
            dropLostPins(server, applied.changed());
        }
    }

    /**
     * Pins {@code listId} to {@code player}'s HUD, or clears their pin when empty, and tells them if
     * they are online. False, and nothing changes, when they do not see the list.
     */
    public static boolean pin(MinecraftServer server, UUID player, Optional<UUID> listId) {
        NotesData data = NotesData.get(server);
        if (listId.isPresent() && !sees(server, player, listId.get())) {
            return false;
        }
        if (data.setPin(player, listId)) {
            sendPin(server, player);
        }
        return true;
    }

    private static boolean sees(MinecraftServer server, UUID player, UUID listId) {
        return NotesData.get(server).book().get(listId).filter(list -> access(server).canSee(player, list)).isPresent();
    }

    /** Clears every pin, online or not, on one of {@code changed} that its player no longer sees. */
    private static void dropLostPins(MinecraftServer server, Set<UUID> changed) {
        NotesData data = NotesData.get(server);
        List<UUID> lost = data.pins().entrySet().stream()
                .filter(pin -> changed.contains(pin.getValue()) && !sees(server, pin.getKey(), pin.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (UUID player : lost) {
            data.setPin(player, Optional.empty());
            sendPin(server, player);
        }
    }

    private static void sendPin(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            PacketDistributor.sendToPlayer(online, new PinnedPayload(NotesData.get(server).pin(player)));
        }
    }

    /** Every list {@code player} sees, with their rights on it. */
    public static List<ListView> visibleTo(MinecraftServer server, UUID player) {
        Access access = access(server);
        return NotesData.get(server).book().all().stream()
                .filter(list -> access.canSee(player, list))
                .map(list -> new ListView(list, access.rights(player, list)))
                .toList();
    }

    /** Sends {@code player} every list they see, replacing what their client has, plus their party and their pin. */
    public static void sendAll(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Viewer viewer = viewer(server, player.getUUID());
        PacketDistributor.sendToPlayer(player, new ViewerPayload(viewer.teamsInstalled(), viewer.party()));
        PacketDistributor.sendToPlayer(player, new ListsPayload(visibleTo(server, player.getUUID()), NotesServerConfig.limits()));
        NotesData data = NotesData.get(server);
        data.pin(player.getUUID()).filter(listId -> !sees(server, player.getUUID(), listId))
                .ifPresent(lost -> data.setPin(player.getUUID(), Optional.empty()));
        PacketDistributor.sendToPlayer(player, new PinnedPayload(data.pin(player.getUUID())));
    }

    /** A player joined: they become a known player and get their lists; everyone learns a new or renamed name. */
    public static void joined(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        NotesData data = NotesData.get(server);
        boolean news = data.remember(person(player));
        KnownPlayersPayload known = new KnownPlayersPayload(data.knownPlayers());
        if (news) {
            PacketDistributor.sendToAllPlayers(known);
        } else {
            PacketDistributor.sendToPlayer(player, known);
        }
        sendAll(player);
    }

    /** Resends the player's lists at the end of this server tick, after their party changed. */
    public static void refreshLater(UUID player) {
        REFRESH.add(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (REFRESH.isEmpty()) {
            return;
        }
        List<UUID> players = List.copyOf(REFRESH);
        REFRESH.clear();
        for (UUID id : players) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                sendAll(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        REFRESH.clear();
    }

    private static void broadcast(MinecraftServer server, NotesBook book, Set<UUID> changed) {
        Access access = access(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (UUID listId : changed) {
                Optional<NoteList> list = book.get(listId).filter(found -> access.canSee(player.getUUID(), found));
                if (list.isPresent()) {
                    PacketDistributor.sendToPlayer(player, new ListPayload(new ListView(list.get(), access.rights(player.getUUID(), list.get()))));
                } else {
                    PacketDistributor.sendToPlayer(player, new ListGonePayload(listId));
                }
            }
        }
    }

    private static Moment now(MinecraftServer server) {
        return new Moment(server.overworld().getOverworldClockTime() / TICKS_PER_DAY, System.currentTimeMillis());
    }
}
