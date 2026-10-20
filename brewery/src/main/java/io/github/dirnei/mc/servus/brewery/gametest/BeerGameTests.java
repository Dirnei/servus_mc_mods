package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.BeerDrunkEvent;
import io.github.dirnei.mc.servus.brewery.beer.CaskBlock;
import io.github.dirnei.mc.servus.brewery.beer.CaskBlockEntity;
import io.github.dirnei.mc.servus.brewery.beer.FermentingRecipe;
import io.github.dirnei.mc.servus.brewery.beer.FermentingVatBlock;
import io.github.dirnei.mc.servus.brewery.beer.FermentingVatBlockEntity;
import io.github.dirnei.mc.servus.brewery.beer.VatContent;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;

/** Yeast, the Fermenting Vat, the Cask and its tap, the glasses and drinking. */
public final class BeerGameTests {

    private static final BlockPos VAT = new BlockPos(2, 1, 2);
    /** The Cask faces north, so its tap points to lower z. */
    private static final BlockPos CASK = new BlockPos(6, 1, 6);
    private static final int BATCH = 1000;
    private static final int GLASS = 500;

    private BeerGameTests() {
    }

    // ---------- the vat ----------

    @ServusGameTest(timeoutTicks = 300)
    public static void vatFermentsWortWithYeastAndReturnsIt(GameTestHelper helper) {
        FermentingVatBlockEntity vat = vat(helper);
        helper.assertTrue(vatFluids(helper).fill(new FluidStack(wort(BreweryFluids.PALE_WORT), BATCH), false) == BATCH,
                "a pipe should fill the Pale Wort");
        helper.assertTrue(vatItems(helper).insertItem(new ItemStack(ServusBreweryItems.YEAST.get()), false).isEmpty(),
                "a hopper should put the Yeast in");
        helper.assertTrue(vat.isFermenting(), "wort and yeast should start a batch");
        helper.assertTrue(content(helper) == VatContent.FERMENTING, "the vat should show foam");
        moveStartBack(helper, vat, recipe(helper, vat).time());
        helper.succeedWhen(() -> {
            helper.assertTrue(vat.contents().is(beer(BreweryFluids.HELLES)) && vat.contents().getAmount() == BATCH,
                    "the Pale Wort should have become 1000 mB Helles, got " + vat.contents().getFluid() + " " + vat.contents().getAmount());
            helper.assertTrue(content(helper) == VatContent.BEER, "the vat should show beer");
            ItemStack yeast = vatItems(helper).extractItem(0, 1, true);
            helper.assertTrue(yeast.is(ServusBreweryItems.YEAST.get()) && yeast.getCount() == 1, "the Yeast should come back out, got " + yeast);
        });
    }

    @ServusGameTest
    public static void noYeastNoBeer(GameTestHelper helper) {
        FermentingVatBlockEntity vat = vat(helper);
        vatFluids(helper).fill(new FluidStack(wort(BreweryFluids.DARK_WORT), BATCH), false);
        vat.check();
        helper.assertTrue(vat.fermentation().isEmpty(), "no batch should run without yeast");
        helper.assertTrue(vat.contents().is(wort(BreweryFluids.DARK_WORT)), "the Dark Wort should stay");
        helper.assertTrue(content(helper) == VatContent.WORT, "the vat should show wort");
        helper.succeed();
    }

    @ServusGameTest
    public static void notDoneBeforeItsTime(GameTestHelper helper) {
        FermentingVatBlockEntity vat = filledVat(helper, BreweryFluids.WHEAT_WORT);
        moveStartBack(helper, vat, recipe(helper, vat).time() - 200);
        vat.check();
        helper.assertTrue(vat.isFermenting() && vat.contents().is(wort(BreweryFluids.WHEAT_WORT)),
                "the batch should still ferment before its time");
        helper.succeed();
    }

    @ServusGameTest
    public static void vatKeepsWortAndYeastWhileFermenting(GameTestHelper helper) {
        vat(helper);
        vatFluids(helper).fill(new FluidStack(wort(BreweryFluids.PALE_WORT), BATCH), false);
        helper.assertTrue(vatFluids(helper).drain(BATCH, false).isEmpty(), "wort should not come out");
        vatItems(helper).insertItem(new ItemStack(ServusBreweryItems.YEAST.get()), false);
        helper.assertTrue(vatItems(helper).extractItem(0, 1, false).isEmpty(), "the Yeast should stay in while it ferments");
        helper.assertTrue(vatFluids(helper).fill(new FluidStack(wort(BreweryFluids.PALE_WORT), BATCH), false) == 0,
                "no wort should go in while it ferments");
        helper.assertTrue(vatItems(helper).insertItem(new ItemStack(ServusBreweryItems.YEAST.get()), false).getCount() == 1,
                "a second Yeast should not go in");
        helper.succeed();
    }

    @ServusGameTest
    public static void vatBucketsByHand(GameTestHelper helper) {
        FermentingVatBlockEntity vat = vat(helper);
        Player player = holding(helper, new ItemStack(BreweryFluids.PALE_WORT.bucket().get()));
        helper.useBlock(VAT, player);
        helper.assertTrue(vat.contents().getAmount() == BATCH, "the wort bucket should fill the vat");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player should get the empty bucket back");
        helper.useBlock(VAT, player);
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && vat.contents().getAmount() == BATCH,
                "an empty bucket should not take wort");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusBreweryItems.YEAST.get(), 3));
        helper.useBlock(VAT, player);
        helper.assertTrue(vat.yeast().getCount() == 1 && player.getMainHandItem().getCount() == 2, "one Yeast should go in by hand");
        moveStartBack(helper, vat, recipe(helper, vat).time());
        vat.check();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.useBlock(VAT, player);
        helper.assertTrue(player.getInventory().countItem(BreweryFluids.HELLES.bucket().get()) == 1, "an empty bucket should take Helles");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        helper.useBlock(VAT, player);
        helper.assertTrue(vat.yeast().isEmpty() && player.getInventory().countItem(ServusBreweryItems.YEAST.get()) == 1,
                "sneaking with an empty hand should take the Yeast back");
        helper.succeed();
    }

    @ServusGameTest
    public static void recipesAreShipped(GameTestHelper helper) {
        for (String path : List.of("fermenting/helles", "fermenting/dunkles", "fermenting/weissbier",
                "tapping/helles", "tapping/dunkles", "tapping/weissbier", "yeast", "beer_glass", "weissbier_glass",
                "fermenting_vat", "cask")) {
            helper.assertTrue(ServusRecipes.byId(helper.getLevel(), Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, path)).isPresent(),
                    "Brewery should ship the recipe " + path);
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void yeastRecipe(GameTestHelper helper) {
        ItemStack water = new ItemStack(Items.POTION);
        water.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.SUGAR), water));
        ItemStack result = ServusRecipes.craftingFor(helper.getLevel(), input).orElseThrow().value().assemble(input);
        helper.assertTrue(result.is(ServusBreweryItems.YEAST.get()) && result.getCount() == 1, "Wheat, Sugar and a Water Bottle should make Yeast, got " + result);
        helper.succeed();
    }

    // ---------- the cask ----------

    @ServusGameTest
    public static void caskFillsAGlassOfHelles(GameTestHelper helper) {
        tapsInto(helper, BreweryFluids.HELLES, ServusBreweryItems.BEER_GLASS.get(), ServusBreweryItems.HELLES.get());
    }

    @ServusGameTest
    public static void caskFillsAGlassOfDunkles(GameTestHelper helper) {
        tapsInto(helper, BreweryFluids.DUNKLES, ServusBreweryItems.BEER_GLASS.get(), ServusBreweryItems.DUNKLES.get());
    }

    private static void tapsInto(GameTestHelper helper, BreweryFluids.BrewFluid beer, Item glass, Item full) {
        CaskBlockEntity cask = cask(helper, beer, BATCH);
        Player player = holding(helper, new ItemStack(glass));
        helper.useBlock(CASK, player, atTap(helper));
        helper.assertTrue(player.getMainHandItem().is(full), "the glass should be full, got " + player.getMainHandItem());
        helper.assertTrue(cask.beer().getAmount() == BATCH - GLASS, "one glass should leave 500 mB, got " + cask.beer().getAmount());
        helper.succeed();
    }

    @ServusGameTest
    public static void weissbierRefusesAPlainGlass(GameTestHelper helper) {
        CaskBlockEntity cask = cask(helper, BreweryFluids.WEISSBIER, BATCH);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.BEER_GLASS.get()));
        helper.useBlock(CASK, player, atTap(helper));
        helper.assertTrue(player.getMainHandItem().is(ServusBreweryItems.BEER_GLASS.get()), "a Beer Glass should stay empty");
        helper.assertTrue(cask.beer().getAmount() == BATCH, "the Weissbier should stay in the Cask");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusBreweryItems.WEISSBIER_GLASS.get(), 4));
        helper.useBlock(CASK, player, atTap(helper));
        helper.assertTrue(player.getMainHandItem().is(ServusBreweryItems.WEISSBIER_GLASS.get()) && player.getMainHandItem().getCount() == 3,
                "one Weissbier Glass of the stack should be used");
        helper.assertTrue(player.getInventory().countItem(ServusBreweryItems.WEISSBIER.get()) == 1, "the player should get a Glass of Weissbier");
        helper.assertTrue(cask.beer().getAmount() == BATCH - GLASS, "the Cask should hold 500 mB less");
        helper.succeed();
    }

    @ServusGameTest
    public static void weissbierGlassTakesOnlyWeissbier(GameTestHelper helper) {
        CaskBlockEntity cask = cask(helper, BreweryFluids.HELLES, BATCH);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.WEISSBIER_GLASS.get()));
        helper.useBlock(CASK, player, atTap(helper));
        helper.assertTrue(player.getMainHandItem().is(ServusBreweryItems.WEISSBIER_GLASS.get()) && cask.beer().getAmount() == BATCH,
                "a Weissbier Glass should not take Helles");
        helper.succeed();
    }

    @ServusGameTest
    public static void tooLittleBeerTapsNothing(GameTestHelper helper) {
        CaskBlockEntity cask = cask(helper, BreweryFluids.HELLES, GLASS - 100);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.BEER_GLASS.get()));
        helper.useBlock(CASK, player, atTap(helper));
        helper.assertTrue(player.getMainHandItem().is(ServusBreweryItems.BEER_GLASS.get()) && cask.beer().getAmount() == GLASS - 100,
                "less than a glass should tap nothing");
        helper.succeed();
    }

    @ServusGameTest
    public static void tappingOnlyAtTheFront(GameTestHelper helper) {
        CaskBlockEntity cask = cask(helper, BreweryFluids.HELLES, BATCH);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.BEER_GLASS.get()));
        BlockPos at = helper.absolutePos(CASK);
        helper.useBlock(CASK, player, new BlockHitResult(new Vec3(at.getX() + 15 / 16.0, at.getY() + 0.5, at.getZ() + 0.5), Direction.EAST, at, false));
        helper.assertTrue(player.getMainHandItem().is(ServusBreweryItems.BEER_GLASS.get()) && cask.beer().getAmount() == BATCH,
                "a glass on the side of the Cask should not be filled");
        helper.succeed();
    }

    @ServusGameTest
    public static void caskTakesOnlyBeer(GameTestHelper helper) {
        cask(helper, null, 0);
        FluidView pipe = caskFluids(helper);
        helper.assertTrue(pipe.fill(new FluidStack(wort(BreweryFluids.PALE_WORT), BATCH), false) == 0, "wort should not go into a Cask");
        helper.assertTrue(pipe.fill(new FluidStack(beer(BreweryFluids.DUNKLES), BATCH), false) == BATCH, "Dunkles should go in");
        helper.assertTrue(pipe.drain(GLASS, false).getAmount() == GLASS, "a pipe should take beer out");
        helper.succeed();
    }

    // ---------- glasses and drinking ----------

    @ServusGameTest
    public static void glassesStackTo16(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ServusBreweryItems.BEER_GLASS.get()).getMaxStackSize() == 16, "Beer Glasses should stack to 16");
        helper.assertTrue(new ItemStack(ServusBreweryItems.WEISSBIER_GLASS.get()).getMaxStackSize() == 16, "Weissbier Glasses should stack to 16");
        for (Item full : List.of(ServusBreweryItems.HELLES.get(), ServusBreweryItems.DUNKLES.get(), ServusBreweryItems.WEISSBIER.get())) {
            helper.assertTrue(new ItemStack(full).getMaxStackSize() == 1, "a full glass should not stack: " + full);
        }
        helper.succeed();
    }

    @ServusGameTest
    public static void drinkingReturnsTheGlass(GameTestHelper helper) {
        drinks(helper, ServusBreweryItems.HELLES.get(), ServusBreweryItems.BEER_GLASS.get());
        drinks(helper, ServusBreweryItems.WEISSBIER.get(), ServusBreweryItems.WEISSBIER_GLASS.get());
        helper.succeed();
    }

    private static void drinks(GameTestHelper helper, Item drink, Item glass) {
        Player player = holding(helper, new ItemStack(drink));
        player.getFoodData().setFoodLevel(10);
        AtomicInteger drunk = new AtomicInteger();
        NeoForge.EVENT_BUS.addListener(BeerDrunkEvent.class, event -> {
            if (event.getEntity() == player && event.getDrink().is(drink)) {
                drunk.incrementAndGet();
            }
        });
        ItemStack left = player.getMainHandItem().finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(left.is(glass) && left.getCount() == 1, "drinking " + drink + " should leave its glass, got " + left);
        helper.assertTrue(player.getFoodData().getFoodLevel() > 10, "drinking should restore some hunger");
        helper.assertTrue(drunk.get() == 1, "one BeerDrunkEvent should be posted, got " + drunk.get());
    }

    // ---------- fixtures ----------

    private static FermentingVatBlockEntity vat(GameTestHelper helper) {
        helper.setBlock(VAT, ServusBreweryBlocks.FERMENTING_VAT.get().defaultBlockState());
        return helper.getBlockEntity(VAT, FermentingVatBlockEntity.class);
    }

    private static FermentingVatBlockEntity filledVat(GameTestHelper helper, BreweryFluids.BrewFluid wort) {
        FermentingVatBlockEntity vat = vat(helper);
        vatFluids(helper).fill(new FluidStack(wort(wort), BATCH), false);
        vatItems(helper).insertItem(new ItemStack(ServusBreweryItems.YEAST.get()), false);
        return vat;
    }

    private static VatContent content(GameTestHelper helper) {
        return helper.getBlockState(VAT).getValue(FermentingVatBlock.CONTENT);
    }

    private static FermentingRecipe recipe(GameTestHelper helper, FermentingVatBlockEntity vat) {
        return ServusBreweryRecipes.fermenting(helper.getLevel(), new ItemStack(ServusBreweryItems.YEAST.get()), vat.contents()).orElseThrow();
    }

    /** Moves the running batch's start {@code ticks} into the past through its saved data. */
    private static void moveStartBack(GameTestHelper helper, FermentingVatBlockEntity vat, long ticks) {
        CompoundTag saved = vat.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putLong("Started", helper.getLevel().getGameTime() - ticks);
        vat.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
    }

    private static FluidView vatFluids(GameTestHelper helper) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(VAT), Direction.UP));
    }

    private static ItemView vatItems(GameTestHelper helper) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(VAT), Direction.UP));
    }

    /** A north facing Cask holding {@code amount} of {@code beer}, or empty when {@code beer} is null. */
    private static CaskBlockEntity cask(GameTestHelper helper, BreweryFluids.BrewFluid beer, int amount) {
        helper.setBlock(CASK, ServusBreweryBlocks.CASK.get().defaultBlockState().setValue(CaskBlock.FACING, Direction.NORTH));
        CaskBlockEntity cask = helper.getBlockEntity(CASK, CaskBlockEntity.class);
        if (beer != null) {
            caskFluids(helper).fill(new FluidStack(beer(beer), amount), false);
        }
        return cask;
    }

    private static FluidView caskFluids(GameTestHelper helper) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(CASK), Direction.EAST));
    }

    /** A click on the barrel front of the north facing Cask, where the tap is. */
    private static BlockHitResult atTap(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(CASK);
        return new BlockHitResult(new Vec3(at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 1 / 16.0), Direction.NORTH, at, false);
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static Fluid wort(BreweryFluids.BrewFluid wort) {
        return wort.source().get();
    }

    private static Fluid beer(BreweryFluids.BrewFluid beer) {
        return beer.source().get();
    }
}
