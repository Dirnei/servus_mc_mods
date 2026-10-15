package io.github.dirnei.mc.servus.alchemy.journal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /servus_alchemy journal}: the team's open and finished chapters with task progress, and for operators
 * {@code journal finish <chapter>} to put the team at a chapter.
 */
public final class JournalCommand {

    private JournalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ServusAlchemyIds.MOD_ID)
                .then(Commands.literal("journal").executes(context -> show(context.getSource()))
                        .then(Commands.literal("finish")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("chapter", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                JournalChapters.INSTANCE.chapters().stream().map(ChapterDef::id), builder))
                                        .executes(context -> finish(context.getSource(),
                                                StringArgumentType.getString(context, "chapter")))))));
    }

    /** Finishes the chapter and its parents for the player's team; returns how many chapters it finished. */
    private static int finish(CommandSourceStack source, String chapter) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (JournalChapters.INSTANCE.chapters().stream().noneMatch(def -> def.id().equals(chapter))) {
            source.sendFailure(Component.translatable("commands.servus_alchemy.journal.unknown", chapter));
            return 0;
        }
        List<String> finished = Journal.finish(player.level().getServer(), Teams.teamOf(player), chapter);
        if (finished.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.already", chapterName(chapter)), false);
        }
        for (String done : finished) {
            source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.now_finished", chapterName(done)), true);
        }
        return finished.size();
    }

    private static Component chapterName(String chapter) {
        Identifier id = Identifier.parse(chapter);
        return Component.translatable("journal." + id.getNamespace() + "." + id.getPath());
    }

    /** Lists every open or finished chapter; returns how many were shown. */
    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TeamJournal journal = Journal.journalOf(player.level().getServer(), Teams.teamOf(player));
        source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.header"), false);
        int shown = 0;
        for (ChapterDef chapter : JournalChapters.INSTANCE.chapters()) {
            boolean finished = journal.isFinished(chapter.id());
            if (finished || JournalLedger.isOpen(chapter, journal)) {
                showChapter(source, journal, chapter, finished);
                shown++;
            }
        }
        return shown;
    }

    private static void showChapter(CommandSourceStack source, TeamJournal journal, ChapterDef chapter, boolean finished) {
        Component name = chapterName(chapter.id());
        Component state = Component.translatable("commands.servus_alchemy.journal." + (finished ? "finished" : "open"));
        source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.chapter", name, state), false);
        for (TaskDef task : chapter.tasks()) {
            int count = journal.count(chapter.id(), task.id());
            source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.task",
                    task.id(), count, task.count()), false);
        }
    }
}
