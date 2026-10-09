package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Terminals, Crafting Stations, Pattern Storages and formed CPUs carry the data network to each other. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdjacentDataGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos DUCT = new BlockPos(2, 2, 3);
    private static final BlockPos PATTERNS = new BlockPos(3, 2, 3);
    private static final BlockPos TERMINAL = new BlockPos(4, 2, 3);
    private static final BlockPos STATION = new BlockPos(5, 2, 3);
    private static final BlockPos CPU = new BlockPos(6, 1, 2);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private AdjacentDataGameTests() {
    }

    private static void patterns(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
    }

    private static void terminal(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState().setValue(StorageTerminalBlock.FACING, Direction.NORTH));
    }

    private static void station(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
    }

    private static void row(GameTestHelper helper) {
        patterns(helper, PATTERNS);
        terminal(helper, TERMINAL);
        station(helper, STATION);
    }

    private static void cube(GameTestHelper helper) {
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        }
    }

    private static Optional<DataNetwork> network(GameTestHelper helper, BlockPos pos) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(pos));
    }

    private static boolean member(DataNetwork network, GameTestHelper helper, BlockPos pos, DataRole role) {
        BlockPos absolute = helper.absolutePos(pos);
        return network.dataMembers(role).stream().anyMatch(member -> member.pos().equals(absolute));
    }

    /** Pattern Storage, terminal and station in a row share one network without any duct. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void stackWithoutDuctIsOneNetwork(GameTestHelper helper) {
        row(helper);
        TerminalWiring.when(helper, () -> network(helper, TERMINAL)
                        .map(net -> member(net, helper, STATION, DataRole.PRODUCER) && member(net, helper, PATTERNS, DataRole.LINK))
                        .orElse(false), 40,
                () -> "the row should be one network with its members, got " + network(helper, TERMINAL).map(DataNetwork::dataMembers), () -> {
            DataNetwork net = network(helper, TERMINAL).orElseThrow();
            helper.assertTrue(network(helper, PATTERNS).orElseThrow() == net && network(helper, STATION).orElseThrow() == net,
                    "all three should report the same network");
            helper.assertTrue(member(net, helper, TERMINAL, DataRole.LINK), "the terminal is a link member");
            helper.assertTrue(net.cables() == 0, "carriers are not cables, got " + net.cables());
            helper.succeed();
        });
    }

    /** A request at a terminal in the row is crafted on the station of the row and lands in the terminal. */
    @GameTest(template = EMPTY, timeoutTicks = 1000)
    public static void craftThroughStack(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity planks = helper.getBlockEntity(CRATE);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 10), false);
        helper.setBlock(new BlockPos(1, 1, 3), ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(ACCESS, ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
        TerminalWiring.railUnder(helper, ACCESS);
        TerminalWiring.dataDuct(helper, DUCT);
        row(helper);
        PatternStorageBlockEntity storage = helper.getBlockEntity(PATTERNS);
        storage.set(0, StationJobs.sticks());
        cube(helper);
        helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> network(helper, TERMINAL)
                        .map(net -> CpuOrders.cpus(helper.getLevel(), net).size() == 1 && member(net, helper, ACCESS, DataRole.LINK))
                        .orElse(false), 60,
                () -> "the CPU and the Access Point should be on the terminal's network", () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should be accepted, got " + menu.stock().message());
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 8, 900,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK), () -> {
                helper.assertTrue(planks.count() == 6, "four planks should have been used, crate has " + planks.count());
                helper.succeed();
            });
        });
    }

    /** Breaking the middle block of the row splits the network. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void breakingMiddleSplits(GameTestHelper helper) {
        row(helper);
        TerminalWiring.when(helper, () -> network(helper, PATTERNS).isPresent()
                        && network(helper, PATTERNS).equals(network(helper, STATION)), 20,
                () -> "the row should join first", () -> {
            helper.setBlock(TERMINAL, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!network(helper, PATTERNS).orElseThrow().equals(network(helper, STATION).orElseThrow()),
                        "the ends should be two networks after the split");
                helper.succeed();
            });
        });
    }

    /** A chest between a Pattern Storage and a terminal carries nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void chestDoesNotConnect(GameTestHelper helper) {
        patterns(helper, PATTERNS);
        helper.setBlock(TERMINAL, Blocks.CHEST.defaultBlockState());
        terminal(helper, STATION);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!network(helper, PATTERNS).orElseThrow().equals(network(helper, STATION).orElseThrow()),
                    "the chest must not link them");
            helper.assertTrue(network(helper, TERMINAL).isEmpty(), "the chest is not on any network");
            helper.succeed();
        });
    }

    /** An Access Point and a Machine Adapter touching a lone terminal do not join its network. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void accessPointAndAdapterDoNotJoin(GameTestHelper helper) {
        terminal(helper, TERMINAL);
        helper.setBlock(PATTERNS, ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
        helper.setBlock(STATION, AdapterContent.BLOCK.get().defaultBlockState());
        helper.runAfterDelay(3, () -> {
            DataNetwork net = network(helper, TERMINAL).orElseThrow();
            List<BlockPos> members = net.dataMembers().stream().map(member -> member.pos()).toList();
            helper.assertTrue(members.equals(List.of(helper.absolutePos(TERMINAL))), "only the terminal is a member, got " + members);
            helper.assertTrue(network(helper, PATTERNS).isEmpty() && network(helper, STATION).isEmpty(),
                    "the Access Point and the adapter are on no network");
            helper.succeed();
        });
    }

    /** A cube touching a terminal joins its network when formed and leaves when a block is broken. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void formedCpuJoinsStack(GameTestHelper helper) {
        terminal(helper, STATION);
        cube(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(CpuOrders.cpus(helper.getLevel(), network(helper, STATION).orElseThrow()).isEmpty(),
                    "an unformed cube carries nothing");
            helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
            TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), network(helper, STATION).orElseThrow()).size() == 1, 20,
                    () -> "the formed CPU should be on the terminal's network", () -> {
                helper.setBlock(CPU.offset(2, 2, 2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(CpuOrders.cpus(helper.getLevel(), network(helper, STATION).orElseThrow()).isEmpty(),
                            "the unformed rest should leave the network");
                    helper.succeed();
                });
            });
        });
    }
}
