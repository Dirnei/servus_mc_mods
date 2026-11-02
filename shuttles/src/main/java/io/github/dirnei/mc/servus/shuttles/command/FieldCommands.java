package io.github.dirnei.mc.servus.shuttles.command;

import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.lower;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.name;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.say;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.warehouseArgument;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** {@code field}, {@code field recheck} and {@code field age} on one Field Head. */
final class FieldCommands {

    private static final String HEAD = "field-head";

    private FieldCommands() {
    }

    static void addTo(LiteralArgumentBuilder<CommandSourceStack> shuttles) {
        shuttles.then(Commands.literal("field")
                .then(Commands.literal("recheck")
                        .then(warehouseArgument().then(headArgument().executes(FieldCommands::recheck))))
                .then(Commands.literal("age")
                        .then(warehouseArgument().then(headArgument()
                                .then(Commands.argument("mB", IntegerArgumentType.integer(1)).executes(FieldCommands::age)))))
                .then(warehouseArgument().then(headArgument().executes(FieldCommands::field))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> headArgument() {
        return Commands.argument(HEAD, StringArgumentType.word()).suggests(CommandTargets.numbers(ServusShuttlesMachineKinds.FIELD_HEAD));
    }

    private static FieldHeadBlockEntity head(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandTargets.Machine found = CommandTargets.machine(context, ServusShuttlesMachineKinds.FIELD_HEAD, HEAD);
        if (!(found.machine() instanceof FieldHeadBlockEntity head)) {
            throw CommandTargets.fail("No Field Head at " + found.pos().toShortString());
        }
        return head;
    }

    private static int field(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        FieldHeadBlockEntity head = head(context);
        boolean trees = head.kind().trees();
        String mode;
        if (head.harvestOnly()) {
            mode = trees ? "fell only" : "harvest only";
        } else if (head.crop().isEmpty()) {
            mode = trees ? "no sapling" : "no crop";
        } else {
            mode = (trees ? "sapling " : "crop ") + name(head.crop()) + (head.twoByTwo() ? " 2x2" : "");
        }
        say(source, head.chatLabel().copy().append(" " + head.getBlockPos().toShortString() + ": " + lower(head.status())
                + ", kind " + lower(head.kind())));
        List<String> lines = List.of(
                "Rails " + head.rails().size() + ", plots " + head.plots().size() + ", planted " + head.plantedCount() + ", ripe " + head.ripeCount()
                        + ", to till " + head.tillableCount() + ", to plant " + head.plantableCount(),
                "Water " + head.water() + " / " + head.capacity() + " mB, " + (head.hydrated() ? "hydrated" : "dry"),
                mode + ", harvest " + (head.harvestDue() ? "due" : "not due"),
                "Work: " + (head.work() == null ? "none" : lower(head.work())),
                "Fertilize: " + lower(head.fertilizeMode())
                        + (head.fertilizer().isEmpty() ? ", no fertilizer" : " with " + name(head.fertilizer()))
                        + ", " + head.fertilizeTargetCount() + " plots need a dose, keep " + head.reserve() + ", status "
                        + (head.fertilizeStatus() == null ? "none" : lower(head.fertilizeStatus())),
                "Buffer: " + bufferLine(head),
                "Produce: " + (head.produce().isEmpty() ? "unknown" : name(head.produce()) + ", " + head.produceStock() + " in stock")
                        + ", stop harvesting at " + level(head.harvestLimit()) + ", fertilize below " + level(head.fertilizeBelow())
                        + (head.harvestPaused() ? ", harvest paused" : "") + (head.fertilizeAllowed() ? "" : ", fertilizing not needed"),
                "Corrosion: " + stageCountsLine(head) + ", " + head.dryRails() + " dry, "
                        + head.agedWater() + " / " + ShuttlesStatsManager.INSTANCE.fieldCorrosionWater() + " mB to the next step, "
                        + head.supplyCount(SupplyBuffer.Part.ANTI_CORROSION) + " anti-corrosion"
                        + ", keep " + head.antiCorrosionReserve());
        for (String line : lines) {
            say(source, line);
        }
        return head.rails().size();
    }

    private static int recheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        FieldHeadBlockEntity head = head(context);
        head.recheckAll();
        say(context.getSource(), "Rechecked " + head.plots().size() + " plots: planted " + head.plantedCount() + ", ripe " + head.ripeCount());
        return head.ripeCount();
    }

    /** Counts water as used for aging, like hydration does, to reach a stage quickly. */
    private static int age(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        FieldHeadBlockEntity head = head(context);
        if (head.status() != FieldStatus.SERVING) {
            throw CommandTargets.fail("The Field Head at " + head.getBlockPos().toShortString() + " serves no field");
        }
        head.age(IntegerArgumentType.getInteger(context, "mB"));
        say(context.getSource(), "Rails " + stageCountsLine(head) + ", " + head.dryRails() + " dry, " + head.agedWater() + " mB toward the next step");
        return head.wetRails().size();
    }

    /** Each part of the head's buffer as it holds it: item, count and level; stale items marked. */
    private static String bufferLine(FieldHeadBlockEntity head) {
        List<String> parts = new ArrayList<>();
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            Item held = head.buffer().item(part);
            String item = held == null ? "empty" : name(new ItemStack(held));
            String stale = held != null && held != head.supplyItem(part) ? " (stale)" : "";
            parts.add(lower(part) + " " + item + " " + head.buffer().count(part) + " / " + head.supplyLevel(part) + stale);
        }
        return String.join(", ", parts);
    }

    /** A stock level, "off" for 0. */
    private static String level(int value) {
        return value > 0 ? String.valueOf(value) : "off";
    }

    /** The rails per corrosion stage, only stages that have rails; "none" for an empty field. */
    private static String stageCountsLine(FieldHeadBlockEntity head) {
        int[] counts = head.stageCounts();
        List<String> parts = new ArrayList<>();
        for (int stage = FieldCorrosion.COPPER; stage <= FieldCorrosion.CORRODED; stage++) {
            if (counts[stage] > 0) {
                parts.add(FieldCorrosion.name(stage) + " " + counts[stage]);
            }
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }
}
