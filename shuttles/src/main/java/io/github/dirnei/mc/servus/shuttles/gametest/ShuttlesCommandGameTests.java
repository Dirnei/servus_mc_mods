package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.site.SiteChunks;
import io.github.dirnei.mc.servus.shuttles.site.SiteRecord;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The {@code /servus shuttles} commands by warehouse and machine number, {@code here}, dimensions and unloaded warehouses. */
public final class ShuttlesCommandGameTests {

    private static final int WAIT = 120;
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 2);

    private ShuttlesCommandGameTests() {
    }

    private static void assertContains(GameTestHelper helper, String text, String part, String why) {
        helper.assertTrue(text.contains(part), why + ": expected '" + part + "' in\n" + text);
    }

    /** {@code list} names the warehouse by number, {@code machines} with a kind lists its garage. */
    @ServusGameTest(timeoutTicks = WAIT)
    public static void listAndMachinesOfAKind(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        Waits.when(helper, () -> warehouse.garage().number() > 0, WAIT - 10, () -> "the garage should get a number", () -> {
            int number = ShuttlesCommandRuns.number(helper, warehouse.controllerPos());
            String listed = ShuttlesCommandRuns.console(helper, "list");
            assertContains(helper, listed, "W" + number + " Warehouse at", "the list names the warehouse");
            String machines = ShuttlesCommandRuns.console(helper, "machines " + number + " shuttle_garage");
            assertContains(helper, machines, "shuttle_garage 1: ", "the garage is listed by kind and number");
            helper.assertTrue(!machines.contains("inbound_station"), "only garages are listed: " + machines);
            helper.succeed();
        });
    }

    /** {@code machine} on a Crafting Station prints its label, place and the Data Potato report. */
    @ServusGameTest(timeoutTicks = 300)
    public static void machineReportsACraftingStation(GameTestHelper helper) {
        CpuLine.setup(helper, false);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        Waits.when(helper, () -> station.number() > 0, 250, () -> "the station should get a number", () -> {
            int number = ShuttlesCommandRuns.number(helper, CpuLine.CONTROLLER);
            String report = ShuttlesCommandRuns.console(helper, "machine " + number + " crafting_station 1");
            assertContains(helper, report, "Crafting Station #1 at " + station.getBlockPos().toShortString(), "the report names the station");
            assertContains(helper, report, "minecraft:overworld", "the report names the dimension");
            helper.succeed();
        });
    }

    /** {@code here} on a looked at rack is the rack's warehouse. */
    @ServusGameTest(timeoutTicks = WAIT)
    public static void hereIsTheLookedAtWarehouse(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false).stock(new ItemStack(Items.COBBLESTONE, 30));
        helper.runAfterDelay(10, () -> {
            int number = ShuttlesCommandRuns.number(helper, warehouse.controllerPos());
            String info = ShuttlesCommandRuns.lookingAt(helper, warehouse.rackPos(), "info here");
            assertContains(helper, info, "Site W" + number + " ", "here is the rack's warehouse");
            assertContains(helper, info, "Cobblestone x 30", "the report lists the rack's stock");
            String console = ShuttlesCommandRuns.console(helper, "info here");
            assertContains(helper, console, "use a warehouse number", "here from the console asks for a number");
            helper.succeed();
        });
    }

    /** {@code field age here here} ages an unnumbered Field Head whose field reaches no warehouse. */
    @ServusGameTest(timeoutTicks = 40)
    public static void hereHereAgesAnUnnumberedFieldHead(GameTestHelper helper) {
        BlockPos head = new BlockPos(2, 1, 5);
        for (int x = 3; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 6), Blocks.FARMLAND.defaultBlockState());
            FieldFixtures.farmRail(helper, new BlockPos(x, 1, 5));
        }
        FieldHeadBlockEntity field = FieldFixtures.head(helper, head);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(field.number() == 0, "the head has no number");
            String said = ShuttlesCommandRuns.lookingAt(helper, head,
                    "field age here here " + ShuttlesStatsManager.INSTANCE.fieldCorrosionWater());
            helper.assertTrue(field.stageCounts()[FieldCorrosion.EXPOSED] == 5, "every rail is exposed: " + said);
            helper.succeed();
        });
    }

    /** An unknown warehouse number fails naming the known ones; a looked at machine of another kind fails naming its kind. */
    @ServusGameTest(timeoutTicks = WAIT)
    public static void unknownNumberAndKindMismatch(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        Waits.when(helper, () -> warehouse.garage().number() > 0, WAIT - 10, () -> "the garage should get a number", () -> {
            int number = ShuttlesCommandRuns.number(helper, warehouse.controllerPos());
            String unknown = ShuttlesCommandRuns.console(helper, "info 99999");
            assertContains(helper, unknown, "No warehouse W99999; known: ", "an unknown number fails");
            assertContains(helper, unknown, "W" + number, "the known numbers are named");
            String mismatch = ShuttlesCommandRuns.lookingAt(helper, warehouse.garagePos(), "machine here crafting_station here");
            assertContains(helper, mismatch, "The machine you look at is a Shuttle Garage", "the kind mismatch names the garage");
            String missing = ShuttlesCommandRuns.console(helper, "machine " + number + " shuttle_garage 7");
            assertContains(helper, missing, "has no Garage #7; it has #1", "a missing number names the garages there are");
            helper.succeed();
        });
    }

    /** A warehouse in the Nether is reached by number from the console, whose level is the Overworld. */
    @ServusGameTest(timeoutTicks = 60)
    public static void netherWarehouseByNumberFromTheConsole(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the server has a Nether");
        BlockPos base = helper.absolutePos(BlockPos.ZERO);
        BlockPos pos = new BlockPos(base.getX(), 100, base.getZ());
        ChunkPos chunk = ChunkPos.containing(pos);
        nether.setChunkForced(chunk.x(), chunk.z(), true);
        nether.setBlockAndUpdate(pos, ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        WarehouseControllerBlockEntity controller = (WarehouseControllerBlockEntity) nether.getBlockEntity(pos);
        int number = SiteRegistry.of(nether).number(controller);
        helper.runAfterDelay(5, () -> {
            String info = ShuttlesCommandRuns.console(helper, "info " + number);
            nether.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            nether.setChunkForced(chunk.x(), chunk.z(), false);
            assertContains(helper, info, "Site W" + number + " ", "the Nether warehouse reports by number");
            helper.succeed();
        });
    }

    /** An order on a warehouse whose chunks are not loaded fails naming {@code wake} and makes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unloadedWarehouseRefusesOrders(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos far = new BlockPos(29_000_000, 64, 29_000_000);
        SiteRecord record = SiteRegistry.of(level).remember(level.dimension(), far, "Far Camp", UUID.randomUUID(),
                Set.of(SiteChunks.ofBlock(far.getX(), far.getZ())), 0, Map.of(), false, false, false);
        String said = ShuttlesCommandRuns.console(helper, "order " + record.number() + " minecraft:stick 8");
        SiteRegistry.of(level).remove(level.dimension(), far);
        assertContains(helper, said, "W" + record.number() + " Far Camp is not loaded; run /servus shuttles wake " + record.number(),
                "the order names wake");
        helper.succeed();
    }

    /** {@code take} from a player lands in the player's inventory. */
    @ServusGameTest(timeoutTicks = WAIT)
    public static void takeLandsInThePlayersInventory(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false).stock(new ItemStack(Items.COBBLESTONE, 30));
        helper.runAfterDelay(10, () -> {
            int number = ShuttlesCommandRuns.number(helper, warehouse.controllerPos());
            FakePlayer player = ShuttlesCommandRuns.player(helper, warehouse.rackPos());
            player.getInventory().clearContent();
            ShuttlesCommandRuns.run(player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER),
                    "take " + number + " minecraft:cobblestone 12");
            int held = player.getInventory().countItem(Items.COBBLESTONE);
            player.getInventory().clearContent();
            helper.assertTrue(held == 12, "the player holds 12 cobblestone, got " + held);
            helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 18, "the rack keeps 18, has " + warehouse.rackCount(Items.COBBLESTONE));
            helper.succeed();
        });
    }

    /** {@code move} by numbers starts a data line move from Crafting Station #1 to Packing #1. */
    @ServusGameTest(timeoutTicks = 300)
    public static void moveByNumbers(GameTestHelper helper) {
        CpuLine.setup(helper, false);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        Waits.when(helper, () -> station.number() > 0, 250, () -> "the station should get a number", () -> {
            station.outputs().setStackInSlot(0, new ItemStack(Items.STICK, 8));
            int number = ShuttlesCommandRuns.number(helper, CpuLine.CONTROLLER);
            String said = ShuttlesCommandRuns.console(helper, "move " + number + " crafting_station 1 packing_station 1 minecraft:stick 8");
            assertContains(helper, said, "Delivery ", "the move reports its delivery");
            helper.succeedWhen(() -> helper.assertTrue(WarehouseFixtures.count(station.outputs(), Items.STICK) == 0,
                    "the sticks leave the station"));
        });
    }
}
