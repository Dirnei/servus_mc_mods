package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewKettle;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewKettleBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBlockEntity;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBricksBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhousePart;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhousePartBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.KilnMode;
import io.github.dirnei.mc.servus.brewery.brewhouse.KilnTray;
import io.github.dirnei.mc.servus.brewery.brewhouse.TrayLook;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.delicacies.firebox.DelicaciesCapabilities;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Brewhouse forms from its ten parts with the carrot, unforms when one goes, burns fuel only for work, dries malt
 * pale or dark on its kiln trays and brews wort in its kettle only over a hot fire; by hand and through capabilities.
 */
public final class BrewhouseGameTests {

    /** The master brick; the Brewhouse faces north, so its columns run west and its rows south. */
    private static final BlockPos MASTER = new BlockPos(6, 1, 3);
    private static final int KILN_TIME = 600;
    private static final int KILN_DARK_TIME = 600;
    private static final int BREW_TIME = 400;

    private BrewhouseGameTests() {
    }

    // ---------- forming ----------

    @ServusGameTest(timeoutTicks = 20)
    public static void carrotFormsTheBrewhouse(GameTestHelper helper) {
        place(helper, true);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), MASTER, Direction.NORTH);
        for (Cell cell : Cell.values()) {
            BlockState state = helper.getBlockState(at(cell));
            helper.assertTrue(state.getValue(BrewhousePartBlock.FORMED), cell + " should be formed");
            helper.assertTrue(state.getValue(BrewhousePartBlock.FACING) == Direction.NORTH, cell + " should face north");
            BrewhousePart part = ((BrewhousePartBlock) state.getBlock()).part(state);
            helper.assertTrue(part.cell() == cell, cell + " should take its cell, got " + part);
        }
        helper.assertTrue(helper.getLevel().getCapability(DelicaciesCapabilities.FIREBOX, helper.absolutePos(at(Cell.KILN_BACK)), null) != null,
                "every brick offers the firebox");
        helper.assertTrue(helper.getLevel().getCapability(DelicaciesCapabilities.FIREBOX, helper.absolutePos(at(Cell.COPPER_BACK_LEFT)), null) == null,
                "kettle blocks offer no firebox");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void clickOnAKettleBlockForms(GameTestHelper helper) {
        place(helper, true);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), at(Cell.COPPER_FRONT_RIGHT), Direction.NORTH);
        helper.assertTrue(helper.getBlockState(MASTER).getValue(BrewhousePartBlock.FORMED), "the Brewhouse forms from a kettle block");
        helper.assertTrue(master(helper).isMaster(), "the front left brick is the master");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void missingKettleIsRefused(GameTestHelper helper) {
        place(helper, false);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), MASTER, Direction.NORTH);
        for (Cell cell : Cell.values()) {
            if (cell.kind() == BrewhouseLayout.Kind.BRICK) {
                helper.assertFalse(helper.getBlockState(at(cell)).getValue(BrewhousePartBlock.FORMED), cell + " should stay loose");
            }
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void breakingAPartUnformsAndDrops(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 5));
        master.kettle().insertInputs(new ItemStack(ServusBreweryItems.PALE_MALT_GRIST.get(), 3));
        tray(helper, Cell.KILN_FRONT).spread(new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 7));
        helper.getLevel().destroyBlock(helper.absolutePos(at(Cell.KILN_BACK)), true);
        for (Cell cell : Cell.values()) {
            if (cell != Cell.KILN_BACK) {
                helper.assertFalse(helper.getBlockState(at(cell)).getValue(BrewhousePartBlock.FORMED), cell + " should be loose again");
            }
        }
        helper.assertItemEntityCountIs(Items.COAL, MASTER, 2, 5);
        helper.assertItemEntityCountIs(ServusBreweryItems.PALE_MALT_GRIST.get(), MASTER, 2, 3);
        helper.assertItemEntityCountIs(ServusBreweryItems.GREEN_MALT.get(), MASTER, 2, 7);
        helper.assertItemEntityCountIs(ServusBreweryItems.BREWHOUSE_BRICKS.get(), at(Cell.KILN_BACK), 2, 1);
        helper.succeed();
    }

    // ---------- fire ----------

    @ServusGameTest(timeoutTicks = 20)
    public static void fuelThroughTheBricks(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        ItemView underKettle = items(helper, Cell.KETTLE_BACK_RIGHT);
        helper.assertTrue(underKettle.insertItem(new ItemStack(Items.CHARCOAL, 4), false).isEmpty(), "charcoal goes in under the kettle");
        helper.assertTrue(master.fuelStack().is(Items.CHARCOAL) && master.fuelStack().getCount() == 4, "the charcoal is fuel");
        helper.assertTrue(underKettle.insertItem(new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "cobblestone is no fuel");
        helper.assertTrue(items(helper, Cell.KILN_FRONT).insertItem(new ItemStack(Items.COAL), false).getCount() == 1,
                "a kiln tray takes no fuel");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void noWorkNoFire(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 3));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(master.fuelStack().getCount() == 3, "no coal should burn without work");
            helper.assertFalse(master.hot(), "the Brewhouse should stay cold");
            helper.assertFalse(helper.getBlockState(MASTER).getValue(BrewhouseBricksBlock.LIT), "the doors should stay dark");
            helper.succeed();
        });
    }

    // ---------- kiln ----------

    @ServusGameTest(timeoutTicks = 100)
    public static void kilnDriesGreenMaltToPale(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        items(helper, Cell.KILN_FRONT).insertItem(new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 16), false);
        helper.assertTrue(look(helper, Cell.KILN_FRONT) == TrayLook.GREEN, "the tray shows green malt");
        setProgress(helper, master, "Tray0Progress", KILN_TIME - 5);
        helper.succeedWhen(() -> {
            ItemStack stack = tray(helper, Cell.KILN_FRONT).stack();
            helper.assertTrue(stack.is(ServusBreweryItems.PALE_MALT.get()) && stack.getCount() == 16, "16 Pale Malt expected, got " + stack);
            helper.assertTrue(look(helper, Cell.KILN_FRONT) == TrayLook.PALE, "the tray shows pale malt");
            helper.assertTrue(helper.getBlockState(MASTER).getValue(BrewhouseBricksBlock.LIT), "the fire burns");
        });
    }

    @ServusGameTest(timeoutTicks = 120)
    public static void kilnDarkModeMakesDarkMalt(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GameTestSteps.useCarrot(helper, player, at(Cell.KILN_BACK), Direction.UP);
        KilnTray tray = tray(helper, Cell.KILN_BACK);
        helper.assertTrue(tray.mode() == KilnMode.DARK, "the carrot sets the tray to dark mode");
        items(helper, Cell.KILN_BACK).insertItem(new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 16), false);
        setProgress(helper, master, "Tray1Progress", KILN_TIME);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(tray.stack().is(ServusBreweryItems.GREEN_MALT.get()), "after the pale time the malt is still green");
            setProgress(helper, master, "Tray1Progress", KILN_TIME + KILN_DARK_TIME - 5);
            helper.succeedWhen(() -> {
                ItemStack stack = tray.stack();
                helper.assertTrue(stack.is(ServusBreweryItems.DARK_MALT.get()) && stack.getCount() == 16, "16 Dark Malt expected, got " + stack);
                helper.assertTrue(look(helper, Cell.KILN_BACK) == TrayLook.DARK, "the tray shows dark malt");
            });
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void kilnDriesWheatMaltInDarkMode(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        tray(helper, Cell.KILN_FRONT).toggleMode();
        items(helper, Cell.KILN_FRONT).insertItem(new ItemStack(ServusBreweryItems.GREEN_WHEAT_MALT.get(), 8), false);
        setProgress(helper, master, "Tray0Progress", KILN_TIME - 5);
        helper.succeedWhen(() -> {
            ItemStack stack = tray(helper, Cell.KILN_FRONT).stack();
            helper.assertTrue(stack.is(ServusBreweryItems.WHEAT_MALT.get()) && stack.getCount() == 8, "8 Wheat Malt expected, got " + stack);
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void kilnPipesTakeOnlyDriedMalt(GameTestHelper helper) {
        formed(helper);
        ItemView pipe = items(helper, Cell.KILN_FRONT);
        helper.assertTrue(pipe.insertItem(new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 8), false).isEmpty(), "green malt goes in");
        helper.assertTrue(pipe.insertItem(new ItemStack(ServusBreweryItems.PALE_MALT_GRIST.get()), false).getCount() == 1,
                "grist does not go on a tray");
        helper.assertTrue(pipe.extractItem(0, 64, false).isEmpty(), "green malt does not come out");
        tray(helper, Cell.KILN_FRONT).slot().setStackInSlot(0, new ItemStack(ServusBreweryItems.PALE_MALT.get(), 8));
        ItemStack taken = pipe.extractItem(0, 64, false);
        helper.assertTrue(taken.is(ServusBreweryItems.PALE_MALT.get()) && taken.getCount() == 8, "pale malt comes out, got " + taken);
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void emptyHandTakesTheTray(GameTestHelper helper) {
        formed(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        click(helper, player, at(Cell.KILN_FRONT), new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 5));
        helper.assertTrue(tray(helper, Cell.KILN_FRONT).stack().getCount() == 5, "green malt goes on the tray by hand");
        tray(helper, Cell.KILN_FRONT).slot().setStackInSlot(0, new ItemStack(ServusBreweryItems.PALE_MALT.get(), 5));
        click(helper, player, at(Cell.KILN_FRONT), ItemStack.EMPTY);
        helper.assertTrue(tray(helper, Cell.KILN_FRONT).stack().isEmpty(), "the tray is empty");
        helper.assertTrue(player.getInventory().countItem(ServusBreweryItems.PALE_MALT.get()) == 5, "the player has the pale malt");
        helper.succeed();
    }

    // ---------- kettle ----------

    @ServusGameTest(timeoutTicks = 200)
    public static void kettleBrewsOnlyWhileHot(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        fillPaleRecipe(helper);
        helper.runAfterDelay(60, () -> {
            BrewKettle kettle = master.kettle();
            helper.assertTrue(kettle.wort().isEmpty() && kettle.progress() == 0, "nothing brews without fire");
            helper.assertFalse(helper.getBlockState(at(Cell.COPPER_FRONT_LEFT)).getValue(BrewKettleBlock.BREWING), "no steam without fire");
            master.addFuel(new ItemStack(Items.COAL, 2));
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(helper.getBlockState(at(Cell.COPPER_FRONT_LEFT)).getValue(BrewKettleBlock.BREWING), "steam while brewing");
                setProgress(helper, master, "KettleProgress", BREW_TIME - 5);
                helper.succeedWhen(() -> assertPaleWortBrewed(helper, master.kettle()));
            });
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void kettlePipesInAndOut(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        fillPaleRecipe(helper);
        ItemView pipe = items(helper, Cell.COPPER_BACK_RIGHT);
        helper.assertTrue(pipe.insertItem(new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "the kettle refuses cobblestone");
        helper.assertTrue(pipe.extractItem(0, 64, false).isEmpty(), "nothing comes out of the kettle's items");
        FluidView tank = fluids(helper, Cell.COPPER_BACK_RIGHT);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1000), false) == 0, "the kettle refuses lava");
        helper.assertTrue(tank.drain(new FluidStack(Fluids.WATER, 1000), false).isEmpty(), "water never comes out");
        setProgress(helper, master, "KettleProgress", BREW_TIME - 5);
        helper.succeedWhen(() -> {
            assertPaleWortBrewed(helper, master.kettle());
            FluidStack drained = tank.drain(1000, false);
            helper.assertTrue(drained.is(BreweryFluids.PALE_WORT.source().get()) && drained.getAmount() == 1000,
                    "the pipe drains the Pale Wort, got " + drained.getAmount());
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void kettleBucketsByHand(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        click(helper, player, at(Cell.COPPER_FRONT_LEFT), new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(master.kettle().water().getAmount() == 1000, "the water bucket fills the kettle");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player gets the empty bucket back");
        click(helper, player, at(Cell.COPPER_FRONT_LEFT), new ItemStack(ServusBreweryItems.PALE_MALT_GRIST.get(), 4));
        click(helper, player, at(Cell.COPPER_FRONT_LEFT), new ItemStack(ServusBreweryItems.HOPS.get(), 1));
        setProgress(helper, master, "KettleProgress", BREW_TIME - 5);
        helper.succeedWhen(() -> {
            assertPaleWortBrewed(helper, master.kettle());
            click(helper, player, at(Cell.COPPER_FRONT_LEFT), new ItemStack(Items.BUCKET));
            helper.assertTrue(player.getInventory().countItem(BreweryFluids.PALE_WORT.bucket().get()) == 1, "an empty bucket takes Pale Wort");
        });
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void strayItemBlocksTheBrew(GameTestHelper helper) {
        BrewhouseBlockEntity master = formed(helper);
        master.addFuel(new ItemStack(Items.COAL, 2));
        fillPaleRecipe(helper);
        master.kettle().insertInputs(new ItemStack(ServusBreweryItems.DARK_MALT_GRIST.get()));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(master.kettle().progress() == 0 && master.kettle().wort().isEmpty(), "a stray item blocks the brew");
            helper.assertTrue(master.fuelStack().getCount() == 2, "no coal burns for a blocked brew");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void recipesAreShipped(GameTestHelper helper) {
        for (String id : new String[] {"brewing/pale_wort", "brewing/dark_wort", "brewing/wheat_wort", "kilning/pale_malt", "kilning/wheat_malt"}) {
            helper.assertTrue(ServusRecipes.byId(helper.getLevel(), Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, id)).isPresent(),
                    "Brewery should ship " + id);
        }
        helper.succeed();
    }

    // ---------- fixtures ----------

    private static BlockPos at(Cell cell) {
        return MASTER.west(cell.column()).south(cell.row()).above(cell.layer());
    }

    /** Places the six bricks and, if {@code kettle}, the four kettle blocks, all loose. */
    private static void place(GameTestHelper helper, boolean kettle) {
        for (Cell cell : Cell.values()) {
            if (cell.kind() == BrewhouseLayout.Kind.BRICK) {
                helper.setBlock(at(cell), ServusBreweryBlocks.BREWHOUSE_BRICKS.get());
            } else if (kettle) {
                helper.setBlock(at(cell), ServusBreweryBlocks.BREW_KETTLE.get());
            }
        }
    }

    /** A formed Brewhouse facing north; returns its master. */
    private static BrewhouseBlockEntity formed(GameTestHelper helper) {
        place(helper, true);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), MASTER, Direction.NORTH);
        helper.assertTrue(helper.getBlockState(MASTER).getValue(BrewhousePartBlock.FORMED), "the test Brewhouse forms");
        return master(helper);
    }

    private static BrewhouseBlockEntity master(GameTestHelper helper) {
        return helper.getBlockEntity(MASTER, BrewhouseBlockEntity.class);
    }

    private static KilnTray tray(GameTestHelper helper, Cell kiln) {
        return helper.getBlockEntity(at(kiln), BrewhouseBlockEntity.class).tray();
    }

    private static TrayLook look(GameTestHelper helper, Cell kiln) {
        return helper.getBlockState(at(kiln)).getValue(BrewhouseBricksBlock.TRAY);
    }

    private static ItemView items(GameTestHelper helper, Cell cell) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at(cell)), Direction.UP));
    }

    private static FluidView fluids(GameTestHelper helper, Cell cell) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(at(cell)), Direction.UP));
    }

    /** Water, four Pale Malt Grist and one Hops into the kettle through its capabilities. */
    private static void fillPaleRecipe(GameTestHelper helper) {
        helper.assertTrue(fluids(helper, Cell.COPPER_FRONT_LEFT).fill(new FluidStack(Fluids.WATER, 1000), false) == 1000, "water goes in");
        ItemView pipe = items(helper, Cell.COPPER_FRONT_RIGHT);
        helper.assertTrue(pipe.insertItem(new ItemStack(ServusBreweryItems.PALE_MALT_GRIST.get(), 4), false).isEmpty(), "grist goes in");
        helper.assertTrue(pipe.insertItem(new ItemStack(ServusBreweryItems.HOPS.get(), 1), false).isEmpty(), "hops go in");
    }

    private static void assertPaleWortBrewed(GameTestHelper helper, BrewKettle kettle) {
        FluidStack wort = kettle.wort();
        helper.assertTrue(wort.is(BreweryFluids.PALE_WORT.source().get()) && wort.getAmount() == 1000, "1000 mB Pale Wort expected");
        helper.assertTrue(kettle.water().isEmpty(), "the water is used");
        helper.assertTrue(kettle.stacks().stream().allMatch(ItemStack::isEmpty), "the grist and hops are used");
    }

    /** Sets a saved progress of the master by saving, changing and loading it, as a long wait would. */
    private static void setProgress(GameTestHelper helper, BrewhouseBlockEntity master, String key, int progress) {
        CompoundTag saved = master.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putInt(key, progress);
        master.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
    }

    /** A right click on the top of the block at {@code pos}, the item use first, then the empty hand use. */
    private static InteractionResult click(GameTestHelper helper, Player player, BlockPos pos, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        BlockState state = helper.getLevel().getBlockState(absolute);
        if (held.isEmpty()) {
            return state.useWithoutItem(helper.getLevel(), player, hit);
        }
        return state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }
}
