package io.github.dirnei.mc.servus.storage.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /servus_warehouse info|zones <pos>} and {@code take <pos> <item> <count>} for the network at a storage block. */
@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStorageCommands {

    private static final int MAX_LINES = 30;

    private ServusStorageCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("servus_warehouse")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::info)))
                .then(Commands.literal("zones")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::zones)))
                .then(Commands.literal("producers")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::producers)))
                .then(Commands.literal("feed")
                        .then(Commands.argument("station", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 4096))
                                                .executes(ServusStorageCommands::feed)))))
                .then(Commands.literal("orders")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::orders)))
                .then(Commands.literal("order")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 4096))
                                                .executes(ServusStorageCommands::order)))))
                .then(Commands.literal("rules")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::rules)))
                .then(Commands.literal("rule")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("min", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("max", IntegerArgumentType.integer(0))
                                                        .executes(ServusStorageCommands::rule))))))
                .then(Commands.literal("unrule")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(ServusStorageCommands::unrule))))
                .then(Commands.literal("take")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ServusStorageCommands::take))))));
    }

    private static int take(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int count = IntegerArgumentType.getInteger(context, "count");
        ItemStack taken = network.get().extract(ItemKey.of(wanted), count);
        if (taken.isEmpty()) {
            source.sendFailure(Component.literal("Nothing taken"));
            return 0;
        }
        Block.popResource(source.getLevel(), pos.above(), taken);
        source.sendSuccess(() -> Component.literal("Took " + taken.getCount() + " " + taken.getHoverName().getString()), false);
        return taken.getCount();
    }

    private static Optional<WarehouseNetwork> networkAt(CommandSourceStack source, BlockPos pos) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof StorageNodeBlockEntity node)) {
            source.sendFailure(Component.literal("No storage block at " + pos.toShortString()));
            return Optional.empty();
        }
        Optional<WarehouseNetwork> network = node.network();
        if (network.isEmpty()) {
            source.sendFailure(Component.literal("Storage block at " + pos.toShortString() + " is not in a network"));
        }
        return network;
    }

    private static int rules(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        List<io.github.dirnei.mc.servus.storage.terminal.RuleRow> rules = network.get().ruleRows();
        if (rules.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No stock rules"), false);
            return 0;
        }
        for (io.github.dirnei.mc.servus.storage.terminal.RuleRow rule : rules) {
            String line = "  - " + rule.stack().getHoverName().getString() + ": keep " + rule.min() + " to " + rule.max() + ", " + rule.state().replace('_', ' ');
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return rules.size();
    }

    private static int rule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack item = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");
        network.get().setStockRule(ItemKey.of(item), min, max);
        source.sendSuccess(() -> Component.literal("Keeping " + min + " to " + Math.max(min, max) + " " + item.getHoverName().getString()), false);
        return 1;
    }

    private static int unrule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack item = ItemArgument.getItem(context, "item").createItemStack(1, false);
        boolean removed = network.get().removeStockRule(ItemKey.of(item));
        if (!removed) {
            source.sendFailure(Component.literal("No rule for " + item.getHoverName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Rule for " + item.getHoverName().getString() + " removed"), false);
        return 1;
    }

    private static int orders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        List<io.github.dirnei.mc.servus.storage.order.WorkOrder> orders = network.get().orders().orders();
        if (orders.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No orders"), false);
            return 0;
        }
        for (io.github.dirnei.mc.servus.storage.order.WorkOrder order : orders) {
            String head = "  #" + order.id() + " " + order.amount() + " " + order.key().stack(1).getHoverName().getString() + ": "
                    + order.state().name().toLowerCase(java.util.Locale.ROOT) + (order.reason().isEmpty() ? "" : ", " + order.reason())
                    + (order.requestId() > 0 ? " (request " + order.requestId() + ")" : "");
            source.sendSuccess(() -> Component.literal(head), false);
            for (io.github.dirnei.mc.servus.storage.order.OrderStep step : order.steps()) {
                String line = "      station " + step.station().toShortString() + ": " + step.output().getHoverName().getString() + " x" + step.batches()
                        + " " + step.state().name().toLowerCase(java.util.Locale.ROOT) + (step.reason().isEmpty() ? "" : ", " + step.reason());
                source.sendSuccess(() -> Component.literal(line), false);
            }
        }
        return orders.size();
    }

    private static int order(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int count = IntegerArgumentType.getInteger(context, "count");
        String refused = network.get().order(ItemKey.of(wanted), count);
        if (!refused.isEmpty()) {
            source.sendFailure(Component.literal("Order refused: " + refused.replace('_', ' ') + " for " + wanted.getHoverName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ordered " + count + " " + wanted.getHoverName().getString()), false);
        return count;
    }

    private static int producers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        List<WarehouseNetwork.Producer> producers = network.get().producers();
        if (producers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No outbound stations"), false);
            return 0;
        }
        for (WarehouseNetwork.Producer producer : producers) {
            String head = "  - station " + producer.station().toShortString() + " faces " + producer.machineName().getString();
            if (!producer.producer()) {
                source.sendSuccess(() -> Component.literal(head + ": no producer"), false);
                continue;
            }
            if (producer.recipes().isEmpty()) {
                source.sendSuccess(() -> Component.literal(head + ": no recipes configured"), false);
                continue;
            }
            source.sendSuccess(() -> Component.literal(head + (producer.active().map(id -> ", working on " + id.getPath()).orElse(""))), false);
            for (io.github.dirnei.mc.servus.lib.production.ProductionRecipe recipe : producer.recipes()) {
                List<String> inputs = new ArrayList<>();
                for (ItemStack input : recipe.inputs()) {
                    inputs.add(input.getCount() + " " + input.getHoverName().getString());
                }
                List<String> outputs = new ArrayList<>();
                for (ItemStack output : recipe.outputs()) {
                    outputs.add(output.getCount() + " " + output.getHoverName().getString());
                }
                String line = "      " + String.join(", ", outputs) + " from " + String.join(", ", inputs) + ", " + recipe.ticks() + " ticks";
                source.sendSuccess(() -> Component.literal(line), false);
            }
        }
        return producers.size();
    }

    private static int feed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "station");
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel().getBlockEntity(pos) instanceof io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity station)) {
            source.sendFailure(Component.literal("No outbound station at " + pos.toShortString()));
            return 0;
        }
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1, false);
        int count = IntegerArgumentType.getInteger(context, "count");
        String refused = network.get().feedStation(station, ItemKey.of(wanted), count);
        if (!refused.isEmpty()) {
            source.sendFailure(Component.literal("Feed refused: " + refused.replace('_', ' ')));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Feeding " + count + " " + wanted.getHoverName().getString() + " to the station"), false);
        return count;
    }

    private static int zones(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        List<WarehouseNetwork.ZoneSummary> zones = network.get().zoneSummaries();
        if (zones.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No zones"), false);
            return 0;
        }
        for (WarehouseNetwork.ZoneSummary zone : zones) {
            StringBuilder line = new StringBuilder("  - ").append(zone.display()).append(": ").append(zone.blocks()).append(" blocks");
            if (!zone.items().isEmpty()) {
                line.append(", items ").append(zone.items());
            }
            if (!zone.tags().isEmpty()) {
                line.append(", tags ").append(zone.tags());
            }
            String text = line.toString();
            source.sendSuccess(() -> Component.literal(text), false);
        }
        return zones.size();
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        WarehouseNetwork warehouse = network.get();
        source.sendSuccess(warehouse::summary, false);
        List<Map.Entry<ItemKey, Long>> entries = warehouse.index().entries();
        int shown = 0;
        for (Map.Entry<ItemKey, Long> entry : entries) {
            if (shown++ >= MAX_LINES) {
                source.sendSuccess(() -> Component.literal("  ... " + (entries.size() - MAX_LINES) + " more"), false);
                break;
            }
            String name = entry.getKey().stack(1).getHoverName().getString();
            long count = entry.getValue();
            source.sendSuccess(() -> Component.literal("  - " + name + " x " + count), false);
        }
        return entries.size();
    }
}
