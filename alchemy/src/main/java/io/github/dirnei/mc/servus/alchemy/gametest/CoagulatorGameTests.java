package io.github.dirnei.mc.servus.alchemy.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.CoagulatorMenu;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout.Role;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorSeeds;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** The Coagulator as a build: forming, unforming, the Vats, the casting table's items and its seed. */
public final class CoagulatorGameTests {

    static final BlockPos COAGULATOR = new BlockPos(5, 1, 5);

    private CoagulatorGameTests() {
    }

    /**
     * Builds the unformed structure for a front facing {@code facing}, the Coagulator placed facing
     * that way: the build is the same seen from both sides, so the Coagulator's own facing decides.
     */
    static void build(GameTestHelper helper, BlockPos controller, Direction facing) {
        for (Cell cell : CoagulatorLayout.cells()) {
            BlockState state = CoagulatorStructure.expected(cell.role()).defaultBlockState();
            if (cell.role() == Role.COAGULATOR) {
                state = state.setValue(MachineBlock.FACING, facing);
            }
            helper.setBlock(CoagulatorStructure.pos(controller, cell, Structures.facing(facing)), state);
        }
    }

    /** Builds and forms a Coagulator facing north. */
    static CoagulatorBlockEntity formed(GameTestHelper helper, BlockPos controller) {
        build(helper, controller, Direction.NORTH);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), controller);
        helper.assertBlockProperty(controller, CoagulatorBlock.FORMED, true);
        return helper.getBlockEntity(controller, CoagulatorBlockEntity.class);
    }

    static BlockPos vat(BlockPos controller, int piece) {
        return CoagulatorStructure.pos(controller, CoagulatorLayout.vat(piece), Facing.NORTH);
    }

    static FluidView vatHandler(GameTestHelper helper, BlockPos controller, int piece) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(vat(controller, piece)), Direction.UP));
    }

    /** Lays the result of the shipped recipe {@code name} into the seed slot. */
    static void seedWith(GameTestHelper helper, CoagulatorBlockEntity coagulator, String name) {
        Identifier id = AlchemyGameTests.coagulation(name);
        CoagulationRecipe recipe = ServusRecipes.byId(helper.getLevel(), id)
                .map(holder -> (CoagulationRecipe) holder.value()).orElseThrow(() -> new IllegalStateException("missing " + id));
        helper.assertTrue(coagulator.plantSeed(recipe.result()) == 1, "the " + name + " seed should go in");
    }

    static boolean isFormed(GameTestHelper helper, BlockPos controller) {
        return helper.getBlockState(controller).getValue(CoagulatorBlock.FORMED);
    }

    /** The Journal forms the build in every facing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorFormsInEveryFacing(GameTestHelper helper) {
        BlockPos[] at = {new BlockPos(2, 1, 2), new BlockPos(2, 1, 8), new BlockPos(7, 1, 3), new BlockPos(9, 1, 8)};
        Direction[] facings = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < at.length; i++) {
            build(helper, at[i], facings[i]);
            CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), at[i]);
            helper.assertTrue(isFormed(helper, at[i]), "the Coagulator facing " + facings[i] + " should form");
            helper.assertBlockProperty(at[i], MachineBlock.FACING, facings[i]);
            for (int piece = 0; piece < 3; piece++) {
                helper.assertBlockProperty(CoagulatorStructure.pos(at[i], CoagulatorLayout.vat(piece), Structures.facing(facings[i])),
                        VatBlock.PIECE, piece);
            }
        }
        helper.succeed();
    }

    /** Without the Mercury Vat nothing forms, and the check names it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorWithoutAVatDoesNotForm(GameTestHelper helper) {
        build(helper, COAGULATOR, Direction.NORTH);
        helper.setBlock(vat(COAGULATOR, 2), Blocks.AIR);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), COAGULATOR);
        helper.assertTrue(!isFormed(helper, COAGULATOR), "nothing forms");
        helper.assertTrue(CoagulatorStructure.check(helper.getLevel(), helper.absolutePos(COAGULATOR)).firstWrong()
                .map(Cell::role).orElse(null) == Role.VAT, "the Vat is named");
        helper.succeed();
    }

    /** Breaking a Stone Bricks block unforms the build and stops the work. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingAStoneUnformsTheCoagulator(GameTestHelper helper) {
        formed(helper, COAGULATOR);
        helper.getLevel().destroyBlock(helper.absolutePos(COAGULATOR.east()), true);
        helper.assertTrue(!isFormed(helper, COAGULATOR), "the Coagulator unforms");
        for (int piece = 0; piece < 3; piece++) {
            helper.assertBlockProperty(vat(COAGULATOR, piece), VatBlock.FORMED, false);
        }
        helper.succeed();
    }

    /** Each Vat takes only its own principle and gives nothing back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void eachVatTakesItsPrinciple(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Principle[] order = {Principle.SULFUR, Principle.SALT, Principle.MERCURY};
        for (int piece = 0; piece < 3; piece++) {
            FluidView handler = vatHandler(helper, COAGULATOR, piece);
            for (Principle principle : Principle.values()) {
                int filled = handler.fill(new FluidStack(PrincipleFluids.fluid(principle), 100), false);
                helper.assertTrue(filled == (principle == order[piece] ? 100 : 0), "Vat " + piece + " and " + principle + " took " + filled);
            }
            helper.assertTrue(handler.drain(100, false).isEmpty(), "a Vat gives nothing");
        }
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(100, 100, 100)), "each tank holds 100 mB");
        helper.succeed();
    }

    /** A bucket of another principle does nothing at a Vat. */
    @ServusGameTest(timeoutTicks = 20)
    public static void wrongBucketDoesNothing(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(PrincipleFluids.entry(Principle.MERCURY).bucket().get()));
        CalcinerGameTests.use(helper, player, vat(COAGULATOR, 0));
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(0, 0, 0)), "the Sulfur Vat takes no Mercury");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the bucket stays full");
        helper.succeed();
    }

    /** The casting table takes the ingredient and a hopper under it takes the products. */
    @ServusGameTest(timeoutTicks = 200)
    public static void itemsAtTheCastingTable(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        seedWith(helper, coagulator, "salt_crystal");
        ItemView port = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(COAGULATOR), Direction.NORTH));
        helper.assertTrue(port != null && port.getSlots() == 3, "the Coagulator has its item port");
        helper.assertTrue(port.insertItem(1, new ItemStack(Items.STONE), false).getCount() == 1, "nothing goes into the output");
        helper.assertTrue(ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(vat(COAGULATOR, 1)), Direction.NORTH)) == null,
                "a Vat takes no items");
        helper.setBlock(COAGULATOR.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(COAGULATOR.below(), HopperBlockEntity.class);
        vatHandler(helper, COAGULATOR, 1).fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 250), false);
        helper.succeedWhen(() -> helper.assertTrue(hopper.countItem(ServusAlchemyItems.SALT_CRYSTAL.get()) == 2,
                "the hopper should take both Salt Crystals"));
    }

    /** Clay Ball, a Nigredo good, comes from the principles alone. */
    @ServusGameTest(timeoutTicks = 200)
    public static void coagulatorMakesClay(GameTestHelper helper) {
        makesFromPrinciples(helper, "clay_ball", Items.CLAY_BALL);
    }

    /** Bone Meal, a Nigredo good, comes from the principles alone. */
    @ServusGameTest(timeoutTicks = 200)
    public static void coagulatorMakesBoneMeal(GameTestHelper helper) {
        makesFromPrinciples(helper, "bone_meal", Items.BONE_MEAL);
    }

    /** Selects the shipped recipe {@code name}, gives the formed Coagulator exactly its principles and waits for one {@code result}. */
    private static void makesFromPrinciples(GameTestHelper helper, String name, Item result) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Identifier id = AlchemyGameTests.coagulation(name);
        CoagulationRecipe recipe = ServusRecipes.byId(helper.getLevel(), id)
                .map(holder -> (CoagulationRecipe) holder.value()).orElseThrow(() -> new IllegalStateException("missing " + id));
        helper.assertTrue(recipe.ingredient().isEmpty() && recipe.chapter().isEmpty(), name + " needs no item and no chapter");
        seedWith(helper, coagulator, name);
        coagulator.tanks().insert(recipe.input());
        helper.succeedWhen(() -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).is(result), name + " should be made");
            helper.assertTrue(coagulator.tanks().contents().equals(Principles.NONE), "the principles are used up");
        });
    }

    /** An unformed Coagulator with Salt and a recipe makes nothing and offers no port. */
    @ServusGameTest(timeoutTicks = 120)
    public static void unformedCoagulatorDoesNothing(GameTestHelper helper) {
        build(helper, COAGULATOR, Direction.NORTH);
        CoagulatorBlockEntity coagulator = helper.getBlockEntity(COAGULATOR, CoagulatorBlockEntity.class);
        seedWith(helper, coagulator, "salt_crystal");
        coagulator.tanks().insert(new Principles(250, 0, 0));
        helper.assertTrue(vatHandler(helper, COAGULATOR, 1) == null, "an unformed Vat offers nothing");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "nothing is made");
            helper.succeed();
        });
    }

    /** Taking the seed out stops the Coagulator, and it leaves its Salt alone. */
    @ServusGameTest(timeoutTicks = 120)
    public static void takingTheSeedOutStopsTheCoagulator(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        seedWith(helper, coagulator, "salt_crystal");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CoagulatorMenu menu = new CoagulatorMenu(0, player.getInventory(), coagulator);
        helper.assertTrue(menu.seedSlot().getItem().is(ServusAlchemyItems.SALT_CRYSTAL.get()), "the menu shows the seed");
        helper.assertTrue(menu.seedSlot().remove(1).is(ServusAlchemyItems.SALT_CRYSTAL.get()), "the seed comes out of the slot");
        helper.assertTrue(coagulator.seed().isEmpty(), "the seed slot is empty");
        coagulator.tanks().insert(new Principles(250, 0, 0));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "nothing is made");
            helper.assertTrue(coagulator.tanks().contents().equals(new Principles(250, 0, 0)), "the Salt stays in the tank");
            helper.succeed();
        });
    }

    /** Three Kandis grow three Salt Crystals, one used up per cycle, and then the Coagulator stops with Salt left over. */
    @ServusGameTest(timeoutTicks = 300)
    public static void kandisSeedsAreUsedUp(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        helper.assertTrue(coagulator.plantSeed(new ItemStack(ServusAlchemyItems.KANDIS.get(), 3)) == 3, "three Kandis go in");
        coagulator.tanks().insert(new Principles(1000, 0, 0));
        helper.runAfterDelay(240, () -> {
            ItemStack output = coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(output.is(ServusAlchemyItems.SALT_CRYSTAL.get()) && output.getCount() == 3, "three Salt Crystals, got " + output);
            helper.assertTrue(coagulator.seed().isEmpty(), "the Kandis are used up, left " + coagulator.seed());
            helper.assertTrue(coagulator.tanks().amount(Principle.SALT) == 250, "the Coagulator stops with 250 mB Salt left, has "
                    + coagulator.tanks().amount(Principle.SALT));
            helper.succeed();
        });
    }

    /** Two Redstone seeds are used up and come back doubled: four Redstone, and the Coagulator stops. */
    @ServusGameTest(timeoutTicks = 300)
    public static void redstoneSeedsDouble(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        helper.assertTrue(coagulator.plantSeed(new ItemStack(Items.REDSTONE, 2)) == 2, "two Redstone go in");
        coagulator.tanks().insert(new Principles(150, 0, 450));
        helper.runAfterDelay(200, () -> {
            ItemStack output = coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(output.is(Items.REDSTONE) && output.getCount() == 4, "four Redstone, got " + output);
            helper.assertTrue(coagulator.seed().isEmpty(), "the Redstone seeds are used up, left " + coagulator.seed());
            helper.assertTrue(coagulator.tanks().contents().equals(new Principles(50, 0, 150)),
                    "the Coagulator stops with one cycle of principles left, has " + coagulator.tanks().contents());
            helper.succeed();
        });
    }

    /** Autocrafting sees one seed among the inputs: Kandis gives one Salt Crystal, a Salt Crystal gives two. */
    @ServusGameTest(timeoutTicks = 20)
    public static void productionListsTheSeed(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        coagulator.plantSeed(new ItemStack(ServusAlchemyItems.KANDIS.get(), 5));
        ProductionRecipe fromKandis = coagulator.production().recipes().getFirst();
        helper.assertTrue(fromKandis.inputs().size() == 1 && fromKandis.inputs().getFirst().is(ServusAlchemyItems.KANDIS.get())
                && fromKandis.inputs().getFirst().getCount() == 1, "one Kandis is an input, got " + fromKandis.inputs());
        helper.assertTrue(fromKandis.outputs().getFirst().getCount() == 1, "Kandis gives one Salt Crystal");
        coagulator.takeSeed();
        coagulator.plantSeed(new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get(), 5));
        ProductionRecipe fromCrystal = coagulator.production().recipes().getFirst();
        helper.assertTrue(fromCrystal.inputs().size() == 1 && fromCrystal.inputs().getFirst().is(ServusAlchemyItems.SALT_CRYSTAL.get())
                && fromCrystal.inputs().getFirst().getCount() == 1, "one Salt Crystal is an input, got " + fromCrystal.inputs());
        helper.assertTrue(fromCrystal.outputs().getFirst().is(ServusAlchemyItems.SALT_CRYSTAL.get())
                && fromCrystal.outputs().getFirst().getCount() == 2, "a Salt Crystal gives two, got " + fromCrystal.outputs());
        helper.succeed();
    }

    /** Without a seed the Coagulator makes nothing and keeps its Salt. */
    @ServusGameTest(timeoutTicks = 120)
    public static void noSeedDoesNothing(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        coagulator.tanks().insert(new Principles(250, 0, 0));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "nothing is made");
            helper.assertTrue(coagulator.tanks().contents().equals(new Principles(250, 0, 0)), "the Salt stays in the tank");
            helper.succeed();
        });
    }

    /** The seed of a recipe locked behind Villach grows nothing for a team without Villach, and grows once the team finishes it. */
    @ServusGameTest(timeoutTicks = 300)
    public static void lockedSeedGrowsNothing(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        UUID team = UUID.randomUUID();
        coagulator.setTeam(team);
        helper.assertTrue(coagulator.plantSeed(new ItemStack(Items.ECHO_SHARD)) == 1, "the locked recipe's seed goes in");
        helper.assertTrue(coagulator.seedRecipe().filter(holder -> holder.id().identifier().equals(GameTestRecipes.LOCKED_SEED)).isPresent(),
                "the Echo Shard seeds the locked gametest recipe");
        coagulator.tanks().insert(new Principles(50, 0, 0));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CoagulatorMenu menu = new CoagulatorMenu(0, player.getInventory(), coagulator);
        helper.assertTrue(menu.seedLocked(), "the menu reports the seed as locked");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "a locked seed grows nothing");
            helper.assertTrue(coagulator.tanks().amount(Principle.SALT) == 50, "the Salt stays");
            Journal.finish(helper.getLevel().getServer(), team, AlchemyGameTests.VILLACH);
            helper.succeedWhen(() -> helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).is(Items.ECHO_SHARD),
                    "the seed grows once Villach is finished"));
        });
    }

    /** A seed item two recipes name stays with the recipe whose id sorts first; the other recipe loses it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void duplicateSeedStaysWithTheFirstRecipe(GameTestHelper helper) {
        helper.assertTrue(CoagulatorSeeds.recipeFor(helper.getLevel(), new ItemStack(ServusAlchemyItems.KANDIS.get()))
                        .filter(holder -> holder.id().identifier().equals(AlchemyGameTests.coagulation("salt_crystal"))).isPresent(),
                "Kandis grows Salt Crystals");
        helper.assertTrue(ServusRecipes.byId(helper.getLevel(), GameTestRecipes.DUPLICATE_SEED).isPresent(), "the duplicate recipe is loaded");
        helper.assertTrue(CoagulatorSeeds.seedsOf(helper.getLevel(), GameTestRecipes.DUPLICATE_SEED).isEmpty(), "the duplicate recipe lost its seed");
        helper.succeed();
    }

    /** A pipe lays Kandis into the seed slot and adds more of them, but refuses another seed and takes nothing out. */
    @ServusGameTest(timeoutTicks = 20)
    public static void pipeAddsSeedsButNeverTakesThem(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        ResourceHandler<ItemResource> port = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(COAGULATOR), Direction.NORTH);
        ItemStack left = Transfer.insertItem(port, new ItemStack(ServusAlchemyItems.KANDIS.get(), 16), false);
        helper.assertTrue(left.isEmpty(), "the pipe lays all 16 Kandis in, kept " + left.getCount());
        helper.assertTrue(Transfer.insertItem(port, new ItemStack(ServusAlchemyItems.KANDIS.get(), 4), false).isEmpty(), "four more Kandis go in");
        helper.assertTrue(coagulator.seed().is(ServusAlchemyItems.KANDIS.get()) && coagulator.seed().getCount() == 20,
                "20 Kandis are the seed, got " + coagulator.seed());
        helper.assertTrue(Transfer.insertItem(port, new ItemStack(ServusAlchemyItems.BRIMSTONE.get()), false).getCount() == 1,
                "another seed is refused");
        ItemView view = ItemView.of(port);
        for (int slot = 0; slot < view.getSlots(); slot++) {
            helper.assertTrue(view.extractItem(slot, 64, false).isEmpty(), "nothing comes out of slot " + slot);
        }
        helper.assertTrue(coagulator.seed().is(ServusAlchemyItems.KANDIS.get()) && coagulator.seed().getCount() == 20, "the seeds stay");
        helper.succeed();
    }

    /** A pipe lays a stack of Salt Crystals into the seed slot like any other seed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void pipeLaysAStackOfSelfSeeds(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        ResourceHandler<ItemResource> port = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(COAGULATOR), Direction.NORTH);
        ItemStack left = Transfer.insertItem(port, new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get(), 16), false);
        helper.assertTrue(left.isEmpty(), "the pipe lays all 16 Salt Crystals in, kept " + left.getCount());
        helper.assertTrue(coagulator.seed().is(ServusAlchemyItems.SALT_CRYSTAL.get()) && coagulator.seed().getCount() == 16,
                "16 Salt Crystals are the seed");
        helper.succeed();
    }

    /** Cinnabar used on the table goes in as a whole stack, another seed is refused, and sneaking with an empty hand takes them all out. */
    @ServusGameTest(timeoutTicks = 20)
    public static void seedByHand(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.CINNABAR.get(), 3));
        CalcinerGameTests.use(helper, player, COAGULATOR);
        helper.assertTrue(coagulator.seed().is(ServusAlchemyItems.CINNABAR.get()) && coagulator.seed().getCount() == 3, "three Cinnabar are the seed");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "all Cinnabar left the hand");
        helper.assertTrue(coagulator.plantSeed(new ItemStack(ServusAlchemyItems.BRIMSTONE.get())) == 0, "Brimstone is refused while Cinnabar is in");
        player.setShiftKeyDown(true);
        CalcinerGameTests.use(helper, player, COAGULATOR);
        helper.assertTrue(coagulator.seed().isEmpty(), "sneaking takes the seeds out");
        helper.assertTrue(player.getInventory().countItem(ServusAlchemyItems.CINNABAR.get()) == 3, "the player gets the three Cinnabar back");
        helper.succeed();
    }

    /** The journal's schematic accepts the build that forming accepts. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorSchematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulator"));
        helper.assertTrue(schematic != null, "the Coagulator schematic should be loaded");
        build(helper, COAGULATOR, Direction.NORTH);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(COAGULATOR)) != null, "the schematic accepts the build");
        helper.setBlock(vat(COAGULATOR, 0), Blocks.AIR);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(COAGULATOR)) == null, "and refuses it without a Vat");
        helper.succeed();
    }

    /** The Coagulator crafts with cobblestone in the corners; iron ingots there make nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void coagulatorRecipe(GameTestHelper helper) {
        String[] rows = {"SGS", "GCG", "SRS"};
        CraftingGrids.assertCrafts(helper, CraftingGrids.crafted(helper,
                Map.of('S', Items.COBBLESTONE, 'G', Items.GLASS, 'C', Items.CAULDRON, 'R', Items.REDSTONE), rows),
                ServusAlchemyItems.COAGULATOR.get());
        helper.assertTrue(CraftingGrids.crafted(helper,
                Map.of('S', Items.IRON_INGOT, 'G', Items.GLASS, 'C', Items.CAULDRON, 'R', Items.REDSTONE), rows).isEmpty(),
                "iron makes no Coagulator");
        helper.succeed();
    }
}
