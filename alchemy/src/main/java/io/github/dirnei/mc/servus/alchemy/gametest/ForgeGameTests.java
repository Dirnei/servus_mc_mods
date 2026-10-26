package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.forge.SalamanderForgeBlockEntity;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Salamander Forge: smelting without fuel, always lit, a Foundation machine for hoppers and pipes, and a producer. */
public final class ForgeGameTests {

    private static final BlockPos FORGE = AlchemyGameTests.MACHINE.above();
    /** Two items at the shipped pace of 100 ticks each, with room to spare. */
    private static final int TWO_ITEMS = 300;

    private ForgeGameTests() {
    }

    /** A Forge facing north, so west is its right side and north its closed front. */
    static SalamanderForgeBlockEntity forge(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusAlchemyBlocks.SALAMANDER_FORGE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, SalamanderForgeBlockEntity.class);
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

    /** Two Raw Iron and no fuel at all become two Iron Ingots. */
    @ServusGameTest(timeoutTicks = TWO_ITEMS)
    public static void smeltsIronWithoutFuel(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 2));
        helper.succeedWhen(() -> {
            ItemStack output = forge.inventory().getStackInSlot(SalamanderForgeBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(output.is(Items.IRON_INGOT) && output.getCount() == 2, "two Iron Ingots should be made, got " + output);
            helper.assertTrue(forge.inventory().getStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT).isEmpty(), "the Raw Iron is used up");
        });
    }

    /** Only what a furnace takes goes in, through automation as through the menu's rule. */
    @ServusGameTest(timeoutTicks = 20)
    public static void refusesWhatDoesNotSmelt(GameTestHelper helper) {
        forge(helper, FORGE);
        ItemView side = items(helper, FORGE, Direction.WEST);
        helper.assertTrue(side.insertItem(new ItemStack(Items.COBBLESTONE_SLAB, 4), false).getCount() == 4, "slabs do not smelt");
        helper.assertTrue(side.insertItem(new ItemStack(Items.COAL, 4), false).getCount() == 4, "coal is no fuel here and does not smelt");
        helper.assertTrue(side.insertItem(new ItemStack(Items.RAW_IRON, 4), false).isEmpty(), "Raw Iron goes in");
        helper.succeed();
    }

    /** A full output stops the work and keeps the input. */
    @ServusGameTest(timeoutTicks = 60)
    public static void stopsWhenOutputIsFull(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 64));
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 3));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(forge.inventory().getStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT).getCount() == 3, "the Raw Iron stays");
            helper.assertTrue(forge.containerData().get(SalamanderForgeBlockEntity.DATA_PROGRESS) == 0, "no progress while the output is full");
            helper.succeed();
        });
    }

    /** Empty and idle, the Forge is lit, shows its full flame and lights its neighbours. */
    @ServusGameTest(timeoutTicks = 60)
    public static void alwaysLitAndGlows(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(FORGE).getValue(MachineBlock.LIT), "the Forge is lit while idle");
            helper.assertTrue(forge.containerData().get(SalamanderForgeBlockEntity.DATA_BURN_TIME) > 0, "the screen shows the flame");
            int light = helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(FORGE.east()));
            helper.assertTrue(light >= 12, "the block beside it should be lit, light " + light);
        });
    }

    /** A hopper above feeds the Forge and its ingots are pushed into the chest below. */
    @ServusGameTest(timeoutTicks = TWO_ITEMS)
    public static void hopperInChestOut(GameTestHelper helper) {
        BlockPos forgePos = FORGE.above();
        helper.setBlock(FORGE, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(FORGE, ChestBlockEntity.class);
        forge(helper, forgePos);
        helper.setBlock(forgePos.above(), Blocks.HOPPER.defaultBlockState());
        helper.getBlockEntity(forgePos.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.RAW_IRON, 2));
        helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.IRON_INGOT) == 2,
                "the chest below should hold 2 Iron Ingots, got " + count(chest, Items.IRON_INGOT)));
    }

    /** An Item Pipe pulls Raw Iron out of a chest and delivers it into the Forge's side. */
    @ServusGameTest(timeoutTicks = 200)
    public static void pipeFeedsTheForge(GameTestHelper helper) {
        BlockPos duct = FORGE.west();
        BlockPos chestPos = duct.west();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        forge.sideConfig().set(RelativeSide.RIGHT, SideMode.INPUT);
        UtilityDuctBlockEntity pipe = GameTestSteps.duct(helper, duct, Line.ITEM);
        pipe.setMode(Line.ITEM, Direction.WEST, DuctMode.IN_PULL);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(chest, Items.RAW_IRON) == 0, "the pipe should empty the chest");
            int inForge = forge.inventory().getStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT).getCount()
                    + forge.inventory().getStackInSlot(SalamanderForgeBlockEntity.SLOT_OUTPUT).getCount();
            helper.assertTrue(inForge == 8, "all 8 should be in the Forge, raw or smelted, got " + inForge);
        });
    }

    /** A side set to Off takes nothing; the front is closed and stays closed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void offSideAndFrontRefuse(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        forge.sideConfig().set(RelativeSide.RIGHT, SideMode.DISABLED);
        ItemView right = items(helper, FORGE, Direction.WEST);
        helper.assertTrue(right == null || right.insertItem(new ItemStack(Items.RAW_IRON, 2), false).getCount() == 2, "an Off side takes nothing");
        helper.assertTrue(forge.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front starts closed");
        helper.assertTrue(forge.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front cannot be cycled");
        ItemView front = items(helper, FORGE, Direction.NORTH);
        helper.assertTrue(front == null || front.insertItem(new ItemStack(Items.RAW_IRON, 2), false).getCount() == 2, "nothing goes in at the front");
        ItemView top = items(helper, FORGE, Direction.UP);
        helper.assertTrue(top.insertItem(new ItemStack(Items.RAW_IRON, 2), false).isEmpty(), "the Auto top takes Raw Iron");
        helper.succeed();
    }

    /** Every one item smelting recipe is on offer at the Forge's pace; the input's recipe is the active one. */
    @ServusGameTest(timeoutTicks = 20)
    public static void exposesSmeltingProduction(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        ProductionProvider production = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(FORGE), Direction.UP);
        helper.assertTrue(production != null, "the Forge should be a producer");
        Optional<ProductionRecipe> iron = production.recipes().stream()
                .filter(recipe -> recipe.inputs().getFirst().is(Items.RAW_IRON) && recipe.outputs().getFirst().is(Items.IRON_INGOT))
                .findFirst();
        helper.assertTrue(iron.isPresent(), "Raw Iron to Iron Ingot should be on offer");
        helper.assertTrue(iron.get().ticks() == 100, "at the Forge's 100 ticks, got " + iron.get().ticks());
        helper.assertTrue(production.active().isEmpty(), "an empty Forge works on nothing");
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 1));
        helper.assertTrue(production.active().equals(Optional.of(iron.get().id())), "the Raw Iron recipe is active");
        helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(FORGE), Direction.UP) == null,
                "the Forge keeps no upkeep");
        helper.succeed();
    }

    /** On a data line the Forge is a producer and nothing else. */
    @ServusGameTest(timeoutTicks = 60)
    public static void producerOnTheDataLine(GameTestHelper helper) {
        BlockPos duct = FORGE.east();
        forge(helper, FORGE);
        GameTestSteps.duct(helper, duct, Line.DATA);
        helper.succeedWhen(() -> {
            DataMember member = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(duct)).flatMap(network ->
                    network.dataMembers().stream().filter(candidate -> candidate.pos().equals(helper.absolutePos(FORGE))).findFirst())
                    .orElse(null);
            helper.assertTrue(member != null && member.roles().equals(Set.of(DataRole.PRODUCER)), "the Forge should be a producer only, got " + member);
        });
    }

    /** Deepslate Bricks around Ignis Perpetuus over a Furnace make the Forge at a crafting table. */
    @ServusGameTest(timeoutTicks = 20)
    public static void forgeRecipeShipped(GameTestHelper helper) {
        ItemStack bricks = new ItemStack(Items.DEEPSLATE_BRICKS);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(bricks, bricks, bricks,
                bricks, new ItemStack(ServusAlchemyItems.IGNIS_PERPETUUS.get()), bricks,
                bricks, new ItemStack(Items.FURNACE), bricks));
        ItemStack result = ServusRecipes.craftingFor(helper.getLevel(), grid)
                .map(holder -> holder.value().assemble(grid)).orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ServusAlchemyItems.SALAMANDER_FORGE.get()) && result.getCount() == 1, "the grid should make one Forge, got " + result);
        helper.succeed();
    }

    /** Breaking it drops the Forge and what it holds. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingDropsContents(GameTestHelper helper) {
        SalamanderForgeBlockEntity forge = forge(helper, FORGE);
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 3));
        forge.inventory().setStackInSlot(SalamanderForgeBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 5));
        helper.getLevel().destroyBlock(helper.absolutePos(FORGE), true);
        int raw = 0;
        int ingots = 0;
        int forges = 0;
        for (ItemEntity drop : GameTestSteps.drops(helper, FORGE, 2)) {
            ItemStack stack = drop.getItem();
            raw += stack.is(Items.RAW_IRON) ? stack.getCount() : 0;
            ingots += stack.is(Items.IRON_INGOT) ? stack.getCount() : 0;
            forges += stack.is(ServusAlchemyItems.SALAMANDER_FORGE.get()) ? stack.getCount() : 0;
        }
        helper.assertTrue(raw == 3 && ingots == 5 && forges == 1, "drops: " + raw + " raw, " + ingots + " ingots, " + forges + " forge");
        helper.succeed();
    }
}
