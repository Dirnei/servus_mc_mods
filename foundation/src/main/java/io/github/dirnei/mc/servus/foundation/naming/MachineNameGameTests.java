package io.github.dirnei.mc.servus.foundation.naming;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterMenu;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.MachineNameContract;
import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

/** The shared machine naming on the Foundation's own named machine, the Machine Adapter. */
public final class MachineNameGameTests {

    private static final BlockPos FURNACE = new BlockPos(3, 1, 3);
    private static final BlockPos ADAPTER = new BlockPos(3, 1, 4);

    private MachineNameGameTests() {
    }

    private static MachineAdapterBlockEntity adapter(GameTestHelper helper) {
        helper.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST));
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(ADAPTER, MachineAdapterBlockEntity.class);
    }

    private static Object[] args(GameTestHelper helper, Component label, String key) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected " + key + ", got " + label);
        return ((TranslatableContents) label.getContents()).getArgs();
    }

    /** Every Foundation machine kind follows the same naming rules. */
    @ServusGameTest(timeoutTicks = 40)
    public static void everyFoundationKindFollowsTheRules(GameTestHelper helper) {
        MachineNameContract.checkAll(helper, ServusFoundationIds.MOD_ID);
    }

    /** The Coal Generator's chimney reads as the generator below it; the plain label carries no chat colour. */
    @ServusGameTest(timeoutTicks = 20)
    public static void generatorChimneyReadsAsTheGenerator(GameTestHelper helper) {
        BlockPos cube = new BlockPos(2, 1, 2);
        CoalGeneratorGameTests.place(helper, cube, Direction.NORTH);
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(cube, CoalGeneratorBlockEntity.class);
        generator.rename("Boiler");
        Component label = MachineLabels.at(helper.getLevel(), helper.absolutePos(cube.above()));
        helper.assertTrue(label.getString().equals("Boiler"), "the chimney reads Boiler, got " + label.getString());
        helper.assertTrue(generator.chatLabel().getStyle().getColor() == null, "a generator has no chat colour, got " + generator.chatLabel());
        helper.succeed();
    }

    /** An unnamed adapter reads as its faced machine with its own label; a named one by its name. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterReadsAsItsMachine(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = adapter(helper);
        Object[] plain = args(helper, adapter.label(), NamedMachine.NAMED_KEY);
        helper.assertTrue(((Component) plain[0]).getString().equals(Blocks.FURNACE.getName().getString()), "the faced furnace comes first, got " + plain[0]);
        args(helper, (Component) plain[1], "block.servus_foundation.machine_adapter");
        adapter.assignNumber(UUID.randomUUID(), 1);
        args(helper, (Component) args(helper, adapter.label(), NamedMachine.NAMED_KEY)[1], "machine.servus_foundation.machine_adapter");
        adapter.rename("Smelter");
        Object[] named = args(helper, adapter.label(), NamedMachine.NAMED_KEY);
        helper.assertTrue(named[0].equals("Smelter"), "a named adapter reads Smelter (Adapter #1), got " + named[0]);
        helper.succeed();
    }

    /** The adapter GUI renames the adapter; a player who may not use the GUI cannot. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterMenuRenames(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = adapter(helper);
        ServerPlayer player = PlayerClicks.player(helper, ADAPTER, ItemStack.EMPTY);
        new AdapterMenu(1, player.getInventory(), adapter).renameMachine(player, " Smelter\n");
        helper.assertTrue(adapter.name().equals("Smelter"), "the adapter is named Smelter, got '" + adapter.name() + "'");
        ServerPlayer far = PlayerClicks.player(helper, ADAPTER.east(40), ItemStack.EMPTY);
        new AdapterMenu(2, far.getInventory(), adapter).renameMachine(far, "Far Away");
        helper.assertTrue(adapter.name().equals("Smelter"), "a far player cannot rename");
        helper.succeed();
    }

    /** The Data Potato on an adapter prints its label and opens no GUI. */
    @ServusGameTest(timeoutTicks = 20)
    public static void potatoNamesTheAdapter(GameTestHelper helper) {
        adapter(helper).rename("Smelter");
        List<Component> chat = new ArrayList<>();
        ServerPlayer player = PlayerClicks.player(helper, ADAPTER, new ItemStack(ServusFoundationItems.DATA_POTATO.get()), chat);
        PlayerClicks.use(helper, player, ADAPTER);
        helper.assertTrue(chat.size() == 1 && chat.get(0).getString().equals("Smelter"), "chat shows Smelter, got " + chat);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
        helper.succeed();
    }
}
