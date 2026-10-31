package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.data.UpkeepBook;
import io.github.dirnei.mc.servus.foundation.data.UpkeepRequest;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/** The Machine Adapter on a vanilla furnace: recipes, forwarding, upkeep routing and output. */
public final class AdapterGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos MACHINE = new BlockPos(3, 1, 3);
    private static final BlockPos ADAPTER = new BlockPos(3, 1, 4);

    private AdapterGameTests() {
    }

    /** A furnace facing east and an adapter south of it, facing north toward it. */
    private static MachineAdapterBlockEntity onFurnace(GameTestHelper helper) {
        helper.setBlock(MACHINE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST));
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(ADAPTER, MachineAdapterBlockEntity.class);
    }

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper) {
        return helper.getBlockEntity(MACHINE, AbstractFurnaceBlockEntity.class);
    }

    /** The furnace profile gives smelting recipes, one stone from one cobblestone in 200 ticks among them. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterReportsSmelting(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        helper.assertTrue(adapter.profile().isPresent(), "the furnace should have a profile");
        List<ProductionRecipe> recipes = adapter.production().recipes();
        Optional<ProductionRecipe> stone = recipes.stream()
                .filter(recipe -> recipe.inputs().get(0).is(Items.COBBLESTONE) && recipe.outputs().get(0).is(Items.STONE))
                .findFirst();
        helper.assertTrue(stone.isPresent(), "stone from cobblestone should be a recipe, got " + recipes.size() + " recipes");
        helper.assertTrue(stone.get().ticks() == 200 && stone.get().inputs().get(0).getCount() == 1,
                "one cobblestone in 200 ticks, got " + stone.get());
        helper.assertTrue(recipes.stream().noneMatch(recipe -> recipe.inputs().stream().anyMatch(input -> input.is(Items.COAL))),
                "no recipe has coal as an input");
        helper.succeed();
    }

    /** Items put into the adapter land in the furnace's ingredient slot; the cooking item is the active recipe. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterForwardsIngredients(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        ItemStack left = new ItemView(adapter.itemHandler()).insertItemStacked(new ItemStack(Items.COBBLESTONE, 8), false);
        helper.assertTrue(left.isEmpty(), "all cobblestone should be taken, left " + left);
        helper.assertTrue(furnace(helper).getItem(0).is(Items.COBBLESTONE) && furnace(helper).getItem(0).getCount() == 8,
                "8 cobblestone in the ingredient slot, got " + furnace(helper).getItem(0));
        Optional<Identifier> active = adapter.production().active();
        helper.assertTrue(active.isPresent() && active.get().getPath().contains("stone"), "the cobblestone recipe is active, got " + active);
        // Expected upkeep still finds its way while the ingredient slot is taken.
        adapter.setUpkeepItem(new ItemStack(Items.COAL));
        adapter.expect(3);
        ItemStack coalLeft = new ItemView(adapter.itemHandler()).insertItemStacked(new ItemStack(Items.COAL, 3), false);
        helper.assertTrue(coalLeft.isEmpty() && furnace(helper).getItem(1).getCount() == 3,
                "3 coal in the fuel slot beside the cobblestone, left " + coalLeft);
        helper.succeed();
    }

    /** Coal the adapter waits for goes to the fuel slot; coal it does not wait for goes to the ingredients. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterRoutesExpectedUpkeep(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        adapter.setUpkeepItem(new ItemStack(Items.COAL));
        adapter.expect(4);
        new ItemView(adapter.itemHandler()).insertItemStacked(new ItemStack(Items.COAL, 6), false);
        helper.assertTrue(furnace(helper).getItem(1).is(Items.COAL) && furnace(helper).getItem(1).getCount() == 4,
                "4 expected coal in the fuel slot, got " + furnace(helper).getItem(1));
        helper.assertTrue(furnace(helper).getItem(0).getCount() == 2, "the 2 unexpected coal go to the ingredient slot");
        helper.assertTrue(adapter.expected() == 0 && adapter.upkeepCount() == 4, "nothing expected any more, 4 in the machine");
        helper.succeed();
    }

    /** Stone in the furnace's result slot can be taken out through the adapter. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterOffersOutput(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        furnace(helper).setItem(2, new ItemStack(Items.STONE, 8));
        ItemView handler = new ItemView(adapter.itemHandler());
        int taken = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            taken += handler.extractItem(slot, 64, false).getCount();
        }
        helper.assertTrue(taken == 8 && furnace(helper).getItem(2).isEmpty(), "8 stone should come out, got " + taken);
        helper.succeed();
    }

    /** Without a profile the adapter reports nothing and takes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterOnChestOffersNothing(GameTestHelper helper) {
        helper.setBlock(MACHINE, Blocks.CHEST.defaultBlockState());
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.NORTH));
        MachineAdapterBlockEntity adapter = helper.getBlockEntity(ADAPTER, MachineAdapterBlockEntity.class);
        helper.assertTrue(adapter.production().recipes().isEmpty(), "no recipes without a profile");
        ItemStack left = new ItemView(adapter.itemHandler()).insertItemStacked(new ItemStack(Items.COBBLESTONE, 8), false);
        helper.assertTrue(left.getCount() == 8, "the cobblestone is refused");
        helper.succeed();
    }

    /** Like a chest, any held item, the carrot included, falls through to the adapter GUI. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterOpensWithAnyItem(GameTestHelper helper) {
        onFurnace(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(ADAPTER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (ItemStack held : List.of(new ItemStack(Items.COBBLESTONE), new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()), ItemStack.EMPTY)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            InteractionResult result = helper.getBlockState(ADAPTER)
                    .useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.TRY_WITH_EMPTY_HAND,
                    "holding " + held + " should reach the GUI, got " + result);
        }
        helper.succeed();
    }

    /** The adapter saves its open upkeep request with its block. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterUpkeepRequestSurvivesSaveLoad(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        UpkeepRequest request = new UpkeepRequest(helper.absolutePos(ADAPTER.south()).asLong(), 7L, 12, 4, 999L);
        adapter.upkeep().upkeepRequest(request);
        CompoundTag saved = adapter.saveWithoutMetadata(helper.getLevel().registryAccess());
        adapter.upkeep().upkeepRequest(null);
        adapter.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(request.equals(adapter.upkeep().upkeepRequest()), "the request is read back, got " + adapter.upkeep().upkeepRequest());
        helper.succeed();
    }

    /** With the furnace profile's batch of 8 the adapter asks nothing at 10 of 16 coal, and for 8 at 8 coal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void adapterWaitsForBatch(GameTestHelper helper) {
        MachineAdapterBlockEntity adapter = onFurnace(helper);
        adapter.setUpkeepItem(new ItemStack(Items.COAL));
        adapter.setUpkeepLevel(16);
        UpkeepHolder upkeep = adapter.upkeep();
        helper.assertTrue(upkeep.upkeepBatch() == 8, "the furnace profile's batch is 8, got " + upkeep.upkeepBatch());
        furnace(helper).setItem(1, new ItemStack(Items.COAL, 10));
        int atTen = ask(upkeep);
        helper.assertTrue(atTen == 0, "nothing is asked at 10 coal, got " + atTen);
        furnace(helper).setItem(1, new ItemStack(Items.COAL, 8));
        int atEight = ask(upkeep);
        helper.assertTrue(atEight == 8, "8 are asked at 8 coal, got " + atEight);
        helper.succeed();
    }

    /** What the data network's upkeep check would ask for the holder without an open request. */
    private static int ask(UpkeepHolder upkeep) {
        return UpkeepBook.step(null, upkeep.upkeepCount(), upkeep.upkeepLevel(), upkeep.upkeepBatch(),
                UpkeepBook.TicketState.CLOSED, 0).ask();
    }
}
