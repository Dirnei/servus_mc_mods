package io.github.dirnei.mc.servus.manufacturing.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.CatchBasinBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The Purifier build: Ash and water leached in the Vat into liquid Salt in the Catch Basin, Caput Mortuum left behind. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PurifierGameTests {

    /** The Leaching Vat; the Catch Basin stands on the template floor below it. */
    static final BlockPos PURIFIER = new BlockPos(3, 2, 3);

    private PurifierGameTests() {
    }

    /** Builds the unformed Purifier along x: planks beside the Vat, logs beside the Basin. */
    static void build(GameTestHelper helper, BlockPos vat) {
        helper.setBlock(vat, ServusManufacturingBlocks.PURIFIER.get().defaultBlockState());
        helper.setBlock(vat.below(), ServusManufacturingBlocks.CATCH_BASIN.get().defaultBlockState());
        helper.setBlock(vat.east(), Blocks.SPRUCE_PLANKS);
        helper.setBlock(vat.west(), Blocks.OAK_PLANKS);
        helper.setBlock(vat.below().east(), Blocks.BIRCH_LOG);
        helper.setBlock(vat.below().west(), Blocks.OAK_LOG);
    }

    static PurifierBlockEntity purifier(GameTestHelper helper) {
        build(helper, PURIFIER);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), PURIFIER);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, true);
        helper.assertBlockProperty(PURIFIER.below(), CatchBasinBlock.FORMED, true);
        return helper.getBlockEntity(PURIFIER);
    }

    static IFluidHandler vatFluids(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(PURIFIER), Direction.UP);
    }

    static IFluidHandler basinFluids(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(PURIFIER.below()), Direction.NORTH);
    }

    static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    static FluidStack salt(int amount) {
        return new FluidStack(PrincipleFluids.fluid(Principle.SALT), amount);
    }

    /** Eight Ash and 1,000 mB water become 800 mB Salt and two Caput Mortuum and leave 200 mB water. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 500)
    public static void purifierLeachesAsh(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(PURIFIER), Direction.UP);
        helper.assertTrue(items.insertItem(0, new ItemStack(ServusManufacturingItems.ASH.get(), 8), false).isEmpty(), "the Vat takes Ash");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "the Vat refuses dirt");
        helper.assertTrue(vatFluids(helper).fill(water(1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the Vat takes water");
        helper.succeedWhen(() -> {
            helper.assertTrue(purifier.salt().getFluidAmount() == 800, "800 mB Salt, got " + purifier.salt().getFluidAmount());
            helper.assertTrue(purifier.water().getFluidAmount() == 200, "200 mB water left, got " + purifier.water().getFluidAmount());
            helper.assertTrue(items.extractItem(1, 64, true).is(ServusManufacturingItems.CAPUT_MORTUUM.get())
                    && items.extractItem(1, 64, true).getCount() == 2, "two Caput Mortuum wait in the Vat");
        });
    }

    /** Without water the Ash stays. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 100)
    public static void purifierNeedsWater(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get(), 4));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(purifier.salt().isEmpty(), "no Salt without water");
            helper.assertTrue(purifier.inventory().getStackInSlot(PurifierBlockEntity.SLOT_INPUT).getCount() == 4, "the Ash stays");
            helper.succeed();
        });
    }

    /** The Vat takes only water, the Basin gives only Salt. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void vatTakesWaterBasinGivesSalt(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        IFluidHandler vat = vatFluids(helper);
        IFluidHandler basin = basinFluids(helper);
        helper.assertTrue(vat.fill(salt(100), IFluidHandler.FluidAction.EXECUTE) == 0, "the Vat refuses Salt");
        helper.assertTrue(vat.fill(water(500), IFluidHandler.FluidAction.EXECUTE) == 500, "the Vat takes water");
        helper.assertTrue(vat.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "the Vat gives nothing");
        helper.assertTrue(basin.fill(water(100), IFluidHandler.FluidAction.EXECUTE) == 0, "the Basin takes nothing");
        purifier.salt().fill(salt(250), IFluidHandler.FluidAction.EXECUTE);
        FluidStack drained = basin.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getFluid().isSame(PrincipleFluids.fluid(Principle.SALT)) && drained.getAmount() == 250, "the Salt comes out, got " + drained);
        helper.succeed();
    }

    /** A tank beside the Basin receives the Salt. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 60)
    public static void basinPushesSaltIntoATank(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        BlockPos tank = PURIFIER.below().north();
        helper.setBlock(tank, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        purifier.salt().fill(salt(300), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(tank), Direction.UP)
                .getFluidInTank(0).getAmount() == 300, "the tank should hold 300 mB Salt"));
    }

    /** A water bucket fills the Vat, an empty bucket takes Salt at the Basin, an empty hand takes the residue. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void purifierByHand(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.WATER_BUCKET));
        CalcinerGameTests.use(helper, player, PURIFIER);
        helper.assertTrue(purifier.water().getFluidAmount() == 1000, "the water bucket pours in, got " + purifier.water().getFluidAmount());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player holds an empty bucket");
        purifier.salt().fill(salt(1000), IFluidHandler.FluidAction.EXECUTE);
        CalcinerGameTests.use(helper, player, PURIFIER.below());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.SALT).bucket().get()),
                "the bucket fills with Salt at the Basin, holds " + player.getItemInHand(InteractionHand.MAIN_HAND));
        purifier.inventory().setStackInSlot(PurifierBlockEntity.SLOT_RESIDUE, new ItemStack(ServusManufacturingItems.CAPUT_MORTUUM.get(), 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CalcinerGameTests.use(helper, player, PURIFIER);
        helper.assertTrue(player.getInventory().countItem(ServusManufacturingItems.CAPUT_MORTUUM.get()) == 2, "an empty hand takes the residue");
        helper.succeed();
    }

    /** Without its logs nothing forms; breaking a plank of a formed Purifier unforms it. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void purifierFormsAndUnforms(GameTestHelper helper) {
        build(helper, PURIFIER);
        helper.setBlock(PURIFIER.below().east(), Blocks.STONE);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), PURIFIER);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        purifier(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(PURIFIER.west()), true);
        helper.assertBlockProperty(PURIFIER, PurifierBlock.FORMED, false);
        helper.assertBlockProperty(PURIFIER.below(), CatchBasinBlock.FORMED, false);
        helper.succeed();
    }

    /** A finished purification counts for the Villach task of the owner's team. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 300)
    public static void purifierReportsToTheJournal(GameTestHelper helper) {
        PurifierBlockEntity purifier = purifier(helper);
        UUID team = UUID.randomUUID();
        purifier.setTeam(team);
        purifier.insert(new ItemStack(ServusManufacturingItems.ASH.get(), 4));
        purifier.water().fill(water(400), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(helper.getLevel().getServer(), team);
            helper.assertTrue(journal.count(AlchemyGameTests.VILLACH, "purify") >= 1, "the purification should count");
        });
    }

    /** The journal's schematic accepts the build, anchored on the Basin. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void purifierSchematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier"));
        helper.assertTrue(schematic != null, "the Purifier schematic should be loaded");
        build(helper, PURIFIER);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(PURIFIER.below())) != null, "the schematic accepts the build");
        helper.succeed();
    }
}
