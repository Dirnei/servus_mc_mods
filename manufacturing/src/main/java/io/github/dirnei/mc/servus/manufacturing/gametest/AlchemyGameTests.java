package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The principles as fluids. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlchemyGameTests {

    static final String EMPTY = "empty_12x6x12";

    private AlchemyGameTests() {
    }

    /** A Fluid Tank stores Mercury, and an empty bucket takes it out as a Mercury bucket. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void bucketFillsFromATankOfMercury(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(tank != null, "the tank should expose a fluid handler");
        int filled = tank.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "the tank should take 1000 mB of Mercury, took " + filled);
        FluidActionResult result = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
        helper.assertTrue(result.isSuccess() && result.getResult().is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the bucket should become a Mercury bucket, got " + result.getResult());
        helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "the tank should be empty");
        helper.succeed();
    }

    static final BlockPos MACHINE = new BlockPos(3, 1, 3);

    static CalcinerBlockEntity calciner(GameTestHelper helper) {
        helper.setBlock(MACHINE, ServusManufacturingBlocks.CALCINER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CalcinerBlockEntity calciner = helper.getBlockEntity(MACHINE);
        calciner.energyStorage().receiveEnergy(8000, false);
        return calciner;
    }

    /** One cobblestone becomes 100 mB Salt and 10 mB Mercury in the Calciner's tanks. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerBurnsCobblestoneIntoPrinciples(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            Principles held = calciner.tanks().contents();
            helper.assertTrue(held.equals(new Principles(100, 0, 10)), "expected 100 Salt and 10 Mercury, got " + held);
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the cobblestone is used up");
        });
    }

    /** With no room for the Salt the Calciner waits and spends no energy. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerWaitsWhenATankIsFull(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.tanks().insert(new Principles(3950, 0, 0));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        int energy = calciner.energyStorage().getEnergyStored();
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(calciner.energyStorage().getEnergyStored() == energy, "a blocked Calciner spends no energy");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).is(Items.COBBLESTONE), "the cobblestone stays");
            helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 3950, "no Salt was added");
            helper.succeed();
        });
    }

    /** A Fluid Tank touching an Auto side receives the principles the Calciner makes. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void calcinerPushesIntoATouchingTank(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        BlockPos tankPos = MACHINE.east();
        helper.setBlock(tankPos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(tankPos), Direction.WEST);
            helper.assertTrue(tank != null && tank.getFluidInTank(0).getFluid().isSame(PrincipleFluids.fluid(Principle.SALT))
                    && tank.getFluidInTank(0).getAmount() >= 100, "the tank should hold Salt, got " + (tank == null ? null : tank.getFluidInTank(0)));
        });
    }

    /** Items without a calcination recipe are refused. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void calcinerRefusesUnknownItems(GameTestHelper helper) {
        calciner(helper);
        IItemHandler side = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MACHINE), Direction.EAST);
        helper.assertTrue(side != null, "the side should take items");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(side, new ItemStack(Items.DIAMOND), false).getCount() == 1, "a diamond is refused");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(side, new ItemStack(Items.OAK_LOG), false).isEmpty(), "a log is taken");
        helper.succeed();
    }

    static ResourceLocation coagulation(String name) {
        return ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulation/" + name);
    }

    static CoagulatorBlockEntity coagulator(GameTestHelper helper, String recipe) {
        helper.setBlock(MACHINE, ServusManufacturingBlocks.COAGULATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CoagulatorBlockEntity coagulator = helper.getBlockEntity(MACHINE);
        coagulator.energyStorage().receiveEnergy(8000, false);
        if (recipe != null) {
            coagulator.select(coagulation(recipe));
        }
        return coagulator;
    }

    static IFluidHandler coagulatorFluid(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(MACHINE), Direction.EAST);
    }

    /** Salt piped into a Coagulator making Salt Crystals becomes a Salt Crystal. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void coagulatorMakesSaltCrystalFromPipedSalt(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "salt_crystal");
        int filled = coagulatorFluid(helper).fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 250, "the Salt tank should take 250 mB, took " + filled);
        helper.succeedWhen(() -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).is(ServusManufacturingItems.SALT_CRYSTAL.get()),
                    "a Salt Crystal should be made");
            helper.assertTrue(coagulator.tanks().amount(Principle.SALT) == 0, "the Salt is used up");
        });
    }

    /** Only principles go in, each into its own tank. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coagulatorRefusesForeignFluid(GameTestHelper helper) {
        coagulator(helper, "salt_crystal");
        IFluidHandler handler = coagulatorFluid(helper);
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water is refused");
        helper.assertTrue(handler.fill(new FluidStack(PrincipleFluids.fluid(Principle.SULFUR), 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                "Sulfur is taken");
        helper.assertTrue(handler.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "the Coagulator gives nothing back");
        helper.succeed();
    }

    /** Without enough Mercury the Coagulator waits and spends no energy. */
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void coagulatorWaitsForMissingPrinciple(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "quicksilver");
        coagulator.tanks().insert(new Principles(0, 0, 20));
        int energy = coagulator.energyStorage().getEnergyStored();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(coagulator.energyStorage().getEnergyStored() == energy, "a waiting Coagulator spends no energy");
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "nothing is made");
            helper.succeed();
        });
    }

    /** The selected recipe is saved with the block entity and reported as the only production recipe. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coagulatorKeepsAndReportsItsSelection(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "gunpowder");
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(production != null && production.recipes().size() == 1
                && production.recipes().get(0).outputs().get(0).is(Items.GUNPOWDER), "only the gunpowder recipe should be reported");
        CompoundTag saved = coagulator.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
        CoagulatorBlockEntity fresh = coagulator(helper, null);
        helper.assertTrue(fresh.selected().isEmpty(), "a new Coagulator has nothing selected");
        fresh.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(fresh.selected().filter(id -> id.equals(coagulation("gunpowder"))).isPresent(), "the selection should be loaded");
        helper.succeed();
    }

    /** A Calciner's work counts for its team's journal: the item and every principle it made. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerReportsToItsTeamJournal(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        UUID team = UUID.randomUUID();
        calciner.setTeam(team);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(helper.getLevel().getServer(), team);
            helper.assertTrue(journal.count(VILLACH, "calcine") == 1, "one calcined item should count");
            helper.assertTrue(journal.count(VILLACH, "salt") == 100, "100 mB Salt should count, got " + journal.count(VILLACH, "salt"));
        });
    }

    static final String VILLACH = "servus_manufacturing:villach";

    /** Finishing Villach through team events opens chapter locked recipes; its advancement exists for the book. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void finishingVillachAwardsItsAdvancement(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        coagulator.setTeam(team);
        CoagulationRecipe locked = new CoagulationRecipe(new Principles(10, 0, 0), Optional.empty(), new ItemStack(Items.FLINT),
                40, Optional.of(ResourceLocation.parse(VILLACH)));
        helper.assertTrue(!coagulator.unlocked(locked), "a recipe of an unfinished chapter is locked");
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()), 1);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, VILLACH), "Villach should be finished");
        helper.assertTrue(coagulator.unlocked(locked), "the recipe opens with its chapter");
        // Awarding to a mock player is left to the play check: Modonomicon syncs book data on every award,
        // which a mock connection cannot carry.
        AdvancementHolder advancement = level.getServer().getAdvancements().get(Journal.advancementId(VILLACH));
        helper.assertTrue(advancement != null, "the Villach advancement should exist");
        helper.succeed();
    }

    /** An Output side lets neighbours take but pushes nothing; a Push side pushes (shared machine rule). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outputSideDoesNotPush(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        BlockPos chestPos = MACHINE.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        coagulator.inventory().setStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT, new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get(), 5));
        coagulator.sideConfig().set(RelativeSide.LEFT, SideMode.OUTPUT);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(chest.countItem(ServusManufacturingItems.SALT_CRYSTAL.get()) == 0, "an Output side must not push");
            coagulator.sideConfig().set(RelativeSide.LEFT, SideMode.PUSH);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(chest.countItem(ServusManufacturingItems.SALT_CRYSTAL.get()) == 5, "a Push side pushes");
                helper.succeed();
            });
        });
    }
}
