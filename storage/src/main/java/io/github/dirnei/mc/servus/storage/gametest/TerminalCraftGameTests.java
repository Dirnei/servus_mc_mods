package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Crafting from the Storage Terminal: craftable items, previews, starting a craft, instant content, Packing Station GUI. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerminalCraftGameTests {

    private static final String EMPTY = "empty_12x6x12";
    /** Where {@link StationHandsGameTests#setup} puts the Packing Station; the terminal sits two blocks above. */
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private TerminalCraftGameTests() {
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(TerminalWiring.terminalPos(ACCESS)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 2
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** Ready and the warehouse index lists the planks. */
    private static boolean stocked(GameTestHelper helper, Player player) {
        return ready(helper) && menu(helper, player).stock().entries().stream().anyMatch(entry -> entry.stack().is(Items.OAK_PLANKS));
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, Player player) {
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
        return new StorageTerminalMenu(1, player.getInventory(), terminal);
    }

    private static int count(List<StockEntry> rows, net.minecraft.world.item.Item item) {
        return rows.stream().filter(row -> row.stack().is(item)).mapToInt(row -> (int) row.count()).sum();
    }

    /** With no sticks in stock, the sticks pattern on the line makes sticks a craftable entry. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftablesListSticks(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> ready(helper), 100, () -> "stations and CPU should join the line", () -> {
            List<ItemStack> craftables = menu(helper, player).stock().craftables();
            helper.assertTrue(craftables.stream().anyMatch(stack -> stack.is(Items.STICK)), "sticks should be craftable, got " + craftables);
            helper.succeed();
        });
    }

    /** Eight sticks take four planks from stock. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftPreviewFromStock(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> stocked(helper, player), 160, () -> "stations and CPU should join the line", () -> {
            CraftPreviewPayload preview = menu(helper, player).preview(STICK, 8);
            helper.assertTrue(preview.refusal().isEmpty(), "no refusal, got " + preview.refusal());
            helper.assertTrue(count(preview.toCraft(), Items.STICK) == 8, "8 sticks to craft, got " + preview.toCraft());
            helper.assertTrue(count(preview.fromStock(), Items.OAK_PLANKS) == 4, "4 planks from stock, got " + preview.fromStock());
            helper.assertTrue(preview.missing().isEmpty() && preview.startable(), "nothing missing, got " + preview.missing());
            helper.succeed();
        });
    }

    /** Without planks the preview lists them as missing and cannot start. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftPreviewMissing(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> ready(helper), 100, () -> "stations and CPU should join the line", () -> {
            CraftPreviewPayload preview = menu(helper, player).preview(STICK, 8);
            helper.assertTrue(count(preview.missing(), Items.OAK_PLANKS) > 0, "planks should be missing, got " + preview.missing());
            helper.assertFalse(preview.startable(), "a preview with missing material cannot start");
            StorageTerminalMenu menu = menu(helper, player);
            menu.craft(STICK, 8);
            helper.assertTrue(TerminalWiring.at(helper, ACCESS).requests().isEmpty(), "no request is made while planks are missing");
            helper.succeed();
        });
    }

    /** Once the CPU cube is broken the preview says there is no CPU. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftPreviewNoCpu(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> stocked(helper, player), 160, () -> "stations and CPU should join the line", () -> {
            helper.setBlock(CPU, Blocks.AIR.defaultBlockState());
            TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), helper.absolutePos(TerminalWiring.terminalPos(ACCESS))).isEmpty(),
                    40, () -> "the CPU should unform", () -> {
                        CraftPreviewPayload preview = menu(helper, player).preview(STICK, 8);
                        helper.assertTrue(preview.refusal().equals("no_cpu"), "refusal no_cpu, got " + preview.refusal());
                        helper.assertFalse(preview.startable(), "cannot start without a CPU");
                        helper.succeed();
                    });
        });
    }

    /** Start makes a producing request, and the crafted sticks land in the output slots. */
    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void craftStartDeliversSticks(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        // A crate compartment holds one item; the planks crate cannot take the crafted sticks.
        helper.setBlock(new BlockPos(2, 1, 1), ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> stocked(helper, player), 160, () -> "stations and CPU should join the line", () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            menu(helper, player).craft(STICK, 8);
            helper.assertTrue(terminal.requests().size() == 1, "one request should be listed");
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) >= 8, 1200,
                    () -> "8 sticks should land in the output slots, got " + terminal.outputCount(Items.STICK), helper::succeed);
        });
    }

    /** Opening the screen sends the content in the same tick. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void terminalSendsOnOpen(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        TerminalWiring.when(helper, () -> ready(helper), 100, () -> "stations and CPU should join the line", () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            StorageTerminalMenu menu = menu(helper, player);
            long now = helper.getLevel().getGameTime();
            try {
                menu.setSynchronizer(TerminalSpeedGameTests.QUIET);
            } catch (RuntimeException refused) {
                // The mock connection negotiated no Servus payloads; the send was attempted, which is what counts here.
            }
            helper.assertTrue(menu.firstSyncTick() == now, "the content should go out on open, sent at " + menu.firstSyncTick() + " opened " + now);
            helper.succeed();
        });
    }

    /** The buffer GUI of a Packing Station stays valid for a player in reach. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void packingStationMenuStaysValid(GameTestHelper helper) {
        TerminalWiring.place(helper, ACCESS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(TerminalWiring.packingStationPos(ACCESS));
        player.moveTo(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() + 1.5);
        PackingStationBlockEntity point = helper.getBlockEntity(TerminalWiring.packingStationPos(ACCESS));
        io.github.dirnei.mc.servus.storage.menu.PackingStationMenu menu =
                new io.github.dirnei.mc.servus.storage.menu.PackingStationMenu(1, player.getInventory(), point);
        helper.assertTrue(menu.stillValid(player), "the Packing Station GUI should stay open");
        helper.succeed();
    }
}
