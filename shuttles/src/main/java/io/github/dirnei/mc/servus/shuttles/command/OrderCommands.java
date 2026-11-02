package io.github.dirnei.mc.servus.shuttles.command;

import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.count;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.item;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.itemArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.lower;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.name;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.say;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.warehouseArgument;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBuffer;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuShape;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.OrderStep;
import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;
import io.github.dirnei.mc.servus.shuttles.order.WorkOrder;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** {@code orders}, {@code order}, {@code rules}, {@code rule}, {@code cap}, {@code uncap} and {@code unrule}. */
final class OrderCommands {

    private OrderCommands() {
    }

    static void addTo(LiteralArgumentBuilder<CommandSourceStack> shuttles, CommandBuildContext context) {
        shuttles.then(Commands.literal("orders").then(warehouseArgument().executes(OrderCommands::orders)))
                .then(Commands.literal("order")
                        .then(warehouseArgument().then(itemArgument(context)
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 4096)).executes(OrderCommands::order)))))
                .then(Commands.literal("rules").then(warehouseArgument().executes(OrderCommands::rules)))
                .then(Commands.literal("rule")
                        .then(warehouseArgument().then(itemArgument(context)
                                .then(Commands.argument("min", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(0)).executes(OrderCommands::rule))))))
                .then(Commands.literal("cap")
                        .then(warehouseArgument().then(itemArgument(context)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(OrderCommands::cap)))))
                .then(Commands.literal("uncap").then(warehouseArgument().then(itemArgument(context).executes(OrderCommands::uncap))))
                .then(Commands.literal("unrule").then(warehouseArgument().then(itemArgument(context).executes(OrderCommands::unrule))));
    }

    private static int orders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        WarehouseNetwork warehouse = target.loaded();
        ServerLevel level = target.level();
        List<CpuBlockEntity> cpus = CpuOrders.network(level, warehouse).map(line -> CpuOrders.cpus(level, line)).orElse(List.of());
        if (cpus.isEmpty()) {
            throw CommandTargets.fail("No Crafting CPU on the data line of " + target.label());
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
                sayOrder(source, level, order, buffered);
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
    private static void sayOrder(CommandSourceStack source, ServerLevel level, WorkOrder order, List<CpuBuffer.Entry<ItemKey>> buffered) {
        say(source, "  #" + order.id() + " " + order.amount() + " " + name(order.key().stack(1)) + ": "
                + lower(order.state()) + (order.reason().isEmpty() ? "" : ", " + order.reason())
                + (order.requestId() > 0 ? " (request " + order.requestId() + ")" : order.requestId() < 0 ? " (rule)" : ""));
        for (OrderStep step : order.steps()) {
            String machine = level.getBlockState(step.producer()).getBlock().getName().getString();
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
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        target.loaded();
        ItemStack wanted = item(context);
        int count = count(context);
        CpuOrders.Placed placed = CpuOrders.create(target.level(), target.controllerPos(), ItemKey.of(wanted), count, 0);
        if (!placed.made()) {
            throw CommandTargets.fail("Order refused: " + placed.refusal().replace('_', ' ') + " for " + name(wanted));
        }
        say(source, "Ordered " + count + " " + name(wanted) + " on the CPU at " + placed.cpu().toShortString());
        return count;
    }

    private static int rules(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<RuleRow> rules = CommandTargets.warehouse(context).loaded().ruleRows();
        if (rules.isEmpty()) {
            say(source, "No stock rules");
            return 0;
        }
        for (RuleRow rule : rules) {
            say(source, ruleLine(rule));
        }
        return rules.size();
    }

    /** "  - Cobblestone: refuse above 4096, refused at Inbound #1"; the station label is resolved by the client. */
    private static Component ruleLine(RuleRow rule) {
        List<String> parts = new ArrayList<>();
        if (rule.hasMake()) {
            parts.add("keep " + rule.min() + " to " + rule.max());
        }
        if (rule.hasCap()) {
            parts.add("refuse above " + rule.cap());
        }
        if (rule.hasExport()) {
            parts.add("export above " + rule.export());
        }
        boolean refused = rule.state().equals("refusing");
        parts.add(refused ? "refused at " : rule.state().replace('_', ' '));
        MutableComponent line = Component.literal("  - " + name(rule.stack()) + ": " + String.join(", ", parts));
        return refused ? line.append(rule.detail()) : line;
    }

    /** The reason a rule command was refused, in words. */
    private static String thresholdReason(String check) {
        return switch (check) {
            case RuleThresholds.MAKE_ORDER -> "the minimum must be below the maximum";
            case RuleThresholds.CAP_BELOW_MAX -> "the cap must not be below the maximum";
            case RuleThresholds.EMPTY -> "a rule needs a Make pair or a cap";
            default -> check.replace('_', ' ');
        };
    }

    private static int rule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WarehouseNetwork network = CommandTargets.warehouse(context).loaded();
        ItemStack item = item(context);
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");
        String check = RuleThresholds.check(min, max, 0);
        if (check.equals(RuleThresholds.OK)) {
            check = network.setStockRule(ItemKey.of(item), min, max);
        }
        if (!check.equals(RuleThresholds.OK)) {
            throw CommandTargets.fail("Rule refused: " + thresholdReason(check));
        }
        say(context.getSource(), "Keeping " + min + " to " + max + " " + name(item));
        return 1;
    }

    private static int cap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WarehouseNetwork network = CommandTargets.warehouse(context).loaded();
        ItemStack item = item(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        String check = network.setStockCap(ItemKey.of(item), amount);
        if (!check.equals(RuleThresholds.OK)) {
            throw CommandTargets.fail("Cap refused: " + thresholdReason(check));
        }
        say(context.getSource(), "Refusing " + name(item) + " at Inbound Stations above " + amount);
        return 1;
    }

    private static int uncap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WarehouseNetwork network = CommandTargets.warehouse(context).loaded();
        ItemStack item = item(context);
        if (!network.clearStockCap(ItemKey.of(item))) {
            throw CommandTargets.fail("No cap for " + name(item));
        }
        say(context.getSource(), "Cap for " + name(item) + " removed");
        return 1;
    }

    private static int unrule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WarehouseNetwork network = CommandTargets.warehouse(context).loaded();
        ItemStack item = item(context);
        if (!network.removeStockRule(ItemKey.of(item))) {
            throw CommandTargets.fail("No rule for " + name(item));
        }
        say(context.getSource(), "Rule for " + name(item) + " removed");
        return 1;
    }
}
