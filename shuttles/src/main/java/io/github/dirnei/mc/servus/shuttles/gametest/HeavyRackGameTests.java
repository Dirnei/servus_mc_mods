package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.RackClimbing;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Heavy Duty Rack: placing, wood, sections, its menu and climbing its sides. */
public final class HeavyRackGameTests {

    private static final BlockPos STACK_BASE = new BlockPos(10, 1, 10);
    private static final BlockPos WOOD_RACK = new BlockPos(2, 1, 2);
    private static final int WOOD_Z = 4;
    private static final int SEC_Z = 11;
    private static final int UP_Z = 11;
    private static final int SM_Z = 6;
    private static final BlockPos FG_RACK = new BlockPos(2, 1, 11);
    private static final BlockPos NR_RACK = new BlockPos(1, 1, 1);
    private static final BlockPos CLIMB_RACK = new BlockPos(6, 2, 6);

    private HeavyRackGameTests() {
    }

    /** A rack placed on top of a rack takes the lower rack's facing, whatever way the player looks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackOnRackCopiesFacing(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, STACK_BASE, Direction.EAST);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        BlockPos above = helper.absolutePos(STACK_BASE.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(above), Direction.UP, above, false);
        ItemStack rackItem = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        InteractionResult placed = rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.runAfterDelay(2, () -> {
            BlockState top = helper.getBlockState(STACK_BASE.above());
            helper.assertTrue(top.getBlock() instanceof HeavyRackBlock, "a rack should have been placed on top, got " + top.getBlock() + " result " + placed);
            helper.assertTrue(top.getValue(HeavyRackBlock.FACING) == Direction.EAST, "the top rack should face east like the one below");
            helper.succeed();
        });
    }

    /** Assembles the rack recipe from a grid whose middle row holds the given planks. */
    private static ItemStack craftRack(GameTestHelper helper, Item left, Item middle, Item right) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                iron, ItemStack.EMPTY, iron,
                new ItemStack(left), new ItemStack(middle), new ItemStack(right),
                iron, ItemStack.EMPTY, iron));
        RecipeHolder<?> holder = helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "heavy_rack"))).orElseThrow();
        @SuppressWarnings("unchecked")
        CraftingRecipe recipe = ((RecipeHolder<CraftingRecipe>) holder).value();
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the rack recipe should match the grid");
        return recipe.assemble(input);
    }

    private static Identifier idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    /** One craft gives three racks; they remember spruce, the majority, and the middle of three different woods. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rackRecipePicksWood(GameTestHelper helper) {
        ItemStack spruce = craftRack(helper, Items.SPRUCE_PLANKS, Items.SPRUCE_PLANKS, Items.SPRUCE_PLANKS);
        helper.assertTrue(spruce.is(ServusShuttlesItems.HEAVY_RACK.get()), "the result is a rack");
        helper.assertTrue(spruce.getCount() == 3, "one craft gives three racks, got " + spruce.getCount());
        helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(spruce.get(ServusShuttlesComponents.RACK_WOOD.get())), "three spruce planks give spruce");
        ItemStack majority = craftRack(helper, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.OAK_PLANKS);
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(majority.get(ServusShuttlesComponents.RACK_WOOD.get())), "oak, spruce, oak gives oak");
        ItemStack middle = craftRack(helper, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS, Items.OAK_PLANKS);
        helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(middle.get(ServusShuttlesComponents.RACK_WOOD.get())), "birch, spruce, oak gives spruce");
        helper.succeed();
    }

    /** A placed spruce rack keeps spruce, and its drops and dismantled item carry it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackKeepsWood(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(WOOD_RACK);
        ItemStack rackItem = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        rackItem.set(ServusShuttlesComponents.RACK_WOOD.get(), idOf(Blocks.SPRUCE_PLANKS));
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.runAfterDelay(2, () -> {
            HeavyRackBlockEntity rack = helper.getBlockEntity(WOOD_RACK, HeavyRackBlockEntity.class);
            helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(rack.wood()), "the placed rack remembers spruce, had " + rack.wood());
            List<ItemStack> drops = Block.getDrops(helper.getBlockState(WOOD_RACK), helper.getLevel(), absolute, rack);
            helper.assertTrue(drops.size() == 1 && HeavyRackBlockEntity.woodOf(drops.get(0)).equals(idOf(Blocks.SPRUCE_PLANKS)),
                    "the dropped rack carries spruce, drops " + drops);
            ItemStack dismantled = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
            BlockEntityItems.saveToItem(rack, dismantled, helper.getLevel().registryAccess());
            helper.assertTrue(HeavyRackBlockEntity.woodOf(dismantled).equals(idOf(Blocks.SPRUCE_PLANKS)), "the dismantled rack carries spruce");
            helper.succeed();
        });
    }

    /** A rack without a stored wood shows oak. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rackWithoutWoodIsOak(GameTestHelper helper) {
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, WOOD_RACK, Direction.NORTH);
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(rack.wood()), "a rack without wood is oak");
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(HeavyRackBlockEntity.woodOf(new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get()))),
                "a rack item without wood is oak");
        helper.succeed();
    }

    /** Places a rack of that wood by hand with the mock player looking south, so it faces north unless a rack is below. */
    private static void placeWoodRack(GameTestHelper helper, Player player, BlockPos pos, Block planks) {
        BlockPos absolute = helper.absolutePos(pos);
        ItemStack rackItem = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        rackItem.set(ServusShuttlesComponents.RACK_WOOD.get(), idOf(planks));
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    /** Spruce beside oak stays apart, spruce beside spruce joins, and a rack on top copies the facing whatever its wood. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackJoinsOnlySameWood(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        BlockPos oak = new BlockPos(1, 1, WOOD_Z);
        BlockPos spruce = new BlockPos(2, 1, WOOD_Z);
        BlockPos spruce2 = new BlockPos(3, 1, WOOD_Z);
        BlockPos top = new BlockPos(1, 2, WOOD_Z);
        placeWoodRack(helper, player, oak, Blocks.OAK_PLANKS);
        placeWoodRack(helper, player, spruce, Blocks.SPRUCE_PLANKS);
        placeWoodRack(helper, player, spruce2, Blocks.SPRUCE_PLANKS);
        placeWoodRack(helper, player, top, Blocks.SPRUCE_PLANKS);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(spruce).getValue(HeavyRackBlock.FACING) == Direction.NORTH, "racks face north");
            helper.assertTrue(!helper.getBlockState(spruce).getValue(HeavyRackBlock.LEFT), "spruce does not join the oak beside it");
            helper.assertTrue(!helper.getBlockState(oak).getValue(HeavyRackBlock.RIGHT), "oak does not join the spruce beside it");
            helper.assertTrue(helper.getBlockState(spruce).getValue(HeavyRackBlock.RIGHT)
                    && helper.getBlockState(spruce2).getValue(HeavyRackBlock.LEFT), "two spruce racks join");
            helper.assertTrue(helper.getBlockState(top).getValue(HeavyRackBlock.FACING) == Direction.NORTH, "the stacked rack copies the facing");
            helper.succeed();
        });
    }

    /** Places a rack through its item with the mock player looking south, so the rack faces north. */
    private static void placeRackByHand(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack rackItem = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static boolean joined(GameTestHelper helper, int x, BooleanProperty side) {
        return helper.getBlockState(new BlockPos(x, 1, SEC_Z)).getValue(side);
    }

    /** Sections form in placement order and never move: three join, a fourth at either end stands alone. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackSectionsStayPut(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 4; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "bays 4 and 5 join");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "bays 5 and 6 join");
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 6, HeavyRackBlock.RIGHT), "the section has posts at both ends");

        placeRackByHand(helper, player, new BlockPos(7, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 6, HeavyRackBlock.RIGHT) && !joined(helper, 7, HeavyRackBlock.LEFT), "a fourth bay on the right stands alone");
        placeRackByHand(helper, player, new BlockPos(3, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 3, HeavyRackBlock.RIGHT), "a fourth bay on the left stands alone");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "the first three are unchanged");

        placeRackByHand(helper, player, new BlockPos(8, 1, SEC_Z));
        helper.assertTrue(joined(helper, 7, HeavyRackBlock.RIGHT) && joined(helper, 8, HeavyRackBlock.LEFT), "a fifth bay joins the lone fourth");
        helper.assertTrue(!joined(helper, 7, HeavyRackBlock.LEFT), "the fourth still does not join the full section");
        helper.succeed();
    }

    /** An upper row placed shifted by one splits above the same post as the row below. */
    @ServusGameTest(timeoutTicks = 100)
    public static void upperRowFollowsLowerSections(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 1; x <= 5; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, UP_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT), "lower row splits after the third bay");
        for (int x = 2; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 2, UP_Z));
        }
        BlockState up2 = helper.getBlockState(new BlockPos(2, 2, UP_Z));
        BlockState up3 = helper.getBlockState(new BlockPos(3, 2, UP_Z));
        BlockState up4 = helper.getBlockState(new BlockPos(4, 2, UP_Z));
        BlockState up5 = helper.getBlockState(new BlockPos(5, 2, UP_Z));
        BlockState up6 = helper.getBlockState(new BlockPos(6, 2, UP_Z));
        helper.assertTrue(up2.getValue(HeavyRackBlock.RIGHT) && up3.getValue(HeavyRackBlock.LEFT), "upper 2 and 3 join like below");
        helper.assertTrue(!up3.getValue(HeavyRackBlock.RIGHT) && !up4.getValue(HeavyRackBlock.LEFT), "upper row splits above the lower post");
        helper.assertTrue(up4.getValue(HeavyRackBlock.RIGHT) && up5.getValue(HeavyRackBlock.LEFT), "upper 4 and 5 join like below");
        helper.assertTrue(!up5.getValue(HeavyRackBlock.RIGHT) && !up6.getValue(HeavyRackBlock.LEFT), "upper 6 has no rack below and the rack below 5 ends its section");
        helper.succeed();
    }

    /** Four in a row, a gap, one more, then the gap filled: the filler joins the lone bays on both sides. */
    @ServusGameTest(timeoutTicks = 100)
    public static void gapFillerJoinsBothLoneBays(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x : new int[] {1, 2, 3, 4, 6, 5}) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT) && !joined(helper, 4, HeavyRackBlock.LEFT), "the fourth bay stands apart from the first three");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "filler joins the fourth");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "filler joins the sixth");
        helper.succeed();
    }

    /** Places a row of racks by hand from x {@code first} to {@code last}; they face north and join. */
    private static Player rackRow(GameTestHelper helper, int first, int last) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = first; x <= last; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SM_Z));
        }
        // Stand in front of the row so the menu's reach check holds.
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(first + 1, 1, SM_Z - 1))));
        return player;
    }

    private static HeavyRackBlockEntity rackAt(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 1, SM_Z), HeavyRackBlockEntity.class);
    }

    /** The middle bay of a section of three opens all three bays, left to right, and shift click spills into the next bay. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackSectionSharesMenu(GameTestHelper helper) {
        Player player = rackRow(helper, 2, 4);
        HeavyRackBlockEntity left = rackAt(helper, 2);
        HeavyRackBlockEntity middle = rackAt(helper, 3);
        HeavyRackBlockEntity right = rackAt(helper, 4);
        int bay = left.items().getSlots();
        for (int slot = 0; slot < bay; slot++) {
            left.items().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
        }
        middle.items().setStackInSlot(0, new ItemStack(Items.DIRT, 5));
        right.items().setStackInSlot(0, new ItemStack(Items.SAND, 7));

        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), middle);
        helper.assertTrue(menu.rackSlots() == 3 * bay, "a section of three shows " + 3 * bay + " slots, had " + menu.rackSlots());
        helper.assertTrue(menu.rows() == (3 * bay + 8) / 9, "the GUI has a row per nine slots, had " + menu.rows());
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.STONE), "the left bay comes first");
        helper.assertTrue(menu.getSlot(bay).getItem().is(Items.DIRT), "the middle bay follows the left one");
        helper.assertTrue(menu.getSlot(2 * bay).getItem().is(Items.SAND), "the right bay comes last");
        HeavyRackMenu fromLeft = HeavyRackMenu.forSection(2, player.getInventory(), left);
        helper.assertTrue(fromLeft.bays().equals(menu.bays()), "every bay of the section opens the same bays");
        helper.assertTrue(menu.stillValid(player), "the menu is valid while the section stands");

        player.getInventory().setItem(0, new ItemStack(Items.APPLE, 30));
        menu.quickMoveStack(player, menu.rackSlots() + 27);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the apples left the hotbar");
        helper.assertTrue(middle.items().getStackInSlot(1).is(Items.APPLE) && middle.items().getStackInSlot(1).getCount() == 30,
                "with the left bay full, the apples land in the middle bay, had " + middle.items().getStackInSlot(1));
        helper.succeed();
    }

    /** Breaking the right bay of a section spills only its items; the left bay keeps its own. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackBayBreaksAlone(GameTestHelper helper) {
        rackRow(helper, 2, 3);
        rackAt(helper, 2).items().setStackInSlot(0, new ItemStack(Items.DIRT, 5));
        rackAt(helper, 3).items().setStackInSlot(0, new ItemStack(Items.SAND, 7));
        helper.destroyBlock(new BlockPos(3, 1, SM_Z));
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(Items.SAND, new BlockPos(3, 1, SM_Z), 2.0);
            helper.assertItemEntityNotPresent(Items.DIRT, new BlockPos(3, 1, SM_Z), 4.0);
            ItemStack kept = rackAt(helper, 2).items().getStackInSlot(0);
            helper.assertTrue(kept.is(Items.DIRT) && kept.getCount() == 5, "the left bay keeps its dirt, had " + kept);
            helper.succeed();
        });
    }

    /** The section menu turns invalid once a bay of the section is removed. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackMenuClosesWhenSectionChanges(GameTestHelper helper) {
        Player player = rackRow(helper, 2, 4);
        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), rackAt(helper, 2));
        helper.assertTrue(menu.stillValid(player), "the menu is valid while the section stands");
        helper.setBlock(new BlockPos(4, 1, SM_Z), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!menu.stillValid(player), "the menu turns invalid once a bay is gone");
        helper.succeed();
    }

    /** With a rack in hand, the side and the front of a rack both take a new rack. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackPlacesAgainstRackSideAndFront(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, FG_RACK, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        useRackOn(helper, player, FG_RACK, Direction.EAST);
        helper.assertTrue(helper.getBlockState(FG_RACK.east()).getBlock() instanceof HeavyRackBlock, "clicking the side places a rack beside it");
        useRackOn(helper, player, FG_RACK, Direction.NORTH);
        helper.assertTrue(helper.getBlockState(FG_RACK.north()).getBlock() instanceof HeavyRackBlock, "clicking the front places a rack in front of it");
        helper.succeed();
    }

    /** Uses a rack item on the given face of the block at pos, through the block's own interaction. */
    private static void useRackOn(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
        ItemStack rackItem = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        BlockState state = helper.getBlockState(pos);
        InteractionResult result = state.useItemOn(rackItem, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == InteractionResult.PASS) {
            rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        } else if (result == InteractionResult.TRY_WITH_EMPTY_HAND) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }

    /** The carrot leaves a rack's facing and joins alone. */
    @ServusGameTest(timeoutTicks = 100)
    public static void carrotDoesNotTurnRack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        placeRackByHand(helper, player, NR_RACK);
        placeRackByHand(helper, player, NR_RACK.east());
        GameTestSteps.useCarrot(helper, player, NR_RACK, Direction.UP);
        BlockState after = helper.getBlockState(NR_RACK);
        helper.assertTrue(after.getBlock() instanceof HeavyRackBlock, "the rack is still there");
        helper.assertTrue(after.getValue(HeavyRackBlock.FACING) == Direction.NORTH, "the carrot must not turn the rack");
        helper.assertTrue(after.getValue(HeavyRackBlock.RIGHT), "the join to the neighbour survives the click");
        helper.succeed();
    }

    /** A player pressed against a rack side climbs or slides without fall damage; the front stays a wall. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackSideClimbsAndFrontDoesNot(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, CLIMB_RACK, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos rack = helper.absolutePos(CLIMB_RACK);
        double gravity = player.getGravity();

        // West side: the player's east face rests on the rack's west face, feet a little above the rack bottom.
        player.setPos(rack.getX() - 0.3, rack.getY() + 0.3, rack.getZ() + 0.5);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = false;
        RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 0.0F, "hanging on the side clears the fall distance");
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.15) < 1.0E-9, "hanging slides at the ladder speed, got " + player.getDeltaMovement().y);

        player.horizontalCollision = true;
        RackClimbing.tick(player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - (0.2 - gravity) * 0.98) < 1.0E-9, "pushing climbs at the ladder speed, got " + player.getDeltaMovement().y);

        // Front: the player's south face rests on the rack's north face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() - 0.3);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = true;
        RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the front is not climbable");
        helper.assertTrue(player.getDeltaMovement().y == -0.5, "the front leaves the motion alone");

        // Back: the player's north face rests on the rack's south face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() + 1.3);
        player.horizontalCollision = true;
        RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the back is not climbable");
        helper.succeed();
    }
}
