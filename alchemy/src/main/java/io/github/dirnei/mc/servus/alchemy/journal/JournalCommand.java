package io.github.dirnei.mc.servus.alchemy.journal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import io.github.dirnei.mc.servus.foundation.command.ServusRoot;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /servus alchemy journal [player]}: the team's open and finished chapters with task progress, and for
 * operators {@code journal finish <player> <chapter>} to put the player's team at a chapter.
 */
public final class JournalCommand {

    private static final SimpleCommandExceptionType NEEDS_PLAYER =
            new SimpleCommandExceptionType(Component.translatable("commands.servus_alchemy.journal.needs_player"));
    private static final SimpleCommandExceptionType OTHER_PLAYER =
            new SimpleCommandExceptionType(Component.translatable("commands.servus_alchemy.journal.other_player"));

    private JournalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(ServusRoot.with(Commands.literal("alchemy")
                .then(Commands.literal("journal").executes(context -> show(context.getSource(), self(context.getSource())))
                        .then(Commands.literal("finish")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("chapter", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        JournalChapters.INSTANCE.chapters().stream().map(ChapterDef::id), builder))
                                                .executes(context -> finish(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "chapter"))))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> show(context.getSource(), named(context)))))));
    }

    /** The player who runs the command; a source that is not a player is asked to name one. */
    private static ServerPlayer self(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw NEEDS_PLAYER.create();
        }
        return player;
    }

    /** The named player; naming another player needs operator permission. */
    private static ServerPlayer named(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();
        if (player != source.getPlayer() && !Commands.LEVEL_GAMEMASTERS.check(source.permissions())) {
            throw OTHER_PLAYER.create();
        }
        return player;
    }

    /** Finishes the chapter and its parents for the player's team; returns how many chapters it finished. */
    private static int finish(CommandSourceStack source, ServerPlayer player, String chapter) {
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
    private static int show(CommandSourceStack source, ServerPlayer player) {
        TeamJournal journal = Journal.journalOf(player.level().getServer(), Teams.teamOf(player));
        source.sendSuccess(() -> Component.translatable("commands.servus_alchemy.journal.header", player.getDisplayName()), false);
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
