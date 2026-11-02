package io.github.dirnei.mc.servus.shuttles.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.dirnei.mc.servus.foundation.naming.MachineKind;
import io.github.dirnei.mc.servus.foundation.naming.MachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.MachineLabels;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.site.SiteRecord;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Resolves the warehouse and machine parameters of the Shuttles commands. A warehouse is
 * its number or {@code here}, the warehouse of the block the player looks at; a machine is
 * its kind and its number within the warehouse, or {@code here} for the looked at machine.
 * Machines are found among the warehouse's known members only, never by scanning the world.
 */
public final class CommandTargets {

    /** The token for the looked at warehouse or machine. */
    public static final String HERE = "here";
    /** The parameter naming the warehouse. */
    public static final String WAREHOUSE = "warehouse";

    private CommandTargets() {
    }

    /** A warehouse named in a command: its record and the level of its dimension. */
    public record Warehouse(SiteRecord site, ServerLevel level) {

        public int number() {
            return site.number();
        }

        public BlockPos controllerPos() {
            return site.pos();
        }

        /** "W2 North Mine". */
        public String label() {
            return "W" + site.number() + " " + site.name();
        }

        /** The warehouse's network while its controller is loaded; fails naming {@code wake} otherwise. */
        public WarehouseNetwork loaded() throws CommandSyntaxException {
            if (level.isLoaded(site.pos()) && level.getBlockEntity(site.pos()) instanceof WarehouseControllerBlockEntity controller) {
                Optional<WarehouseNetwork> network = controller.network();
                if (network.isPresent()) {
                    return network.get();
                }
            }
            throw fail(label() + " is not loaded; run /servus shuttles wake " + number() + " first");
        }
    }

    /** A named machine found by a command, in the level it stands in. */
    public record Machine(NamedMachine machine, ServerLevel level) {

        /** The position of the block that holds the machine's name and number. */
        public BlockPos pos() {
            return machine.nameSlot().pos();
        }
    }

    /** The block a player looks at. */
    private record Looked(ServerLevel level, BlockPos pos) {
    }

    /** A command failure with the message. */
    public static CommandSyntaxException fail(String message) {
        return fail(Component.literal(message));
    }

    /** A command failure with the message. */
    public static CommandSyntaxException fail(Component message) {
        return new SimpleCommandExceptionType(message).create();
    }

    /** The warehouse of the {@value #WAREHOUSE} parameter. */
    public static Warehouse warehouse(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String token = StringArgumentType.getString(context, WAREHOUSE);
        CommandSourceStack source = context.getSource();
        SiteRegistry registry = SiteRegistry.get(source.getServer());
        if (token.equals(HERE)) {
            Looked looked = looked(source, "a warehouse number");
            WarehouseControllerBlockEntity controller = controllerOf(looked.level(), looked.pos())
                    .orElseThrow(() -> fail("The block you look at belongs to no warehouse with a controller"));
            registry.number(controller);
            SiteRecord site = registry.site(looked.level().dimension(), controller.getBlockPos())
                    .orElseThrow(() -> fail("The warehouse you look at has no record yet"));
            return new Warehouse(site, looked.level());
        }
        int number = number(token);
        SiteRecord site = registry.byNumber(number).orElseThrow(() -> fail("No warehouse W" + number + "; known: " + known(registry)));
        ServerLevel level = source.getServer().getLevel(site.dimension());
        if (level == null) {
            throw fail("The dimension " + site.dimension().identifier() + " of W" + number + " does not exist");
        }
        return new Warehouse(site, level);
    }

    /** The kind of a kind parameter: its id with or without the namespace. */
    public static MachineKind kind(CommandContext<CommandSourceStack> context, String parameter) throws CommandSyntaxException {
        String token = StringArgumentType.getString(context, parameter);
        return kindOf(token).orElseThrow(() -> fail("Unknown machine kind " + token + "; kinds: "
                + MachineKinds.all().stream().map(kind -> kind.id().getPath()).collect(Collectors.joining(", "))));
    }

    private static Optional<MachineKind> kindOf(String token) {
        return MachineKinds.all().stream().filter(kind -> kind.id().toString().equals(token) || kind.id().getPath().equals(token)).findFirst();
    }

    /**
     * The machine of a number parameter: number {@code n} of the kind in the warehouse, or the looked
     * at machine of that kind for {@code here}. With the warehouse {@code here} too, the looked at
     * machine is taken without needing a warehouse.
     */
    public static Machine machine(CommandContext<CommandSourceStack> context, MachineKind kind, String parameter) throws CommandSyntaxException {
        String token = StringArgumentType.getString(context, parameter);
        if (token.equals(HERE)) {
            Looked looked = looked(context.getSource(), "a machine number");
            NamedMachine machine = MachineLabels.machineAt(looked.level(), looked.pos())
                    .orElseThrow(() -> fail("The block you look at is no named machine"));
            if (!machine.machineKind().equals(kind)) {
                throw fail(Component.literal("The machine you look at is a ").append(machine.machineKind().plain())
                        .append(", not a ").append(kind.plain()));
            }
            if (!StringArgumentType.getString(context, WAREHOUSE).equals(HERE)) {
                Warehouse warehouse = warehouse(context);
                if (!warehouse.loaded().numberedMachines().contains(machine)) {
                    throw fail(Component.literal("The machine you look at is not numbered by " + warehouse.label()));
                }
            }
            return new Machine(machine, looked.level());
        }
        Warehouse warehouse = warehouse(context);
        int number = number(token);
        List<NamedMachine> ofKind = ofKind(warehouse.loaded(), kind);
        for (NamedMachine machine : ofKind) {
            if (machine.number() == number) {
                return new Machine(machine, warehouse.level());
            }
        }
        MutableComponent message = Component.literal(warehouse.label() + " has no ").append(kind.numbered(number));
        if (ofKind.isEmpty()) {
            message.append(Component.literal(" and no loaded ")).append(kind.plain());
        } else {
            message.append(Component.literal("; it has " + ofKind.stream().map(machine -> "#" + machine.number())
                    .collect(Collectors.joining(", "))));
        }
        throw fail(message);
    }

    /** The loaded numbered machines of the kind in the warehouse, by number. */
    public static List<NamedMachine> ofKind(WarehouseNetwork network, MachineKind kind) {
        List<NamedMachine> machines = new ArrayList<>(network.numberedMachines().stream()
                .filter(machine -> machine.machineKind().equals(kind)).toList());
        machines.sort(Comparator.comparingInt(NamedMachine::number));
        return machines;
    }

    /** Suggests every warehouse number with its name, and {@code here}. */
    public static SuggestionProvider<CommandSourceStack> warehouses() {
        return (context, builder) -> {
            SharedSuggestionProvider.suggest(List.of(HERE), builder);
            for (SiteRecord site : SiteRegistry.get(context.getSource().getServer()).all()) {
                if (String.valueOf(site.number()).startsWith(builder.getRemaining())) {
                    builder.suggest(String.valueOf(site.number()), Component.literal(site.name()));
                }
            }
            return builder.buildFuture();
        };
    }

    /** Suggests the kinds the warehouse has, or every kind while the warehouse is not known yet. */
    public static SuggestionProvider<CommandSourceStack> kinds() {
        return (context, builder) -> {
            Set<String> kinds = new LinkedHashSet<>();
            Optional<WarehouseNetwork> typed = typedWarehouse(context);
            if (typed.isPresent()) {
                typed.get().numberedMachines().forEach(machine -> kinds.add(machine.machineKind().id().getPath()));
            } else {
                MachineKinds.all().forEach(kind -> kinds.add(kind.id().getPath()));
            }
            return SharedSuggestionProvider.suggest(kinds, builder);
        };
    }

    /** Suggests the numbers the warehouse has of the kind in the kind parameter, and {@code here}. */
    public static SuggestionProvider<CommandSourceStack> numbers(String kindParameter) {
        return (context, builder) -> {
            boolean typed = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(kindParameter));
            Optional<MachineKind> kind = typed ? kindOf(StringArgumentType.getString(context, kindParameter)) : Optional.empty();
            return kind.isPresent() ? numbers(context, kind.get(), builder) : SharedSuggestionProvider.suggest(List.of(HERE), builder);
        };
    }

    /** Suggests the numbers the warehouse has of one kind, and {@code here}. */
    public static SuggestionProvider<CommandSourceStack> numbers(MachineKind kind) {
        return (context, builder) -> numbers(context, kind, builder);
    }

    private static CompletableFuture<Suggestions> numbers(CommandContext<CommandSourceStack> context, MachineKind kind, SuggestionsBuilder builder) {
        SharedSuggestionProvider.suggest(List.of(HERE), builder);
        for (NamedMachine machine : typedWarehouse(context).map(network -> ofKind(network, kind)).orElse(List.of())) {
            if (String.valueOf(machine.number()).startsWith(builder.getRemaining())) {
                builder.suggest(String.valueOf(machine.number()), machine.label());
            }
        }
        return builder.buildFuture();
    }

    /** The loaded warehouse typed so far, empty while it is unknown, unloaded or not typed yet. */
    private static Optional<WarehouseNetwork> typedWarehouse(CommandContext<CommandSourceStack> context) {
        try {
            return Optional.of(warehouse(context).loaded());
        } catch (CommandSyntaxException | IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }

    /** The warehouse controller of the block: a warehouse member, the Field Head of a field on its rails, or a member of its data line. */
    private static Optional<WarehouseControllerBlockEntity> controllerOf(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node) {
            return node.network().flatMap(WarehouseNetwork::controller);
        }
        if (level.getBlockEntity(pos) instanceof FieldHeadBlockEntity head) {
            return head.rails().stream().findFirst()
                    .filter(level::isLoaded)
                    .map(level::getBlockEntity)
                    .filter(StorageNodeBlockEntity.class::isInstance)
                    .map(StorageNodeBlockEntity.class::cast)
                    .flatMap(StorageNodeBlockEntity::network)
                    .flatMap(WarehouseNetwork::controller);
        }
        var line = CpuOrders.network(level, pos);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        for (SiteRecord site : SiteRegistry.of(level).all()) {
            if (site.dimension().equals(level.dimension()) && level.isLoaded(site.pos())
                    && level.getBlockEntity(site.pos()) instanceof WarehouseControllerBlockEntity controller
                    && controller.network().flatMap(network -> CpuOrders.network(level, network)).equals(line)) {
                return Optional.of(controller);
            }
        }
        return Optional.empty();
    }

    /** The block the running player looks at; fails asking for {@code wanted} when there is none. */
    private static Looked looked(CommandSourceStack source, String wanted) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw fail("Here needs a player looking at a block; use " + wanted);
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult block)) {
            throw fail("You look at no block; look at one or use " + wanted);
        }
        return new Looked(player.level(), block.getBlockPos());
    }

    private static int number(String token) throws CommandSyntaxException {
        String digits = token.startsWith("W") || token.startsWith("w") ? token.substring(1) : token;
        if (!digits.matches("[1-9][0-9]{0,8}")) {
            throw fail("Expected a number or here, got " + token);
        }
        return Integer.parseInt(digits);
    }

    private static String known(SiteRegistry registry) {
        List<SiteRecord> sites = new ArrayList<>(registry.all());
        if (sites.isEmpty()) {
            return "none";
        }
        sites.sort(Comparator.comparingInt(SiteRecord::number));
        return sites.stream().map(site -> "W" + site.number()).collect(Collectors.joining(", "));
    }
}
