package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldWater;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailCorrosion;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Copper corrosion of farm rails: stages per rail, aging of wet rails, upkeep, water blocked by corroded rails, axe scraping and drops. */
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
        return ShuttlesStatsManager.INSTANCE.fieldCorrosionWater();
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

    private static RailBlockEntity railAt(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 1, RAIL_Z), RailBlockEntity.class);
    }

    /** Sets the stage of the rails from x {@code from} to {@code to} and tells the head. */
    private static void setStages(GameTestHelper helper, FieldHeadBlockEntity head, int from, int to, int stage) {
        for (int x = from; x <= to; x++) {
            railAt(helper, x).setCorrosion(stage);
        }
        head.railsChanged();
    }

    /** A new field is copper on every rail and goes to exposed after one step of water without upkeep. */
    @ServusGameTest(timeoutTicks = 40)
    public static void agingWithoutUpkeep(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            assertRails(helper, FIRST, LAST, FieldCorrosion.COPPER, "a new field is copper");
            helper.assertTrue(head.stageCounts()[FieldCorrosion.COPPER] == RAILS, "the head counts every rail as copper");
            head.age(perStep() - 1);
            assertRails(helper, FIRST, LAST, FieldCorrosion.COPPER, "not a full step yet");
            head.age(1);
            assertRails(helper, FIRST, LAST, FieldCorrosion.EXPOSED, "one step makes every wet rail exposed");
            helper.assertTrue(head.stageCounts()[FieldCorrosion.EXPOSED] == RAILS, "the head counts the exposed rails");
            helper.succeed();
        });
    }

    /** Honeycomb in the buffer holds every rail's stage for one step each. */
    @ServusGameTest(timeoutTicks = 40)
    public static void honeycombHoldsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, FIRST, LAST, FieldCorrosion.WEATHERED);
            helper.assertTrue(head.fillSupply(SupplyBuffer.Part.ANTI_CORROSION, Items.HONEYCOMB, 3) == 3, "the buffer takes three honeycomb");
            head.age(perStep());
            helper.assertTrue(head.supplyCount(SupplyBuffer.Part.ANTI_CORROSION) == 2, "one honeycomb used");
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "rails stay weathered");
            helper.succeed();
        });
    }

    /**
     * Anti-corrosion Off: the part asks for nothing, honeycomb already in the
     * buffer is stale and holds no stage, and the rails age.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void antiCorrosionOffAges(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(head.antiCorrosion().is(Items.HONEYCOMB), "a new head uses the first item of the tag");
            head.fillSupply(SupplyBuffer.Part.ANTI_CORROSION, Items.HONEYCOMB, 3);
            head.setAntiCorrosion(ItemStack.EMPTY);
            helper.assertTrue(head.supplyLevel(SupplyBuffer.Part.ANTI_CORROSION) == 0 && head.holdsStale(),
                    "Off asks for no honeycomb and the held ones go back");
            head.age(perStep());
            assertRails(helper, FIRST, LAST, FieldCorrosion.EXPOSED, "the rails age with Off");
            helper.succeed();
        });
    }

    /** Only items of the anti-corrosion tag can be chosen, and the choice survives saving. */
    @ServusGameTest(timeoutTicks = 40)
    public static void antiCorrosionChoiceSaved(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            head.setAntiCorrosion(ItemStack.EMPTY);
            head.setAntiCorrosion(new ItemStack(Items.DIRT));
            helper.assertTrue(head.antiCorrosion().isEmpty(), "dirt is refused, Off stays");
            CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
            FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(copy.antiCorrosion().isEmpty(), "Off survives saving");
            head.setAntiCorrosion(new ItemStack(Items.HONEYCOMB));
            helper.assertTrue(head.antiCorrosion().is(Items.HONEYCOMB), "honeycomb can be chosen");
            helper.succeed();
        });
    }

    /** The head reports its water use per minute and the watering time to the next aging step. */
    @ServusGameTest(timeoutTicks = 40)
    public static void waterRateAndAgingEstimate(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            int perRail = ShuttlesStatsManager.INSTANCE.fieldWaterPerRail();
            int interval = ShuttlesStatsManager.INSTANCE.fieldWaterInterval();
            helper.assertTrue(head.waterPerMinute() == FieldWater.perMinute(RAILS, perRail, interval),
                    "water use per minute, got " + head.waterPerMinute());
            long full = FieldWater.ticksToUse(perStep(), RAILS, perRail, interval) / 20;
            helper.assertTrue(head.secondsToAging() == full, "a fresh field ages after " + full + " s, got " + head.secondsToAging());
            head.age(perStep() / 2);
            helper.assertTrue(head.secondsToAging() < full, "half the water used brings the step closer");
            setStages(helper, head, LAST, LAST, FieldCorrosion.CORRODED);
            helper.assertTrue(head.waterPerMinute() == FieldWater.perMinute(RAILS - 1, perRail, interval),
                    "a dry rail uses no water, got " + head.waterPerMinute());
            setStages(helper, head, FIRST, FIRST, FieldCorrosion.CORRODED);
            helper.assertTrue(head.waterPerMinute() == 0 && head.secondsToAging() == -1, "a corroded first rail stops all water");
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
                long need = FieldWater.use(RAILS, ShuttlesStatsManager.INSTANCE.fieldWaterPerRail());
                helper.assertTrue(head.agedWater() == need, "the used water counts, got " + head.agedWater() + " expected " + need);
            });
        });
    }

    /** A fresh rail placed at the end of a weathered field stays fresh copper. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedRailStaysFresh(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, FIRST, LAST, FieldCorrosion.WEATHERED);
            rail(helper, LAST + 1);
            helper.succeedWhen(() -> {
                helper.assertTrue(head.rails().size() == RAILS + 1, "the head sees the new rail");
                helper.assertTrue(stage(helper, LAST + 1) == FieldCorrosion.COPPER, "the new rail stays copper, got " + stage(helper, LAST + 1));
                assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "the other rails stay weathered");
                helper.assertTrue(head.stageCounts()[FieldCorrosion.COPPER] == 1 && head.stageCounts()[FieldCorrosion.WEATHERED] == RAILS,
                        "the head counts one copper and " + RAILS + " weathered rails");
            });
        });
    }

    /** A broken rail's drop keeps its stage and scrape progress, and placing it gives them back; a fresh drop carries nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void droppedRailKeepsStage(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            railAt(helper, LAST).setCorrosion(FieldCorrosion.OXIDIZED, 2);
            head.railsChanged();
            ItemStack fresh = drop(helper, FIRST);
            helper.assertTrue(fresh.get(ServusShuttlesComponents.RAIL_CORROSION.get()) == null, "a fresh rail drops without a stage");
            ItemStack stack = drop(helper, LAST);
            RailCorrosion carried = stack.get(ServusShuttlesComponents.RAIL_CORROSION.get());
            helper.assertTrue(carried != null && carried.stage() == FieldCorrosion.OXIDIZED && carried.progress() == 2,
                    "the drop keeps oxidized and two points, got " + carried);
            helper.assertTrue(!ItemStack.isSameItemSameComponents(fresh, stack), "an oxidized rail does not stack with a fresh one");
            BlockPos at = new BlockPos(5, 1, 10);
            WarehouseFixtures.stone(helper, at.below());
            TestPlayers.useOnTop(helper, TestPlayers.holding(helper, stack), stack, at.below());
            helper.assertBlockPresent(ServusShuttlesBlocks.FARM_RAIL.get(), at);
            helper.runAfterDelay(2, () -> {
                RailBlockEntity placed = helper.getBlockEntity(at, RailBlockEntity.class);
                helper.assertTrue(placed.corrosion() == FieldCorrosion.OXIDIZED && placed.scrapeProgress() == 2,
                        "the placed drop is oxidized with two points, got " + placed.corrosion() + " and " + placed.scrapeProgress());
                helper.succeed();
            });
        });
    }

    private static ItemStack drop(GameTestHelper helper, int x) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, RAIL_Z));
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusShuttlesItems.FARM_RAIL.get()), "one farm rail drops, got " + drops);
        return drops.get(0);
    }

    /** An aging step moves the wet rails and leaves the rails behind a corroded one. */
    @ServusGameTest(timeoutTicks = 40)
    public static void agingSkipsDryRails(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, 5, 5, FieldCorrosion.CORRODED);
            head.age(perStep());
            assertRails(helper, FIRST, 4, FieldCorrosion.EXPOSED, "wet rails age");
            assertRails(helper, 5, 5, FieldCorrosion.CORRODED, "the corroded rail stays corroded");
            assertRails(helper, 6, LAST, FieldCorrosion.COPPER, "rails behind the corroded one keep their stage");
            helper.succeed();
        });
    }

    /** A corroded middle rail dries the rails behind it; the rails before it stay watered and the rust rim shows. */
    @ServusGameTest(timeoutTicks = 80)
    public static void corrodedMiddleDriesBehind(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.hydrated() && wet(helper, LAST), "the field should be watered first");
                setStages(helper, head, 4, 4, FieldCorrosion.CORRODED);
                helper.assertTrue(head.dryRails() == RAILS - 1, "every rail from the corroded one on is dry, got " + head.dryRails());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(head.hydrated() && wet(helper, FIRST), "the rail before the corroded one stays watered");
                    helper.assertTrue(!wet(helper, LAST), "farmland beside the dry rails dries");
                    helper.assertTrue(helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the head shows the rust rim");
                    helper.succeed();
                });
            });
        });
    }

    /** A corroded first rail stops all water: no hydration and no water used. */
    @ServusGameTest(timeoutTicks = 80)
    public static void corrodedFirstRailStopsWater(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(head.hydrated() && wet(helper, 4), "the field should be watered first");
                setStages(helper, head, FIRST, FIRST, FieldCorrosion.CORRODED);
                helper.runAfterDelay(2, () -> {
                    int water = head.water();
                    helper.assertTrue(!head.hydrated() && !wet(helper, 4), "a field with a corroded first rail is not watered");
                    helper.assertTrue(head.dryRails() == RAILS, "every rail is dry");
                    helper.runAfterDelay(5, () -> {
                        helper.assertTrue(head.water() == water, "no water is used");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** An iron axe scrapes five oxidized rails to weathered for {@code axe_damage_per_rail} each. */
    @ServusGameTest(timeoutTicks = 40)
    public static void axeScrapesField(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, FIRST, LAST, FieldCorrosion.OXIDIZED);
            Player player = TestPlayers.holding(helper, new ItemStack(Items.IRON_AXE));
            useOnRail(helper, player, 5);
            assertRails(helper, FIRST, LAST, FieldCorrosion.WEATHERED, "every rail is scraped one stage");
            int perRail = ShuttlesStatsManager.INSTANCE.axeDamagePerRail();
            helper.assertTrue(player.getMainHandItem().getDamageValue() == RAILS * perRail,
                    perRail + " durability per rail, got " + player.getMainHandItem().getDamageValue());
            helper.assertTrue(head.stageCounts()[FieldCorrosion.WEATHERED] == RAILS, "the head counts the scraped rails");
            helper.succeed();
        });
    }

    /** A worn axe spends what it has left: two rails go back a stage and the rest stays on the third as progress. */
    @ServusGameTest(timeoutTicks = 40)
    public static void wornAxeLeavesProgress(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, FIRST, LAST, FieldCorrosion.WEATHERED);
            int perRail = ShuttlesStatsManager.INSTANCE.axeDamagePerRail();
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            axe.setDamageValue(axe.getMaxDamage() - (2 * perRail + 2));
            Player player = TestPlayers.holding(helper, axe);
            useOnRail(helper, player, FIRST);
            assertRails(helper, FIRST, FIRST + 1, FieldCorrosion.EXPOSED, "the first two rails are scraped");
            assertRails(helper, FIRST + 2, LAST, FieldCorrosion.WEATHERED, "the rest stay weathered");
            helper.assertTrue(railAt(helper, FIRST + 2).scrapeProgress() == 2, "the third rail holds two points, got "
                    + railAt(helper, FIRST + 2).scrapeProgress());
            helper.assertTrue(railAt(helper, FIRST + 3).scrapeProgress() == 0, "the fourth rail holds none");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the axe breaks");
            helper.succeed();
        });
    }

    /** Scrape progress on a rail counts toward its next scrape. */
    @ServusGameTest(timeoutTicks = 40)
    public static void progressFinishesRail(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            railAt(helper, 5).setCorrosion(FieldCorrosion.WEATHERED, 2);
            head.railsChanged();
            Player player = TestPlayers.holding(helper, new ItemStack(Items.IRON_AXE));
            useOnRail(helper, player, 5);
            int perRail = ShuttlesStatsManager.INSTANCE.axeDamagePerRail();
            helper.assertTrue(stage(helper, 5) == FieldCorrosion.EXPOSED && railAt(helper, 5).scrapeProgress() == 0,
                    "the rail becomes exposed, got " + stage(helper, 5));
            helper.assertTrue(player.getMainHandItem().getDamageValue() == perRail - 2,
                    "the axe pays the rest, got " + player.getMainHandItem().getDamageValue());
            helper.succeed();
        });
    }

    /** Scraping a corroded first rail lets the water through again and takes the rust rim away. */
    @ServusGameTest(timeoutTicks = 60)
    public static void scrapingOpensWater(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        helper.runAfterDelay(3, () -> {
            setStages(helper, head, FIRST, FIRST, FieldCorrosion.CORRODED);
            FieldFixtures.fluid(helper, HEAD).fill(new FluidStack(Fluids.WATER, 1000), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(!head.hydrated(), "a corroded first rail keeps the field dry");
                useOnRail(helper, TestPlayers.holding(helper, new ItemStack(Items.IRON_AXE)), FIRST);
                helper.assertTrue(stage(helper, FIRST) == FieldCorrosion.OXIDIZED, "scraped to oxidized, got " + stage(helper, FIRST));
                helper.succeedWhen(() -> {
                    helper.assertTrue(head.hydrated() && head.dryRails() == 0, "the scraped field is watered again");
                    helper.assertTrue(!helper.getBlockState(HEAD).getValue(FieldHeadBlock.CORRODED), "the rust rim is gone");
                });
            });
        });
    }

    /** Fresh copper is not scraped. */
    @ServusGameTest(timeoutTicks = 40)
    public static void freshCopperNotScraped(GameTestHelper helper) {
        field(helper);
        helper.runAfterDelay(3, () -> {
            Player player = TestPlayers.holding(helper, new ItemStack(Items.IRON_AXE));
            useOnRail(helper, player, 4);
            assertRails(helper, FIRST, LAST, FieldCorrosion.COPPER, "copper stays copper");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "the axe keeps its durability");
            helper.succeed();
        });
    }

    /** A hopper above the head keeps its honeycomb: only supply runs fill the buffer. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperCannotFillBuffer(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        BlockPos hopperPos = HEAD.above();
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.HONEYCOMB, 2));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 2, "the hopper keeps its honeycomb");
            helper.assertTrue(head.buffer().count(SupplyBuffer.Part.ANTI_CORROSION) == 0, "the anti-corrosion part stays empty");
            helper.succeed();
        });
    }

    /** The anti-corrosion part takes anti-corrosion items up to its level and nothing else. */
    @ServusGameTest(timeoutTicks = 20)
    public static void antiCorrosionPartTakesOnlyItsItem(GameTestHelper helper) {
        FieldHeadBlockEntity head = field(helper);
        int level = ShuttlesStatsManager.INSTANCE.fieldAntiCorrosionLevel();
        helper.assertTrue(head.fillSupply(SupplyBuffer.Part.ANTI_CORROSION, Items.WHEAT_SEEDS, 2) == 0, "seeds are refused");
        helper.assertTrue(head.fillSupply(SupplyBuffer.Part.ANTI_CORROSION, Items.HONEYCOMB, level + 5) == level,
                "honeycomb goes in up to the level");
        helper.assertTrue(head.supplyCount(SupplyBuffer.Part.ANTI_CORROSION) == level, "the part holds its level");
        helper.succeed();
    }
}
