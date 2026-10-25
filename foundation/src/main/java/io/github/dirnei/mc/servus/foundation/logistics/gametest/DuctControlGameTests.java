package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctControl;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpGameTests;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** GameTests of the duct Control: Off, redstone, independent lines, saving, and the side picker reaching a disabled side. */
public final class DuctControlGameTests {

    /** Three ducts in a row along x with a block north of each. */
    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos DUCT_C = new BlockPos(4, 1, 6);
    private static final BlockPos CHEST_A = DUCT_A.north();
    private static final BlockPos CHEST_B = DUCT_B.north();
    private static final BlockPos CHEST_C = DUCT_C.north();
    /** A lever on the floor south of duct A, beside no other duct. */
    private static final BlockPos LEVER = DUCT_A.south();
    /** A duct position in the air, away from the floor. */
    private static final BlockPos HIGH = new BlockPos(5, 3, 3);

    private DuctControlGameTests() {
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void lever(GameTestHelper helper, boolean on) {
        helper.setBlock(LEVER, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, on));
    }

    /** Chest A pulls in on duct A, chest B on duct B receives; both on Default. */
    private static UtilityDuctBlockEntity pullRow(GameTestHelper helper, int cobblestone) {
        chest(helper, CHEST_A).setItem(0, new ItemStack(Items.COBBLESTONE, cobblestone));
        chest(helper, CHEST_B);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        duct(helper, DUCT_B, Line.ITEM);
        a.setMode(Line.ITEM, Direction.NORTH, SideMode.INPUT);
        return a;
    }

    /** An item pull in side with Control Off moves nothing and keeps its arm; back on Always the items move. */
    @ServusGameTest(timeoutTicks = 300)
    public static void controlOffPausesSide(GameTestHelper helper) {
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setControl(Line.ITEM, Direction.NORTH, DuctControl.OFF);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 20, "chest A keeps its cobblestone while Off");
            helper.assertTrue(a.hasArm(Line.ITEM, Direction.NORTH), "the arm toward chest A stays");
            a.setControl(Line.ITEM, Direction.NORTH, DuctControl.ALWAYS);
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 20,
                        "back on Always chest B receives the cobblestone, got " + count(helper, CHEST_B, Items.COBBLESTONE));
                helper.succeed();
            });
        });
    }

    /** While powered moves only while the lever beside the duct is on. */
    @ServusGameTest(timeoutTicks = 400)
    public static void whilePoweredFollowsLever(GameTestHelper helper) {
        lever(helper, false);
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setControl(Line.ITEM, Direction.NORTH, DuctControl.POWERED);

        helper.runAfterDelay(60, () -> {
            helper.assertFalse(a.isPowered(), "the duct is not powered with the lever off");
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 20, "nothing moves while unpowered");
            helper.pullLever(LEVER);
            helper.assertTrue(a.isPowered(), "the lever powers the duct at once");
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 20,
                        "while powered chest B receives the cobblestone, got " + count(helper, CHEST_B, Items.COBBLESTONE));
                helper.pullLever(LEVER);
                helper.assertFalse(a.isPowered(), "switching the lever off unpowers the duct");
                helper.getBlockEntity(CHEST_A, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIRT, 10));
                helper.runAfterDelay(80, () -> {
                    helper.assertTrue(count(helper, CHEST_A, Items.DIRT) == 10, "once the lever is off nothing more moves");
                    helper.succeed();
                });
            });
        });
    }

    /** A fluid side with Control Off accepts no fluid pushed into the duct, although a cauldron on its channel has room. */
    @ServusGameTest(timeoutTicks = 60)
    public static void controlOffRefusesPushedFluid(GameTestHelper helper) {
        helper.setBlock(CHEST_C, Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setControl(Line.FLUID, Direction.NORTH, DuctControl.OFF);

        helper.runAfterDelay(2, () -> {
            FluidView side = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(DUCT_A), Direction.NORTH));
            helper.assertTrue(side != null, "the in and out side still shows a fluid handler");
            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            helper.assertTrue(side.fill(water, true) == 0 && side.fill(water, false) == 0, "an Off side accepts no water");
            a.setControl(Line.FLUID, Direction.NORTH, DuctControl.ALWAYS);
            helper.assertTrue(side.fill(water, false) == 1000, "back on Always the water passes on");
            helper.succeed();
        });
    }

    /**
     * A Coal Generator's side carries the Item Pipe on While unpowered and the Power Cable on Always under a
     * powered duct: coal from chest B never reaches it, while its energy reaches an Electric Pump.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void linesAreIndependent(GameTestHelper helper) {
        lever(helper, true);
        CoalGeneratorGameTests.place(helper, CHEST_A, Direction.NORTH);
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(CHEST_A, CoalGeneratorBlockEntity.class);
        chest(helper, CHEST_B).setItem(0, new ItemStack(Items.COAL, 16));
        PumpGameTests.placeElectricPump(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM, Line.ENERGY);
        UtilityDuctBlockEntity b = duct(helper, DUCT_B, Line.ITEM, Line.ENERGY);
        duct(helper, DUCT_C, Line.ITEM, Line.ENERGY);
        b.setMode(Line.ITEM, Direction.NORTH, SideMode.INPUT);
        a.setControl(Line.ITEM, Direction.NORTH, DuctControl.UNPOWERED);
        generator.energyStorage().setEnergy(generator.energyStorage().getMaxEnergyStored());

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(a.isPowered(), "the lever powers duct A");
            helper.assertTrue(count(helper, CHEST_B, Items.COAL) == 16, "no coal reaches the generator, chest B holds "
                    + count(helper, CHEST_B, Items.COAL));
            helper.assertTrue(generator.inventory().getStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL).isEmpty(), "the fuel slot stays empty");
            PoweredMachineBlockEntity pump = helper.getBlockEntity(CHEST_C, PoweredMachineBlockEntity.class);
            helper.assertTrue(pump.energyStorage().getEnergyStored() > 0, "the generator's energy reaches the pump");
            helper.succeed();
        });
    }

    /** Controls survive a save and load, the item and power Controls of the side stay Always. */
    @ServusGameTest(timeoutTicks = 20)
    public static void controlsSurviveReload(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.ENERGY, Line.FLUID);
        duct.setControl(Line.FLUID, Direction.EAST, DuctControl.POWERED);
        duct.setControl(Line.ITEM, Direction.UP, DuctControl.OFF);
        CompoundTag saved = duct.saveWithoutMetadata(helper.getLevel().registryAccess());
        duct.setControl(Line.FLUID, Direction.EAST, DuctControl.ALWAYS);
        duct.setControl(Line.ITEM, Direction.UP, DuctControl.ALWAYS);
        duct.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(duct.control(Line.FLUID, Direction.EAST) == DuctControl.POWERED, "the fluid Control of east is While powered");
        helper.assertTrue(duct.control(Line.ITEM, Direction.EAST) == DuctControl.ALWAYS
                && duct.control(Line.ENERGY, Direction.EAST) == DuctControl.ALWAYS, "the other east Controls stay Always");
        helper.assertTrue(duct.control(Line.ITEM, Direction.UP) == DuctControl.OFF, "the item Control of up is Off");
        helper.succeed();
    }

    /**
     * A duct whose only neighbour, a chest to the west, sits on a disabled side has no arm; the carrot on the
     * duct opens the GUI on west, the item mode goes back to in and out and the arm returns.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void disabledSideCanBeEnabledAgain(GameTestHelper helper) {
        chest(helper, HIGH.west());
        UtilityDuctBlockEntity duct = duct(helper, HIGH, Line.ITEM);
        duct.setMode(Line.ITEM, Direction.WEST, SideMode.DISABLED);
        helper.assertFalse(duct.hasArm(Line.ITEM, Direction.WEST), "the disabled side has no arm");

        helper.runAfterDelay(2, () -> {
            ServerPlayer player = PlayerClicks.player(helper, HIGH, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()));
            PlayerClicks.use(helper, player, HIGH);
            helper.assertTrue(player.containerMenu instanceof UtilityDuctMenu, "the carrot opens the endpoint GUI, got " + player.containerMenu);
            UtilityDuctMenu menu = (UtilityDuctMenu) player.containerMenu;
            helper.assertTrue(menu.side() == Direction.WEST, "the GUI opens on the only side with a block, got " + menu.side());
            helper.assertFalse(menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_SELECT_SIDE + Direction.EAST.ordinal()),
                    "an empty side cannot be picked");
            helper.assertTrue(menu.side() == Direction.WEST, "the side stays west");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_SELECT_SIDE + Direction.WEST.ordinal());
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_CYCLE_MODE + Line.ITEM.ordinal());
            helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == SideMode.BOTH, "the mode is in and out again");
            helper.assertTrue(duct.hasArm(Line.ITEM, Direction.WEST), "the arm toward the chest is back");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_CYCLE_CONTROL + Line.ITEM.ordinal());
            helper.assertTrue(duct.control(Line.ITEM, Direction.WEST) == DuctControl.POWERED, "the Control button cycles to While powered");
            helper.succeed();
        });
    }

    /** A duct that touches only other ducts opens nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void ductAmongDuctsOpensNothing(GameTestHelper helper) {
        duct(helper, HIGH.west(), Line.ITEM);
        duct(helper, HIGH, Line.ITEM);
        duct(helper, HIGH.east(), Line.ITEM);

        helper.runAfterDelay(2, () -> {
            ServerPlayer player = PlayerClicks.player(helper, HIGH, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()));
            PlayerClicks.use(helper, player, HIGH);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
            helper.succeed();
        });
    }
}
