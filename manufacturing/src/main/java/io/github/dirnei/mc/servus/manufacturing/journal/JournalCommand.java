package io.github.dirnei.mc.servus.manufacturing.journal;

import com.mojang.brigadier.CommandDispatcher;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TeamJournal;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** {@code /servus_manufacturing journal}: the team's open and finished chapters with task progress. */
public final class JournalCommand {

    private JournalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("servus_manufacturing")
                .then(Commands.literal("journal").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamJournal journal = Journal.journalOf(player.server, Teams.teamOf(player));
                    CommandSourceStack source = context.getSource();
                    source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.header"), false);
                    int shown = 0;
                    for (ChapterDef chapter : JournalChapters.INSTANCE.chapters()) {
                        boolean finished = journal.isFinished(chapter.id());
                        if (!finished && !JournalLedger.isOpen(chapter, journal)) {
                            continue;
                        }
                        shown++;
                        ResourceLocation id = ResourceLocation.parse(chapter.id());
                        Component name = Component.translatable("journal." + id.getNamespace() + "." + id.getPath());
                        Component state = Component.translatable("commands.servus_manufacturing.journal." + (finished ? "finished" : "open"));
                        source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.chapter", name, state), false);
                        for (TaskDef task : chapter.tasks()) {
                            int count = journal.count(chapter.id(), task.id());
                            source.sendSuccess(() -> Component.translatable("commands.servus_manufacturing.journal.task",
                                    task.id(), count, task.count()), false);
                        }
                    }
                    return shown;
                })));
    }
}
