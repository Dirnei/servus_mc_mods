package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import io.github.dirnei.mc.servus.foundation.team.Teams;
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
    public static final Identifier ROOT = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal/root");
    private static final String TIER_FILTER = "tier:";

    private Journal() {
    }

    /** The advancement a finished chapter grants: {@code <namespace>:journal/<chapter>}. */
    public static Identifier advancementId(String chapter) {
        Identifier id = Identifier.parse(chapter);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "journal/" + id.getPath());
    }

    /** An item event, such as {@code calcine} or {@code coagulate}. Filters are an item id or {@code #tag}. */
    public static void recordItem(ServerLevel level, UUID team, String event, ItemStack stack, int amount) {
        record(level, team, event, task -> task.filter().isEmpty() || matchesItem(task.filter(), stack), amount);
    }

    /** An {@code athanor} event: a finished Athanor craft. Filters are {@code tier:<name>}, an item id or {@code #tag}. */
    public static void recordAthanor(ServerLevel level, UUID team, ItemStack result, String tierName) {
        record(level, team, "athanor", task -> {
            String filter = task.filter();
            if (filter.isEmpty()) {
                return true;
            }
            if (filter.startsWith(TIER_FILTER)) {
                return filter.substring(TIER_FILTER.length()).equals(tierName);
            }
            return matchesItem(filter, result);
        }, 1);
    }

    /** Whether {@code stack} is the item id or in the {@code #tag} that {@code filter} names. */
    private static boolean matchesItem(String filter, ItemStack stack) {
        if (filter.startsWith("#")) {
            Identifier tag = Identifier.tryParse(filter.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return filter.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** A Calciner of the team burned something with the calcination recipe; the journal's table reveals its row. */
    public static void recordCalcination(ServerLevel level, UUID team, Identifier recipe) {
        if (team == null) {
            return;
        }
        JournalSavedData data = JournalSavedData.get(level.getServer());
        if (data.forTeam(team).markTried(recipe.toString())) {
            data.setDirty();
            JournalSync.markChanged(team);
        }
    }

    /** An event without a filter, such as {@code sight}. */
    public static void recordEvent(ServerLevel level, UUID team, String event, int amount) {
        record(level, team, event, task -> task.filter().isEmpty(), amount);
    }

    /** A named event, such as {@code find} of the old gnome. Filters are the name. */
    public static void recordNamed(ServerLevel level, UUID team, String event, String name) {
        record(level, team, event, task -> task.filter().isEmpty() || task.filter().equals(name), 1);
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
        JournalSync.markChanged(team);
        awardOnline(server, team, finished);
    }

    /**
     * Finishes the chapter and its unfinished parents for the team, as if every task were done, and returns the
     * newly finished chapters; empty when the chapter is unknown or already finished.
     */
    public static List<String> finish(MinecraftServer server, UUID team, String chapter) {
        JournalSavedData data = JournalSavedData.get(server);
        List<String> finished = JournalLedger.finish(JournalChapters.INSTANCE.chapters(), data.forTeam(team), chapter);
        data.setDirty();
        JournalSync.markChanged(team);
        awardOnline(server, team, finished);
        return finished;
    }

    private static void awardOnline(MinecraftServer server, UUID team, List<String> chapters) {
        for (String chapter : chapters) {
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
