package io.github.dirnei.mc.servus.shuttles.command;

import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.count;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.item;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.itemArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.kindArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.name;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.numberArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.say;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.warehouseArgument;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.command.ServusFoundationCommands;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** {@code take}, {@code feed}, {@code ship} and {@code move}: items out of a warehouse or along its data line. */
final class StockCommands {

    private static final String OUTBOUND = "outbound";

    private StockCommands() {
    }

    static void addTo(LiteralArgumentBuilder<CommandSourceStack> shuttles, CommandBuildContext context) {
        shuttles.then(Commands.literal("take")
                        .then(warehouseArgument().then(itemArgument(context)
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes(StockCommands::take)))))
                .then(Commands.literal("feed")
                        .then(warehouseArgument()
                                .then(Commands.argument(OUTBOUND, StringArgumentType.word())
                                        .suggests(CommandTargets.numbers(ServusShuttlesMachineKinds.OUTBOUND_STATION))
                                        .then(itemCount(context, StockCommands::feed)))))
                .then(Commands.literal("ship")
                        .then(warehouseArgument()
                                .then(kindArgument("kind")
                                        .then(numberArgument("number", "kind").then(itemCount(context, StockCommands::ship))))))
                .then(Commands.literal("move")
                        .then(warehouseArgument()
                                .then(kindArgument("from-kind")
                                        .then(numberArgument("from", "from-kind")
                                                .then(kindArgument("to-kind")
                                                        .then(numberArgument("to", "to-kind").then(itemCount(context, StockCommands::move))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> itemCount(CommandBuildContext context, Command<CommandSourceStack> run) {
        return itemArgument(context).then(Commands.argument("count", IntegerArgumentType.integer(1, 4096)).executes(run));
    }

    /** Takes straight out of the racks: into the running player's inventory, else dropped at the controller. */
    private static int take(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        ItemStack taken = target.loaded().takeDirect(ItemKey.of(item(context)), count(context));
        if (taken.isEmpty()) {
            throw CommandTargets.fail("Nothing taken");
        }
        int amount = taken.getCount();
        String what = name(taken);
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            player.getInventory().placeItemBackInInventory(taken);
        } else {
            Block.popResource(target.level(), target.controllerPos().above(), taken);
        }
        say(source, "Took " + amount + " " + what + " from " + target.label());
        return amount;
    }

    private static int feed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Machine found = CommandTargets.machine(context, ServusShuttlesMachineKinds.OUTBOUND_STATION, OUTBOUND);
        if (!(found.machine() instanceof OutboundStationBlockEntity station)) {
            throw CommandTargets.fail("No Outbound Station at " + found.pos().toShortString());
        }
        WarehouseNetwork network = station.network().orElseThrow(() -> CommandTargets.fail("The station is in no warehouse"));
        ItemStack wanted = item(context);
        int count = count(context);
        String refused = network.feedStation(station, ItemKey.of(wanted), count);
        if (!refused.isEmpty()) {
            throw CommandTargets.fail("Feed refused: " + refused.replace('_', ' '));
        }
        say(source, "Feeding " + count + " " + name(wanted) + " to the station");
        return count;
    }

    private static int ship(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        CommandTargets.Machine to = CommandTargets.machine(context, CommandTargets.kind(context, "kind"), "number");
        ItemStack wanted = item(context);
        WarehouseNetwork.Shipping shipping = target.loaded().ship(to.pos(), ItemKey.of(wanted), count(context));
        if (!shipping.shipped()) {
            throw CommandTargets.fail("Shipping refused: " + shipping.refusal().replace('_', ' ') + " for " + name(wanted));
        }
        int planned = shipping.shipment().map(shipment -> shipment.planned()).orElse(0);
        say(source, "Shipping " + planned + " " + name(wanted) + " via the Packing Station at " + shipping.packingStation().toShortString());
        return planned;
    }

    /** A data line move between two numbered members of the warehouse's line. */
    private static int move(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandTargets.Machine from = CommandTargets.machine(context, CommandTargets.kind(context, "from-kind"), "from");
        CommandTargets.Machine to = CommandTargets.machine(context, CommandTargets.kind(context, "to-kind"), "to");
        if (!from.level().equals(to.level())) {
            throw CommandTargets.fail("Both machines must stand in one dimension");
        }
        return ServusFoundationCommands.startMove(context.getSource(), from.level(), from.pos(), to.pos(), item(context), count(context));
    }
}
