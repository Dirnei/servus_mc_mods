package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.prelum.PrelumBlockEntity;
import io.github.dirnei.mc.servus.alchemy.prelum.PressingRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Prelum: Caput Mortuum pressed into Netherite Scrap without fuel, a Foundation machine, a producer and the end of the journal. */
public final class PrelumGameTests {

    private static final BlockPos PRELUM = AlchemyGameTests.MACHINE.above();
    private static final int CHARGE = 16;
    /** One pressing at the shipped 600 ticks, with room to spare. */
    private static final int ONE_PRESS = 700;
    private static final int TWO_PRESSES = 1300;

    private PrelumGameTests() {
    }

    /** A Prelum facing north, so west is its right side and north its closed front. */
    static PrelumBlockEntity prelum(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusAlchemyBlocks.PRELUM.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, PrelumBlockEntity.class);
    }

    private static ItemStack caputMortuum(int count) {
        return new ItemStack(ServusAlchemyItems.CAPUT_MORTUUM.get(), count);
    }

    private static ItemView items(GameTestHelper helper, BlockPos pos, Direction side) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), side));
    }

    private static int count(ChestBlockEntity chest, Item item) {
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).is(item)) {
                total += chest.getItem(slot).getCount();
            }
        }
        return total;
    }

    /** Two charges of Caput Mortuum and no fuel become two Netherite Scrap. */
    @ServusGameTest(timeoutTicks = TWO_PRESSES)
    public static void pressesCaputMortuumIntoScrap(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(2 * CHARGE));
        helper.succeedWhen(() -> {
            ItemStack output = prelum.inventory().getStackInSlot(PrelumBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(output.is(Items.NETHERITE_SCRAP) && output.getCount() == 2, "two Netherite Scrap should be made, got " + output);
            helper.assertTrue(prelum.inventory().getStackInSlot(PrelumBlockEntity.SLOT_INPUT).isEmpty(), "the Caput Mortuum is used up");
        });
    }

    /** Less than a full charge does not press and keeps the progress at zero. */
    @ServusGameTest(timeoutTicks = 60)
    public static void waitsForAFullCharge(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(CHARGE - 1));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(prelum.containerData().get(PrelumBlockEntity.DATA_PROGRESS) == 0, "no progress with 15 Caput Mortuum");
            helper.assertFalse(helper.getBlockState(PRELUM).getValue(MachineBlock.LIT), "an idle Prelum is not lit");
            helper.succeed();
        });
    }

    /** Only what a pressing recipe takes goes in, and the arrow runs at the recipe's time. */
    @ServusGameTest(timeoutTicks = 20)
    public static void refusesOtherItems(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        ItemView side = items(helper, PRELUM, Direction.WEST);
        helper.assertTrue(side.insertItem(new ItemStack(Items.COBBLESTONE, 4), false).getCount() == 4, "Cobblestone is refused");
        helper.assertTrue(side.insertItem(new ItemStack(Items.NETHERITE_SCRAP, 1), false).getCount() == 1, "the result is no input");
        helper.assertTrue(side.insertItem(caputMortuum(CHARGE), false).isEmpty(), "Caput Mortuum goes in");
        helper.assertTrue(prelum.containerData().get(PrelumBlockEntity.DATA_WORK_TICKS) == PressingRecipe.DEFAULT_TIME,
                "the arrow runs at the recipe's time");
        helper.succeed();
    }

    /** A full output stops the press and keeps the input. */
    @ServusGameTest(timeoutTicks = 60)
    public static void stopsWhenOutputIsFull(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_OUTPUT, new ItemStack(Items.NETHERITE_SCRAP, 64));
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(CHARGE));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(prelum.inventory().getStackInSlot(PrelumBlockEntity.SLOT_INPUT).getCount() == CHARGE, "the Caput Mortuum stays");
            helper.assertTrue(prelum.containerData().get(PrelumBlockEntity.DATA_PROGRESS) == 0, "no progress while the output is full");
            helper.succeed();
        });
    }

    /** Through the item capability the scrap comes out and the input stays in. */
    @ServusGameTest(timeoutTicks = 20)
    public static void outputThroughTheCapability(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(5));
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_OUTPUT, new ItemStack(Items.NETHERITE_SCRAP, 3));
        ItemView side = items(helper, PRELUM, Direction.DOWN);
        int scrap = 0;
        int caput = 0;
        for (int slot = 0; slot < side.getSlots(); slot++) {
            ItemStack taken = side.extractItem(slot, 64, false);
            scrap += taken.is(Items.NETHERITE_SCRAP) ? taken.getCount() : 0;
            caput += taken.is(ServusAlchemyItems.CAPUT_MORTUUM.get()) ? taken.getCount() : 0;
        }
        helper.assertTrue(scrap == 3 && caput == 0, "the scrap comes out and the input stays, got " + scrap + " scrap, " + caput + " caput");
        helper.succeed();
    }

    /** The front is closed; the Auto top takes Caput Mortuum. */
    @ServusGameTest(timeoutTicks = 20)
    public static void frontRefuses(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        helper.assertTrue(prelum.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front is closed");
        ItemView front = items(helper, PRELUM, Direction.NORTH);
        helper.assertTrue(front == null || front.insertItem(caputMortuum(CHARGE), false).getCount() == CHARGE, "nothing goes in at the front");
        ItemView top = items(helper, PRELUM, Direction.UP);
        helper.assertTrue(top.insertItem(caputMortuum(CHARGE), false).isEmpty(), "the Auto top takes Caput Mortuum");
        helper.succeed();
    }

    /** A hopper above feeds the Prelum, 8 ticks per item, and its scrap is pushed into the chest below. */
    @ServusGameTest(timeoutTicks = ONE_PRESS + 8 * CHARGE + 100)
    public static void hopperInChestOut(GameTestHelper helper) {
        BlockPos prelumPos = PRELUM.above();
        helper.setBlock(PRELUM, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(PRELUM, ChestBlockEntity.class);
        prelum(helper, prelumPos);
        helper.setBlock(prelumPos.above(), Blocks.HOPPER.defaultBlockState());
        helper.getBlockEntity(prelumPos.above(), HopperBlockEntity.class).setItem(0, caputMortuum(CHARGE));
        helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.NETHERITE_SCRAP) == 1,
                "the chest below should hold 1 Netherite Scrap, got " + count(chest, Items.NETHERITE_SCRAP)));
    }

    /** The pressing recipe is on offer with its count and time; the input's recipe is the active one; no upkeep. */
    @ServusGameTest(timeoutTicks = 20)
    public static void exposesPressingProduction(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(PRELUM), Direction.UP);
        helper.assertTrue(production != null, "the Prelum should be a producer");
        Optional<ProductionRecipe> scrap = production.recipes().stream()
                .filter(recipe -> recipe.outputs().getFirst().is(Items.NETHERITE_SCRAP))
                .findFirst();
        helper.assertTrue(scrap.isPresent(), "Netherite Scrap should be on offer");
        ItemStack input = scrap.get().inputs().getFirst();
        helper.assertTrue(input.is(ServusAlchemyItems.CAPUT_MORTUUM.get()) && input.getCount() == CHARGE, "from 16 Caput Mortuum, got " + input);
        helper.assertTrue(scrap.get().ticks() == PressingRecipe.DEFAULT_TIME, "in 600 ticks, got " + scrap.get().ticks());
        helper.assertTrue(production.active().isEmpty(), "an empty Prelum works on nothing");
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(CHARGE));
        helper.assertTrue(production.active().equals(Optional.of(scrap.get().id())), "the Caput Mortuum recipe is active");
        helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(PRELUM), Direction.UP) == null,
                "the Prelum keeps no upkeep");
        helper.succeed();
    }

    /** On a data line the Prelum is a producer and nothing else. */
    @ServusGameTest(timeoutTicks = 60)
    public static void producerOnTheDataLine(GameTestHelper helper) {
        BlockPos duct = PRELUM.east();
        prelum(helper, PRELUM);
        GameTestSteps.duct(helper, duct, Line.DATA);
        helper.succeedWhen(() -> {
            DataMember member = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(duct)).flatMap(network ->
                    network.dataMembers().stream().filter(candidate -> candidate.pos().equals(helper.absolutePos(PRELUM))).findFirst())
                    .orElse(null);
            helper.assertTrue(member != null && member.roles().equals(Set.of(DataRole.PRODUCER)), "the Prelum should be a producer only, got " + member);
        });
    }

    /** Finishes the chapters before Basel and Salzburg for a team. */
    private static void openBasel(ServerLevel level, UUID team) {
        HollowGameTests.finishVillach(level, team);
        HollowGameTests.finishSchwaz(level, team);
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "citrinitas");
        Journal.recordItem(level, team, "bind", new ItemStack(Items.RAW_GOLD), 1);
        Journal.recordItem(level, team, "bind", new ItemStack(Items.RAW_IRON), 3);
    }

    /** The first pressed scrap counts for Basel and Salzburg and, with its other tasks, finishes the chapter. */
    @ServusGameTest(timeoutTicks = ONE_PRESS)
    public static void pressingCountsForBasel(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        openBasel(level, team);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, AlchemyGameTests.WANDERING_YEARS), "Wandering years is finished");
        Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "rubedo");
        Journal.recordItem(level, team, "bind", new ItemStack(Items.DIAMOND), 1);
        for (int craft = 1; craft < 16; craft++) {
            Journal.recordAthanor(level, team, new ItemStack(Items.BLAZE_POWDER), "citrinitas");
        }
        helper.assertFalse(Journal.isFinished(level.getServer(), team, AlchemyGameTests.BASEL_SALZBURG), "Basel waits for the Prelum");
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.setTeam(team);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(CHARGE));
        helper.succeedWhen(() -> {
            helper.assertTrue(Journal.journalOf(level.getServer(), team).count(AlchemyGameTests.BASEL_SALZBURG, "scrap") == 1,
                    "the pressed scrap counts");
            helper.assertTrue(Journal.isFinished(level.getServer(), team, AlchemyGameTests.BASEL_SALZBURG), "Basel and Salzburg is finished");
        });
    }

    /** The shipped pressing takes 16 Caput Mortuum and makes one Netherite Scrap. */
    @ServusGameTest(timeoutTicks = 20)
    public static void pressingRecipeShipped(GameTestHelper helper) {
        List<PressingRecipe> recipes = ServusRecipes.all(helper.getLevel(), ServusAlchemyRecipes.PRESSING.get()).stream()
                .map(holder -> holder.value()).toList();
        helper.assertTrue(recipes.stream().anyMatch(recipe -> recipe.accepts(caputMortuum(1)) && recipe.input().count() == CHARGE
                && recipe.result().is(Items.NETHERITE_SCRAP) && recipe.result().getCount() == 1), "16 Caput Mortuum into 1 Netherite Scrap");
        helper.succeed();
    }

    /** Only a Rubedo Athanor makes the screw, from Dragon's Breath; the Prelum is crafted from the screw. */
    @ServusGameTest(timeoutTicks = 20)
    public static void screwNeedsDragonsBreathOnRubedo(GameTestHelper helper) {
        List<AthanorRecipe> screws = ServusRecipes.all(helper.getLevel(), ServusAlchemyRecipes.ATHANOR.get()).stream()
                .map(holder -> holder.value()).filter(recipe -> recipe.result().is(ServusAlchemyItems.PRELUM_SCREW.get())).toList();
        helper.assertTrue(screws.size() == 1, "one Athanor recipe makes the screw, got " + screws.size());
        AthanorRecipe screw = screws.getFirst();
        helper.assertTrue(screw.accepts(new ItemStack(Items.DRAGON_BREATH)), "the screw takes Dragon's Breath");
        helper.assertTrue(screw.tier() == 3, "on a Rubedo Athanor, got tier " + screw.tier());
        ItemStack log = new ItemStack(Items.STRIPPED_DARK_OAK_LOG);
        ItemStack deepslate = new ItemStack(Items.POLISHED_DEEPSLATE);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(ServusAlchemyItems.PRELUM_SCREW.get()), ItemStack.EMPTY,
                log, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE), log,
                deepslate, deepslate, deepslate));
        ItemStack result = ServusRecipes.craftingFor(helper.getLevel(), grid)
                .map(holder -> holder.value().assemble(grid)).orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ServusAlchemyItems.PRELUM.get()) && result.getCount() == 1, "the grid should make one Prelum, got " + result);
        CraftingInput withoutScrew = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                log, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE), log,
                deepslate, deepslate, deepslate));
        helper.assertTrue(ServusRecipes.craftingFor(helper.getLevel(), withoutScrew).isEmpty(), "no Prelum without the screw");
        helper.succeed();
    }

    /** Breaking it drops the Prelum and what it holds. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingDropsContents(GameTestHelper helper) {
        PrelumBlockEntity prelum = prelum(helper, PRELUM);
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_INPUT, caputMortuum(7));
        prelum.inventory().setStackInSlot(PrelumBlockEntity.SLOT_OUTPUT, new ItemStack(Items.NETHERITE_SCRAP, 2));
        helper.getLevel().destroyBlock(helper.absolutePos(PRELUM), true);
        int caput = 0;
        int scrap = 0;
        int presses = 0;
        for (ItemEntity drop : GameTestSteps.drops(helper, PRELUM, 2)) {
            ItemStack stack = drop.getItem();
            caput += stack.is(ServusAlchemyItems.CAPUT_MORTUUM.get()) ? stack.getCount() : 0;
            scrap += stack.is(Items.NETHERITE_SCRAP) ? stack.getCount() : 0;
            presses += stack.is(ServusAlchemyItems.PRELUM.get()) ? stack.getCount() : 0;
        }
        helper.assertTrue(caput == 7 && scrap == 2 && presses == 1, "drops: " + caput + " caput, " + scrap + " scrap, " + presses + " prelum");
        helper.succeed();
    }
}
