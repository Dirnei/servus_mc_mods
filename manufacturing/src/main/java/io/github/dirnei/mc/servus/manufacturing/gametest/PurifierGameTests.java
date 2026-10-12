package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierSideBlock;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/** The Purifier build: Ash and water leached in the Vat into liquid Salt in the Catch Basin, Caput Mortuum in the Residue Crock. */
public final class PurifierGameTests {

    /** The Leaching Vat; the log stands below it. */
    static final BlockPos PURIFIER = new BlockPos(3, 2, 3);
    /** Built along x facing north: the Basin east of the log, the Crock west of it. */
    static final BlockPos BASIN = PURIFIER.below().east();
    static final BlockPos CROCK = PURIFIER.below().west();

    private PurifierGameTests() {
    }

    /** Builds the unformed Purifier along x: a log below the Vat, the Basin and the Crock beside the log. */
    static void build(GameTestHelper helper, BlockPos vat, boolean mirrored) {
        helper.setBlock(vat, ServusManufacturingBlocks.PURIFIER.get().defaultBlockState());
        helper.setBlock(vat.below(), Blocks.BIRCH_LOG);
        helper.setBlock(mirrored ? vat.below().west() : vat.below().east(), ServusManufacturingBlocks.CATCH_BASIN.get().defaultBlockState());
        helper.setBlock(mirrored ? vat.below().east() : vat.below().west(), ServusManufacturingBlocks.RESIDUE_CROCK.get().defaultBlockState());
    }

    static void form(GameTestHelper helper) {
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), PURIFIER);
    }

    static PurifierBlockEntity purifier(GameTestHelper helper) {
        build(helper, PURIFIER, false);
        form(helper);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, true);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FORMED, true);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FORMED, true);
        return helper.getBlockEntity(PURIFIER, PurifierBlockEntity.class);
    }

    static FluidView vatFluids(GameTestHelper helper) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(PURIFIER), Direction.UP));
    }

    static FluidView basinFluids(GameTestHelper helper) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(BASIN), Direction.NORTH));
    }

    static ItemView crockItems(GameTestHelper helper) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(CROCK), Direction.DOWN));
    }

    static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    static FluidStack salt(int amount) {
        return new FluidStack(PrincipleFluids.fluid(Principle.SALT), amount);
    }

    /** Eight Ash and 1,000 mB water become 800 mB Salt and two Caput Mortuum and leave 200 mB water. */
    @ServusGameTest(timeoutTicks = 500)
    public static void purifierLeachesAsh(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        ItemView items = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(PURIFIER), Direction.UP));
        helper.assertTrue(items.getSlots() == 1, "the Vat shows one slot");
        helper.assertTrue(items.insertItem(0, new ItemStack(ServusManufacturingItems.ASH.get(), 8), false).isEmpty(), "the Vat takes Ash");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "the Vat refuses dirt");
        helper.assertTrue(vatFluids(helper).fill(water(1000), false) == 1000, "the Vat takes water");
        helper.succeedWhen(() -> {
            helper.assertTrue(purifier.salt().getFluidAmount() == 800, "800 mB Salt, got " + purifier.salt().getFluidAmount());
            helper.assertTrue(purifier.water().getFluidAmount() == 200, "200 mB water left, got " + purifier.water().getFluidAmount());
            helper.assertTrue(items.extractItem(0, 64, true).isEmpty(), "the Vat gives nothing");
            ItemStack residue = crockItems(helper).extractItem(0, 64, true);
            helper.assertTrue(residue.is(ServusManufacturingItems.CAPUT_MORTUUM.get()) && residue.getCount() == 2, "two Caput Mortuum wait in the Crock");
        });
    }

    /** Without water the Ash stays. */
    @ServusGameTest(timeoutTicks = 100)
    public static void purifierNeedsWater(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get(), 4));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(purifier.salt().isEmpty(), "no Salt without water");
            helper.assertTrue(purifier.inventory().getStackInSlot(PurifierBlockEntity.SLOT_INPUT).getCount() == 4, "the Ash stays");
            helper.succeed();
        });
    }

    /** The Vat takes only water, the Basin gives only Salt, the Crock takes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void vatTakesWaterBasinGivesSalt(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        FluidView vat = vatFluids(helper);
        FluidView basin = basinFluids(helper);
        helper.assertTrue(vat.fill(salt(100), false) == 0, "the Vat refuses Salt");
        helper.assertTrue(vat.fill(water(500), false) == 500, "the Vat takes water");
        helper.assertTrue(vat.drain(100, false).isEmpty(), "the Vat gives nothing");
        helper.assertTrue(basin.fill(water(100), false) == 0, "the Basin takes nothing");
        helper.assertTrue(crockItems(helper).insertItem(0, new ItemStack(ServusManufacturingItems.ASH.get()), false).getCount() == 1,
                "the Crock takes nothing");
        purifier.salt().fill(salt(250), false);
        FluidStack drained = basin.drain(1000, false);
        helper.assertTrue(drained.getFluid().isSame(PrincipleFluids.fluid(Principle.SALT)) && drained.getAmount() == 250, "the Salt comes out, got " + drained);
        helper.succeed();
    }

    /** A tank beside the Basin receives the Salt. */
    @ServusGameTest(timeoutTicks = 60)
    public static void basinPushesSaltIntoATank(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        BlockPos tank = BASIN.north();
        helper.setBlock(tank, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        purifier.salt().fill(salt(300), false);
        helper.succeedWhen(() -> helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(tank), Direction.UP))
                .getFluidInTank(0).getAmount() == 300, "the tank should hold 300 mB Salt"));
    }

    /** A hopper under the Crock collects the Caput Mortuum. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperTakesResidueFromTheCrock(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        purifier.inventory().setStackInSlot(PurifierBlockEntity.SLOT_RESIDUE, new ItemStack(ServusManufacturingItems.CAPUT_MORTUUM.get(), 2));
        BlockPos hopper = CROCK.below();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper, HopperBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(hopperEntity.countItem(ServusManufacturingItems.CAPUT_MORTUUM.get()) == 2,
                "the hopper should hold two Caput Mortuum, holds " + hopperEntity.countItem(ServusManufacturingItems.CAPUT_MORTUUM.get())));
    }

    /** A water bucket fills the Vat, an empty bucket takes Salt at the Basin, an empty hand takes the residue at the Crock. */
    @ServusGameTest(timeoutTicks = 20)
    public static void purifierByHand(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.WATER_BUCKET));
        CalcinerGameTests.use(helper, player, PURIFIER);
        helper.assertTrue(purifier.water().getFluidAmount() == 1000, "the water bucket pours in, got " + purifier.water().getFluidAmount());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player holds an empty bucket");
        purifier.salt().fill(salt(1000), false);
        CalcinerGameTests.use(helper, player, BASIN);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.SALT).bucket().get()),
                "the bucket fills with Salt at the Basin, holds " + player.getItemInHand(InteractionHand.MAIN_HAND));
        purifier.inventory().setStackInSlot(PurifierBlockEntity.SLOT_RESIDUE, new ItemStack(ServusManufacturingItems.CAPUT_MORTUUM.get(), 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CalcinerGameTests.use(helper, player, PURIFIER);
        helper.assertTrue(player.getInventory().countItem(ServusManufacturingItems.CAPUT_MORTUUM.get()) == 0, "an empty hand on the Vat takes no residue");
        CalcinerGameTests.use(helper, player, CROCK);
        helper.assertTrue(player.getInventory().countItem(ServusManufacturingItems.CAPUT_MORTUUM.get()) == 2, "an empty hand on the Crock takes the residue");
        helper.succeed();
    }

    /** Without its Crock nothing forms; breaking the log of a formed Purifier unforms it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void purifierFormsAndUnforms(GameTestHelper helper) {
        build(helper, PURIFIER, false);
        helper.setBlock(CROCK, Blocks.STONE);
        form(helper);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        purifier(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(PURIFIER.below()), true);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FORMED, false);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FORMED, false);
        helper.succeed();
    }

    /** Breaking the Crock unforms the Vat and the Basin. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheCrockUnforms(GameTestHelper helper) {
        purifier(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(CROCK), true);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FORMED, false);
        helper.succeed();
    }

    /** The Basin and the Crock may swap sides; the Basin still gives the Salt. */
    @ServusGameTest(timeoutTicks = 20)
    public static void mirroredPurifierForms(GameTestHelper helper) {
        build(helper, PURIFIER, true);
        form(helper);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, true);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FORMED, true);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FORMED, true);
        PurifierBlockEntity purifier = helper.getBlockEntity(PURIFIER, PurifierBlockEntity.class);
        purifier.salt().fill(salt(100), false);
        FluidView basin = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(CROCK), Direction.NORTH));
        helper.assertTrue(basin != null && basin.drain(100, true).getAmount() == 100,
                "the mirrored Basin, west of the log, gives the Salt");
        helper.succeed();
    }

    /** Forming turns the Vat to the build's front and the Basin and the Crock to the log; unforming shows the plain blocks again. */
    @ServusGameTest(timeoutTicks = 20)
    public static void formedStatesFollowTheBuild(GameTestHelper helper) {
        purifier(helper);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FACING, Direction.WEST);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FACING, Direction.EAST);
        helper.getLevel().destroyBlock(helper.absolutePos(PURIFIER.below()), true);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FORMED, false);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FORMED, false);
        build(helper, PURIFIER, true);
        form(helper);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FACING, Direction.SOUTH);
        helper.assertBlockProperty(CROCK, PurifierSideBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(BASIN, PurifierSideBlock.FACING, Direction.WEST);
        helper.succeed();
    }

    /** A finished purification counts for the Villach task of the owner's team. */
    @ServusGameTest(timeoutTicks = 300)
    public static void purifierReportsToTheJournal(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        UUID team = UUID.randomUUID();
        purifier.setTeam(team);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get(), 4));
        purifier.water().fill(water(400), false);
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(helper.getLevel().getServer(), team);
            helper.assertTrue(journal.count(AlchemyGameTests.VILLACH, "purify") >= 1, "the purification should count");
        });
    }

    /** The journal's schematic accepts the build, anchored on the log. */
    @ServusGameTest(timeoutTicks = 20)
    public static void purifierSchematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier"));
        helper.assertTrue(schematic != null, "the Purifier schematic should be loaded");
        build(helper, PURIFIER, false);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(PURIFIER.below())) != null, "the schematic accepts the build");
        helper.succeed();
    }
}
