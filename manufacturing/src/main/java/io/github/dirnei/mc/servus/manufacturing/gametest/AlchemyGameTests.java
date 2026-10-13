package io.github.dirnei.mc.servus.manufacturing.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.research.ResearchServices;
import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalBookItem;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger;
import io.github.dirnei.mc.servus.manufacturing.journal.ParacelsusJournalItem;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** The principles as fluids. */
public final class AlchemyGameTests {

    private AlchemyGameTests() {
    }

    /** A Fluid Tank stores Mercury, and an empty bucket takes it out as a Mercury bucket. */
    @ServusGameTest(timeoutTicks = 20)
    public static void bucketFillsFromATankOfMercury(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        FluidView tank = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), Direction.UP));
        helper.assertTrue(tank != null, "the tank should expose a fluid handler");
        int filled = tank.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 1000), false);
        helper.assertTrue(filled == 1000, "the tank should take 1000 mB of Mercury, took " + filled);
        ItemStack result = Transfer.fillContainer(new ItemStack(Items.BUCKET), tank.handler());
        helper.assertTrue(result.is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the bucket should become a Mercury bucket, got " + result);
        helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "the tank should be empty");
        helper.succeed();
    }

    static final BlockPos MACHINE = new BlockPos(3, 1, 3);

    /** A formed Calciner structure with four coal in its Firebox. */
    static CalcinerBlockEntity calciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = CalcinerGameTests.formed(helper, CalcinerGameTests.CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        return calciner;
    }

    /** One cobblestone becomes 100 mB Salt and 10 mB Mercury in the Calciner's tanks. */
    @ServusGameTest(timeoutTicks = 200)
    public static void calcinerBurnsCobblestoneIntoPrinciples(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            Principles held = calciner.tanks().contents();
            // The Mercury runs on from the Receiver into the Mercury tank of the structure.
            BlockPos mercuryTank = CalcinerStructure.pos(CalcinerGameTests.CALCINER,
                    CalcinerLayout.cell(15),
                    Facing.NORTH);
            int mercury = held.mercury() + CalcinerGameTests.tankAt(helper, mercuryTank).getFluidInTank(0).getAmount();
            helper.assertTrue(held.salt() == 100 && held.sulfur() == 0 && mercury == 10,
                    "expected 100 Salt and 10 Mercury, got " + held + " and " + mercury + " Mercury in all");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the cobblestone is used up");
        });
    }

    /** With no room for the Salt the Calciner waits and lights no fuel. */
    @ServusGameTest(timeoutTicks = 200)
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

    /** Without fuel the Calciner does nothing. */
    @ServusGameTest(timeoutTicks = 120)
    public static void calcinerNeedsFuel(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, ItemStack.EMPTY);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(calciner.tanks().contents().equals(new Principles(0, 0, 0)), "nothing is calcined without fuel");
            helper.succeed();
        });
    }

    /** The Coagulator offers no energy storage: it needs no power. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorNeedsNoPower(GameTestHelper helper) {
        coagulator(helper, "salt_crystal");
        helper.assertTrue(EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(MACHINE), Direction.EAST)) == null,
                "no energy storage");
        helper.succeed();
    }

    static Identifier coagulation(String name) {
        return Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulation/" + name);
    }

    /** A formed Coagulator build around {@link #MACHINE}, with {@code recipe} selected when not null. */
    static CoagulatorBlockEntity coagulator(GameTestHelper helper, String recipe) {
        CoagulatorBlockEntity coagulator = CoagulatorGameTests.formed(helper, MACHINE);
        if (recipe != null) {
            coagulator.select(coagulation(recipe));
        }
        return coagulator;
    }

    /** The Salt Vat above the Coagulator. */
    static FluidView coagulatorFluid(GameTestHelper helper) {
        return CoagulatorGameTests.vatHandler(helper, MACHINE, 1);
    }

    /** Salt piped into the Salt Vat of a Coagulator making Salt Crystals becomes a Salt Crystal. */
    @ServusGameTest(timeoutTicks = 200)
    public static void coagulatorMakesSaltCrystalFromPipedSalt(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "salt_crystal");
        int filled = coagulatorFluid(helper).fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 250), false);
        helper.assertTrue(filled == 250, "the Salt tank should take 250 mB, took " + filled);
        helper.succeedWhen(() -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).is(ServusManufacturingItems.SALT_CRYSTAL.get()),
                    "a Salt Crystal should be made");
            helper.assertTrue(coagulator.tanks().amount(Principle.SALT) == 0, "the Salt is used up");
        });
    }

    /** Only its principle goes into a Vat. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorRefusesForeignFluid(GameTestHelper helper) {
        coagulator(helper, "salt_crystal");
        FluidView handler = coagulatorFluid(helper);
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), false) == 0, "water is refused");
        helper.assertTrue(handler.fill(new FluidStack(PrincipleFluids.fluid(Principle.SULFUR), 100), false) == 0,
                "the Salt Vat refuses Sulfur");
        helper.assertTrue(handler.fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 100), false) == 100,
                "the Salt Vat takes Salt");
        helper.assertTrue(handler.drain(100, false).isEmpty(), "the Coagulator gives nothing back");
        helper.succeed();
    }

    /** Without enough Mercury the Coagulator waits. */
    @ServusGameTest(timeoutTicks = 120)
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
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorKeepsAndReportsItsSelection(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "gunpowder");
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(production != null && production.recipes().size() == 1
                && production.recipes().get(0).outputs().get(0).is(Items.GUNPOWDER), "only the gunpowder recipe should be reported");
        CompoundTag saved = coagulator.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
        CoagulatorBlockEntity fresh = coagulator(helper, null);
        helper.assertTrue(fresh.selected().isEmpty(), "a new Coagulator has nothing selected");
        fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(fresh.selected().filter(id -> id.equals(coagulation("gunpowder"))).isPresent(), "the selection should be loaded");
        helper.succeed();
    }

    /** A Calciner's work counts for its team's journal: the item and every principle it made. */
    @ServusGameTest(timeoutTicks = 200)
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
    @ServusGameTest(timeoutTicks = 20)
    public static void finishingVillachAwardsItsAdvancement(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        coagulator.setTeam(team);
        CoagulationRecipe locked = new CoagulationRecipe(new Principles(10, 0, 0), Optional.empty(), new ItemStackTemplate(Items.FLINT),
                40, Optional.of(Identifier.parse(VILLACH)));
        helper.assertTrue(!coagulator.unlocked(locked), "a recipe of an unfinished chapter is locked");
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordEvent(level, team, "purify", 100);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()), 1);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, VILLACH), "Villach should be finished");
        helper.assertTrue(coagulator.unlocked(locked), "the recipe opens with its chapter");
        // Awarding is not tested: Modonomicon syncs book data on every award, which a mock connection cannot carry.
        AdvancementHolder advancement = level.getServer().getAdvancements().get(Journal.advancementId(VILLACH));
        helper.assertTrue(advancement != null, "the Villach advancement should exist");
        helper.succeed();
    }

    /** The journal recipe makes Manufacturing's own journal item, which opens Paracelsus' Journal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalRecipeMakesTheJournal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack result = ServusRecipes.byId(level, Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal"))
                .map(holder -> ServusRecipes.displayResult(level, holder.value()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(!result.isEmpty(), "the journal recipe should exist");
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, JournalBookItem.create()), "the result should be the journal, got " + result);
        helper.assertTrue(result.is(ServusManufacturingItems.PARACELSUS_JOURNAL.get()), "the result should be the Servus journal item, got " + result);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(ServusManufacturingIds.MOD_ID),
                "the journal should belong to Servus: Manufacturing");
        helper.assertTrue(((ParacelsusJournalItem) result.getItem()).bookId.equals(JournalBookItem.BOOK), "the item should open the journal book");
        helper.succeed();
    }

    /** An Athanor on its hearth at {@link #MACHINE}, formed with the Journal. */
    static AthanorBlockEntity athanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = unformedAthanor(helper);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, true);
        return athanor;
    }

    /** An Athanor facing north on a complete hearth with its three tanks that nobody formed. */
    static AthanorBlockEntity unformedAthanor(GameTestHelper helper) {
        for (AthanorTiers.BlockAt brick : AthanorLayout.hearth()) {
            helper.setBlock(MACHINE.offset(brick.dx(), brick.dy(), brick.dz()), Blocks.BRICKS);
        }
        helper.setBlock(MACHINE, ServusManufacturingBlocks.ATHANOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        for (AthanorLayout.Tank tank : AthanorLayout.tanks(Facing.NORTH)) {
            helper.setBlock(athanorTank(tank.side()), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        }
        for (AthanorLayout.Duct duct : AthanorLayout.ducts(Facing.NORTH)) {
            helper.setBlock(athanorDuct(duct.side(), duct.overTank()), ServusManufacturingBlocks.COPPER_DUCT.get().defaultBlockState());
        }
        return helper.getBlockEntity(MACHINE, AthanorBlockEntity.class);
    }

    /** Where the Athanor's duct toward the tank on {@code side} stands in the test: beside the Athanor or over the tank. */
    static BlockPos athanorDuct(AthanorLayout.TankSide side, boolean overTank) {
        AthanorLayout.Duct duct = AthanorLayout.ducts(Facing.NORTH).stream()
                .filter(each -> each.side() == side && each.overTank() == overTank).findFirst().orElseThrow();
        return MACHINE.offset(duct.dx(), 1, duct.dz());
    }

    /** Where the Athanor's tank on {@code side} stands in the test. */
    static BlockPos athanorTank(AthanorLayout.TankSide side) {
        AthanorLayout.Tank tank = AthanorLayout.tanks(Facing.NORTH).stream().filter(each -> each.side() == side).findFirst().orElseThrow();
        return MACHINE.offset(tank.dx(), 0, tank.dz());
    }

    static FluidView athanorTankHandler(GameTestHelper helper, AthanorLayout.TankSide side) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(athanorTank(side)), Direction.UP));
    }

    /** Pipes the principles into the tanks: Salt left, Sulfur behind, Mercury right. Call once the tanks had two ticks to join. */
    static void pour(GameTestHelper helper, Principles principles) {
        AthanorLayout.TankSide[] sides = {AthanorLayout.TankSide.LEFT, AthanorLayout.TankSide.BACK, AthanorLayout.TankSide.RIGHT};
        for (Principle principle : Principle.values()) {
            int amount = principles.get(principle);
            if (amount > 0) {
                int filled = athanorTankHandler(helper, sides[principle.ordinal()])
                        .fill(new FluidStack(PrincipleFluids.fluid(principle), amount), false);
                helper.assertTrue(filled == amount, "the tank should take " + amount + " mB " + principle + ", took " + filled);
            }
        }
    }

    /** Without its hearth the Journal forms nothing; on a complete one it does, and a missing corner brick unforms it at the next check. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorFormsOnItsHearth(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(MACHINE.offset(1, -1, 1), Blocks.STONE);
        Player player = CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get()));
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.setBlock(MACHINE.offset(1, -1, 1), Blocks.BRICKS);
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, true);
        AthanorBlockEntity athanor = helper.getBlockEntity(MACHINE, AthanorBlockEntity.class);
        helper.assertTrue(athanor.owner().filter(player.getUUID()::equals).isPresent(), "the forming player owns the Athanor");
        helper.setBlock(MACHINE.offset(-1, -1, -1), Blocks.AIR);
        athanor.checkTier();
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.succeed();
    }

    /** The journal's Athanor schematics accept the hearth and the Albedo ring, anchored on the hearth centre. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorSchematicsMatchTheBuilds(GameTestHelper helper) {
        Multiblock nigredo = ModonomiconAPI.get()
                .getMultiblock(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor_nigredo"));
        Multiblock albedo = ModonomiconAPI.get()
                .getMultiblock(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor_albedo"));
        helper.assertTrue(nigredo != null && albedo != null, "both Athanor schematics should be loaded");
        unformedAthanor(helper);
        BlockPos anchor = helper.absolutePos(MACHINE.below());
        helper.assertTrue(nigredo.validate(helper.getLevel(), anchor) != null, "the hearth schematic accepts the build");
        helper.assertTrue(albedo.validate(helper.getLevel(), anchor) == null, "the Albedo schematic wants the ring");
        saltRing(helper);
        helper.assertTrue(albedo.validate(helper.getLevel(), anchor) != null, "the Albedo schematic accepts the ring");
        helper.succeed();
    }

    /** Breaking the brick right under a formed Athanor unforms it at once. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorUnformsWithoutTheBrickBelow(GameTestHelper helper) {
        athanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(MACHINE.below()), true);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.succeed();
    }

    /** An Athanor nobody formed crafts nothing. */
    @ServusGameTest(timeoutTicks = 140)
    public static void unformedAthanorCraftsNothing(GameTestHelper helper) {
        AthanorBlockEntity athanor = unformedAthanor(helper);
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        helper.runAfterDelay(2, () -> pour(helper, new Principles(0, 500, 0)));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Blaze Powder unformed");
            helper.succeed();
        });
    }

    /** The Albedo ring: twelve Salt Crystal Blocks filling the hearth layer out to a 5 by 5 square. */
    static void saltRing(GameTestHelper helper) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 2) {
                    helper.setBlock(MACHINE.offset(dx, -1, dz), ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get().defaultBlockState());
                }
            }
        }
    }

    /** A Salt Crystal and Salt grow into a Salt Crystal Block on the Athanor; no crafting table recipe makes or splits one. */
    @ServusGameTest(timeoutTicks = 200)
    public static void athanorGrowsASaltCrystalBlock(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "salt_crystal_block"))).isEmpty(), "no crafting table block");
        helper.assertTrue(helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "salt_crystal_from_block"))).isEmpty(), "no split");
        AthanorBlockEntity athanor = athanor(helper);
        helper.runAfterDelay(2, () -> {
            pour(helper, new Principles(1000, 0, 0));
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()));
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get()),
                        "a Salt Crystal Block should grow");
                helper.assertTrue(athanor.principles().get(Principle.SALT) == 0, "the Salt is burned");
            });
        });
    }

    /** Gunpowder and Sulfur become Blaze Powder on a bare Athanor. */
    @ServusGameTest(timeoutTicks = 200)
    public static void athanorMakesBlazePowderAtNigredo(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        helper.runAfterDelay(2, () -> {
            pour(helper, new Principles(0, 200, 0));
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.BLAZE_POWDER), "Blaze Powder should be made");
                helper.assertTrue(athanor.principles().get(Principle.SULFUR) == 0, "the Sulfur is burned");
            });
        });
    }

    /** The Salt Crystal ring makes the Athanor Albedo and allows the Albedo craft; a gap brings it back to Nigredo. */
    @ServusGameTest(timeoutTicks = 300)
    public static void athanorTierFollowsTheRing(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
        helper.runAfterDelay(2, () -> pour(helper, new Principles(50, 0, 100)));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Albedo craft at Nigredo");
            saltRing(helper);
            athanor.checkTier();
            helper.assertTrue(athanor.tier() == 1 && helper.getBlockState(MACHINE).getValue(AthanorBlock.TIER) == 1, "the ring makes Albedo");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.EXPERIENCE_BOTTLE),
                        "the Albedo craft should run");
                helper.setBlock(MACHINE.offset(2, -1, 1), Blocks.AIR.defaultBlockState());
                athanor.checkTier();
                helper.assertTrue(athanor.tier() == 0, "a gap in the ring is Nigredo again");
                helper.succeed();
            });
        });
    }

    /** A Sulfur bucket emptied into a tank of the build is there for the Athanor. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorTakesABucket(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        helper.runAfterDelay(2, () -> {
            ItemStack result = Transfer.emptyContainer(new ItemStack(PrincipleFluids.entry(Principle.SULFUR).bucket().get()),
                    athanorTankHandler(helper, AthanorLayout.TankSide.LEFT).handler());
            helper.assertTrue(result.is(Items.BUCKET), "the bucket should be emptied");
            helper.assertTrue(athanor.principles().get(Principle.SULFUR) == 1000, "the Athanor should see 1000 mB Sulfur");
            helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(MACHINE), Direction.NORTH)) == null,
                    "the Athanor itself takes no fluid");
            helper.succeed();
        });
    }

    /** No GUI: an item is laid on by hand, an empty hand takes the result, sneaking takes the item back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorByHand(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.GLASS_BOTTLE, 3));
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 3, "the bottles lie on top");
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_OUTPUT, new ItemStack(Items.EXPERIENCE_BOTTLE, 2));
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertTrue(player.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 2, "an empty hand takes the result");
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 3, "the bottles stay");
        player.setShiftKeyDown(true);
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 3, "sneaking takes the bottles back");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
        helper.succeed();
    }

    /** Any tank may hold any principle: Sulfur in the tank behind burns the same and leaves that tank lower. */
    @ServusGameTest(timeoutTicks = 200)
    public static void athanorBurnsFromAnyTank(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        helper.runAfterDelay(2, () -> {
            athanorTankHandler(helper, AthanorLayout.TankSide.RIGHT)
                    .fill(new FluidStack(PrincipleFluids.fluid(Principle.SULFUR), 300), false);
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.BLAZE_POWDER), "Blaze Powder should be made");
                helper.assertTrue(athanorTankHandler(helper, AthanorLayout.TankSide.RIGHT).getFluidInTank(0).getAmount() == 100,
                        "100 mB Sulfur stay in the right tank");
            });
        });
    }

    /** Without its left tank the Journal forms nothing; a formed Athanor that loses a tank unforms at the next check. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNeedsItsTanks(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.LEFT), Blocks.AIR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get()));
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.LEFT), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, true);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.BACK), Blocks.AIR);
        AthanorBlockEntity athanor = helper.getBlockEntity(MACHINE, AthanorBlockEntity.class);
        athanor.checkTier();
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.succeed();
    }

    /** Forming turns the six ducts into the Athanor's pipes; breaking one unforms it at once and gives the others their straight look back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorFormsItsDucts(GameTestHelper helper) {
        athanor(helper);
        for (AthanorLayout.Duct duct : AthanorLayout.ducts(Facing.NORTH)) {
            BlockState state = helper.getBlockState(athanorDuct(duct.side(), duct.overTank()));
            helper.assertTrue(CopperDuctBlock.athanorPiece(state), "every duct is formed into the Athanor");
            helper.assertTrue(state.getValue(CopperDuctBlock.PIECE) == (duct.overTank() ? CopperDuctBlock.ATHANOR_OVER : CopperDuctBlock.ATHANOR_NEAR),
                    "the duct over the tank turns down, the one beside runs straight");
        }
        helper.getLevel().destroyBlock(helper.absolutePos(athanorDuct(AthanorLayout.TankSide.BACK, true)), true);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.assertBlockProperty(athanorDuct(AthanorLayout.TankSide.LEFT, false), CopperDuctBlock.FORMED, false);
        helper.succeed();
    }

    /** Without the duct over the tank behind it the Journal forms nothing; a tank stacked there takes the duct's place. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNeedsItsDucts(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(athanorDuct(AthanorLayout.TankSide.BACK, true), Blocks.AIR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get()));
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.setBlock(athanorDuct(AthanorLayout.TankSide.BACK, true), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        CalcinerGameTests.use(helper, player, MACHINE);
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, true);
        helper.succeed();
    }

    /** A formed Athanor stands as its tower, up to its plate; a duct's pipe is low enough to jump onto. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCanBeStoodOn(GameTestHelper helper) {
        athanor(helper);
        double top = helper.getBlockState(MACHINE).getCollisionShape(helper.getLevel(), helper.absolutePos(MACHINE)).max(Direction.Axis.Y);
        helper.assertTrue(Math.abs(top - 23 / 16.0) < 1e-6, "the plate is the top of the tower, not " + top);
        BlockPos duct = athanorDuct(AthanorLayout.TankSide.LEFT, false);
        double pipe = helper.getBlockState(duct).getCollisionShape(helper.getLevel(), helper.absolutePos(duct)).max(Direction.Axis.Y);
        helper.assertTrue(1 + pipe <= 1.25, "the pipe can be jumped onto, its top is " + (1 + pipe));
        helper.succeed();
    }

    /** A formed Athanor gets its crown above; using the crown lays the item on the Athanor; unforming takes the crown away. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownTakesTheLook(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        helper.assertBlockPresent(ServusManufacturingBlocks.ATHANOR_CROWN.get(), MACHINE.above());
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.GUNPOWDER));
        CalcinerGameTests.use(helper, player, MACHINE.above());
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).is(Items.GUNPOWDER), "the crown hands the item to the Athanor");
        helper.setBlock(athanorTank(AthanorLayout.TankSide.RIGHT), Blocks.AIR);
        athanor.checkTier();
        helper.assertBlockProperty(MACHINE, AthanorBlock.FORMED, false);
        helper.assertBlockNotPresent(ServusManufacturingBlocks.ATHANOR_CROWN.get(), MACHINE.above());
        helper.succeed();
    }

    /** A real right click on the crown, through the server's packet handling, lays the item on and takes the result. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownByPlayerClick(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        ServerPlayer player = PlayerClicks.player(helper, MACHINE, new ItemStack(Items.GUNPOWDER, 2));
        PlayerClicks.use(helper, player, MACHINE.above());
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 2,
                "a click on the crown lays the item on the Athanor");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the item left the hand");
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_OUTPUT, new ItemStack(Items.BLAZE_POWDER));
        PlayerClicks.use(helper, player, MACHINE.above());
        helper.assertTrue(player.getInventory().countItem(Items.BLAZE_POWDER) == 1, "an empty hand on the crown takes the result");
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "the result is gone");
        helper.succeed();
    }

    /** A look at the crown picks it as the client does; the click lays the item on and takes the result, in both game modes. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownByLook(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        BlockPos crown = helper.absolutePos(MACHINE.above());
        for (GameType gameType : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, ItemStack.EMPTY);
            ServerPlayer player = PlayerClicks.player(helper, MACHINE, new ItemStack(Items.GUNPOWDER, 2), gameType);
            BlockHitResult hit = PlayerClicks.lookAndUse(helper, player, Vec3.atCenterOf(crown));
            helper.assertTrue(hit.getBlockPos().equals(crown), gameType + ": the look picks the crown, not " + hit.getBlockPos() + " " + hit.getType());
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).is(Items.GUNPOWDER),
                    gameType + ": a click on the crown lays the item on the Athanor");
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_OUTPUT, new ItemStack(Items.BLAZE_POWDER, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            PlayerClicks.lookAndUse(helper, player, Vec3.atCenterOf(crown));
            helper.assertTrue(player.getInventory().countItem(Items.BLAZE_POWDER) == 2, gameType + ": an empty hand on the crown takes the result");
        }
        helper.succeed();
    }

    /** The Villach chapter advancement unlocks the journal's Villach research node, which opens Schwaz and the Albedo entries. */
    @ServusGameTest(timeoutTicks = 20)
    public static void villachAdvancementUnlocksTheResearchNode(GameTestHelper helper) {
        ServerPlayer player = PlayerClicks.player(helper, MACHINE, ItemStack.EMPTY);
        Identifier node = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal/villach");
        var research = ResearchServices.state();
        helper.assertFalse(research.isNodeUnlocked(player, node), "the Villach node starts locked");
        AdvancementHolder villach = helper.getLevel().getServer().getAdvancements().get(Journal.advancementId(ServusManufacturingIds.MOD_ID + ":villach"));
        helper.assertTrue(villach != null, "the Villach chapter advancement exists");
        villach.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(villach, criterion));
        helper.assertTrue(research.isNodeUnlocked(player, node), "the Villach advancement unlocks the Villach node");
        helper.succeed();
    }

    /** Breaking the crown breaks the Athanor below. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheCrownBreaksTheAthanor(GameTestHelper helper) {
        athanor(helper);
        Player player = CalcinerGameTests.player(helper, ItemStack.EMPTY);
        BlockPos crown = helper.absolutePos(MACHINE.above());
        helper.getLevel().getBlockState(crown).getBlock().playerWillDestroy(helper.getLevel(), crown, helper.getLevel().getBlockState(crown), player);
        helper.assertBlockNotPresent(ServusManufacturingBlocks.ATHANOR.get(), MACHINE);
        helper.assertBlockNotPresent(ServusManufacturingBlocks.ATHANOR_CROWN.get(), MACHINE.above());
        helper.succeed();
    }

    /** A craft on an Albedo Athanor counts for Schwaz once the team finished Villach. */
    @ServusGameTest(timeoutTicks = 300)
    public static void albedoCraftCountsForSchwaz(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordEvent(level, team, "purify", 100);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()), 1);
        AthanorBlockEntity athanor = athanor(helper);
        athanor.setTeam(team);
        saltRing(helper);
        helper.runAfterDelay(2, () -> pour(helper, new Principles(50, 0, 100)));
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
    @ServusGameTest(timeoutTicks = 20)
    public static void elixirGivesElementalSight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack left = new ItemStack(ElementalSight.ELIXIR.get()).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(ElementalSight.active(player), "the drinker should have Elemental Sight");
        helper.assertTrue(player.getEffect(ElementalSight.EFFECT).getDuration() == ElementalSight.DURATION, "for three minutes");
        helper.assertTrue(left.is(Items.GLASS_BOTTLE), "a glass bottle should be left, got " + left);
        helper.succeed();
    }

    /** The elixir is an Albedo craft: nothing at Nigredo, the elixir once the ring stands. */
    @ServusGameTest(timeoutTicks = 400)
    public static void elixirNeedsAnAlbedoAthanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusManufacturingItems.QUICKSILVER.get()));
        helper.runAfterDelay(2, () -> pour(helper, new Principles(100, 100, 250)));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no elixir at Nigredo");
            saltRing(helper);
            athanor.checkTier();
            helper.succeedWhen(() -> helper.assertTrue(
                    athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ElementalSight.ELIXIR.get()), "the elixir should be made"));
        });
    }

    /** Production lists only the crafts the tier allows. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorProductionFollowsTheTier(GameTestHelper helper) {
        athanor(helper);
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(production != null, "the Athanor should be a producer");
        helper.assertTrue(production.recipes().stream().anyMatch(recipe -> recipe.outputs().get(0).is(Items.BLAZE_POWDER)), "Blaze Powder at Nigredo");
        helper.assertTrue(production.recipes().stream().noneMatch(recipe -> recipe.outputs().get(0).is(Items.EXPERIENCE_BOTTLE)),
                "no Albedo craft at Nigredo");
        helper.succeed();
    }

    /** A Salt bucket used on the Salt Vat pours into the Salt tank and leaves an empty bucket. */
    @ServusGameTest(timeoutTicks = 100)
    public static void coagulatorTakesAPrincipleBucket(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bucket = new ItemStack(PrincipleFluids.entry(Principle.SALT).bucket().get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(CoagulatorGameTests.vat(MACHINE, 1));
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.getLevel().getBlockState(absolute).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(1000, 0, 0)), "the Salt tank should hold 1,000 mB, got "
                + coagulator.tanks().contents());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "an empty bucket should be left");
        helper.succeed();
    }

    /** The loaded journal has the Workshop category and an entry per machine and topic. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalHasTheWorkshop(GameTestHelper helper) {
        Book book = BookDataManager.get().getBook(JournalBookItem.BOOK);
        helper.assertTrue(book != null, "the journal book should be loaded");
        helper.assertTrue(book.getCategory(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "workshop")) != null,
                "the Workshop category should exist");
        for (String entry : List.of("principles", "calciner", "purifier", "coagulator", "athanor", "pipes", "salt_crystal_block",
                "elixir_of_sight", "gnome_hollow")) {
            // The server keeps only categories until the book is built for clients, so look at the data files.
            Identifier file = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID,
                    "modonomicon/books/journal/entries/workshop/" + entry + ".json");
            helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(file).isPresent(),
                    "the Workshop should have the entry " + entry);
        }
        helper.succeed();
    }
}
