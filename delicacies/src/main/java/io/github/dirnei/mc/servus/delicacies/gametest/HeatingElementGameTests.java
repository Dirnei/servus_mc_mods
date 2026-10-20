package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * A Tiled Stove with a Heating Element takes FE through its port only, preheats and cooks without fuel, cools and stops
 * without power, refuses fuel, and gives the element back by hand or when it is broken.
 */
public final class HeatingElementGameTests {

    /** The test stove faces north and runs east from {@link KitchenTestSupport#DOOR}, so the port is the door tile's west face. */
    private static final Direction PORT_FACE = Direction.WEST;
    private static final BlockPos ON_DOOR = DOOR.above();

    private HeatingElementGameTests() {
    }

    private static EnergyHandler energy(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), side);
    }

    private static StoveTileBlockEntity stoveWithElement(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.stove(helper, 2);
        helper.assertTrue(master.installElement(), "the element goes in");
        return master;
    }

    /** A Pot on the door tile with two Potatoes and water: Boiled Potatoes in 200 ticks. */
    private static PotBlockEntity potatoPot(GameTestHelper helper) {
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.POT.get());
        PotBlockEntity pot = helper.getBlockEntity(ON_DOOR, PotBlockEntity.class);
        pot.insertInputs(new ItemStack(Items.POTATO, 2));
        FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ON_DOOR), Direction.UP))
                .fill(new FluidStack(Fluids.WATER, 1000), false);
        return pot;
    }

    private static int elements(GameTestHelper helper, BlockPos around) {
        return GameTestSteps.drops(helper, around, 3).stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(ServusFoundationItems.HEATING_ELEMENT.get())).mapToInt(ItemStack::getCount).sum();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void elementInstallsShowsPortAndTakesFe(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.stove(helper, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, DOOR.east(), new ItemStack(ServusFoundationItems.HEATING_ELEMENT.get(), 2));
        helper.assertTrue(master.hasElement(), "a click with the element installs it");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "one element is used");
        BlockState door = helper.getBlockState(DOOR);
        BlockState plate = helper.getBlockState(DOOR.east());
        helper.assertTrue(door.getValue(StoveTileBlock.HEATER) && plate.getValue(StoveTileBlock.HEATER), "every tile shows the element");
        helper.assertTrue(door.getValue(StoveTileBlock.PORT) && !plate.getValue(StoveTileBlock.PORT), "the west end tile carries the port");
        EnergyHandler port = energy(helper, DOOR, PORT_FACE);
        helper.assertTrue(port != null, "the port face takes FE");
        helper.assertTrue(energy(helper, DOOR, Direction.NORTH) == null, "the front takes none");
        helper.assertTrue(energy(helper, DOOR.east(), Direction.EAST) == null, "the other end takes none");
        helper.assertTrue(energy(helper, DOOR.east(), Direction.UP) == null, "the plate takes none");
        helper.assertTrue(Transfer.receiveEnergy(port, 100, false) == 100, "100 FE go in");
        helper.assertTrue(Transfer.extractEnergy(port, 100, false) == 0, "the port gives nothing out");
        helper.assertTrue(master.heating().storedEnergy() == 100, "the buffer holds them");
        KitchenTestSupport.click(helper, player, DOOR, player.getMainHandItem());
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a second element is refused");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void elementStoveCooksWithoutFuel(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 1));
        helper.assertTrue(master.installElement(), "the element goes in");
        PotBlockEntity pot = potatoPot(helper);
        int[] fed = {0};
        helper.onEachTick(() -> fed[0] += Transfer.receiveEnergy(energy(helper, DOOR, PORT_FACE), 40, false));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pot.progress() == 0, "nothing cooks while the stove preheats");
            helper.assertTrue(master.heating().heat().heat() > 0, "the temperature climbs");
            helper.assertFalse(master.heating().heat().hot(), "not hot yet");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pot.output().is(ServusDelicaciesItems.BOILED_POTATOES.get()), "Boiled Potatoes on FE, got " + pot.output());
            helper.assertTrue(master.fuelStack().is(Items.COAL) && master.fuelStack().getCount() == 1, "the Coal is untouched");
            helper.assertTrue(fed[0] > master.heating().storedEnergy(), "FE was drawn");
            helper.assertTrue(helper.getBlockState(DOOR).getValue(StoveTileBlock.LIT), "the coils glow");
        });
    }

    @ServusGameTest(timeoutTicks = 120)
    public static void noPowerCoolsAndStops(GameTestHelper helper) {
        StoveTileBlockEntity master = stoveWithElement(helper);
        float hotAt = master.heating().stats().hotAt();
        master.heating().setHeat(hotAt + 0.02F);
        PotBlockEntity pot = potatoPot(helper);
        helper.runAfterDelay(5, () -> helper.assertTrue(pot.progress() > 0, "the Pot cooks while the stove is still hot"));
        helper.runAfterDelay(60, () -> {
            float heat = master.heating().heat().heat();
            helper.assertFalse(master.heating().heat().hot(), "below the threshold without FE, heat " + heat);
            helper.assertTrue(heat > hotAt - 0.1F && heat < hotAt, "it cools slowly, heat " + heat);
            helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.LIT), "the coils are dull");
            int progress = pot.progress();
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(pot.progress() == progress, "the Pot stopped");
                helper.assertTrue(pot.output().isEmpty(), "nothing was cooked");
                helper.succeed();
            });
        });
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void elementRefusesFuel(GameTestHelper helper) {
        StoveTileBlockEntity master = stoveWithElement(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, DOOR, new ItemStack(Items.COAL, 3));
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "Coal by hand is refused");
        BlockPos above = DOOR.east().above();
        helper.setBlock(above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(above, HopperBlockEntity.class).setItem(0, new ItemStack(Items.CHARCOAL, 2));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(master.fuelStack().isEmpty(), "no fuel went in");
            helper.assertTrue(helper.getBlockEntity(above, HopperBlockEntity.class).getItem(0).getCount() == 2, "the hopper keeps it");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void elementDropsOnUnform(GameTestHelper helper) {
        stoveWithElement(helper);
        helper.destroyBlock(DOOR.east());
        helper.succeedWhen(() -> {
            helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.FORMED), "the stove unformed");
            helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.HEATER), "the loose tile shows no element");
            helper.assertTrue(elements(helper, DOOR) == 1, "the element dropped");
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void sneakingEmptyHandTakesTheElement(GameTestHelper helper) {
        StoveTileBlockEntity master = stoveWithElement(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        KitchenTestSupport.click(helper, player, DOOR.east(), ItemStack.EMPTY);
        helper.assertFalse(master.hasElement(), "the element is out");
        helper.assertTrue(player.getInventory().countItem(ServusFoundationItems.HEATING_ELEMENT.get()) == 1, "the player has it");
        helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.HEATER), "no coils, gauge or port");
        helper.assertTrue(energy(helper, DOOR, PORT_FACE) == null, "no FE without the element");
        helper.assertTrue(master.addFuel(new ItemStack(Items.COAL)).isEmpty(), "fuel goes in again");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void noElementNoEnergy(GameTestHelper helper) {
        KitchenTestSupport.stove(helper, 2);
        for (Direction side : Direction.values()) {
            helper.assertTrue(energy(helper, DOOR, side) == null && energy(helper, DOOR.east(), side) == null,
                    "no face takes FE without the element, " + side);
        }
        helper.succeed();
    }
}
