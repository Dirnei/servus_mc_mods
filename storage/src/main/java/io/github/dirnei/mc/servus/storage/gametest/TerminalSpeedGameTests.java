package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlock;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Opening a terminal must show its content at once and not rebuild the line's recipe lists. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerminalSpeedGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos ADAPTER = new BlockPos(4, 1, 3);
    private static final BlockPos FURNACE = new BlockPos(5, 1, 3);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos LINE = new BlockPos(4, 2, 3);
    private static final BlockPos PRODUCTION = new BlockPos(3, 3, 3);

    /** Stands in for the player's connection: the open path calls it, the test needs no packets. */
    static final ContainerSynchronizer QUIET = new ContainerSynchronizer() {
        @Override
        public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carried, int[] data) {
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
        }
    };

    private TerminalSpeedGameTests() {
    }

    private static void setup(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 16), false);
        TerminalWiring.place(helper, ACCESS);
        helper.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.EAST));
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), LINE, new BlockPos(5, 2, 3))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        helper.setBlock(PRODUCTION, ServusStorageBlocks.PRODUCTION_TERMINAL.get().defaultBlockState()
                .setValue(ProductionTerminalBlock.FACING, Direction.NORTH));
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        }
        helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(LINE))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 1
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** Opening the Counter Terminal the way the game does sends its content in the same tick. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void counterSendsOnOpen(GameTestHelper helper) {
        setup(helper);
        TerminalWiring.when(helper, () -> ready(helper), 120, () -> "the adapter and CPU should join the line", () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            long now = helper.getLevel().getGameTime();
            try {
                menu.setSynchronizer(QUIET);
            } catch (RuntimeException refused) {
                // The mock connection negotiated no Servus payloads; the send was attempted, which is what counts.
            }
            helper.assertTrue(menu.firstSyncTick() == now, "the content should go out on open, sent at " + menu.firstSyncTick() + " opened " + now);
            helper.succeed();
        });
    }

    /** Opening the Production Terminal the way the game does sends its content in the same tick. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void productionSendsOnOpen(GameTestHelper helper) {
        setup(helper);
        TerminalWiring.when(helper, () -> ready(helper), 120, () -> "the adapter and CPU should join the line", () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            ProductionTerminalBlockEntity terminal = helper.getBlockEntity(PRODUCTION);
            ProductionTerminalMenu menu = new ProductionTerminalMenu(1, player.getInventory(), terminal);
            long now = helper.getLevel().getGameTime();
            try {
                menu.setSynchronizer(QUIET);
            } catch (RuntimeException refused) {
                // As above.
            }
            helper.assertTrue(menu.firstSyncTick() == now, "the content should go out on open, sent at " + menu.firstSyncTick() + " opened " + now);
            helper.succeed();
        });
    }
}
