package io.github.dirnei.mc.servus.alchemy.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.datagen.ServusAlchemyMultiblockProvider;
import io.github.dirnei.mc.servus.alchemy.journal.JournalSync;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.alchemy.venter.CucurbitBlock;
import io.github.dirnei.mc.servus.alchemy.venter.VenterEquinusBlockEntity;
import io.github.dirnei.mc.servus.alchemy.venter.VenterFrameBlock;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Piece;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Role;
import io.github.dirnei.mc.servus.alchemy.venter.VenterStructure;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixingInput;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Venter Equinus: a 3 by 3 straw bed whose Cucurbit putrefies rot into Humus, faster the more straw is left. */
public final class VenterGameTests {

    /** The Cucurbit in the middle of the bed. */
    static final BlockPos CUCURBIT = new BlockPos(3, 1, 3);

    private VenterGameTests() {
    }

    static BlockPos cell(int index, Facing front) {
        return VenterStructure.pos(CUCURBIT, VenterLayout.cell(index), front);
    }

    static BlockPos hatch() {
        return cell(1, Facing.NORTH);
    }

    /** A Dung Bed of a bed facing north: the one on the left side. */
    static BlockPos bed() {
        return cell(4, Facing.NORTH);
    }

    /** Builds the unformed bed with its front facing {@code front}. */
    static void build(GameTestHelper helper, Facing front) {
        for (Cell cell : VenterLayout.cells()) {
            helper.setBlock(VenterStructure.pos(CUCURBIT, cell, front), VenterStructure.shown(cell.role()).defaultBlockState());
        }
    }

    static void form(GameTestHelper helper) {
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), CUCURBIT);
    }

    /** A formed bed facing north. */
    static VenterEquinusBlockEntity venter(GameTestHelper helper) {
        build(helper, Facing.NORTH);
        form(helper);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, true);
        return helper.getBlockEntity(CUCURBIT, VenterEquinusBlockEntity.class);
    }

    static ItemView items(GameTestHelper helper, BlockPos pos) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.UP));
    }

    static ItemStack hay(int count) {
        return new ItemStack(Items.HAY_BLOCK, count);
    }

    static ItemStack flesh(int count) {
        return new ItemStack(Items.ROTTEN_FLESH, count);
    }

    static int humus(VenterEquinusBlockEntity venter) {
        ItemStack held = venter.hatchStack();
        return held.is(ServusAlchemyItems.HUMUS.get()) ? held.getCount() : 0;
    }

    static void assertFormed(GameTestHelper helper, boolean formed) {
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, formed);
        for (Cell cell : VenterLayout.cells()) {
            if (cell.role() != Role.CUCURBIT) {
                helper.assertBlockProperty(VenterStructure.pos(CUCURBIT, cell, Facing.NORTH), VenterFrameBlock.FORMED, formed);
            }
        }
    }

    /** The bed forms whichever side the hatch is on, and the Cucurbit faces that way. */
    @ServusGameTest(timeoutTicks = 20)
    public static void formsWithHatchOnEachSide(GameTestHelper helper) {
        for (Facing front : Facing.values()) {
            build(helper, front);
            form(helper);
            helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, true);
            helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FACING, Structures.direction(front));
            for (Cell cell : VenterLayout.cells()) {
                helper.setBlock(VenterStructure.pos(CUCURBIT, cell, front), Blocks.AIR);
            }
        }
        helper.succeed();
    }

    /** Without the Dung Bed in the back left corner nothing forms and the message names that corner. */
    @ServusGameTest(timeoutTicks = 20)
    public static void missingCornerIsNamed(GameTestHelper helper) {
        build(helper, Facing.NORTH);
        helper.setBlock(cell(6, Facing.NORTH), Blocks.AIR);
        List<String> messages = new ArrayList<>();
        CalcinerGameTests.use(helper, AlchemyGameTests.listeningPlayer(helper, messages), CUCURBIT);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, false);
        helper.assertTrue(messages.equals(List.of("message.servus_alchemy.venter_equinus.missing block.servus_alchemy.dung_bed"
                + " message.servus_alchemy.venter_equinus.at.in_the_back_left_corner")), "the back left corner is named, got " + messages);
        helper.succeed();
    }

    /** Breaking a Dung Bed unforms the whole bed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingABedUnforms(GameTestHelper helper) {
        venter(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(cell(8, Facing.NORTH)), true);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, false);
        helper.assertBlockProperty(hatch(), VenterFrameBlock.FORMED, false);
        helper.assertBlockProperty(bed(), VenterFrameBlock.FORMED, false);
        helper.succeed();
    }

    /** Breaking the hatch unforms the whole bed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheHatchUnforms(GameTestHelper helper) {
        venter(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(hatch()), true);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, false);
        for (int index = 2; index < 9; index++) {
            helper.assertBlockProperty(cell(index, Facing.NORTH), VenterFrameBlock.FORMED, false);
        }
        helper.succeed();
    }

    /** Forming sets each frame block's piece and outward side; unforming clears them all. */
    @ServusGameTest(timeoutTicks = 20)
    public static void formedStatesSetAndCleared(GameTestHelper helper) {
        venter(helper);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FACING, Direction.NORTH);
        for (Cell cell : VenterLayout.cells()) {
            if (cell.role() == Role.CUCURBIT) {
                continue;
            }
            BlockPos pos = VenterStructure.pos(CUCURBIT, cell, Facing.NORTH);
            helper.assertBlockProperty(pos, VenterFrameBlock.PIECE,
                    cell.piece() == Piece.CORNER ? VenterFrameBlock.FramePiece.CORNER : VenterFrameBlock.FramePiece.EDGE);
            helper.assertBlockProperty(pos, VenterFrameBlock.FACING, Structures.direction(VenterLayout.pieceFacing(cell, Facing.NORTH)));
            helper.assertTrue(VenterFrameBlock.cucurbit(helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)))
                    .equals(helper.absolutePos(CUCURBIT)), "cell " + cell.index() + " finds its Cucurbit");
        }
        helper.assertBlockProperty(cell(3, Facing.NORTH), VenterFrameBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(cell(6, Facing.NORTH), VenterFrameBlock.FACING, Direction.SOUTH);
        helper.getLevel().destroyBlock(helper.absolutePos(bed()), true);
        helper.setBlock(bed(), ServusAlchemyBlocks.DUNG_BED.get().defaultBlockState());
        assertFormed(helper, false);
        helper.succeed();
    }

    /** With eight fresh Hay Bales a batch takes the warm time, about 40 ticks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fullBedTakesWarmTime(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(8));
        venter.insertRot(flesh(8));
        helper.runAfterDelay(36, () -> helper.assertTrue(humus(venter) == 0, "not done before the warm time"));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(humus(venter) == 1, "one Humus after the warm time, got " + humus(venter));
            helper.succeed();
        });
    }

    /** Without straw a batch still runs, in the cold time of about 160 ticks. */
    @ServusGameTest(timeoutTicks = 220)
    public static void emptyBedTakesColdTime(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.insertRot(flesh(8));
        helper.runAfterDelay(150, () -> helper.assertTrue(humus(venter) == 0, "not done before the cold time"));
        helper.runAfterDelay(165, () -> {
            helper.assertTrue(humus(venter) == 1, "one Humus after the cold time, got " + humus(venter));
            helper.succeed();
        });
    }

    /** An idle bed keeps its straw; a working bed rots it down one tick at a time. */
    @ServusGameTest(timeoutTicks = 100)
    public static void strawBurnsOnlyWhileWorking(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(2));
        long full = venter.warmthLeft();
        venter.insertRot(flesh(7));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(venter.warmthLeft() == full, "no straw rots without a batch");
            helper.assertBlockProperty(CUCURBIT, CucurbitBlock.WORKING, false);
            venter.insertRot(flesh(1));
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(venter.warmthLeft() < full, "the straw rots while a batch runs");
                helper.assertBlockProperty(CUCURBIT, CucurbitBlock.WORKING, true);
                helper.succeed();
            });
        });
    }

    /** A full hatch holds the work until the Humus is taken out. */
    @ServusGameTest(timeoutTicks = 200)
    public static void fullHatchStops(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(8));
        venter.hatch().setStackInSlot(0, new ItemStack(ServusAlchemyItems.HUMUS.get(), 64));
        venter.insertRot(flesh(8));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(venter.rotCount() == 8, "the rot waits while the hatch is full");
            helper.assertBlockProperty(CUCURBIT, CucurbitBlock.WORKING, false);
            venter.takeHatch();
            helper.succeedWhen(() -> helper.assertTrue(humus(venter) == 1 && venter.rotCount() == 0, "the batch runs once there is room"));
        });
    }

    /** Five Rotten Flesh and three Spider Eyes make one batch. */
    @ServusGameTest(timeoutTicks = 100)
    public static void mixedRotMakesOneBatch(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(8));
        venter.insertRot(flesh(5));
        venter.insertRot(new ItemStack(Items.SPIDER_EYE, 3));
        helper.succeedWhen(() -> helper.assertTrue(humus(venter) == 1 && venter.rotCount() == 0, "one Humus from all eight"));
    }

    /** A pipe pushes Spider Eyes into the Cucurbit; Hay Bales and Cobblestone stay out, and nothing comes back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rotByPipeHayRefused(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        ItemView cucurbit = items(helper, CUCURBIT);
        helper.assertTrue(cucurbit != null, "the Cucurbit takes items");
        helper.assertTrue(cucurbit.insertItem(0, new ItemStack(Items.SPIDER_EYE, 16), false).isEmpty(), "the Spider Eyes go in");
        helper.assertTrue(cucurbit.insertItem(0, hay(4), false).getCount() == 4, "the Hay Bales stay out");
        helper.assertTrue(cucurbit.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "stone is no rot");
        helper.assertTrue(cucurbit.extractItem(0, 64, false).isEmpty(), "the Cucurbit gives nothing");
        helper.assertTrue(venter.rotCount() == 16 && venter.bales() == 0, "16 rot and no straw");
        helper.succeed();
    }

    /** A pipe lays Hay Bales into a Dung Bed up to the room left; Wheat is no bedding. */
    @ServusGameTest(timeoutTicks = 20)
    public static void balesByPipeUpToRoom(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(6));
        ItemView dungBed = items(helper, cell(7, Facing.NORTH));
        helper.assertTrue(dungBed != null, "a Dung Bed takes items");
        helper.assertTrue(dungBed.insertItem(0, new ItemStack(Items.WHEAT, 4), false).getCount() == 4, "Wheat is no bedding");
        helper.assertTrue(dungBed.insertItem(0, hay(5), false).getCount() == 3, "two Hay Bales go in");
        helper.assertTrue(venter.bales() == 8, "the bed is full");
        helper.assertTrue(dungBed.extractItem(0, 64, false).isEmpty(), "a Dung Bed gives nothing");
        helper.succeed();
    }

    /** A hopper below the hatch pulls the Humus out. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hatchGivesOnlyHumus(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.hatch().setStackInSlot(0, new ItemStack(ServusAlchemyItems.HUMUS.get(), 3));
        venter.insertRot(flesh(5));
        helper.assertTrue(items(helper, CUCURBIT).extractItem(0, 64, true).isEmpty(), "the Cucurbit gives no rot");
        BlockPos hopper = hatch().below();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper, HopperBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(hopperEntity.countItem(ServusAlchemyItems.HUMUS.get()) == 3
                && hopperEntity.countItem(Items.ROTTEN_FLESH) == 0, "the hopper holds the 3 Humus only"));
    }

    /** The hatch takes nothing, neither rot nor straw. */
    @ServusGameTest(timeoutTicks = 20)
    public static void nothingInAtTheHatch(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        ItemView hatch = items(helper, hatch());
        helper.assertTrue(hatch != null, "the hatch is a port");
        helper.assertTrue(hatch.insertItem(0, flesh(8), false).getCount() == 8, "no rot goes in at the hatch");
        helper.assertTrue(hatch.insertItem(0, hay(2), false).getCount() == 2, "no straw goes in at the hatch");
        helper.assertTrue(hatch.insertItem(0, new ItemStack(ServusAlchemyItems.HUMUS.get()), false).getCount() == 1, "no Humus goes in");
        helper.assertTrue(venter.rotCount() == 0 && venter.bales() == 0, "nothing went in");
        helper.succeed();
    }

    /** Unformed blocks expose no ports. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unformedHasNoPorts(GameTestHelper helper) {
        build(helper, Facing.NORTH);
        helper.assertTrue(items(helper, CUCURBIT) == null, "an unformed Cucurbit takes nothing");
        helper.assertTrue(items(helper, bed()) == null, "an unformed Dung Bed takes nothing");
        helper.assertTrue(items(helper, hatch()) == null, "an unformed hatch gives nothing");
        helper.succeed();
    }

    /** Rot goes into the gourd and Hay Bales into the bed by hand, and an empty hand takes the Humus at the hatch. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rotStrawAndHumusByHand(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        Player player = CalcinerGameTests.player(helper, new ItemStack(Items.SPIDER_EYE, 16));
        CalcinerGameTests.use(helper, player, CUCURBIT);
        helper.assertTrue(venter.rotCount() == 16 && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the 16 Spider Eyes go in");
        player.setItemInHand(InteractionHand.MAIN_HAND, hay(10));
        CalcinerGameTests.use(helper, player, bed());
        helper.assertTrue(venter.bales() == 8, "8 bales go in, " + venter.bales());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "the player keeps 2 Hay Bales");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
        CalcinerGameTests.use(helper, player, CUCURBIT);
        helper.assertTrue(venter.rotCount() == 16 && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "the Cucurbit takes no stone");
        venter.hatch().setStackInSlot(0, new ItemStack(ServusAlchemyItems.HUMUS.get(), 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CalcinerGameTests.use(helper, player, hatch());
        helper.assertTrue(player.getInventory().countItem(ServusAlchemyItems.HUMUS.get()) == 5, "the player receives the 5 Humus");
        helper.succeed();
    }

    /** The Journal forms the bed from a Dung Bed and from the Bed Hatch too. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalFormsFromAnyBlock(GameTestHelper helper) {
        for (BlockPos clicked : List.of(bed(), hatch())) {
            build(helper, Facing.NORTH);
            CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), clicked);
            assertFormed(helper, true);
            for (Cell cell : VenterLayout.cells()) {
                helper.setBlock(VenterStructure.pos(CUCURBIT, cell, Facing.NORTH), Blocks.AIR);
            }
        }
        helper.succeed();
    }

    /** Rot and Hay Bales go in wherever on the bed the player clicks; Wheat is no rot. */
    @ServusGameTest(timeoutTicks = 20)
    public static void rotAndHayAnywhereOnTheBed(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        Player player = CalcinerGameTests.player(helper, flesh(8));
        CalcinerGameTests.use(helper, player, hatch());
        helper.assertTrue(venter.rotCount() == 8, "Rotten Flesh on the hatch goes into the Cucurbit, " + venter.rotCount());
        player.setItemInHand(InteractionHand.MAIN_HAND, hay(3));
        CalcinerGameTests.use(helper, player, CUCURBIT);
        helper.assertTrue(venter.bales() == 3, "Hay Bales on the Cucurbit go into the straw, " + venter.bales());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
        CalcinerGameTests.use(helper, player, bed());
        helper.assertTrue(venter.rotCount() == 8 && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 4, "Wheat is no rot");
        helper.succeed();
    }

    /** Sneaking with an empty hand on the Cucurbit gives the rot back. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sneakTakesRotBack(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.insertRot(flesh(12));
        Player player = CalcinerGameTests.player(helper, ItemStack.EMPTY);
        CalcinerGameTests.use(helper, player, CUCURBIT);
        helper.assertTrue(venter.rotCount() == 12, "an empty hand without sneaking takes nothing");
        player.setShiftKeyDown(true);
        CalcinerGameTests.use(helper, player, CUCURBIT);
        helper.assertTrue(venter.rotCount() == 0, "the rot is taken out");
        helper.assertTrue(player.getInventory().countItem(Items.ROTTEN_FLESH) == 12, "the player receives the 12 Rotten Flesh");
        helper.succeed();
    }

    /** A Cucurbit that stays keeps its rot and straw while the bed is unformed and works again once it is formed. */
    @ServusGameTest(timeoutTicks = 200)
    public static void unformedKeepsRotAndStraw(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.layBedding(hay(8));
        venter.insertRot(flesh(16));
        helper.getLevel().destroyBlock(helper.absolutePos(bed()), true);
        helper.assertBlockProperty(CUCURBIT, CucurbitBlock.FORMED, false);
        long warmth = venter.warmthLeft();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(venter.rotCount() == 16 && humus(venter) == 0, "an unformed bed does not work");
            helper.assertTrue(venter.warmthLeft() == warmth && venter.bales() == 8, "the straw is kept");
            helper.setBlock(bed(), ServusAlchemyBlocks.DUNG_BED.get().defaultBlockState());
            form(helper);
            helper.succeedWhen(() -> helper.assertTrue(humus(venter) == 2, "the bed works again"));
        });
    }

    /** A broken Cucurbit drops itself, its rot and its Humus. */
    @ServusGameTest(timeoutTicks = 20)
    public static void brokenCucurbitDropsRotAndHumus(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        venter.insertRot(flesh(40));
        venter.hatch().setStackInSlot(0, new ItemStack(ServusAlchemyItems.HUMUS.get(), 3));
        helper.getLevel().destroyBlock(helper.absolutePos(CUCURBIT), true);
        helper.assertBlockProperty(hatch(), VenterFrameBlock.FORMED, false);
        assertDropped(helper, Items.ROTTEN_FLESH, 40);
        assertDropped(helper, ServusAlchemyItems.HUMUS.get(), 3);
        assertDropped(helper, ServusAlchemyItems.CUCURBIT.get(), 1);
        helper.succeed();
    }

    private static void assertDropped(GameTestHelper helper, Item item, int count) {
        helper.assertItemEntityCountIs(item, CUCURBIT, 3.0, count);
    }

    /** The journal's schematic accepts the bed, anchored on the Cucurbit. */
    @ServusGameTest(timeoutTicks = 20)
    public static void schematicAcceptsTheBed(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(ServusAlchemyMultiblockProvider.VENTER_EQUINUS);
        helper.assertTrue(schematic != null, "the Venter Equinus schematic should be loaded");
        build(helper, Facing.NORTH);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(CUCURBIT)) != null, "the schematic accepts the bed");
        helper.succeed();
    }

    /** Both Dirt recipes load, and the Mixer's matcher takes their contents, any Sand will do. */
    @ServusGameTest(timeoutTicks = 20)
    public static void mixingRecipesMakeDirt(GameTestHelper helper) {
        assertMixes(helper, "dirt_from_humus", List.of(new ItemStack(Items.SAND, 4), new ItemStack(Items.CLAY_BALL),
                new ItemStack(ServusAlchemyItems.HUMUS.get())), 8);
        assertMixes(helper, "dirt_from_bone_meal", List.of(new ItemStack(Items.BONE_MEAL), new ItemStack(Items.RED_SAND, 2),
                new ItemStack(Items.CLAY_BALL)), 2);
        helper.succeed();
    }

    private static void assertMixes(GameTestHelper helper, String name, List<ItemStack> items, int dirt) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "mixing/" + name);
        MixingRecipe recipe = ServusRecipes.byId(helper.getLevel(), id)
                .filter(holder -> holder.value() instanceof MixingRecipe)
                .map(holder -> (MixingRecipe) holder.value())
                .orElseThrow(() -> helper.assertionException(Component.literal("the recipe " + id + " should load")));
        helper.assertTrue(recipe.matches(new MixingInput(items, new FluidStack(Fluids.WATER, 250)), helper.getLevel()),
                name + " should match its contents");
        helper.assertTrue(!recipe.matches(new MixingInput(items, FluidStack.EMPTY), helper.getLevel()), name + " needs water");
        helper.assertTrue(recipe.result().is(Items.DIRT) && recipe.result().getCount() == dirt, name + " makes " + dirt + " Dirt");
    }

    /** A finished batch reaches the journal of the owner's team as a putrefy event. */
    @ServusGameTest(timeoutTicks = 20)
    public static void putrefyCountsInJournal(GameTestHelper helper) {
        VenterEquinusBlockEntity venter = venter(helper);
        UUID team = UUID.randomUUID();
        venter.setTeam(team);
        venter.layBedding(hay(8));
        venter.insertRot(flesh(8));
        JournalSync.drain(helper.getLevel().getServer());
        for (int tick = 0; tick < 60 && humus(venter) == 0; tick++) {
            VenterEquinusBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CUCURBIT), venter.getBlockState(), venter);
        }
        helper.assertTrue(humus(venter) == 1, "the batch is done");
        helper.assertTrue(JournalSync.isQueued(team), "the putrefy event reached the team's journal");
        helper.succeed();
    }
}
