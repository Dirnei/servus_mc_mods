package io.github.dirnei.mc.servus.shuttles.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.command.ServusRoot;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /servus shuttles ...}: the op commands of Servus: Shuttles, verb first, naming a warehouse
 * by number or {@code here} and a machine by kind and number. The groups live in
 * {@link WarehouseCommands}, {@link FieldCommands}, {@link StockCommands} and {@link OrderCommands};
 * this class registers them and holds their shared argument and output helpers.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class ServusShuttlesCommands {

    private ServusShuttlesCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> shuttles = Commands.literal("shuttles")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        CommandBuildContext context = event.getBuildContext();
        WarehouseCommands.addTo(shuttles);
        FieldCommands.addTo(shuttles);
        StockCommands.addTo(shuttles, context);
        OrderCommands.addTo(shuttles, context);
        event.getDispatcher().register(ServusRoot.with(shuttles));
    }

    /** The {@code <warehouse>} parameter: a number or {@code here}. */
    static RequiredArgumentBuilder<CommandSourceStack, String> warehouseArgument() {
        return Commands.argument(CommandTargets.WAREHOUSE, StringArgumentType.word()).suggests(CommandTargets.warehouses());
    }

    /** A machine kind parameter. */
    static RequiredArgumentBuilder<CommandSourceStack, String> kindArgument(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests(CommandTargets.kinds());
    }

    /** A machine number parameter whose kind is in another parameter. */
    static RequiredArgumentBuilder<CommandSourceStack, String> numberArgument(String name, String kindParameter) {
        return Commands.argument(name, StringArgumentType.word()).suggests(CommandTargets.numbers(kindParameter));
    }

    /** The {@code <item>} parameter. */
    static RequiredArgumentBuilder<CommandSourceStack, ?> itemArgument(CommandBuildContext context) {
        return Commands.argument("item", ItemArgument.item(context));
    }

    static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    static void say(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, false);
    }

    static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    static ItemStack item(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ItemArgument.getItem(context, "item").createItemStack(1);
    }

    static int count(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "count");
    }

    static String name(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    /** "2 Oak Planks, 1 Coal" for stacks with counts. */
    static String counted(List<ItemStack> stacks) {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : stacks) {
            parts.add(stack.getCount() + " " + name(stack));
        }
        return String.join(", ", parts);
    }
}
