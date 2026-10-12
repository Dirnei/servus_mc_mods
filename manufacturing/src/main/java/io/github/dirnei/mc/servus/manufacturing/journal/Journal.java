package io.github.dirnei.mc.servus.manufacturing.journal;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TeamJournal;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** The server side of Paracelsus' journal: machines report events here, finished chapters become advancements. */
public final class Journal {

    /** Criterion of every chapter advancement. */
    public static final String CRITERION = "done";
    /** The root of the journal's advancement tab, given to everyone so the tab shows. */
    public static final Identifier ROOT = Identifier.fromNamespaceAndPath("servus_manufacturing", "journal/root");

    private Journal() {
    }

    /** The advancement a finished chapter grants: {@code <namespace>:journal/<chapter>}. */
    public static Identifier advancementId(String chapter) {
        Identifier id = Identifier.parse(chapter);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "journal/" + id.getPath());
    }

    /** An item event, such as {@code calcine} or {@code coagulate}. Filters are an item id or {@code #tag}. */
    public static void recordItem(ServerLevel level, UUID team, String event, ItemStack stack, int amount) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        record(level, team, event, task -> {
            String filter = task.filter();
            if (filter.isEmpty()) {
                return true;
            }
            if (filter.startsWith("#")) {
                Identifier tag = Identifier.tryParse(filter.substring(1));
                return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
            }
            return filter.equals(id);
        }, amount);
    }

    /** An {@code athanor} event: a finished Athanor craft. Filters are {@code tier:<name>}, an item id or {@code #tag}. */
    public static void recordAthanor(ServerLevel level, UUID team, ItemStack result, String tierName) {
        String id = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        record(level, team, "athanor", task -> {
            String filter = task.filter();
            if (filter.isEmpty()) {
                return true;
            }
            if (filter.startsWith("tier:")) {
                return filter.substring(5).equals(tierName);
            }
            if (filter.startsWith("#")) {
                Identifier tag = Identifier.tryParse(filter.substring(1));
                return tag != null && result.is(TagKey.create(Registries.ITEM, tag));
            }
            return filter.equals(id);
        }, 1);
    }

    /** An event without a filter, such as {@code sight}. */
    public static void recordEvent(ServerLevel level, UUID team, String event, int amount) {
        record(level, team, event, task -> task.filter().isEmpty(), amount);
    }

    /** A {@code principle} event: mB of one principle made. Filters are a principle name. */
    public static void recordPrinciple(ServerLevel level, UUID team, Principle principle, int amount) {
        record(level, team, "principle", task -> task.filter().isEmpty() || task.filter().equals(principle.id()), amount);
    }

    private static void record(ServerLevel level, UUID team, String event, Predicate<TaskDef> matches, int amount) {
        if (team == null || amount <= 0) {
            return;
        }
        MinecraftServer server = level.getServer();
        JournalSavedData data = JournalSavedData.get(server);
        List<String> finished = JournalLedger.record(JournalChapters.INSTANCE.chapters(), data.forTeam(team), event, matches, amount);
        data.setDirty();
        for (String chapter : finished) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (team.equals(Teams.teamOf(player))) {
                    award(player, chapter);
                }
            }
        }
    }

    /** Whether the team finished the chapter. */
    public static boolean isFinished(MinecraftServer server, UUID team, String chapter) {
        return team != null && JournalSavedData.get(server).forTeam(team).isFinished(chapter);
    }

    public static TeamJournal journalOf(MinecraftServer server, UUID team) {
        return JournalSavedData.get(server).forTeam(team);
    }

    /** Gives the player every chapter advancement their team earned. Safe to call often. */
    public static void catchUp(ServerPlayer player) {
        AdvancementHolder root = player.level().getServer().getAdvancements().get(ROOT);
        if (root != null) {
            player.getAdvancements().award(root, CRITERION);
        }
        for (String chapter : journalOf(player.level().getServer(), Teams.teamOf(player)).finished()) {
            award(player, chapter);
        }
    }

    private static void award(ServerPlayer player, String chapter) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancementId(chapter));
        if (holder != null) {
            player.getAdvancements().award(holder, CRITERION);
        }
    }
}
