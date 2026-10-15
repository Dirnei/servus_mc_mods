package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.HomunculusData;
import io.github.dirnei.mc.servus.alchemy.homunculus.ReceptaculumBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlock;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Receptaculum and the homunculus in its Vas: placement, growth on Elixir, harvesting, gifts, freeing and automation. */
public final class HomunculusGameTests {

    private static final BlockPos RECEPTACULUM = new BlockPos(5, 1, 5);
    private static final BlockPos VAS = RECEPTACULUM.above();
    private static final int IRON_STEPS = 20;
    /** Two work intervals of the shipped data with room to spare. */
    private static final int TWO_HARVESTS = 260;

    private HomunculusGameTests() {
    }

    static ReceptaculumBlockEntity receptaculum(GameTestHelper helper) {
        helper.setBlock(RECEPTACULUM, ServusAlchemyBlocks.RECEPTACULUM.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(RECEPTACULUM, ReceptaculumBlockEntity.class);
    }

    /** A Receptaculum with a Vas on top whose homunculus is grown or new. */
    static VasBlockEntity vas(GameTestHelper helper, boolean grown) {
        receptaculum(helper);
        helper.setBlock(VAS, ServusAlchemyBlocks.VAS_HERMETICUM.get().defaultBlockState());
        VasBlockEntity vas = helper.getBlockEntity(VAS, VasBlockEntity.class);
        if (grown) {
            vas.life().load(HomunculusData.INSTANCE.settings().growTicks(), true, false, 0);
        }
        return vas;
    }

    private static ReceptaculumBlockEntity receptaculumOf(GameTestHelper helper) {
        return helper.getBlockEntity(RECEPTACULUM, ReceptaculumBlockEntity.class);
    }

    /** A bound, ripe iron Hollow at {@code pos} with {@code stored} Raw Iron in its store. */
    static GnomeHollowBlockEntity ironHollow(GameTestHelper helper, BlockPos pos, int stored) {
        UUID team = UUID.randomUUID();
        HollowGameTests.finishVillach(helper.getLevel(), team);
        helper.setBlock(pos, ServusAlchemyBlocks.GNOME_HOLLOW.get().defaultBlockState());
        GnomeHollowBlockEntity hollow = helper.getBlockEntity(pos, GnomeHollowBlockEntity.class);
        hollow.setTeam(team);
        hollow.growth().bound = true;
        hollow.seed(HollowGameTests.seed("iron"));
        hollow.growth().ripeness = IRON_STEPS;
        hollow.store().load(stored);
        return hollow;
    }

    private static int yieldCount(ReceptaculumBlockEntity receptaculum, Item item) {
        int total = 0;
        for (int slot = 0; slot < ReceptaculumBlockEntity.YIELD_SLOTS; slot++) {
            ItemStack stack = receptaculum.inventory().getStackInSlot(slot);
            total += stack.is(item) ? stack.getCount() : 0;
        }
        return total;
    }

    private static void elixir(ReceptaculumBlockEntity receptaculum, int count) {
        receptaculum.inventory().setStackInSlot(ReceptaculumBlockEntity.SLOT_ELIXIR, new ItemStack(ElementalSight.ELIXIR.get(), count));
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** The Sealed Flask places the Vas on a Receptaculum and nowhere else. */
    @ServusGameTest(timeoutTicks = 20)
    public static void vasOnlyOnReceptaculum(GameTestHelper helper) {
        BlockPos stone = new BlockPos(2, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        ItemStack flasks = new ItemStack(ServusAlchemyItems.SEALED_FLASK.get(), 2);
        Player player = holding(helper, flasks);
        GameTestSteps.useOn(helper, player, flasks, stone, Direction.UP);
        helper.assertBlockPresent(Blocks.AIR, stone.above());
        helper.assertTrue(flasks.getCount() == 2, "the flask stays in the hand");
        receptaculum(helper);
        GameTestSteps.useOn(helper, player, flasks, RECEPTACULUM, Direction.UP);
        helper.assertBlockPresent(ServusAlchemyBlocks.VAS_HERMETICUM.get(), VAS);
        helper.assertTrue(flasks.getCount() == 1, "one flask is placed");
        helper.succeed();
    }

    /** Breaking the Receptaculum breaks the Vas, which drops its Sealed Flask. */
    @ServusGameTest(timeoutTicks = 20)
    public static void vasBreaksWithReceptaculum(GameTestHelper helper) {
        vas(helper, true);
        helper.getLevel().destroyBlock(helper.absolutePos(RECEPTACULUM), true);
        helper.assertBlockPresent(Blocks.AIR, VAS);
        int flasks = 0;
        for (ItemEntity drop : GameTestSteps.drops(helper, VAS, 2)) {
            flasks += drop.getItem().is(ServusAlchemyItems.SEALED_FLASK.get()) ? drop.getItem().getCount() : 0;
        }
        helper.assertTrue(flasks == 1, "one Sealed Flask should drop, got " + flasks);
        helper.succeed();
    }

    /** With an Elixir in the slot the homunculus drinks it and grows up. */
    @ServusGameTest(timeoutTicks = 100)
    public static void growsWithElixir(GameTestHelper helper) {
        VasBlockEntity vas = vas(helper, false);
        int growTicks = HomunculusData.INSTANCE.settings().growTicks();
        vas.life().load(growTicks - 20, false, false, 0);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        elixir(receptaculum, 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(vas.life().grown(), "the homunculus should be grown");
            helper.assertTrue(!receptaculum.hasElixir(), "the Elixir is drunk");
        });
    }

    /** Without Elixir it keeps its growth and waits. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pausesWithoutElixir(GameTestHelper helper) {
        VasBlockEntity vas = vas(helper, false);
        int growTicks = HomunculusData.INSTANCE.settings().growTicks();
        vas.life().load(growTicks - 20, false, false, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(!vas.life().grown(), "it must not grow without Elixir");
            helper.assertTrue(vas.life().growth() == growTicks - 20, "its growth is kept, got " + vas.life().growth());
            helper.succeed();
        });
    }

    /** It empties a Hollow two blocks away and leaves one three blocks away and one a level up alone. */
    @ServusGameTest(timeoutTicks = TWO_HARVESTS)
    public static void harvestsHollowsInReachOnly(GameTestHelper helper) {
        vas(helper, true);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        elixir(receptaculum, 1);
        ironHollow(helper, RECEPTACULUM.east(2), 5);
        GnomeHollowBlockEntity far = ironHollow(helper, RECEPTACULUM.east(3), 5);
        GnomeHollowBlockEntity upper = ironHollow(helper, RECEPTACULUM.offset(-2, 1, 0), 5);
        helper.succeedWhen(() -> {
            int iron = yieldCount(receptaculum, Items.RAW_IRON);
            helper.assertTrue(iron >= 5 && iron < 10, "the near Hollow's iron should arrive, got " + iron);
            helper.assertTrue(far.store().count() >= 5, "the Hollow three blocks away keeps its iron");
            helper.assertTrue(upper.store().count() >= 5, "the Hollow a level up keeps its iron");
        });
    }

    /** A grown homunculus without Elixir takes nothing. */
    @ServusGameTest(timeoutTicks = TWO_HARVESTS + 20)
    public static void stopsWithoutElixir(GameTestHelper helper) {
        vas(helper, true);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        GnomeHollowBlockEntity hollow = ironHollow(helper, RECEPTACULUM.west(2), 5);
        helper.runAfterDelay(TWO_HARVESTS, () -> {
            helper.assertTrue(yieldCount(receptaculum, Items.RAW_IRON) == 0, "nothing is taken without Elixir");
            helper.assertTrue(hollow.store().count() >= 5, "the Hollow keeps its iron");
            helper.succeed();
        });
    }

    /** A gnome without a boost gets one cookie from the gift slot. */
    @ServusGameTest(timeoutTicks = TWO_HARVESTS)
    public static void deliversGifts(GameTestHelper helper) {
        vas(helper, true);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        elixir(receptaculum, 1);
        receptaculum.inventory().setStackInSlot(ReceptaculumBlockEntity.GIFT_FIRST, new ItemStack(Items.COOKIE, 3));
        GnomeHollowBlockEntity hollow = ironHollow(helper, RECEPTACULUM.north(2), 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.boostTicksLeft() > 0, "the gnome should be boosted");
            int cookies = receptaculum.inventory().getStackInSlot(ReceptaculumBlockEntity.GIFT_FIRST).getCount();
            helper.assertTrue(cookies == 2, "one cookie is given, " + cookies + " left");
        });
    }

    /** A gift the gnome does not like stays in its slot. */
    @ServusGameTest(timeoutTicks = TWO_HARVESTS + 20)
    public static void dislikedGiftStays(GameTestHelper helper) {
        vas(helper, true);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        elixir(receptaculum, 1);
        receptaculum.inventory().setStackInSlot(ReceptaculumBlockEntity.GIFT_FIRST, new ItemStack(Items.COBBLESTONE, 4));
        GnomeHollowBlockEntity hollow = ironHollow(helper, RECEPTACULUM.south(2), 0);
        helper.runAfterDelay(TWO_HARVESTS, () -> {
            helper.assertTrue(hollow.boostTicksLeft() == 0, "no boost from cobblestone");
            helper.assertTrue(receptaculum.inventory().getStackInSlot(ReceptaculumBlockEntity.GIFT_FIRST).getCount() == 4, "the cobblestone stays");
            helper.succeed();
        });
    }

    /** A Nether Star frees a grown homunculus: the seal breaks, no Elixir is needed and every item taken arrives twice. */
    @ServusGameTest(timeoutTicks = TWO_HARVESTS)
    public static void netherStarFreesAndDoubles(GameTestHelper helper) {
        VasBlockEntity vas = vas(helper, true);
        ReceptaculumBlockEntity receptaculum = receptaculumOf(helper);
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        helper.useBlock(VAS, holding(helper, star));
        helper.assertTrue(star.isEmpty(), "the star is used up");
        helper.assertBlockProperty(VAS, VasBlock.FREED, true);
        helper.assertTrue(vas.life().freed(), "the homunculus is freed");
        ironHollow(helper, RECEPTACULUM.east(), 5);
        helper.succeedWhen(() -> {
            int iron = yieldCount(receptaculum, Items.RAW_IRON);
            helper.assertTrue(iron >= 10 && iron % 2 == 0, "every item should arrive twice, got " + iron);
        });
    }

    /** A Nether Star does nothing to a homunculus that is still growing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void starDoesNothingWhileGrowing(GameTestHelper helper) {
        VasBlockEntity vas = vas(helper, false);
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        helper.useBlock(VAS, holding(helper, star));
        helper.assertTrue(star.getCount() == 1, "the star stays in the hand");
        helper.assertBlockProperty(VAS, VasBlock.FREED, false);
        helper.assertTrue(!vas.life().freed(), "nothing is freed");
        helper.succeed();
    }

    /** Automation takes the yield out and brings Elixir and gifts into their slots. */
    @ServusGameTest(timeoutTicks = 20)
    public static void automationReachesTheSlots(GameTestHelper helper) {
        ReceptaculumBlockEntity receptaculum = receptaculum(helper);
        receptaculum.inventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON, 5));
        ItemView side = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(RECEPTACULUM), Direction.WEST));
        helper.assertTrue(side != null, "the Receptaculum should expose items");
        ItemStack taken = side.extractItem(0, 64, false);
        helper.assertTrue(taken.is(Items.RAW_IRON) && taken.getCount() == 5, "the yield comes out, got " + taken);
        helper.assertTrue(side.insertItem(new ItemStack(ElementalSight.ELIXIR.get()), false).isEmpty(), "the Elixir goes in");
        helper.assertTrue(receptaculum.hasElixir(), "into the Elixir slot");
        helper.assertTrue(side.insertItem(new ItemStack(Items.COOKIE, 2), false).isEmpty(), "the cookies go in");
        helper.assertTrue(receptaculum.inventory().getStackInSlot(ReceptaculumBlockEntity.GIFT_FIRST).is(Items.COOKIE), "into a gift slot");
        for (int slot = 0; slot < ReceptaculumBlockEntity.YIELD_SLOTS; slot++) {
            helper.assertTrue(receptaculum.inventory().getStackInSlot(slot).isEmpty(), "nothing goes into the yield slots");
        }
        helper.succeed();
    }

    /** The Elixir slot takes only the Elixir, the gift slots everything else, the yield slots nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void slotRules(GameTestHelper helper) {
        ReceptaculumBlockEntity receptaculum = receptaculum(helper);
        ItemStack elixir = new ItemStack(ElementalSight.ELIXIR.get());
        ItemStack cookie = new ItemStack(Items.COOKIE);
        helper.assertTrue(receptaculum.isItemValid(ReceptaculumBlockEntity.SLOT_ELIXIR, elixir), "the Elixir slot takes the Elixir");
        helper.assertTrue(!receptaculum.isItemValid(ReceptaculumBlockEntity.SLOT_ELIXIR, cookie), "the Elixir slot refuses a cookie");
        helper.assertTrue(receptaculum.isItemValid(ReceptaculumBlockEntity.GIFT_FIRST, cookie), "a gift slot takes a cookie");
        helper.assertTrue(!receptaculum.isItemValid(ReceptaculumBlockEntity.GIFT_FIRST, elixir), "a gift slot refuses the Elixir");
        helper.assertTrue(!receptaculum.isItemValid(0, new ItemStack(Items.RAW_IRON)), "the yield slots take nothing");
        helper.succeed();
    }

    /** With a grown homunculus the Receptaculum offers the yield of the Hollows in reach; without one, nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void exposesProduction(GameTestHelper helper) {
        VasBlockEntity vas = vas(helper, false);
        ironHollow(helper, RECEPTACULUM.east(2), 0);
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION,
                helper.absolutePos(RECEPTACULUM), Direction.UP);
        helper.assertTrue(production != null, "the Receptaculum should be a producer");
        helper.assertTrue(production.recipes().isEmpty(), "a growing homunculus offers nothing");
        vas.life().load(HomunculusData.INSTANCE.settings().growTicks(), true, false, 0);
        List<ProductionRecipe> recipes = production.recipes();
        helper.assertTrue(recipes.size() == 1, "one vein in reach, got " + recipes.size());
        helper.assertTrue(recipes.getFirst().inputs().isEmpty() && recipes.getFirst().outputs().getFirst().is(Items.RAW_IRON),
                "nothing in, Raw Iron out");
        helper.assertTrue(production.active().isEmpty(), "without Elixir it works on nothing");
        elixir(receptaculumOf(helper), 1);
        helper.assertTrue(production.active().equals(Optional.of(recipes.getFirst().id())), "with Elixir the vein is active");
        helper.succeed();
    }

    /** Deepslate Bricks, a Chest, Ignis Perpetuus and Bricks make the Receptaculum at a crafting table. */
    @ServusGameTest(timeoutTicks = 20)
    public static void receptaculumRecipeShipped(GameTestHelper helper) {
        ItemStack deepslate = new ItemStack(Items.DEEPSLATE_BRICKS);
        ItemStack bricks = new ItemStack(Items.BRICKS);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(deepslate, new ItemStack(Items.CHEST), deepslate,
                deepslate, new ItemStack(ServusAlchemyItems.IGNIS_PERPETUUS.get()), deepslate,
                bricks, bricks, bricks));
        ItemStack result = ServusRecipes.craftingFor(helper.getLevel(), grid)
                .map(holder -> holder.value().assemble(grid)).orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ServusAlchemyItems.RECEPTACULUM.get()) && result.getCount() == 1, "the grid should make one Receptaculum, got " + result);
        helper.succeed();
    }

    /** Crying Obsidian with Salt and Mercury makes a Sealed Flask on a Rubedo Athanor, not on a Citrinitas one. */
    @ServusGameTest(timeoutTicks = 600)
    public static void sealedFlaskNeedsRubedo(GameTestHelper helper) {
        AthanorBlockEntity athanor = AlchemyGameTests.athanor(helper);
        AlchemyGameTests.saltRing(helper);
        AlchemyGameTests.amethystRing(helper);
        athanor.checkTier();
        helper.assertTrue(athanor.tier() == 2, "the amethyst ring makes Citrinitas, got " + athanor.tier());
        athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, new ItemStack(Items.CRYING_OBSIDIAN));
        helper.runAfterDelay(2, () -> AlchemyGameTests.pour(helper, new Principles(250, 0, 500)));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).isEmpty(), "no Sealed Flask at Citrinitas");
            AlchemyGameTests.rubedoPillars(helper);
            athanor.checkTier();
            helper.assertTrue(athanor.tier() == 3, "the pillars make Rubedo, got " + athanor.tier());
            helper.succeedWhen(() -> helper.assertTrue(
                    athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT).is(ServusAlchemyItems.SEALED_FLASK.get()),
                    "a Sealed Flask should be made"));
        });
    }
}
