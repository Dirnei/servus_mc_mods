package io.github.dirnei.mc.servus.alchemy.journal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** {@code /servus_manufacturing journal}: the team's open and finished chapters with task progress. */
public final class JournalCommand {

    private JournalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ServusAlchemyIds.MOD_ID)
                .then(Commands.literal("journal").executes(context -> show(context.getSource()))));
    }

    /** Lists every open or finished chapter; returns how many were shown. */
    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TeamJournal journal = Journal.journalOf(player.level().getServer(), Teams.teamOf(player));
        source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.header"), false);
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
        Identifier id = Identifier.parse(chapter.id());
        Component name = Component.translatable("journal." + id.getNamespace() + "." + id.getPath());
        Component state = Component.translatable("commands.servus_manufacturing.journal." + (finished ? "finished" : "open"));
        source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.chapter", name, state), false);
        for (TaskDef task : chapter.tasks()) {
            int count = journal.count(chapter.id(), task.id());
            source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.task",
                    task.id(), count, task.count()), false);
        }
    }
}
