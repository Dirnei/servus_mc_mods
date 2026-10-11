package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldHeadLook;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The Field Head look: wood, facing, ripe bar state and what clients learn (m10-field-head-look). */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FieldLookGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final int RAIL_Z = 5;
    private static final BlockPos HEAD = new BlockPos(2, 1, RAIL_Z);
    private static final BlockPos AT = new BlockPos(5, 1, 5);
    private static final ResourceLocation BIRCH = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS);

    private FieldLookGameTests() {
    }

    // Helpers

    /** Farm rails from x 3 to 5 at the rail row, the head west of them, four wheat plots north of the rails. */
    private static FieldHeadBlockEntity wheatField(GameTestHelper helper) {
        for (int x = 3; x <= 6; x++) {
            BlockPos plot = new BlockPos(x, 1, RAIL_Z + 1);
            helper.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState());
            helper.setBlock(plot, ((CropBlock) Blocks.WHEAT).getStateForAge(2));
        }
        for (int x = 3; x <= 5; x++) {
            BlockPos rail = new BlockPos(x, 1, RAIL_Z);
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            helper.setBlock(rail, Block.updateFromNeighbourShapes(ServusStorageBlocks.FARM_RAIL.get().defaultBlockState(),
                    helper.getLevel(), helper.absolutePos(rail)));
        }
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        return helper.getBlockEntity(HEAD);
    }

    /** Grows the wheat at that x of the plot row to its last stage, the way random ticks do. */
    private static void ripen(GameTestHelper helper, int x) {
        BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(7);
        BlockPos absolute = helper.absolutePos(new BlockPos(x, 1, RAIL_Z + 1));
        helper.getLevel().setBlock(absolute, ripe, Block.UPDATE_CLIENTS);
        CommonHooks.fireCropGrowPost(helper.getLevel(), absolute, ripe);
    }

    private static int ripeState(GameTestHelper helper) {
        return helper.getBlockState(HEAD).getValue(FieldHeadBlock.RIPE);
    }

    /** Sets the head's threshold to that percent in whole steps. */
    private static void threshold(FieldHeadBlockEntity head, int percent) {
        head.stepHarvestPercent((percent - head.harvestPercent()) / 10);
    }

    private static Player playerHolding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void placeOnFloor(GameTestHelper helper, Player player, ItemStack stack) {
        BlockPos floor = helper.absolutePos(AT.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    // Wood and facing

    /** A player looking north places a head whose front faces south, toward the player. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void placementFacesPlayer(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
        ItemStack stack = new ItemStack(ServusStorageItems.FIELD_HEAD.get());
        Player player = playerHolding(helper, stack);
        player.setYRot(180.0F);
        placeOnFloor(helper, player, stack);
        helper.assertBlockPresent(ServusStorageBlocks.FIELD_HEAD.get(), AT);
        Direction facing = helper.getBlockState(AT).getValue(FieldHeadBlock.FACING);
        helper.assertTrue(facing == Direction.SOUTH, "the front should face the player in the south, got " + facing);
        helper.succeed();
    }

    /** A birch head places birch boards and drops a birch head. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void woodSurvivesPlaceAndBreak(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
        ItemStack stack = new ItemStack(ServusStorageItems.FIELD_HEAD.get());
        stack.set(WoodContent.WOOD.get(), BIRCH);
        placeOnFloor(helper, playerHolding(helper, stack), stack);
        helper.assertBlockPresent(ServusStorageBlocks.FIELD_HEAD.get(), AT);
        BlockPos pos = helper.absolutePos(AT);
        WoodHolder placed = (WoodHolder) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(BIRCH.equals(placed.woodSlot().wood()), "the placed head is birch, got " + placed.woodSlot().wood());
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(AT), helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos),
                null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusStorageItems.FIELD_HEAD.get())
                && BIRCH.equals(drops.get(0).get(WoodContent.WOOD.get())), "the drop keeps birch, got " + drops);
        helper.succeed();
    }

    /** The recipe takes planks and a Fluid Tank, and the head remembers the planks. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void recipeRecordsWood(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "field_head"))
                .orElseThrow(() -> new AssertionError("no field_head recipe"));
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        List<ItemStack> grid = new ArrayList<>();
        for (Item item : new Item[] {
                Items.IRON_INGOT, ServusLogisticsItems.FLUID_TANK.get(), Items.IRON_INGOT,
                Items.BIRCH_PLANKS, Items.COMPOSTER, Items.BIRCH_PLANKS,
                Items.BIRCH_PLANKS, ServusStorageItems.FARM_RAIL.get(), Items.BIRCH_PLANKS}) {
            grid.add(new ItemStack(item));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the recipe should take birch planks");
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ServusStorageItems.FIELD_HEAD.get()) && BIRCH.equals(result.get(WoodContent.WOOD.get())),
                "a birch Field Head, got " + result + " " + result.get(WoodContent.WOOD.get()));
        helper.succeed();
    }

    // Ripe bar

    /** The bar starts empty, turns gold part way with one of four ripe at 50 percent, and green at two of four. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ripeStateFollowsRipeness(GameTestHelper helper) {
        FieldHeadBlockEntity head = wheatField(helper);
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            threshold(head, 50);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(head.plantedCount() == 4, "four planted plots, got " + head.plantedCount());
                helper.assertTrue(ripeState(helper) == FieldHeadLook.EMPTY, "nothing ripe: empty bar, got " + ripeState(helper));
                ripen(helper, 3);
                helper.runAfterDelay(2, () -> {
                    int gold = ripeState(helper);
                    helper.assertTrue(gold == FieldHeadLook.ripeStep(4, 1, 50, false) && gold > FieldHeadLook.EMPTY
                            && gold < FieldHeadLook.READY, "one of four ripe: a gold step, got " + gold);
                    ripen(helper, 4);
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(head.harvestDue(), "two of four ripe at 50 percent is due");
                        helper.assertTrue(ripeState(helper) == FieldHeadLook.READY, "due: green bar, got " + ripeState(helper));
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** Lowering the threshold to the ripe share turns the bar green without any crop changing. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ripeStateFollowsThreshold(GameTestHelper helper) {
        FieldHeadBlockEntity head = wheatField(helper);
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            threshold(head, 50);
            ripen(helper, 5);
            helper.runAfterDelay(2, () -> {
                int gold = ripeState(helper);
                helper.assertTrue(gold > FieldHeadLook.EMPTY && gold < FieldHeadLook.READY, "a quarter ripe at 50 percent is gold, got " + gold);
                threshold(head, 20);
                helper.runAfterDelay(1, () -> {
                    helper.assertTrue(ripeState(helper) == FieldHeadLook.READY, "a quarter ripe at 20 percent is green, got " + ripeState(helper));
                    head.setHarvestOnly(false);
                    head.setCrop(ItemStack.EMPTY);
                    helper.runAfterDelay(1, () -> {
                        int noCrop = ripeState(helper);
                        helper.assertTrue(noCrop > FieldHeadLook.EMPTY && noCrop < FieldHeadLook.READY,
                                "without a crop nothing is due, the bar stays gold, got " + noCrop);
                        helper.succeed();
                    });
                });
            });
        });
    }

    // Client sync

    /** The update tag carries the water level in steps, the crop and the mode. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void updateTagCarriesWaterAndCrop(GameTestHelper helper) {
        helper.setBlock(AT, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        FieldHeadBlockEntity head = helper.getBlockEntity(AT);
        CompoundTag empty = head.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(empty.getByte("WaterLevel") == 0, "an empty head sends level 0");
        int half = head.capacity() / 2;
        head.fluidHandler().fill(new FluidStack(Fluids.WATER, half), IFluidHandler.FluidAction.EXECUTE);
        head.setCrop(new ItemStack(Items.CARROT));
        CompoundTag tag = head.getUpdateTag(helper.getLevel().registryAccess());
        int expected = FieldHeadLook.waterLevel(half, head.capacity());
        helper.assertTrue(tag.getByte("WaterLevel") == expected && expected == FieldHeadLook.WATER_LEVELS / 2,
                "half full sends half the levels, got " + tag.getByte("WaterLevel"));
        ItemStackHandler crop = new ItemStackHandler(1);
        crop.deserializeNBT(helper.getLevel().registryAccess(), tag.getCompound("Crop"));
        helper.assertTrue(crop.getStackInSlot(0).is(Items.CARROT), "the crop goes to clients, got " + crop.getStackInSlot(0));
        helper.assertTrue(!tag.getBoolean("HarvestOnly"), "harvest only is off");
        helper.succeed();
    }
}
