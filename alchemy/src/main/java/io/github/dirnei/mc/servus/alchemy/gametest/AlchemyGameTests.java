package io.github.dirnei.mc.servus.alchemy.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.research.ResearchServices;
import com.mojang.authlib.GameProfile;
import io.github.dirnei.mc.servus.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTierData;
import io.github.dirnei.mc.servus.alchemy.athanor.PlacedBlocks;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitualData;
import io.github.dirnei.mc.servus.alchemy.datagen.ServusAlchemyMultiblockProvider;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.journal.JournalBookItem;
import io.github.dirnei.mc.servus.alchemy.journal.JournalChapters;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger;
import io.github.dirnei.mc.servus.alchemy.journal.JournalSavedData;
import io.github.dirnei.mc.servus.alchemy.journal.JournalSync;
import io.github.dirnei.mc.servus.alchemy.journal.ParacelsusJournalItem;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.alchemy.sight.ElixirDose;
import io.github.dirnei.mc.servus.alchemy.sight.ElixirDoseData;
import io.github.dirnei.mc.servus.alchemy.sight.ElixirOfSightItem;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** GameTests of the principles as fluids, the Calciner and Coagulator at work, the journal and the Athanor. */
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

    /** One cobblestone becomes 100 mB Salt in the Ash Pan and 10 mB Mercury in the Mercury tank. */
    @ServusGameTest(timeoutTicks = 200)
    public static void calcinerBurnsCobblestoneIntoPrinciples(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            BlockPos mercuryTank = CalcinerGameTests.mercuryTank(CalcinerGameTests.CALCINER);
            BlockPos sulfurTank = CalcinerGameTests.sulfurTank(CalcinerGameTests.CALCINER);
            int mercury = CalcinerGameTests.tankAt(helper, mercuryTank).getFluidInTank(0).getAmount();
            int sulfur = CalcinerGameTests.tankAt(helper, sulfurTank).getFluidInTank(0).getAmount();
            helper.assertTrue(calciner.salt() == 100 && sulfur == 0 && mercury == 10,
                    "expected 100 Salt and 10 Mercury, got " + calciner.salt() + " Salt, " + sulfur + " Sulfur and " + mercury + " Mercury");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the cobblestone is used up");
        });
    }

    /** With no room in the Ash Pan the Calciner waits and lights no fuel. */
    @ServusGameTest(timeoutTicks = 200)
    public static void calcinerWaitsWhenATankIsFull(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.putSalt(6350);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 4, "a blocked Calciner burns no fuel");
            helper.assertTrue(!calciner.isBurning(), "a blocked Calciner is not lit");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).is(Items.COBBLESTONE), "the cobblestone stays");
            helper.assertTrue(calciner.salt() == 6350, "no Salt was added");
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
            helper.assertTrue(calciner.salt() == 0, "nothing is calcined without fuel");
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
        return Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulation/" + name);
    }

    /** A formed Coagulator build around {@link #MACHINE}, seeded with the result of {@code recipe} when not null. */
    static CoagulatorBlockEntity coagulator(GameTestHelper helper, String recipe) {
        CoagulatorBlockEntity coagulator = CoagulatorGameTests.formed(helper, MACHINE);
        if (recipe != null) {
            CoagulatorGameTests.seedWith(helper, coagulator, recipe);
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
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).is(ServusAlchemyItems.SALT_CRYSTAL.get()),
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

    /** The seed is saved with the block entity and its recipe reported as the only production recipe. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorKeepsItsSeedAndReportsItsRecipe(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = coagulator(helper, "gunpowder");
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(production != null && production.recipes().size() == 1
                && production.recipes().get(0).outputs().get(0).is(Items.GUNPOWDER), "only the gunpowder recipe should be reported");
        CompoundTag saved = coagulator.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
        CoagulatorBlockEntity fresh = coagulator(helper, null);
        helper.assertTrue(fresh.seed().isEmpty(), "a new Coagulator has no seed");
        fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(fresh.seed().is(Items.GUNPOWDER), "the seed should be loaded");
        helper.assertTrue(fresh.seedRecipe().filter(holder -> holder.id().identifier().equals(coagulation("gunpowder"))).isPresent(),
                "the seed grows gunpowder again");
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

    /** A calcine reveals the recipe's row of the journal table for the Calciner's team, and only that row. */
    @ServusGameTest(timeoutTicks = 200)
    public static void calcineRevealsTheRowForTheTeam(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        UUID team = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        calciner.setTeam(team);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.NETHER_WART));
        String netherWart = ServusAlchemyIds.MOD_ID + ":calcination/nether_wart";
        helper.succeedWhen(() -> {
            MinecraftServer server = helper.getLevel().getServer();
            JournalLedger.TeamJournal journal = Journal.journalOf(server, team);
            helper.assertTrue(journal.tried().equals(Set.of(netherWart)), "only the Nether Wart recipe should be tried, got " + journal.tried());
            helper.assertTrue(JournalSync.payload(journal).tried().contains(netherWart), "the members should get the tried recipe");
            helper.assertTrue(Journal.journalOf(server, other).tried().isEmpty(), "another team should have tried nothing");
        });
    }

    /** Tried calcination recipes are saved with the team's journal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void triedRecipesSurviveASave(GameTestHelper helper) {
        JournalSavedData data = new JournalSavedData();
        UUID team = UUID.randomUUID();
        String netherrack = ServusAlchemyIds.MOD_ID + ":calcination/netherrack";
        data.forTeam(team).markTried(netherrack);
        CompoundTag saved = data.save(new CompoundTag(), helper.getLevel().registryAccess());
        JournalSavedData loaded = JournalSavedData.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.forTeam(team).tried().equals(Set.of(netherrack)),
                "the tried recipe should survive a save, got " + loaded.forTeam(team).tried());
        helper.succeed();
    }

    static final String VILLACH = "servus_alchemy:villach";

    /** Finishing Villach through team events opens chapter locked recipes; its advancement exists for the book. */
    @ServusGameTest(timeoutTicks = 20)
    public static void finishingVillachAwardsItsAdvancement(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        CoagulatorBlockEntity coagulator = coagulator(helper, null);
        coagulator.setTeam(team);
        CoagulationRecipe locked = new CoagulationRecipe(new Principles(10, 0, 0), Optional.empty(), new ItemStackTemplate(Items.FLINT),
                40, Optional.empty(), Optional.of(Identifier.parse(VILLACH)));
        helper.assertTrue(!coagulator.unlocked(locked), "a recipe of an unfinished chapter is locked");
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordEvent(level, team, "purify", 100);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get()), 1);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, VILLACH), "Villach should be finished");
        helper.assertTrue(coagulator.unlocked(locked), "the recipe opens with its chapter");
        // Awarding is not tested: Modonomicon syncs book data on every award, which a mock connection cannot carry.
        AdvancementHolder advancement = level.getServer().getAdvancements().get(Journal.advancementId(VILLACH));
        helper.assertTrue(advancement != null, "the Villach advancement should exist");
        helper.succeed();
    }

    /** Finishing Wandering years by command finishes the shipped chapters above it and opens Basel and Salzburg. */
    @ServusGameTest(timeoutTicks = 20)
    public static void finishingAChapterFinishesItsParents(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        MinecraftServer server = helper.getLevel().getServer();
        List<String> finished = Journal.finish(server, team, "servus_alchemy:wandering_years");
        helper.assertTrue(finished.equals(List.of(VILLACH, "servus_alchemy:schwaz", "servus_alchemy:wandering_years")),
                "Villach, Schwaz and Wandering years should finish in order, got " + finished);
        for (String chapter : finished) {
            helper.assertTrue(server.getAdvancements().get(Journal.advancementId(chapter)) != null, "missing advancement of " + chapter);
        }
        helper.assertTrue(!Journal.isFinished(server, team, "servus_alchemy:basel_salzburg"), "Basel and Salzburg stays open");
        helper.assertTrue(Journal.finish(server, team, VILLACH).isEmpty(), "a finished chapter finishes only once");
        helper.succeed();
    }

    /** Every task of every shipped chapter has a name for the journal's task page. */
    @ServusGameTest(timeoutTicks = 20)
    public static void everyJournalTaskHasAName(GameTestHelper helper) {
        for (JournalLedger.ChapterDef chapter : JournalChapters.INSTANCE.chapters()) {
            Identifier id = Identifier.parse(chapter.id());
            for (JournalLedger.TaskDef task : chapter.tasks()) {
                String key = "journal." + id.getNamespace() + "." + id.getPath() + ".task." + task.id();
                helper.assertTrue(Language.getInstance().has(key), "missing task name " + key);
            }
        }
        helper.succeed();
    }

    /** Journal work queues the team's progress for its members, and the tick drain sends and clears it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalWorkQueuesTheTeamsProgress(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 1);
        helper.assertTrue(JournalSync.isQueued(team), "calcining should queue the team");
        JournalSync.drain(level.getServer());
        helper.assertTrue(!JournalSync.isQueued(team), "the drain should clear the queue");
        helper.succeed();
    }

    /** The journal recipe makes Alchemy's own journal item, which opens Paracelsus' Journal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalRecipeMakesTheJournal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack result = ServusRecipes.byId(level, Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal"))
                .map(holder -> ServusRecipes.displayResult(level, holder.value()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(!result.isEmpty(), "the journal recipe should exist");
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, JournalBookItem.create()), "the result should be the journal, got " + result);
        helper.assertTrue(result.is(ServusAlchemyItems.PARACELSUS_JOURNAL.get()), "the result should be the Servus journal item, got " + result);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(ServusAlchemyIds.MOD_ID),
                "the journal should belong to Servus: Alchemy");
        helper.assertTrue(((ParacelsusJournalItem) result.getItem()).bookId.equals(JournalBookItem.BOOK), "the item should open the journal book");
        helper.succeed();
    }

    /** Where the Athanor stands in its tests: room for the salt ring below it and the vats beside its tanks. */
    static final BlockPos ATHANOR = new BlockPos(5, 2, 5);

    /** An Athanor facing north on its hearth at {@link #ATHANOR}, formed with the Journal. */
    static AthanorBlockEntity athanor(GameTestHelper helper) {
        return athanor(helper, Facing.NORTH);
    }

    /** An Athanor facing {@code facing} on its hearth at {@link #ATHANOR}, formed with the Journal. */
    static AthanorBlockEntity athanor(GameTestHelper helper, Facing facing) {
        AthanorBlockEntity athanor = unformedAthanor(helper, facing);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        return athanor;
    }

    /** An Athanor facing north on a complete hearth with its step, three tanks and three ducts that nobody formed. */
    static AthanorBlockEntity unformedAthanor(GameTestHelper helper) {
        return unformedAthanor(helper, Facing.NORTH);
    }

    static AthanorBlockEntity unformedAthanor(GameTestHelper helper, Facing facing) {
        AthanorLayout.hearth(facing).forEach(block -> place(helper, block, facing));
        place(helper, AthanorLayout.step(facing), facing);
        helper.setBlock(ATHANOR, ServusAlchemyBlocks.ATHANOR.get().defaultBlockState().setValue(MachineBlock.FACING, direction(facing)));
        AthanorLayout.tankBlocks(facing).forEach(block -> place(helper, block, facing));
        for (AthanorLayout.Duct duct : AthanorLayout.ducts(facing)) {
            helper.setBlock(ATHANOR.offset(duct.dx(), 0, duct.dz()), ServusAlchemyBlocks.COPPER_DUCT.get().defaultBlockState());
        }
        return helper.getBlockEntity(ATHANOR, AthanorBlockEntity.class);
    }

    private static Direction direction(Facing facing) {
        return switch (facing) {
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /** Places {@code block} at its offset from the Athanor with its properties; a stair rises toward an Athanor facing {@code facing}. */
    static void place(GameTestHelper helper, AthanorTiers.BlockAt block, Facing facing) {
        BlockState state = PlacedBlocks.state(block, Blocks.AIR.defaultBlockState()).orElseThrow();
        if (state.getBlock() instanceof StairBlock) {
            state = state.setValue(StairBlock.FACING, direction(facing).getOpposite());
        }
        helper.setBlock(ATHANOR.offset(block.dx(), block.dy(), block.dz()), state);
    }

    /** Where the Athanor's duct toward the tank on {@code side} stands in the test. */
    static BlockPos athanorDuct(AthanorLayout.TankSide side) {
        AthanorLayout.Duct duct = AthanorLayout.ducts(Facing.NORTH).stream().filter(each -> each.side() == side).findFirst().orElseThrow();
        return ATHANOR.offset(duct.dx(), 0, duct.dz());
    }

    /** Where the Athanor's tank on {@code side} stands on the Athanor's level in the test. */
    static BlockPos athanorTank(AthanorLayout.TankSide side) {
        AthanorLayout.Tank tank = AthanorLayout.tanks(Facing.NORTH).stream().filter(each -> each.side() == side).findFirst().orElseThrow();
        return ATHANOR.offset(tank.dx(), 0, tank.dz());
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

    /** A mock player holding the Journal that keeps the translation keys of the messages it gets, with the keys of their arguments. */
    static Player listeningPlayer(GameTestHelper helper, List<String> messages) {
        Player player = new Player(helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }

            @Override
            public void sendSystemMessage(Component message) {
                messages.add(keys(message));
            }
        };
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get()));
        return player;
    }

    private static String keys(Component message) {
        if (!(message.getContents() instanceof TranslatableContents translatable)) {
            return message.getString();
        }
        StringBuilder keys = new StringBuilder(translatable.getKey());
        for (Object arg : translatable.getArgs()) {
            if (arg instanceof Component component) {
                keys.append(' ').append(keys(component));
            }
        }
        return keys.toString();
    }

    /** Without a slab the Journal names the slabs and forms nothing; on a complete hearth it forms, and a missing front brick unforms it at the next check. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorFormsOnItsHearth(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(ATHANOR.offset(1, -1, 1), Blocks.STONE);
        List<String> messages = new ArrayList<>();
        Player player = listeningPlayer(helper, messages);
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.missing_slab")), "the slabs are named, got " + messages);
        helper.setBlock(ATHANOR.offset(1, -1, 1), Blocks.BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        AthanorBlockEntity athanor = helper.getBlockEntity(ATHANOR, AthanorBlockEntity.class);
        helper.assertTrue(athanor.owner().filter(player.getUUID()::equals).isPresent(), "the forming player owns the Athanor");
        helper.setBlock(ATHANOR.offset(0, -1, -1), Blocks.AIR);
        athanor.checkTier();
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        messages.clear();
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.missing")), "the bricks are named, got " + messages);
        helper.succeed();
    }

    /** Without the Brick Stair in front the Journal names the step and forms nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNamesTheMissingStep(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(ATHANOR.offset(0, -1, -2), Blocks.BRICK_SLAB);
        List<String> messages = new ArrayList<>();
        CalcinerGameTests.use(helper, listeningPlayer(helper, messages), ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.missing_step")), "the step is named, got " + messages);
        helper.succeed();
    }

    /** A left tank only two high is no tank: the Journal names the tank on the left and forms nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNamesAShortTank(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.LEFT).above(), Blocks.AIR);
        List<String> messages = new ArrayList<>();
        CalcinerGameTests.use(helper, listeningPlayer(helper, messages), ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.missing_tank message.servus_alchemy.athanor.tank.left")),
                "the left tank is named, got " + messages);
        helper.succeed();
    }

    /** The journal's Athanor schematics accept the builds: Nigredo anchored on the hearth centre, the stages on the open middle of the salt ring. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorSchematicsMatchTheBuilds(GameTestHelper helper) {
        Multiblock nigredo = ModonomiconAPI.get().getMultiblock(ServusAlchemyMultiblockProvider.athanor("nigredo"));
        Multiblock albedo = ModonomiconAPI.get().getMultiblock(ServusAlchemyMultiblockProvider.athanor("albedo"));
        Multiblock citrinitas = ModonomiconAPI.get().getMultiblock(ServusAlchemyMultiblockProvider.athanor("citrinitas"));
        Multiblock rubedo = ModonomiconAPI.get().getMultiblock(ServusAlchemyMultiblockProvider.athanor("rubedo"));
        helper.assertTrue(nigredo != null && albedo != null && citrinitas != null && rubedo != null, "all four Athanor schematics should be loaded");
        unformedAthanor(helper);
        BlockPos hearth = helper.absolutePos(ATHANOR.below());
        BlockPos ring = helper.absolutePos(ATHANOR.below(2));
        helper.assertTrue(nigredo.validate(helper.getLevel(), hearth) != null, "the hearth schematic accepts the build, misses " + misses(helper, nigredo, hearth));
        helper.assertTrue(albedo.validate(helper.getLevel(), ring) == null, "the Albedo schematic wants the ring");
        saltRing(helper);
        helper.assertTrue(albedo.validate(helper.getLevel(), ring) != null, "the Albedo schematic accepts the ring, misses " + misses(helper, albedo, ring));
        helper.assertTrue(citrinitas.validate(helper.getLevel(), ring) == null, "the Citrinitas schematic wants the vats");
        citrinitasVats(helper);
        helper.assertTrue(citrinitas.validate(helper.getLevel(), ring) != null, "the Citrinitas schematic accepts the vats, misses " + misses(helper, citrinitas, ring));
        helper.assertTrue(rubedo.validate(helper.getLevel(), ring) == null, "the Rubedo schematic wants the Obsidian");
        AthanorRitualData.readShipped("rubedo").turns(Facing.NORTH).stream().map(AthanorRitual.Turn::from)
                .filter(from -> from.block().equals("minecraft:obsidian")).forEach(from -> place(helper, from, Facing.NORTH));
        helper.assertTrue(rubedo.validate(helper.getLevel(), ring) != null, "the Rubedo schematic accepts the Obsidian, misses " + misses(helper, rubedo, ring));
        helper.succeed();
    }

    /** The cells of {@code multiblock} anchored at {@code anchor}, unturned, that the world does not match. */
    private static List<String> misses(GameTestHelper helper, Multiblock multiblock, BlockPos anchor) {
        return multiblock.simulate(helper.getLevel(), anchor, Rotation.NONE, false, false).getSecond().stream()
                .filter(result -> !result.test(helper.getLevel(), Rotation.NONE))
                .map(result -> result.character() + " at " + helper.relativePos(result.worldPosition()) + " is "
                        + helper.getLevel().getBlockState(result.worldPosition()))
                .toList();
    }

    /** Breaking the brick right under a formed Athanor unforms it at once. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorUnformsWithoutTheBrickBelow(GameTestHelper helper) {
        athanor(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(ATHANOR.below()), true);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
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

    /** Places the shipped blocks of the tier {@code name} around an Athanor facing {@code facing}. */
    static void stage(GameTestHelper helper, String name, Facing facing) {
        AthanorTierData.readShipped(name).blocks().forEach(block -> place(helper, AthanorTiers.turn(block, facing), facing));
    }

    /** The Albedo ring: sixteen Salt Crystal Blocks one layer under the hearth of a north facing Athanor. */
    static void saltRing(GameTestHelper helper) {
        stage(helper, "albedo", Facing.NORTH);
    }

    /** Citrinitas as the ritual leaves it: a Principle Vat with grown citrine beside each tank. */
    static void citrinitasVats(GameTestHelper helper) {
        stage(helper, "citrinitas", Facing.NORTH);
    }

    /** Rubedo as the ritual leaves it: orange vats and four obelisks beside the back tank and behind the side tanks. */
    static void rubedoPillars(GameTestHelper helper) {
        stage(helper, "rubedo", Facing.NORTH);
    }

    /** A Salt Crystal and Salt grow into a Salt Crystal Block on the Athanor; no crafting table recipe makes or splits one. */
    @ServusGameTest(timeoutTicks = 200)
    public static void athanorGrowsASaltCrystalBlock(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salt_crystal_block"))).isEmpty(), "no crafting table block");
        helper.assertTrue(helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salt_crystal_from_block"))).isEmpty(), "no split");
        AthanorBlockEntity athanor = athanor(helper);
        helper.runAfterDelay(2, () -> {
            pour(helper, new Principles(1000, 0, 0));
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get()));
            helper.succeedWhen(() -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ServusAlchemyItems.SALT_CRYSTAL_BLOCK.get()),
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
            helper.assertTrue(athanor.tier() == 1 && helper.getBlockState(ATHANOR).getValue(AthanorBlock.TIER) == 1, "the ring makes Albedo");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(Items.EXPERIENCE_BOTTLE),
                        "the Albedo craft should run");
                helper.setBlock(ATHANOR.offset(2, -2, 1), Blocks.AIR.defaultBlockState());
                athanor.checkTier();
                helper.assertTrue(athanor.tier() == 0, "a gap in the ring is Nigredo again");
                helper.succeed();
            });
        });
    }

    /** Grown vats beside the tanks make Citrinitas on the salt ring; without the ring they count for nothing, and a plain vat falls back to Albedo. */
    @ServusGameTest(timeoutTicks = 20)
    public static void citrinitasVatsMakeTierTwo(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        citrinitasVats(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 0, "vats without the salt ring are Nigredo, got " + athanor.tier());
        saltRing(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2 && helper.getBlockState(ATHANOR).getValue(AthanorBlock.TIER) == 2,
                "the vats make Citrinitas, got " + athanor.tier());
        helper.assertTrue(athanor.tierName().equals("citrinitas"), "the stage is Citrinitas, got " + athanor.tierName());
        helper.setBlock(ATHANOR.offset(0, -1, 3), ServusAlchemyBlocks.VAT.get().defaultBlockState());
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 1, "a plain vat is Albedo, got " + athanor.tier());
        helper.succeed();
    }

    /** Obelisks beside the back tank and behind the side tanks make Rubedo; a broken obelisk falls back to Citrinitas. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rubedoPillarsMakeTierThree(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        saltRing(helper);
        citrinitasVats(helper);
        rubedoPillars(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 3 && helper.getBlockState(ATHANOR).getValue(AthanorBlock.TIER) == 3,
                "the pillars make Rubedo, got " + athanor.tier());
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        helper.setBlock(ATHANOR.offset(-2, 0, 1), Blocks.AIR);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "a broken obelisk is Citrinitas, got " + athanor.tier());
        helper.succeed();
    }

    /** An Athanor facing east forms on its turned hearth and reaches Rubedo with the ring, vats and obelisks turned with it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void turnedAthanorReachesRubedo(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper, Facing.EAST);
        stage(helper, "albedo", Facing.EAST);
        stage(helper, "citrinitas", Facing.EAST);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "the turned vats make Citrinitas, got " + athanor.tier());
        stage(helper, "rubedo", Facing.EAST);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 3, "the turned obelisks make Rubedo, got " + athanor.tier());
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        helper.succeed();
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
            helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ATHANOR), Direction.NORTH)) == null,
                    "the Athanor itself takes no fluid");
            helper.succeed();
        });
    }

    /** No GUI: an item is laid on by hand, an empty hand takes the result, sneaking takes the item back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorByHand(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.GLASS_BOTTLE, 3));
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 3, "the bottles lie on top");
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_OUTPUT, new ItemStack(Items.EXPERIENCE_BOTTLE, 2));
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertTrue(player.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 2, "an empty hand takes the result");
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 3, "the bottles stay");
        player.setShiftKeyDown(true);
        CalcinerGameTests.use(helper, player, ATHANOR);
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

    /** Without its left tank the Journal forms nothing; a formed Athanor that loses the bottom of a tank unforms at the next check. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNeedsItsTanks(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.LEFT), Blocks.AIR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get()));
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.LEFT), ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        helper.setBlock(athanorTank(AthanorLayout.TankSide.BACK).below(), Blocks.AIR);
        AthanorBlockEntity athanor = helper.getBlockEntity(ATHANOR, AthanorBlockEntity.class);
        athanor.checkTier();
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.succeed();
    }

    /** Forming turns the three ducts into the Athanor's pipes; breaking one unforms it at once and gives the others their straight look back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorFormsItsDucts(GameTestHelper helper) {
        athanor(helper);
        for (AthanorLayout.Duct duct : AthanorLayout.ducts(Facing.NORTH)) {
            BlockState state = helper.getBlockState(athanorDuct(duct.side()));
            helper.assertTrue(CopperDuctBlock.athanorPiece(state) && state.getValue(CopperDuctBlock.PIECE) == CopperDuctBlock.ATHANOR,
                    "every duct is formed into the Athanor's pipe");
        }
        helper.getLevel().destroyBlock(helper.absolutePos(athanorDuct(AthanorLayout.TankSide.BACK)), true);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertBlockProperty(athanorDuct(AthanorLayout.TankSide.LEFT), CopperDuctBlock.FORMED, false);
        helper.succeed();
    }

    /** Without the duct to the tank behind it the Journal names it and forms nothing; with it the Athanor forms. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorNeedsItsDucts(GameTestHelper helper) {
        unformedAthanor(helper);
        helper.setBlock(athanorDuct(AthanorLayout.TankSide.BACK), Blocks.AIR);
        List<String> messages = new ArrayList<>();
        Player player = listeningPlayer(helper, messages);
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.athanor.missing_duct message.servus_alchemy.athanor.tank.back")),
                "the duct behind is named, got " + messages);
        helper.setBlock(athanorDuct(AthanorLayout.TankSide.BACK), ServusAlchemyBlocks.COPPER_DUCT.get().defaultBlockState());
        CalcinerGameTests.use(helper, player, ATHANOR);
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, true);
        helper.succeed();
    }

    /** A formed Athanor stands as its tower up to its plate; a pipe is within a jump of the hearth slab and the plate within a jump of the pipe. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCanBeStoodOn(GameTestHelper helper) {
        athanor(helper);
        double top = helper.getBlockState(ATHANOR).getCollisionShape(helper.getLevel(), helper.absolutePos(ATHANOR)).max(Direction.Axis.Y);
        helper.assertTrue(Math.abs(top - 23.5 / 16.0) < 1e-6, "the plate is the top of the tower, not " + top);
        BlockPos duct = athanorDuct(AthanorLayout.TankSide.LEFT);
        double pipe = helper.getBlockState(duct).getCollisionShape(helper.getLevel(), helper.absolutePos(duct)).max(Direction.Axis.Y);
        double slab = helper.getBlockState(duct.below()).getCollisionShape(helper.getLevel(), helper.absolutePos(duct.below())).max(Direction.Axis.Y) - 1;
        helper.assertTrue(pipe - slab <= 1.25, "the pipe can be jumped onto from the slab, " + (pipe - slab) + " up");
        helper.assertTrue(top - pipe <= 1.25, "the plate can be jumped onto from the pipe, " + (top - pipe) + " up");
        helper.succeed();
    }

    /** A formed Athanor gets its crown above; using the crown lays the item on the Athanor; unforming takes the crown away. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownTakesTheLook(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        helper.assertBlockPresent(ServusAlchemyBlocks.ATHANOR_CROWN.get(), ATHANOR.above());
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.GUNPOWDER));
        CalcinerGameTests.use(helper, player, ATHANOR.above());
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).is(Items.GUNPOWDER), "the crown hands the item to the Athanor");
        helper.setBlock(athanorTank(AthanorLayout.TankSide.RIGHT), Blocks.AIR);
        athanor.checkTier();
        helper.assertBlockProperty(ATHANOR, AthanorBlock.FORMED, false);
        helper.assertBlockNotPresent(ServusAlchemyBlocks.ATHANOR_CROWN.get(), ATHANOR.above());
        helper.succeed();
    }

    /** A real right click on the crown, through the server's packet handling, lays the item on and takes the result. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownByPlayerClick(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        ServerPlayer player = PlayerClicks.player(helper, ATHANOR, new ItemStack(Items.GUNPOWDER, 2));
        PlayerClicks.use(helper, player, ATHANOR.above());
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).getCount() == 2,
                "a click on the crown lays the item on the Athanor");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the item left the hand");
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_OUTPUT, new ItemStack(Items.BLAZE_POWDER));
        PlayerClicks.use(helper, player, ATHANOR.above());
        helper.assertTrue(player.getInventory().countItem(Items.BLAZE_POWDER) == 1, "an empty hand on the crown takes the result");
        helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "the result is gone");
        helper.succeed();
    }

    /** A look at the crown picks it as the client does; the click lays the item on and takes the result, in both game modes. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorCrownByLook(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        BlockPos crown = helper.absolutePos(ATHANOR.above());
        for (GameType gameType : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
            athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, ItemStack.EMPTY);
            ServerPlayer player = PlayerClicks.player(helper, ATHANOR, new ItemStack(Items.GUNPOWDER, 2), gameType);
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
        ServerPlayer player = PlayerClicks.player(helper, ATHANOR, ItemStack.EMPTY);
        Identifier node = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal/villach");
        var research = ResearchServices.state();
        helper.assertFalse(research.isNodeUnlocked(player, node), "the Villach node starts locked");
        AdvancementHolder villach = helper.getLevel().getServer().getAdvancements().get(Journal.advancementId(ServusAlchemyIds.MOD_ID + ":villach"));
        helper.assertTrue(villach != null, "the Villach chapter advancement exists");
        villach.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(villach, criterion));
        helper.assertTrue(research.isNodeUnlocked(player, node), "the Villach advancement unlocks the Villach node");
        helper.succeed();
    }

    /** The new chapters have advancements, and the Schwaz and Wandering years advancements unlock their research nodes. */
    @ServusGameTest(timeoutTicks = 20)
    public static void newChapterAdvancementsUnlockTheirNodes(GameTestHelper helper) {
        ServerPlayer player = PlayerClicks.player(helper, ATHANOR, ItemStack.EMPTY);
        var research = ResearchServices.state();
        for (String chapter : List.of("schwaz", "wandering_years")) {
            Identifier node = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "journal/" + chapter);
            helper.assertFalse(research.isNodeUnlocked(player, node), "the " + chapter + " node starts locked");
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(Journal.advancementId(ServusAlchemyIds.MOD_ID + ":" + chapter));
            helper.assertTrue(advancement != null, "the " + chapter + " advancement exists");
            advancement.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
            helper.assertTrue(research.isNodeUnlocked(player, node), "the " + chapter + " advancement unlocks its node");
        }
        helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Journal.advancementId(BASEL_SALZBURG)) != null,
                "the Basel and Salzburg advancement exists");
        helper.succeed();
    }

    /** Breaking the crown breaks the Athanor below. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheCrownBreaksTheAthanor(GameTestHelper helper) {
        athanor(helper);
        Player player = CalcinerGameTests.player(helper, ItemStack.EMPTY);
        BlockPos crown = helper.absolutePos(ATHANOR.above());
        helper.getLevel().getBlockState(crown).getBlock().playerWillDestroy(helper.getLevel(), crown, helper.getLevel().getBlockState(crown), player);
        helper.assertBlockNotPresent(ServusAlchemyBlocks.ATHANOR.get(), ATHANOR);
        helper.assertBlockNotPresent(ServusAlchemyBlocks.ATHANOR_CROWN.get(), ATHANOR.above());
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
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get()), 1);
        AthanorBlockEntity athanor = athanor(helper);
        athanor.setTeam(team);
        saltRing(helper);
        helper.runAfterDelay(2, () -> pour(helper, new Principles(50, 0, 100)));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GLASS_BOTTLE));
        helper.succeedWhen(() -> {
            JournalLedger.TeamJournal journal = Journal.journalOf(level.getServer(), team);
            helper.assertTrue(journal.count("servus_alchemy:schwaz", "albedo_craft") == 1, "the Albedo craft should count");
            Journal.recordEvent(level, team, "sight", 1);
            helper.assertTrue(Journal.journalOf(level.getServer(), team).count("servus_alchemy:schwaz", "sight") == 1,
                    "the sight should count for Schwaz");
        });
    }

    static final String WANDERING_YEARS = "servus_alchemy:wandering_years";
    static final String BASEL_SALZBURG = "servus_alchemy:basel_salzburg";

    /** A craft on a Citrinitas Athanor counts for Wandering years once the team finished Schwaz, not before. */
    @ServusGameTest(timeoutTicks = 300)
    public static void citrinitasCraftCountsForWanderingYears(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        HollowGameTests.finishVillach(level, team);
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "citrinitas");
        helper.assertTrue(Journal.journalOf(level.getServer(), team).count(WANDERING_YEARS, "citrinitas_craft") == 0,
                "nothing counts for Wandering years before Schwaz");
        HollowGameTests.finishSchwaz(level, team);
        AthanorBlockEntity athanor = athanor(helper);
        athanor.setTeam(team);
        saltRing(helper);
        citrinitasVats(helper);
        helper.runAfterDelay(2, () -> pour(helper, new Principles(0, 200, 0)));
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.GUNPOWDER));
        helper.succeedWhen(() -> helper.assertTrue(
                Journal.journalOf(level.getServer(), team).count(WANDERING_YEARS, "citrinitas_craft") == 1,
                "the Citrinitas craft should count"));
    }

    /** Wandering years finishes from its events and opens Basel and Salzburg. */
    @ServusGameTest(timeoutTicks = 20)
    public static void wanderingYearsFinishesAndOpensBasel(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        HollowGameTests.finishVillach(level, team);
        HollowGameTests.finishSchwaz(level, team);
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "rubedo");
        helper.assertTrue(Journal.journalOf(level.getServer(), team).count(BASEL_SALZBURG, "rubedo_craft") == 0, "Basel waits");
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "citrinitas");
        Journal.recordItem(level, team, "ripen", new ItemStack(Items.RAW_GOLD), 1);
        Journal.recordEvent(level, team, "bind", 3);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, WANDERING_YEARS), "Wandering years should be finished");
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "rubedo");
        JournalLedger.TeamJournal journal = Journal.journalOf(level.getServer(), team);
        helper.assertTrue(journal.count(BASEL_SALZBURG, "rubedo_craft") == 1 && journal.count(BASEL_SALZBURG, "crafts") == 1,
                "the Rubedo craft counts for Basel and Salzburg");
        helper.succeed();
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

    /** A dose of certain poison gives Poison and Nausea and still Elemental Sight. */
    @ServusGameTest(timeoutTicks = 20)
    public static void elixirChanceOnePoisonsAndGrantsSight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        boolean poisoned = ElixirOfSightItem.drink(helper.getLevel(), player, new ElixirDose(1, 0, 200, 100));
        helper.assertTrue(poisoned, "the drink should poison");
        helper.assertTrue(player.getEffect(MobEffects.POISON) != null && player.getEffect(MobEffects.POISON).getDuration() == 200,
                "Poison for 200 ticks");
        helper.assertTrue(player.getEffect(MobEffects.NAUSEA) != null && player.getEffect(MobEffects.NAUSEA).getDuration() == 100,
                "Nausea for 100 ticks");
        helper.assertTrue(ElementalSight.active(player), "the poisoned drinker still has Elemental Sight");
        helper.succeed();
    }

    /** A dose without chance never poisons, however often the elixir is drunk, also under the sight. */
    @ServusGameTest(timeoutTicks = 20)
    public static void elixirChanceZeroNeverPoisons(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ElixirDose harmless = new ElixirDose(0, 0, 200, 200);
        for (int drink = 0; drink < 50; drink++) {
            helper.assertFalse(ElixirOfSightItem.drink(helper.getLevel(), player, harmless), "drink " + drink + " should not poison");
        }
        helper.assertFalse(player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.NAUSEA), "no Poison and no Nausea");
        helper.assertTrue(ElementalSight.active(player), "the drinker has Elemental Sight");
        helper.succeed();
    }

    /** The shipped data gives the Elixir of Sight a 15 % chance, 15 % more under the sight, and 10 s of Poison and Nausea. */
    @ServusGameTest(timeoutTicks = 20)
    public static void shippedElixirDoseLoads(GameTestHelper helper) {
        ElixirDose dose = ElixirDoseData.INSTANCE.forItem(ElementalSight.ELIXIR.get());
        helper.assertTrue(dose.equals(new ElixirDose(0.15, 0.15, 200, 200)), "shipped dose, got " + dose);
        helper.succeed();
    }

    /** The elixir is an Albedo craft: nothing at Nigredo, the elixir once the ring stands. */
    @ServusGameTest(timeoutTicks = 400)
    public static void elixirNeedsAnAlbedoAthanor(GameTestHelper helper) {
        AthanorBlockEntity athanor = athanor(helper);
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(ServusAlchemyItems.QUICKSILVER.get()));
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
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(ATHANOR), Direction.UP);
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
        helper.assertTrue(book.getCategory(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "workshop")) != null,
                "the Workshop category should exist");
        for (String entry : List.of("principles", "calciner", "purifier", "coagulator", "athanor", "pipes", "salt_crystal_block",
                "elixir_of_sight", "gnome_hollow")) {
            // The server keeps only categories until the book is built for clients, so look at the data files.
            Identifier file = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID,
                    "modonomicon/books/journal/entries/workshop/" + entry + ".json");
            helper.assertTrue(helper.getLevel().getServer().getResourceManager().getResource(file).isPresent(),
                    "the Workshop should have the entry " + entry);
        }
        helper.succeed();
    }

    /** The Athanor crafts from Stone Bricks; vanilla Bricks in its pattern make nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void athanorRecipe(GameTestHelper helper) {
        String[] rows = {"BCB", "BFB", "BBB"};
        CraftingGrids.assertCrafts(helper, CraftingGrids.crafted(helper,
                Map.of('B', Items.STONE_BRICKS, 'C', Items.CAULDRON, 'F', Items.BLAST_FURNACE), rows), ServusAlchemyItems.ATHANOR.get());
        helper.assertTrue(CraftingGrids.crafted(helper,
                Map.of('B', Items.BRICKS, 'C', Items.CAULDRON, 'F', Items.BLAST_FURNACE), rows).isEmpty(), "bricks make no Athanor");
        helper.succeed();
    }
}
