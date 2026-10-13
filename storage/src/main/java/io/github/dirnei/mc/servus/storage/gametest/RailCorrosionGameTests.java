package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.field.FieldCorrosion;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldWater;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Copper corrosion of farm rail fields: aging, upkeep, the corroded field, axe scraping and joins. */
public final class RailCorrosionGameTests {

    private static final int RAIL_Z = 5;
    private static final int FIRST = 3;
    private static final int LAST = 7;
    private static final int RAILS = LAST - FIRST + 1;
    private static final BlockPos HEAD = new BlockPos(2, 1, RAIL_Z);

    private RailCorrosionGameTests() {
    }

    private static void rail(GameTestHelper helper, int x) {
        FieldFixtures.farmRail(helper, new BlockPos(x, 1, RAIL_Z));
    }

    /** Farm rails from x 3 to 7, farmland north of them one below, the head west of the rails. */
    private static FieldHeadBlockEntity field(GameTestHelper helper) {
        for (int x = FIRST; x <= LAST; x++) {
            helper.setBlock(new BlockPos(x, 0, RAIL_Z + 1), Blocks.FARMLAND.defaultBlockState());
            rail(helper, x);
        }
        return FieldFixtures.head(helper, HEAD);
    }

    private static int stage(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z), RailBlockEntity.class).corrosion();
    }

    private static void assertRails(GameTestHelper helper, int from, int to, int expected, String why) {
        for (int x = from; x <= to; x++) {
            int stage = stage(helper, x);
            helper.assertTrue(stage == expected, why + ": rail at x " + x + " is " + stage + ", expected " + expected);
        }
    }

    private static int perStep() {
        return StorageStatsManager.INSTANCE.fieldCorrosionWater();
    }

    private static boolean wet(GameTestHelper helper, int x) {
        return FieldFixtures.wet(helper, new BlockPos(x, 0, RAIL_Z + 1));
    }

    /** Uses the player's main hand item on the top of the rail at that x, as a right click does. */
    private static void useOnRail(GameTestHelper helper, Player player, int x) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, RAIL_Z));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    /** A served field starts at copper on every rail and goes to exposed after one step of water without upkeep. */
    @ServusGameTest(timeoutTicks = 40)
    public static void agingWithoutUpkeep(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "a new field is copper, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.COPPER, "served rails take the field's stage");
            head.age(perStep() - 1);
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "not a full step yet");
            head.age(1);
            helper.assertTrue(head.corrosion() == FieldCorrosion.EXPOSED, "one step makes exposed, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.EXPOSED, "the whole field ages");
            helper.succeed();
        });
    }

    /** Honeycomb in the slot holds the stage for one step each. */
    @ServusGameTest(timeoutTicks = 40)
    public static void honeycombHoldsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            ItemStack rest = Transfer.insertItem(head.packetInlet(), 0, new ItemStack(Items.HONEYCOMB, 3), false);
            helper.assertTrue(rest.isEmpty(), "the head takes honeycomb through its item handler");
            head.age(perStep());
            helper.assertTrue(head.corrosion() == FieldCorrosion.WEATHERED, "honeycomb holds weathered, got " + head.corrosion());
            helper.assertTrue(head.antiCorrosion().getStackInSlot(0).getCount() == 2, "one honeycomb used");
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "rails stay weathered");
            helper.succeed();
        });
    }

    /** Hydration water counts toward aging. */
    @ServusGameTest(timeoutTicks = 60)
    public static void waterUseCountsForAging(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.succeedWhen(() -> {
                helper.assertTrue(head.hydrated(), "the head should hydrate");
                long need = FieldWater.use(RAILS, StorageStatsManager.INSTANCE.fieldWaterPerRail());
                helper.assertTrue(head.agedWater() == need, "the used water counts, got " + head.agedWater() + " expected " + need);
            });
        });
    }

    /** A corroded field is no longer watered, uses no water and shows the rust rim. */
    @ServusGameTest(timeoutTicks = 80)
    public static void corrodedStopsHydration(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.hydrated() && wet(helper, 4), "the field should be watered first");
                head.setStage(FieldCorrosion.OXIDIZED);
                head.age(perStep());
                helper.assertTrue(head.corrosion() == FieldCorrosion.CORRODED, "oxidized plus a step is corroded");
                helper.runAfterDelay(2, () -> {
                    int water = head.water();
                    helper.assertTrue(!head.hydrated() && !wet(helper, 4), "a corroded field is not watered");
                    helper.assertTrue(helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the head shows the rust rim");
                    helper.runAfterDelay(5, () -> {
                        helper.assertTrue(head.water() == water, "a corroded field uses no water");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** An axe scrapes every rail back one stage and loses one durability per rail; the field is watered again. */
    @ServusGameTest(timeoutTicks = 60)
    public static void axeScrapesField(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.CORRODED);
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            Player player = TestPlayers.holding(helper, axe);
            useOnRail(helper, player, 5);
            helper.assertTrue(head.corrosion() == FieldCorrosion.OXIDIZED, "scraped to oxidized, got " + head.corrosion());
            assertRails(helper, FIRST, LAST, FieldCorrosion.OXIDIZED, "every rail is scraped");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == RAILS,
                    "one durability per rail, got " + player.getMainHandItem().getDamageValue());
            helper.succeedWhen(() -> {
                helper.assertTrue(head.hydrated(), "the scraped field is watered again");
                helper.assertTrue(!helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the rust rim is gone");
            });
        });
    }

    /** An axe with less durability left than the field has rails does nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void wornAxeRefused(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            axe.setDamageValue(axe.getMaxDamage() - (RAILS - 2));
            int damage = axe.getDamageValue();
            Player player = TestPlayers.holding(helper, axe);
            useOnRail(helper, player, 4);
            helper.assertTrue(head.corrosion() == FieldCorrosion.WEATHERED, "a worn axe changes nothing");
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "rails stay weathered");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == damage, "the worn axe keeps its durability");
            helper.succeed();
        });
    }

    /** Fresh copper is not scraped. */
    @ServusGameTest(timeoutTicks = 40)
    public static void freshCopperNotScraped(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            Player player = TestPlayers.holding(helper, new ItemStack(Items.IRON_AXE));
            useOnRail(helper, player, 4);
            helper.assertTrue(head.corrosion() == FieldCorrosion.COPPER, "copper stays copper");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "the axe keeps its durability");
            helper.succeed();
        });
    }

    /** A rail placed at the end of a headed field takes the field's stage. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedRailAdoptsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.WEATHERED);
            rail(helper, LAST + 1);
            helper.succeedWhen(() -> helper.assertTrue(stage(helper, LAST + 1) == FieldCorrosion.WEATHERED,
                    "the new rail takes weathered, got " + stage(helper, LAST + 1)));
        });
    }

    /** A broken rail's drop places a loose, fresh rail. */
    @ServusGameTest(timeoutTicks = 40)
    public static void droppedRailIsFresh(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setStage(FieldCorrosion.OXIDIZED);
            BlockPos pos = helper.absolutePos(new BlockPos(LAST, 1, RAIL_Z));
            List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                    helper.getLevel().getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusStorageItems.FARM_RAIL.get()), "one farm rail drops, got " + drops);
            ItemStack stack = drops.get(0);
            BlockPos at = new BlockPos(5, 1, 10);
            WarehouseFixtures.stone(helper, at.below());
            TestPlayers.useOnTop(helper, TestPlayers.holding(helper, stack), stack, at.below());
            helper.assertBlockPresent(ServusStorageBlocks.FARM_RAIL.get(), at);
            helper.runAfterDelay(2, () -> {
                int placed = helper.getBlockEntity(at, RailBlockEntity.class).corrosion();
                helper.assertTrue(placed == FieldCorrosion.LOOSE, "the placed drop is fresh and loose, got " + placed);
                helper.succeed();
            });
        });
    }

    /** Without a head, a rail joining a weathered and an exposed run makes all of them exposed. */
    @ServusGameTest(timeoutTicks = 40)
    public static void headlessJoinTakesYoungest(GameTestHelper helper) {
        for (int x : new int[] {3, 4, 6, 7}) {
            rail(helper, x);
        }
        helper.runAfterDelay(2, () -> {
            for (int x : new int[] {3, 4}) {
                helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z), RailBlockEntity.class).setCorrosion(FieldCorrosion.WEATHERED);
            }
            for (int x : new int[] {6, 7}) {
                helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z), RailBlockEntity.class).setCorrosion(FieldCorrosion.EXPOSED);
            }
            rail(helper, 5);
            helper.succeedWhen(() -> assertRails(helper, 3, 7, FieldCorrosion.EXPOSED, "the joined field takes the younger stage"));
        });
    }

    /** A hopper above the head keeps its honeycomb: the anti-corrosion slot is filled by hand or upkeep packets. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperCannotFillUpkeep(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        BlockPos hopperPos = HEAD.above();
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.HONEYCOMB, 2));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 2, "the hopper keeps its honeycomb");
            helper.assertTrue(head.antiCorrosion().getStackInSlot(0).isEmpty(), "the anti-corrosion slot stays empty");
            helper.succeed();
        });
    }

    /** Upkeep packets fill the anti-corrosion slot through the inlet, other items are refused; the head is an upkeep holder. */
    @ServusGameTest(timeoutTicks = 20)
    public static void upkeepPacketsFillSlot(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        UpkeepHolder upkeep = helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(HEAD), Direction.UP);
        helper.assertTrue(upkeep != null && upkeep.upkeepItem().is(Items.HONEYCOMB)
                        && upkeep.upkeepLevel() == StorageStatsManager.INSTANCE.fieldAntiCorrosionLevel(),
                "the head keeps honeycomb as upkeep");
        ItemView inlet = PacketInlets
                .handler(helper.getLevel(), helper.absolutePos(HEAD), Direction.UP);
        helper.assertTrue(inlet != null, "the head has a packet inlet");
        helper.assertTrue(inlet.insertItemStacked(new ItemStack(Items.HONEYCOMB, 2), false).isEmpty(), "both honeycomb go in");
        helper.assertTrue(inlet.insertItemStacked(new ItemStack(Items.WHEAT_SEEDS, 1), false).getCount() == 1, "seeds are refused");
        helper.assertTrue(head.antiCorrosion().getStackInSlot(0).is(Items.HONEYCOMB) && head.antiCorrosion().getStackInSlot(0).getCount() == 2,
                "both honeycomb are in the slot");
        helper.assertTrue(upkeep.upkeepCount() == 2, "the upkeep count follows the slot");
        helper.assertTrue(Transfer.extractItem(head.packetInlet(), 0, 1, true).isEmpty(), "packets take nothing out");
        helper.succeed();
    }
}
