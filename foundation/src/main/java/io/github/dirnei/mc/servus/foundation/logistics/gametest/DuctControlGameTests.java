package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctRedstone;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.menu.UtilityDuctMenu;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpGameTests;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
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

/** GameTests of the duct side settings: In, Out and Pull, On/Off, redstone, independent lines, saving, older saves, and the side tabs reaching an unconnected side. */
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
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        return a;
    }

    /** An item side switched off moves nothing, keeps its arm and its redstone setting; switched on again the items move. */
    @ServusGameTest(timeoutTicks = 300)
    public static void switchedOffPausesSide(GameTestHelper helper) {
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setRedstone(Line.ITEM, Direction.NORTH, DuctRedstone.UNPOWERED);
        a.setOn(Line.ITEM, Direction.NORTH, false);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 20, "chest A keeps its cobblestone while switched off");
            helper.assertTrue(a.hasArm(Line.ITEM, Direction.NORTH), "the arm toward chest A stays");
            helper.assertTrue(a.redstone(Line.ITEM, Direction.NORTH) == DuctRedstone.UNPOWERED, "the redstone setting is kept");
            helper.assertTrue(a.mode(Line.ITEM, Direction.NORTH) == DuctMode.IN_PULL, "the mode is kept");
            a.setOn(Line.ITEM, Direction.NORTH, true);
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 20,
                        "switched on chest B receives the cobblestone, got " + count(helper, CHEST_B, Items.COBBLESTONE));
                helper.succeed();
            });
        });
    }

    /** While powered moves only while the lever beside the duct is on. */
    @ServusGameTest(timeoutTicks = 400)
    public static void whilePoweredFollowsLever(GameTestHelper helper) {
        lever(helper, false);
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setRedstone(Line.ITEM, Direction.NORTH, DuctRedstone.POWERED);

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

    /** A fluid side switched off accepts no fluid pushed into the duct, although a cauldron on its channel has room. */
    @ServusGameTest(timeoutTicks = 60)
    public static void switchedOffRefusesPushedFluid(GameTestHelper helper) {
        helper.setBlock(CHEST_C, Blocks.CAULDRON.defaultBlockState());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setOn(Line.FLUID, Direction.NORTH, false);

        helper.runAfterDelay(2, () -> {
            FluidView side = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(DUCT_A), Direction.NORTH));
            helper.assertTrue(side != null, "the In and Out side still shows a fluid handler");
            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            helper.assertTrue(side.fill(water, true) == 0 && side.fill(water, false) == 0, "a switched off side accepts no water");
            a.setOn(Line.FLUID, Direction.NORTH, true);
            helper.assertTrue(side.fill(water, false) == 1000, "switched on the water passes on");
            helper.succeed();
        });
    }

    /** An item side with only In takes what is pushed into the duct through it, but pulls nothing out of its chest. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inOnlyAcceptsPushedItemsButDoesNotPull(GameTestHelper helper) {
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 20, "a side with only In pulls nothing, chest A holds "
                    + count(helper, CHEST_A, Items.COBBLESTONE));
            ItemView side = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(DUCT_A), Direction.NORTH));
            helper.assertTrue(side != null, "the side shows an item handler");
            ItemStack left = side.insertItem(new ItemStack(Items.DIRT, 5), false);
            helper.assertTrue(left.isEmpty(), "the side accepts pushed dirt, left " + left);
            helper.assertTrue(count(helper, CHEST_B, Items.DIRT) == 5, "the pushed dirt reaches chest B");
            helper.succeed();
        });
    }

    /** In, Out and Pull together pull from the chest and never route the items back into it. */
    @ServusGameTest(timeoutTicks = 300)
    public static void inOutPullNeverRoutesBack(GameTestHelper helper) {
        UtilityDuctBlockEntity a = pullRow(helper, 20);
        a.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_OUT_PULL);

        helper.runAfterDelay(160, () -> {
            helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 20,
                    "the pulled cobblestone goes to chest B, got " + count(helper, CHEST_B, Items.COBBLESTONE));
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 0, "nothing goes back into chest A");
            helper.succeed();
        });
    }

    /**
     * A Coal Generator's side carries the Item Pipe on While unpowered and the Power Cable ignoring redstone
     * under a powered duct: coal from chest B never reaches it, while its energy reaches an Electric Pump.
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
        b.setMode(Line.ITEM, Direction.NORTH, DuctMode.IN_PULL);
        a.setRedstone(Line.ITEM, Direction.NORTH, DuctRedstone.UNPOWERED);
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

    /** Redstone settings and switched off sides survive a save and load; the other settings of the side stay default. */
    @ServusGameTest(timeoutTicks = 20)
    public static void settingsSurviveReload(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.ENERGY, Line.FLUID);
        duct.setRedstone(Line.FLUID, Direction.EAST, DuctRedstone.POWERED);
        duct.setOn(Line.ITEM, Direction.UP, false);
        duct.setMode(Line.ITEM, Direction.UP, DuctMode.IN_OUT_PULL);
        CompoundTag saved = duct.saveWithoutMetadata(helper.getLevel().registryAccess());
        duct.setRedstone(Line.FLUID, Direction.EAST, DuctRedstone.IGNORE);
        duct.setOn(Line.ITEM, Direction.UP, true);
        duct.setMode(Line.ITEM, Direction.UP, DuctMode.IN_OUT);
        duct.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(duct.redstone(Line.FLUID, Direction.EAST) == DuctRedstone.POWERED, "the fluid redstone setting of east is While powered");
        helper.assertTrue(duct.redstone(Line.ITEM, Direction.EAST) == DuctRedstone.IGNORE
                && duct.redstone(Line.ENERGY, Direction.EAST) == DuctRedstone.IGNORE, "the other east settings stay Ignore");
        helper.assertFalse(duct.isOn(Line.ITEM, Direction.UP), "the item side up stays switched off");
        helper.assertTrue(duct.isOn(Line.FLUID, Direction.UP) && duct.isOn(Line.ITEM, Direction.EAST), "the other sides stay on");
        helper.assertTrue(duct.mode(Line.ITEM, Direction.UP) == DuctMode.IN_OUT_PULL, "In, Out and Pull survive");
        helper.succeed();
    }

    /** A duct saved with the older single modes and Controls loads with the matching flags, redstone and on or off. */
    @ServusGameTest(timeoutTicks = 20)
    public static void olderSaveLoadsWithFlags(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT_B, Line.ITEM, Line.ENERGY, Line.FLUID);
        CompoundTag saved = duct.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.remove("Redstone");
        saved.remove("SwitchedOff");
        CompoundTag itemModes = new CompoundTag();
        itemModes.putString("north", "input");
        itemModes.putString("east", "output");
        itemModes.putString("west", "disabled");
        CompoundTag fluidModes = new CompoundTag();
        fluidModes.putString("north", "input");
        CompoundTag modes = new CompoundTag();
        modes.put("item", itemModes);
        modes.put("fluid", fluidModes);
        saved.put("Modes", modes);
        CompoundTag itemControls = new CompoundTag();
        itemControls.putInt("north", 3);
        itemControls.putInt("east", 1);
        CompoundTag controls = new CompoundTag();
        controls.put("item", itemControls);
        saved.put("Controls", controls);
        duct.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        helper.assertTrue(duct.mode(Line.ITEM, Direction.NORTH) == DuctMode.IN_PULL, "pull in loads as In and Pull");
        helper.assertTrue(duct.mode(Line.ITEM, Direction.EAST) == DuctMode.OUT, "push out loads as Out");
        helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == DuctMode.NONE, "disabled loads as not connected");
        helper.assertTrue(duct.mode(Line.ITEM, Direction.SOUTH) == DuctMode.IN_OUT, "in and out stays In and Out");
        helper.assertTrue(duct.mode(Line.FLUID, Direction.NORTH) == DuctMode.IN, "a fluid pull in side loads as In");
        helper.assertFalse(duct.isOn(Line.ITEM, Direction.NORTH), "Control Off loads as switched off");
        helper.assertTrue(duct.redstone(Line.ITEM, Direction.NORTH) == DuctRedstone.IGNORE, "Control Off ignores redstone");
        helper.assertTrue(duct.isOn(Line.ITEM, Direction.EAST) && duct.redstone(Line.ITEM, Direction.EAST) == DuctRedstone.POWERED,
                "Control While powered loads as switched on While powered");
        helper.succeed();
    }

    /**
     * A duct whose only neighbour, a chest to the west, sits on a side without In and Out has no arm; the carrot
     * on the duct opens the GUI on west, Pull switches In on with it and the arm returns, the redstone
     * button cycles, and On/Off keeps the redstone setting.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void disabledSideCanBeEnabledAgain(GameTestHelper helper) {
        chest(helper, HIGH.west());
        UtilityDuctBlockEntity duct = duct(helper, HIGH, Line.ITEM);
        duct.setMode(Line.ITEM, Direction.WEST, DuctMode.NONE);
        helper.assertFalse(duct.hasArm(Line.ITEM, Direction.WEST), "the unconnected side has no arm");

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
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_TOGGLE_PULL + Line.ITEM.ordinal());
            helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == DuctMode.IN_PULL, "Pull switches In on with it");
            helper.assertTrue(duct.hasArm(Line.ITEM, Direction.WEST), "the arm toward the chest is back with Pull");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_TOGGLE_PULL + Line.ITEM.ordinal());
            helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == DuctMode.IN, "switching Pull off keeps In");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_TOGGLE_OUT + Line.ITEM.ordinal());
            helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == DuctMode.IN_OUT, "the side is In and Out again");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_TOGGLE_PULL + Line.ITEM.ordinal());
            helper.assertTrue(duct.mode(Line.ITEM, Direction.WEST) == DuctMode.IN_OUT_PULL, "Pull switches on beside Out");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_CYCLE_REDSTONE + Line.ITEM.ordinal());
            helper.assertTrue(duct.redstone(Line.ITEM, Direction.WEST) == DuctRedstone.POWERED, "the redstone button cycles to While powered");
            menu.clickMenuButton(player, UtilityDuctMenu.BUTTON_TOGGLE_ON + Line.ITEM.ordinal());
            helper.assertFalse(duct.isOn(Line.ITEM, Direction.WEST), "On/Off switches the side off");
            helper.assertTrue(duct.redstone(Line.ITEM, Direction.WEST) == DuctRedstone.POWERED
                    && duct.mode(Line.ITEM, Direction.WEST) == DuctMode.IN_OUT_PULL, "switching off keeps redstone and flags");
            helper.assertTrue(duct.hasArm(Line.ITEM, Direction.WEST), "a switched off side keeps its arm");
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
