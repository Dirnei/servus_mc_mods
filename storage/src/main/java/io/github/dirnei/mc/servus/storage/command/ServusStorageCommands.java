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
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
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
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("info")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::info)))
                .then(Commands.literal("wake")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::wake)))
                .then(Commands.literal("sites")
                        .executes(ServusStorageCommands::sites))
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
                .then(Commands.literal("ship")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("target", BlockPosArgument.blockPos())
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 4096))
                                                        .executes(ServusStorageCommands::ship))))))
                .then(Commands.literal("field")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ServusStorageCommands::field)
                                .then(Commands.literal("recheck")
                                        .executes(ServusStorageCommands::fieldRecheck))
                                .then(Commands.literal("age")
                                        .then(Commands.argument("water", IntegerArgumentType.integer(1))
                                                .executes(ServusStorageCommands::fieldAge)))))
                .then(Commands.literal("take")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ServusStorageCommands::take))))));
    }

    private static int field(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel().getBlockEntity(pos) instanceof FieldHeadBlockEntity head)) {
            source.sendFailure(Component.literal("No Field Head at " + pos.toShortString()));
            return 0;
        }
        String mode = head.harvestOnly() ? "harvest only"
                : head.crop().isEmpty() ? "no crop" : "crop " + head.crop().getHoverName().getString();
        List<String> lines = List.of(
                "Field Head " + pos.toShortString() + ": " + head.status().name().toLowerCase(java.util.Locale.ROOT),
                "Rails " + head.rails().size() + ", plots " + head.plots().size() + ", planted " + head.plantedCount() + ", ripe " + head.ripeCount()
                        + ", to till " + head.tillableCount() + ", to plant " + head.plantableCount(),
                "Water " + head.water() + " / " + head.capacity() + " mB, " + (head.hydrated() ? "hydrated" : "dry"),
                mode + ", harvest " + (head.harvestDue() ? "due" : "not due"),
                "Work: " + (head.work() == null ? "none" : head.work().name().toLowerCase(java.util.Locale.ROOT)),
                "Fertilize: " + head.fertilizeMode().name().toLowerCase(java.util.Locale.ROOT)
                        + (head.fertilizer().isEmpty() ? ", no fertilizer" : " with " + head.fertilizer().getHoverName().getString())
                        + ", " + head.fertilizeTargetCount() + " plots need a dose, keep " + head.reserve() + ", status "
                        + (head.fertilizeStatus() == null ? "none" : head.fertilizeStatus().name().toLowerCase(java.util.Locale.ROOT)),
                "Corrosion: " + (head.corrosion() == FieldCorrosion.LOOSE ? "none" : FieldCorrosion.name(head.corrosion())
                        + ", " + head.agedWater() + " / " + StorageStatsManager.INSTANCE.fieldCorrosionWater() + " mB to the next step, "
                        + head.antiCorrosion().getStackInSlot(0).getCount() + " anti-corrosion"));
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return head.rails().size();
    }

    private static int fieldRecheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel().getBlockEntity(pos) instanceof FieldHeadBlockEntity head)) {
            source.sendFailure(Component.literal("No Field Head at " + pos.toShortString()));
            return 0;
        }
        head.recheckAll();
        source.sendSuccess(() -> Component.literal("Rechecked " + head.plots().size() + " plots: planted "
                + head.plantedCount() + ", ripe " + head.ripeCount()), false);
        return head.ripeCount();
    }

    /** Counts water as used for aging, like hydration does, to reach a stage quickly. */
    private static int fieldAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel().getBlockEntity(pos) instanceof FieldHeadBlockEntity head) || head.status() != FieldStatus.SERVING) {
            source.sendFailure(Component.literal("No serving Field Head at " + pos.toShortString()));
            return 0;
        }
        head.age(IntegerArgumentType.getInteger(context, "water"));
        source.sendSuccess(() -> Component.literal("Field is " + FieldCorrosion.name(head.corrosion()) + ", "
                + head.agedWater() + " mB toward the next step"), false);
        return head.corrosion();
    }

    private static int take(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1);
        int count = IntegerArgumentType.getInteger(context, "count");
        ItemStack taken = network.get().takeDirect(ItemKey.of(wanted), count);
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
        ItemStack item = ItemArgument.getItem(context, "item").createItemStack(1);
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
        ItemStack item = ItemArgument.getItem(context, "item").createItemStack(1);
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
        java.util.List<io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity> cpus = io.github.dirnei.mc.servus.storage.cpu.CpuOrders.cpus(source.getLevel(), pos);
        if (cpus.isEmpty()) {
            source.sendFailure(Component.literal("No Crafting CPU on the data line at " + pos.toShortString()));
            return 0;
        }
        int count = 0;
        for (io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity cpu : cpus) {
            io.github.dirnei.mc.servus.storage.cpu.CpuShape.Vec size = cpu.boxSize();
            int open = cpu.book().openCount();
            String title = "CPU " + cpu.getBlockPos().toShortString()
                    + (size == null ? "" : " " + size.x() + "x" + size.y() + "x" + size.z())
                    + ": " + Math.min(open, cpu.slots()) + " of " + cpu.slots() + " slots, "
                    + cpu.bufferUsed() + " of " + cpu.room() + " buffer stacks";
            source.sendSuccess(() -> Component.literal(title), false);
            java.util.List<io.github.dirnei.mc.servus.storage.cpu.CpuBuffer.Entry<ItemKey>> buffered = cpu.buffer().entries();
            for (io.github.dirnei.mc.servus.storage.order.WorkOrder order : cpu.book().orders()) {
                count++;
                String head = "  #" + order.id() + " " + order.amount() + " " + order.key().stack(1).getHoverName().getString() + ": "
                        + order.state().name().toLowerCase(java.util.Locale.ROOT) + (order.reason().isEmpty() ? "" : ", " + order.reason())
                        + (order.requestId() > 0 ? " (request " + order.requestId() + ")" : order.requestId() < 0 ? " (rule)" : "");
                source.sendSuccess(() -> Component.literal(head), false);
                for (io.github.dirnei.mc.servus.storage.order.OrderStep step : order.steps()) {
                    String name = source.getLevel().getBlockState(step.producer()).getBlock().getName().getString();
                    String line = "      " + name + " " + step.producer().toShortString() + ": " + step.output().getHoverName().getString()
                            + " x" + step.batches() + " " + step.state().name().toLowerCase(java.util.Locale.ROOT)
                            + (step.reason().isEmpty() ? "" : ", " + step.reason());
                    source.sendSuccess(() -> Component.literal(line), false);
                }
                for (io.github.dirnei.mc.servus.storage.cpu.CpuBuffer.Entry<ItemKey> entry : buffered) {
                    if (entry.order() == order.id() && !entry.leaving()) {
                        String line = "      buffer: " + entry.count() + " " + entry.key().stack(1).getHoverName().getString();
                        source.sendSuccess(() -> Component.literal(line), false);
                    }
                }
            }
            for (io.github.dirnei.mc.servus.storage.cpu.CpuBuffer.Entry<ItemKey> entry : buffered) {
                if (entry.leaving()) {
                    String line = "  leaving buffer: " + entry.count() + " " + entry.key().stack(1).getHoverName().getString()
                            + " (#" + entry.order() + ")";
                    source.sendSuccess(() -> Component.literal(line), false);
                }
            }
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No orders"), false);
        }
        return count;
    }

    private static int order(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1);
        int count = IntegerArgumentType.getInteger(context, "count");
        io.github.dirnei.mc.servus.storage.cpu.CpuOrders.Placed placed =
                io.github.dirnei.mc.servus.storage.cpu.CpuOrders.create(source.getLevel(), pos, ItemKey.of(wanted), count, 0);
        if (!placed.made()) {
            source.sendFailure(Component.literal("Order refused: " + placed.refusal().replace('_', ' ') + " for " + wanted.getHoverName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ordered " + count + " " + wanted.getHoverName().getString()
                + " on the CPU at " + placed.cpu().toShortString()), false);
        return count;
    }

    private static int ship(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        BlockPos target = BlockPosArgument.getLoadedBlockPos(context, "target");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1);
        int count = IntegerArgumentType.getInteger(context, "count");
        WarehouseNetwork.Shipping shipping = network.get().ship(target, ItemKey.of(wanted), count);
        if (!shipping.shipped()) {
            source.sendFailure(Component.literal("Shipping refused: " + shipping.refusal().replace('_', ' ') + " for " + wanted.getHoverName().getString()));
            return 0;
        }
        int planned = shipping.shipment().map(shipment -> shipment.planned()).orElse(0);
        BlockPos via = shipping.packingStation();
        source.sendSuccess(() -> Component.literal("Shipping " + planned + " " + wanted.getHoverName().getString()
                + " via the Packing Station at " + via.toShortString()), false);
        return planned;
    }

    private static int producers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<io.github.dirnei.mc.servus.foundation.data.DataNetwork> line =
                io.github.dirnei.mc.servus.storage.cpu.CpuOrders.network(source.getLevel(), pos);
        if (line.isEmpty()) {
            source.sendFailure(Component.literal("No data line at " + pos.toShortString()));
            return 0;
        }
        List<io.github.dirnei.mc.servus.foundation.data.DataMember> producers =
                line.get().dataMembers(io.github.dirnei.mc.servus.foundation.data.DataRole.PRODUCER);
        if (producers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No producers on the data line"), false);
            return 0;
        }
        for (io.github.dirnei.mc.servus.foundation.data.DataMember member : producers) {
            io.github.dirnei.mc.servus.foundation.production.ProductionProvider provider = source.getLevel()
                    .getCapability(io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities.PRODUCTION, member.pos(), null);
            String head = "  - " + source.getLevel().getBlockState(member.pos()).getBlock().getName().getString() + " " + member.pos().toShortString();
            if (provider == null || provider.recipes().isEmpty()) {
                source.sendSuccess(() -> Component.literal(head + ": no recipes configured"), false);
                continue;
            }
            source.sendSuccess(() -> Component.literal(head + (provider.active().map(id -> ", working on " + id.getPath()).orElse(""))), false);
            for (io.github.dirnei.mc.servus.foundation.production.ProductionRecipe recipe : provider.recipes()) {
                List<String> inputs = new ArrayList<>();
                for (ItemStack input : recipe.inputs()) {
                    inputs.add(input.getCount() + " " + input.getHoverName().getString());
                }
                List<String> outputs = new ArrayList<>();
                for (ItemStack output : recipe.outputs()) {
                    outputs.add(output.getCount() + " " + output.getHoverName().getString());
                }
                String line2 = "      " + String.join(", ", outputs) + " from " + String.join(", ", inputs) + ", " + recipe.ticks() + " ticks";
                source.sendSuccess(() -> Component.literal(line2), false);
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
        ItemStack wanted = ItemArgument.getItem(context, "item").createItemStack(1);
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

    /** Asks the site whose controller is at the position to wake; works on unloaded positions. */
    private static int wake(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        var level = source.getLevel();
        var status = io.github.dirnei.mc.servus.storage.site.SiteWakes.request(level, pos);
        if (status.isEmpty()) {
            source.sendFailure(Component.literal("No site has its controller at " + pos.toShortString()));
            return 0;
        }
        String name = io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(level).site(level.dimension(), pos)
                .map(io.github.dirnei.mc.servus.storage.site.SiteRecord::name).orElse("?");
        var wake = status.get();
        if (wake.awake()) {
            source.sendSuccess(() -> Component.literal(name + " is awake, " + wake.forced() + " chunks loaded"), true);
            return 1;
        }
        source.sendFailure(Component.literal(name + " does not wake: ")
                .append(io.github.dirnei.mc.servus.storage.site.SiteWakes.reason(wake.reason())));
        return 0;
    }

    /** Lists every site record. */
    private static int sites(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var sites = io.github.dirnei.mc.servus.storage.site.SiteRegistry.get(source.getServer()).all();
        source.sendSuccess(() -> Component.literal(sites.size() + " sites"), false);
        for (var site : sites) {
            boolean awake = io.github.dirnei.mc.servus.storage.site.SiteWakes.isAwake(site.dimension(), site.pos());
            String team = site.team().map(id -> id.toString().substring(0, 8)).orElse("no team");
            source.sendSuccess(() -> Component.literal("  - " + site.name() + " at " + site.dimension().identifier() + " "
                    + site.pos().toShortString() + ", team " + team + ", " + site.span().size() + " of " + site.capacity()
                    + " chunks, " + (awake ? "awake" : "asleep") + ", " + site.stock().size() + " item types known"), false);
        }
        return sites.size();
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
        warehouse.controller().ifPresent(controller -> source.sendSuccess(() -> Component.literal("Site " + controller.displayName() + ": "
                + controller.span().size() + " of " + controller.capacity() + " chunks, " + controller.upgrades().count()
                + " Anchor Upgrades, " + controller.siteState().name().toLowerCase(java.util.Locale.ROOT)), false));
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
