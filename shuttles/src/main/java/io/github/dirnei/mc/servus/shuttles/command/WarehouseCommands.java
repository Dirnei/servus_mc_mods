package io.github.dirnei.mc.servus.shuttles.command;

import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.counted;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.kindArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.lower;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.name;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.numberArgument;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.say;
import static io.github.dirnei.mc.servus.shuttles.command.ServusShuttlesCommands.warehouseArgument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.site.SiteRecord;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.site.SiteWakes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** {@code list}, {@code machines}, {@code info}, {@code zones}, {@code fluids}, {@code producers}, {@code wake} and {@code machine}. */
final class WarehouseCommands {

    private static final int MAX_LINES = 30;

    private WarehouseCommands() {
    }

    static void addTo(LiteralArgumentBuilder<CommandSourceStack> shuttles) {
        shuttles.then(Commands.literal("list").executes(WarehouseCommands::list))
                .then(Commands.literal("machines")
                        .then(warehouseArgument().executes(context -> machines(context, Optional.empty()))
                                .then(kindArgument("kind").executes(context -> machines(context,
                                        Optional.of(CommandTargets.kind(context, "kind")))))))
                .then(Commands.literal("info").then(warehouseArgument().executes(WarehouseCommands::info)))
                .then(Commands.literal("zones").then(warehouseArgument().executes(WarehouseCommands::zones)))
                .then(Commands.literal("fluids").then(warehouseArgument().executes(WarehouseCommands::fluids)))
                .then(Commands.literal("producers").then(warehouseArgument().executes(WarehouseCommands::producers)))
                .then(Commands.literal("wake").then(warehouseArgument().executes(WarehouseCommands::wake)))
                .then(Commands.literal("machine")
                        .then(warehouseArgument()
                                .then(kindArgument("kind")
                                        .then(numberArgument("number", "kind").executes(WarehouseCommands::machine)))));
    }

    /** Every warehouse of the world by number. */
    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<SiteRecord> sites = new ArrayList<>(SiteRegistry.get(source.getServer()).all());
        sites.sort(Comparator.comparingInt(SiteRecord::number));
        say(source, sites.size() + " warehouses");
        for (SiteRecord site : sites) {
            boolean awake = SiteWakes.isAwake(site.dimension(), site.pos());
            String team = site.team().map(id -> id.toString().substring(0, 8)).orElse("no team");
            say(source, "  W" + site.number() + " " + site.name() + " at " + site.dimension().identifier() + " "
                    + site.pos().toShortString() + ", team " + team + ", " + site.span().size() + " of " + site.capacity()
                    + " chunks, " + (awake ? "awake" : "asleep") + ", " + site.stock().size() + " item types known");
        }
        return sites.size();
    }

    /** The numbered machines of the warehouse, all or of one kind, by kind and number. */
    private static int machines(CommandContext<CommandSourceStack> context, Optional<MachineKind> kind) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse warehouse = CommandTargets.warehouse(context);
        List<NamedMachine> machines = new ArrayList<>(warehouse.loaded().numberedMachines().stream()
                .filter(machine -> kind.isEmpty() || machine.machineKind().equals(kind.get())).toList());
        machines.sort(Comparator.comparing((NamedMachine machine) -> machine.machineKind().id().toString())
                .thenComparingInt(NamedMachine::number));
        say(source, warehouse.label() + ": " + machines.size() + " numbered machines");
        for (NamedMachine machine : machines) {
            say(source, Component.literal("  - " + machine.machineKind().id().getPath() + " " + machine.number() + ": ")
                    .append(machine.label()).append(" at " + machine.nameSlot().pos().toShortString()));
        }
        return machines.size();
    }

    /** The label, place and Data Potato report of one machine. */
    private static int machine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Machine found = CommandTargets.machine(context, CommandTargets.kind(context, "kind"), "number");
        say(source, found.machine().chatLabel().copy().append(" at " + found.pos().toShortString() + " in "
                + found.level().dimension().identifier()));
        List<Component> report = DataPotatoItem.report(found.level(), found.pos(), found.machine(), Vec3.atCenterOf(found.pos()));
        report.stream().skip(1).forEach(line -> say(source, line));
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        WarehouseNetwork warehouse = target.loaded();
        source.sendSuccess(warehouse::summary, false);
        warehouse.controller().ifPresent(controller -> say(source, "Site W" + target.number() + " " + controller.displayName() + ": "
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

    private static int zones(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<WarehouseNetwork.ZoneSummary> zones = CommandTargets.warehouse(context).loaded().zoneSummaries();
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

    /** The warehouse's fluid stock per fluid, then every Fluid Rack shelf. */
    private static int fluids(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        WarehouseNetwork warehouse = CommandTargets.warehouse(context).loaded();
        List<Map.Entry<FluidKey, Long>> totals = warehouse.fluidIndex().entries();
        say(source, totals.isEmpty() ? "No fluid stock" : "Fluid stock:");
        for (Map.Entry<FluidKey, Long> entry : totals) {
            say(source, "  - " + entry.getKey().stack(1).getHoverName().getString() + ": " + entry.getValue() + " mB in "
                    + warehouse.fluidIndex().sourcesHolding(entry.getKey()).size() + " tanks");
        }
        List<WarehouseNetwork.TankView> racks = warehouse.rackViews();
        say(source, racks.isEmpty() ? "No Fluid Racks" : "Fluid Rack shelves:");
        for (WarehouseNetwork.TankView rack : racks) {
            say(source, "  - port " + rack.ref().pos().toShortString() + ", zone " + (rack.zone().isEmpty() ? "none" : rack.zone()) + "; "
                    + (rack.fluid().isEmpty() ? "empty" : rack.fluid().getHoverName().getString() + " " + rack.fluid().getAmount())
                    + " / " + rack.capacity() + " mB");
        }
        return totals.size();
    }

    private static int producers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        WarehouseNetwork warehouse = target.loaded();
        ServerLevel level = target.level();
        Optional<DataNetwork> line = CpuOrders.network(level, warehouse);
        if (line.isEmpty()) {
            say(source, target.label() + " has no data line");
            return 0;
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

    /** Asks the warehouse's site to wake; works while it sleeps. */
    private static int wake(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        CommandTargets.Warehouse target = CommandTargets.warehouse(context);
        Optional<SiteWakes.Status> status = SiteWakes.request(target.level(), target.controllerPos());
        if (status.isEmpty()) {
            throw CommandTargets.fail(target.label() + " has no site to wake");
        }
        SiteWakes.Status wake = status.get();
        if (wake.awake()) {
            source.sendSuccess(() -> Component.literal(target.label() + " is awake, " + wake.forced() + " chunks loaded"), true);
            return 1;
        }
        source.sendFailure(Component.literal(target.label() + " does not wake: ").append(SiteWakes.reason(wake.reason())));
        return 0;
    }
}
