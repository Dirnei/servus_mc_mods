package io.github.dirnei.mc.servus.notes.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.server.NotesData;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Op commands of Servus: Notes: {@code /notes transfer <from> <to>} hands every list of one player to another. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID)
public final class NotesCommands {

    private NotesCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("notes")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("transfer")
                        .then(knownPlayer("from")
                                .then(knownPlayer("to").executes(NotesCommands::transfer)))));
    }

    /** A player name argument suggesting everyone who has been on the server since Notes was installed. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> knownPlayer(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        NotesData.get(context.getSource().getServer()).knownNames(), builder));
    }

    private static int transfer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NotesData data = NotesData.get(source.getServer());
        Optional<Person> from = known(source, data, StringArgumentType.getString(context, "from"));
        Optional<Person> to = known(source, data, StringArgumentType.getString(context, "to"));
        if (from.isEmpty() || to.isEmpty()) {
            return 0;
        }
        if (from.get().id().equals(to.get().id())) {
            source.sendFailure(Component.translatable("commands.servus_notes.transfer.same"));
            return 0;
        }
        int moved = NotesService.transfer(source.getServer(), from.get().id(), to.get());
        source.sendSuccess(() -> Component.translatable("commands.servus_notes.transfer.done", moved, from.get().name(), to.get().name()), true);
        return moved;
    }

    private static Optional<Person> known(CommandSourceStack source, NotesData data, String name) {
        Optional<Person> person = data.knownByName(name);
        if (person.isEmpty()) {
            source.sendFailure(Component.translatable("commands.servus_notes.transfer.unknown", name));
        }
        return person;
    }
}
