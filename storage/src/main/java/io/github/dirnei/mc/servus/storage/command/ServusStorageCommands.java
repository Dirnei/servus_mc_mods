package io.github.dirnei.mc.servus.storage.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBuffer;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static int fail(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
        return 0;
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static ItemStack item(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return ItemArgument.getItem(context, "item").createItemStack(1);
    }

    private static int count(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "count");
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    /** "2 Oak Planks, 1 Coal" for stacks with counts. */
    private static String counted(List<ItemStack> stacks) {
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : stacks) {
            parts.add(stack.getCount() + " " + name(stack));
        }
        return String.join(", ", parts);
    }

    private static Optional<FieldHeadBlockEntity> fieldHead(CommandContext<CommandSourceStack> context, BlockPos pos) {
        return context.getSource().getLevel().getBlockEntity(pos) instanceof FieldHeadBlockEntity head ? Optional.of(head) : Optional.empty();
    }

    private static int field(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<FieldHeadBlockEntity> found = fieldHead(context, pos);
        if (found.isEmpty()) {
            return fail(source, "No Field Head at " + pos.toShortString());
        }
        FieldHeadBlockEntity head = found.get();
        String mode = head.harvestOnly() ? "harvest only"
                : head.crop().isEmpty() ? "no crop" : "crop " + name(head.crop());
        List<String> lines = List.of(
                "Field Head " + pos.toShortString() + ": " + lower(head.status()),
                "Rails " + head.rails().size() + ", plots " + head.plots().size() + ", planted " + head.plantedCount() + ", ripe " + head.ripeCount()
                        + ", to till " + head.tillableCount() + ", to plant " + head.plantableCount(),
                "Water " + head.water() + " / " + head.capacity() + " mB, " + (head.hydrated() ? "hydrated" : "dry"),
                mode + ", harvest " + (head.harvestDue() ? "due" : "not due"),
                "Work: " + (head.work() == null ? "none" : lower(head.work())),
                "Fertilize: " + lower(head.fertilizeMode())
                        + (head.fertilizer().isEmpty() ? ", no fertilizer" : " with " + name(head.fertilizer()))
                        + ", " + head.fertilizeTargetCount() + " plots need a dose, keep " + head.reserve() + ", status "
                        + (head.fertilizeStatus() == null ? "none" : lower(head.fertilizeStatus())),
                "Corrosion: " + (head.corrosion() == FieldCorrosion.LOOSE ? "none" : FieldCorrosion.name(head.corrosion())
                        + ", " + head.agedWater() + " / " + StorageStatsManager.INSTANCE.fieldCorrosionWater() + " mB to the next step, "
                        + head.supplyCount(SupplyBuffer.Part.ANTI_CORROSION) + " anti-corrosion"));
        for (String line : lines) {
            say(source, line);
        }
        return head.rails().size();
    }

    private static int fieldRecheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<FieldHeadBlockEntity> found = fieldHead(context, pos);
        if (found.isEmpty()) {
            return fail(source, "No Field Head at " + pos.toShortString());
        }
        FieldHeadBlockEntity head = found.get();
        head.recheckAll();
        say(source, "Rechecked " + head.plots().size() + " plots: planted " + head.plantedCount() + ", ripe " + head.ripeCount());
        return head.ripeCount();
    }

    /** Counts water as used for aging, like hydration does, to reach a stage quickly. */
    private static int fieldAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<FieldHeadBlockEntity> found = fieldHead(context, pos).filter(head -> head.status() == FieldStatus.SERVING);
        if (found.isEmpty()) {
            return fail(source, "No serving Field Head at " + pos.toShortString());
        }
        FieldHeadBlockEntity head = found.get();
        head.age(IntegerArgumentType.getInteger(context, "water"));
        say(source, "Field is " + FieldCorrosion.name(head.corrosion()) + ", " + head.agedWater() + " mB toward the next step");
        return head.corrosion();
    }

    private static int take(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack taken = network.get().takeDirect(ItemKey.of(item(context)), count(context));
        if (taken.isEmpty()) {
            return fail(source, "Nothing taken");
        }
        Block.popResource(source.getLevel(), pos.above(), taken);
        say(source, "Took " + taken.getCount() + " " + name(taken));
        return taken.getCount();
    }

    private static Optional<WarehouseNetwork> networkAt(CommandSourceStack source, BlockPos pos) {
        if (!(source.getLevel().getBlockEntity(pos) instanceof StorageNodeBlockEntity node)) {
            fail(source, "No storage block at " + pos.toShortString());
            return Optional.empty();
        }
        Optional<WarehouseNetwork> network = node.network();
        if (network.isEmpty()) {
            fail(source, "Storage block at " + pos.toShortString() + " is not in a network");
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
        List<RuleRow> rules = network.get().ruleRows();
        if (rules.isEmpty()) {
            say(source, "No stock rules");
            return 0;
        }
        for (RuleRow rule : rules) {
            say(source, "  - " + name(rule.stack()) + ": keep " + rule.min() + " to " + rule.max() + ", " + rule.state().replace('_', ' '));
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
        ItemStack item = item(context);
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");
        network.get().setStockRule(ItemKey.of(item), min, max);
        say(source, "Keeping " + min + " to " + Math.max(min, max) + " " + name(item));
        return 1;
    }

    private static int unrule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack item = item(context);
        if (!network.get().removeStockRule(ItemKey.of(item))) {
            return fail(source, "No rule for " + name(item));
        }
        say(source, "Rule for " + name(item) + " removed");
        return 1;
    }

    private static int orders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        List<CpuBlockEntity> cpus = CpuOrders.cpus(source.getLevel(), pos);
        if (cpus.isEmpty()) {
            return fail(source, "No Crafting CPU on the data line at " + pos.toShortString());
        }
        int count = 0;
        for (CpuBlockEntity cpu : cpus) {
            CpuShape.Vec size = cpu.boxSize();
            int open = cpu.book().openCount();
            say(source, "CPU " + cpu.getBlockPos().toShortString()
                    + (size == null ? "" : " " + size.x() + "x" + size.y() + "x" + size.z())
                    + ": " + Math.min(open, cpu.slots()) + " of " + cpu.slots() + " slots, "
                    + cpu.bufferUsed() + " of " + cpu.room() + " buffer stacks");
            List<CpuBuffer.Entry<ItemKey>> buffered = cpu.buffer().entries();
            for (WorkOrder order : cpu.book().orders()) {
                count++;
                sayOrder(source, order, buffered);
            }
            for (CpuBuffer.Entry<ItemKey> entry : buffered) {
                if (entry.leaving()) {
                    say(source, "  leaving buffer: " + entry.count() + " " + name(entry.key().stack(1)) + " (#" + entry.order() + ")");
                }
            }
        }
        if (count == 0) {
            say(source, "No orders");
        }
        return count;
    }

    /** One order with its steps and what waits for it in the buffer. */
    private static void sayOrder(CommandSourceStack source, WorkOrder order, List<CpuBuffer.Entry<ItemKey>> buffered) {
        say(source, "  #" + order.id() + " " + order.amount() + " " + name(order.key().stack(1)) + ": "
                + lower(order.state()) + (order.reason().isEmpty() ? "" : ", " + order.reason())
                + (order.requestId() > 0 ? " (request " + order.requestId() + ")" : order.requestId() < 0 ? " (rule)" : ""));
        for (OrderStep step : order.steps()) {
            String machine = source.getLevel().getBlockState(step.producer()).getBlock().getName().getString();
            say(source, "      " + machine + " " + step.producer().toShortString() + ": " + name(step.output())
                    + " x" + step.batches() + " " + lower(step.state())
                    + (step.reason().isEmpty() ? "" : ", " + step.reason()));
        }
        for (CpuBuffer.Entry<ItemKey> entry : buffered) {
            if (entry.order() == order.id() && !entry.leaving()) {
                say(source, "      buffer: " + entry.count() + " " + name(entry.key().stack(1)));
            }
        }
    }

    private static int order(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        ItemStack wanted = item(context);
        int count = count(context);
        CpuOrders.Placed placed = CpuOrders.create(source.getLevel(), pos, ItemKey.of(wanted), count, 0);
        if (!placed.made()) {
            return fail(source, "Order refused: " + placed.refusal().replace('_', ' ') + " for " + name(wanted));
        }
        say(source, "Ordered " + count + " " + name(wanted) + " on the CPU at " + placed.cpu().toShortString());
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
        ItemStack wanted = item(context);
        WarehouseNetwork.Shipping shipping = network.get().ship(target, ItemKey.of(wanted), count(context));
        if (!shipping.shipped()) {
            return fail(source, "Shipping refused: " + shipping.refusal().replace('_', ' ') + " for " + name(wanted));
        }
        int planned = shipping.shipment().map(shipment -> shipment.planned()).orElse(0);
        say(source, "Shipping " + planned + " " + name(wanted) + " via the Packing Station at " + shipping.packingStation().toShortString());
        return planned;
    }

    private static int producers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Optional<DataNetwork> line = CpuOrders.network(level, pos);
        if (line.isEmpty()) {
            return fail(source, "No data line at " + pos.toShortString());
        }
        List<DataMember> producers = line.get().dataMembers(DataRole.PRODUCER);
        if (producers.isEmpty()) {
            say(source, "No producers on the data line");
            return 0;
        }
        for (DataMember member : producers) {
            ProductionProvider provider = level.getCapability(ServusFoundationCapabilities.PRODUCTION, member.pos(), null);
            String head = "  - " + level.getBlockState(member.pos()).getBlock().getName().getString() + " " + member.pos().toShortString();
            if (provider == null || provider.recipes().isEmpty()) {
                say(source, head + ": no recipes configured");
                continue;
            }
            say(source, head + provider.active().map(id -> ", working on " + id.getPath()).orElse(""));
            for (ProductionRecipe recipe : provider.recipes()) {
                say(source, "      " + counted(recipe.outputs()) + " from " + counted(recipe.inputs()) + ", " + recipe.ticks() + " ticks");
            }
        }
        return producers.size();
    }

    private static int feed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "station");
        CommandSourceStack source = context.getSource();
        if (!(source.getLevel().getBlockEntity(pos) instanceof OutboundStationBlockEntity station)) {
            return fail(source, "No outbound station at " + pos.toShortString());
        }
        Optional<WarehouseNetwork> network = networkAt(source, pos);
        if (network.isEmpty()) {
            return 0;
        }
        ItemStack wanted = item(context);
        int count = count(context);
        String refused = network.get().feedStation(station, ItemKey.of(wanted), count);
        if (!refused.isEmpty()) {
            return fail(source, "Feed refused: " + refused.replace('_', ' '));
        }
        say(source, "Feeding " + count + " " + name(wanted) + " to the station");
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
            say(source, "No zones");
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
            say(source, line.toString());
        }
        return zones.size();
    }

    /** Asks the site whose controller is at the position to wake; works on unloaded positions. */
    private static int wake(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Optional<SiteWakes.Status> status = SiteWakes.request(level, pos);
        if (status.isEmpty()) {
            return fail(source, "No site has its controller at " + pos.toShortString());
        }
        String name = SiteRegistry.of(level).site(level.dimension(), pos).map(SiteRecord::name).orElse("?");
        SiteWakes.Status wake = status.get();
        if (wake.awake()) {
            source.sendSuccess(() -> Component.literal(name + " is awake, " + wake.forced() + " chunks loaded"), true);
            return 1;
        }
        source.sendFailure(Component.literal(name + " does not wake: ").append(SiteWakes.reason(wake.reason())));
        return 0;
    }

    /** Lists every site record. */
    private static int sites(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<SiteRecord> sites = SiteRegistry.get(source.getServer()).all();
        say(source, sites.size() + " sites");
        for (SiteRecord site : sites) {
            boolean awake = SiteWakes.isAwake(site.dimension(), site.pos());
            String team = site.team().map(id -> id.toString().substring(0, 8)).orElse("no team");
            say(source, "  - " + site.name() + " at " + site.dimension().identifier() + " "
                    + site.pos().toShortString() + ", team " + team + ", " + site.span().size() + " of " + site.capacity()
                    + " chunks, " + (awake ? "awake" : "asleep") + ", " + site.stock().size() + " item types known");
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
        warehouse.controller().ifPresent(controller -> say(source, "Site " + controller.displayName() + ": "
                + controller.span().size() + " of " + controller.capacity() + " chunks, " + controller.upgrades().count()
                + " Anchor Upgrades, " + lower(controller.siteState())));
        List<Map.Entry<ItemKey, Long>> entries = warehouse.index().entries();
        int shown = 0;
        for (Map.Entry<ItemKey, Long> entry : entries) {
            if (shown++ >= MAX_LINES) {
                say(source, "  ... " + (entries.size() - MAX_LINES) + " more");
                break;
            }
            say(source, "  - " + name(entry.getKey().stack(1)) + " x " + entry.getValue());
        }
        return entries.size();
    }
}
