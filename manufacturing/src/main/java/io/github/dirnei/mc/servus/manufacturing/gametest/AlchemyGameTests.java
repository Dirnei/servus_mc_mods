package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalBookItem;
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
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
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

    /** With no room for the Salt the Calciner waits and lights no fuel. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerWaitsWhenATankIsFull(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.tanks().insert(new Principles(3950, 0, 0));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 4, "a blocked Calciner burns no fuel");
            helper.assertTrue(!calciner.isBurning(), "a blocked Calciner is not lit");
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

    /** Without fuel the Calciner does nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void calcinerNeedsFuel(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, ItemStack.EMPTY);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(calciner.tanks().contents().equals(new Principles(0, 0, 0)), "nothing is calcined without fuel");
            helper.succeed();
        });
    }

    /** The Calciner offers no energy storage and reports its fuel as upkeep for the data line. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void calcinerBurnsFuelInsteadOfEnergy(GameTestHelper helper) {
        calciner(helper);
        BlockPos pos = helper.absolutePos(MACHINE);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.EAST) == null, "no energy storage");
        UpkeepHolder upkeep = helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, pos, null);
        helper.assertTrue(upkeep != null && upkeep.upkeepItem().is(Items.COAL), "coal is the Calciner's upkeep");
        helper.assertTrue(upkeep.upkeepCount() == 4, "the fuel slot counts as upkeep, got " + upkeep.upkeepCount());
        helper.succeed();
    }

    /** The Coagulator offers no energy storage: it needs no power. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coagulatorNeedsNoPower(GameTestHelper helper) {
        coagulator(helper, "salt_crystal");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(MACHINE), Direction.EAST) == null,
                "no energy storage");
        helper.succeed();
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

    /** Without enough Mercury the Coagulator waits. */
    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void coagulatorWaitsForMissingPrinciple(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "quicksilver");
        coagulator.tanks().insert(new Principles(0, 0, 20));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(coagulator.tanks().amount(Principle.MERCURY) == 20, "the Mercury stays");
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

    /** The journal recipe makes Modonomicon's book item set to Paracelsus' Journal. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void journalRecipeMakesTheJournal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack result = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal"))
                .map(holder -> holder.value().getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(!result.isEmpty(), "the journal recipe should exist");
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, JournalBookItem.create()), "the result should be the journal, got " + result);
        helper.assertTrue(result.getComponentsPatch().toString().contains("servus_manufacturing:journal"), "the book id should be set, got " + result.getComponentsPatch());
        helper.succeed();
    }

    static AthanorBlockEntity athanor(GameTestHelper helper) {
        helper.setBlock(MACHINE, ServusManufacturingBlocks.ATHANOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(MACHINE);
    }

    static void saltRing(GameTestHelper helper) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(MACHINE.offset(dx, 0, dz), ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get().defaultBlockState());
                }
            }
        }
    }

    /** Gunpowder and Sulfur become Blaze Powder on a bare Athanor. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void athanorMakesBlazePowderAtNigredo(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.tanks().insert(new Principles(0, 200, 0));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        helper.succeedWhen(() -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.BLAZE_POWDER), "Blaze Powder should be made");
            helper.assertTrue(athanor.tanks().amount(Principle.SULFUR) == 0, "the Sulfur is burned");
        });
    }

    /** The Salt Crystal ring makes the Athanor Albedo and allows the Albedo craft; a gap brings it back to Nigredo. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void athanorTierFollowsTheRing(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.tanks().insert(new Principles(50, 0, 100));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Albedo craft at Nigredo");
            saltRing(helper);
            athanor.checkTier();
            helper.assertTrue(athanor.tier() == 1 && helper.getBlockState(MACHINE).getValue(AthanorBlock.TIER) == 1, "the ring makes Albedo");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.EXPERIENCE_BOTTLE),
                        "the Albedo craft should run");
                helper.setBlock(MACHINE.east(), Blocks.AIR.defaultBlockState());
                athanor.checkTier();
                helper.assertTrue(athanor.tier() == 0, "a gap in the ring is Nigredo again");
                helper.succeed();
            });
        });
    }

    /** A Sulfur bucket empties into the Athanor's Sulfur tank. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void athanorTakesABucket(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        FluidActionResult result = FluidUtil.tryEmptyContainer(new ItemStack(PrincipleFluids.entry(Principle.SULFUR).bucket().get()),
                athanor.tanks(), 1000, null, true);
        helper.assertTrue(result.isSuccess() && result.getResult().is(Items.BUCKET), "the bucket should be emptied");
        helper.assertTrue(athanor.tanks().amount(Principle.SULFUR) == 1000, "the Sulfur tank should hold 1000 mB");
        helper.succeed();
    }

    /** A craft on an Albedo Athanor counts for Schwaz once the team finished Villach. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void albedoCraftCountsForSchwaz(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()), 1);
        AthanorBlockEntity athanor = athanor(helper);
        athanor.setTeam(team);
        saltRing(helper);
        athanor.tanks().insert(new Principles(50, 0, 100));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(level.getServer(), team);
            helper.assertTrue(journal.count("servus_manufacturing:schwaz", "albedo_craft") == 1, "the Albedo craft should count");
            Journal.recordEvent(level, team, "sight", 1);
            helper.assertTrue(Journal.journalOf(level.getServer(), team).count("servus_manufacturing:schwaz", "sight") == 1,
                    "the sight should count for Schwaz");
        });
    }

    /** Drinking the Elixir of Sight gives Elemental Sight and leaves a glass bottle. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void elixirGivesElementalSight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack left = ElementalSight.ELIXIR.get().finishUsingItem(new ItemStack(ElementalSight.ELIXIR.get()), helper.getLevel(), player);
        helper.assertTrue(ElementalSight.active(player), "the drinker should have Elemental Sight");
        helper.assertTrue(player.getEffect(ElementalSight.EFFECT).getDuration() == ElementalSight.DURATION, "for three minutes");
        helper.assertTrue(left.is(Items.GLASS_BOTTLE), "a glass bottle should be left, got " + left);
        helper.succeed();
    }

    /** The elixir is an Albedo craft: nothing at Nigredo, the elixir once the ring stands. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void elixirNeedsAnAlbedoAthanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.tanks().insert(new Principles(100, 100, 250));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusManufacturingItems.QUICKSILVER.get()));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no elixir at Nigredo");
            saltRing(helper);
            athanor.checkTier();
            helper.succeedWhen(() -> helper.assertTrue(
                    athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ElementalSight.ELIXIR.get()), "the elixir should be made"));
        });
    }

    /** Production lists only the crafts the tier allows. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void athanorProductionFollowsTheTier(GameTestHelper helper) {
        athanor(helper);
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(production != null, "the Athanor should be a producer");
        helper.assertTrue(production.recipes().stream().anyMatch(recipe -> recipe.outputs().get(0).is(Items.BLAZE_POWDER)), "Blaze Powder at Nigredo");
        helper.assertTrue(production.recipes().stream().noneMatch(recipe -> recipe.outputs().get(0).is(Items.EXPERIENCE_BOTTLE)),
                "no Albedo craft at Nigredo");
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

    /** Facing north the Calciner's left is east and its right is west. */
    private static void onlyFluidSides(CalcinerBlockEntity calciner, int left, int right) {
        FluidSides sides = calciner.fluidSides();
        for (RelativeSide side : RelativeSide.values()) {
            sides.set(side, FluidSides.OFF);
        }
        sides.set(RelativeSide.LEFT, left);
        sides.set(RelativeSide.RIGHT, right);
    }

    /** A Salt side and a Mercury side fill two touching tanks with one principle each. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void calcinerSeparatesPrinciplesBySide(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        onlyFluidSides(calciner, FluidSides.TANK_FIRST + Principle.SALT.ordinal(), FluidSides.TANK_FIRST + Principle.MERCURY.ordinal());
        BlockPos east = MACHINE.east();
        BlockPos west = MACHINE.west();
        helper.setBlock(east, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(west, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        IFluidHandler eastSide = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(MACHINE), Direction.EAST);
        helper.assertTrue(eastSide != null && eastSide.getTanks() == 1, "the Salt side should offer one tank");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(MACHINE), Direction.UP) == null,
                "an Off side offers nothing");
        helper.succeedWhen(() -> {
            IFluidHandler saltTank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(east), Direction.WEST);
            IFluidHandler mercuryTank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(west), Direction.EAST);
            helper.assertTrue(saltTank.getFluidInTank(0).getFluid().isSame(PrincipleFluids.fluid(Principle.SALT))
                    && saltTank.getFluidInTank(0).getAmount() == 200, "the east tank should hold 200 Salt, got " + saltTank.getFluidInTank(0));
            helper.assertTrue(mercuryTank.getFluidInTank(0).getFluid().isSame(PrincipleFluids.fluid(Principle.MERCURY))
                    && mercuryTank.getFluidInTank(0).getAmount() == 20, "the west tank should hold 20 Mercury, got " + mercuryTank.getFluidInTank(0));
        });
    }

    /** A Coagulator side set to Mercury takes Mercury and refuses Salt. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coagulatorMercurySideRefusesSalt(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "salt_crystal");
        coagulator.fluidSides().set(RelativeSide.LEFT, FluidSides.TANK_FIRST + Principle.MERCURY.ordinal());
        helper.getLevel().invalidateCapabilities(helper.absolutePos(MACHINE));
        IFluidHandler handler = coagulatorFluid(helper);
        helper.assertTrue(handler.fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                "the Mercury side refuses Salt");
        helper.assertTrue(handler.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                "the Mercury side takes Mercury");
        helper.succeed();
    }

    /** A carrot set to Fluids cycles the clicked face's Fluids side and leaves its Items side alone. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void fluidCarrotCyclesTheFluidsLayer(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        carrot.set(ServusFoundationComponents.CARROT_LAYER.get(), SideLayer.FLUIDS.name());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        BlockPos pos = helper.absolutePos(MACHINE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.5, 0, 0), Direction.EAST, pos, false);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(calciner.fluidSides().value(RelativeSide.LEFT) == FluidSides.TANK_FIRST,
                "the left Fluids side should move from All to Salt, got " + calciner.fluidSides().value(RelativeSide.LEFT));
        helper.assertTrue(calciner.sideConfig().mode(RelativeSide.LEFT) == SideMode.AUTO, "the Items side stays Auto");
        helper.succeed();
    }

    /** Both layers survive saving and loading the block entity, as in a dismantled item. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void fluidSidesAreSaved(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.fluidSides().set(RelativeSide.TOP, FluidSides.TANK_FIRST + Principle.SULFUR.ordinal());
        calciner.sideConfig().set(RelativeSide.TOP, SideMode.INPUT);
        CompoundTag tag = calciner.saveWithoutMetadata(helper.getLevel().registryAccess());
        BlockPos other = MACHINE.east(3);
        helper.setBlock(other, ServusManufacturingBlocks.CALCINER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CalcinerBlockEntity copy = helper.getBlockEntity(other);
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(copy.fluidSides().tank(RelativeSide.TOP) == Principle.SULFUR.ordinal(), "the Fluids layer is kept");
        helper.assertTrue(copy.sideConfig().mode(RelativeSide.TOP) == SideMode.INPUT, "the Items layer is kept");
        helper.succeed();
    }

    /** An empty bucket on the Calciner's Salt gauge takes Salt out; the Mercury stays. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void calcinerGaugeFillsABucket(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.tanks().insert(new Principles(1500, 0, 1000));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerMenu menu =
                new io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerMenu(0, player.getInventory(), calciner);
        menu.setCarried(new ItemStack(Items.BUCKET));
        boolean clicked = menu.clickMenuButton(player,
                io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleBuckets.GAUGE_BUTTON_FIRST + Principle.SALT.ordinal());
        helper.assertTrue(clicked, "the gauge click should be handled");
        helper.assertTrue(menu.getCarried().is(PrincipleFluids.entry(Principle.SALT).bucket().get()), "the cursor should hold a Salt bucket");
        helper.assertTrue(calciner.tanks().contents().equals(new Principles(500, 0, 1000)), "1,000 mB Salt should be gone, got "
                + calciner.tanks().contents());
        helper.succeed();
    }

    /** A Salt bucket used on the Coagulator block pours into its Salt tank and leaves an empty bucket. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void coagulatorTakesAPrincipleBucket(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bucket = new ItemStack(PrincipleFluids.entry(Principle.SALT).bucket().get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(MACHINE);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.getLevel().getBlockState(absolute).useItemOn(bucket, helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(1000, 0, 0)), "the Salt tank should hold 1,000 mB, got "
                + coagulator.tanks().contents());
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).is(Items.BUCKET), "an empty bucket should be left");
        helper.succeed();
    }
}
