package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.dataDuct;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Data cable tiers: mixed tiers link, ducts drop their tier, higher tiers replace lower ones in place. */
public final class DataCableTierGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private DataCableTierGameTests() {
    }

    private static void use(GameTestHelper helper, Player player, Item item, BlockPos pos, Direction face) {
        GameTestSteps.useOn(helper, player, new ItemStack(item), pos, face);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** A Data Cable duct and a Fast Data Cable duct that touch form one data network. */
    @ServusGameTest(timeoutTicks = 40)
    public static void mixedTiersOneNetwork(GameTestHelper helper) {
        UtilityDuctBlockEntity slow = dataDuct(helper, new BlockPos(3, 1, 3), ServusLogisticsItems.DATA_CABLE.get());
        UtilityDuctBlockEntity fast = dataDuct(helper, new BlockPos(4, 1, 3), ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.succeedWhen(() -> {
            Optional<DataNetwork> a = slow.dataNetwork();
            Optional<DataNetwork> b = fast.dataNetwork();
            helper.assertTrue(a.isPresent() && b.isPresent() && a.get() == b.get(), "both ducts share one data network");
            helper.assertTrue(a.get().cables() == 2, "the network has two cables, got " + a.get().cables());
            helper.assertTrue(fast.dataTier().itemsPerSecond() == 128 && fast.dataTier().duplex(), "the fast duct is 128 duplex");
            helper.assertTrue(slow.dataTier().itemsPerSecond() == 32 && !slow.dataTier().duplex(), "the base duct is 32 simplex");
        });
    }

    /** A broken duct carrying a Fast Data Cable line drops one Fast Data Cable and no base cable. */
    @ServusGameTest(timeoutTicks = 40)
    public static void brokenFastDuctDropsFast(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        dataDuct(helper, pos, ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            helper.destroyBlock(pos);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(dropped(helper, ServusLogisticsItems.FAST_DATA_CABLE.get()) == 1, "one Fast Data Cable drops");
                helper.assertTrue(dropped(helper, ServusLogisticsItems.DATA_CABLE.get()) == 0, "no Data Cable drops");
                helper.succeed();
            });
        });
    }

    /** A Fast Data Cable used on a Data Cable duct replaces the line in place and drops the old cable. */
    @ServusGameTest(timeoutTicks = 40)
    public static void upgradeCableDropsOld(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(3, 1, 3);
        UtilityDuctBlockEntity first = dataDuct(helper, pos, ServusLogisticsItems.DATA_CABLE.get());
        dataDuct(helper, pos.east(), ServusLogisticsItems.DATA_CABLE.get());
        first.setMode(Line.DATA, Direction.UP, DuctMode.NONE);
        ItemStack held = new ItemStack(ServusLogisticsItems.FAST_DATA_CABLE.get(), 3);
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        held.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, held,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false)));
        helper.runAfterDelay(1, () -> {
            UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
            helper.assertTrue(duct.dataTierId().equals(ServusLogisticsItems.FAST_DATA_CABLE.get().tierId()),
                    "the duct carries the Fast Data Cable line, got " + duct.dataTierId());
            helper.assertTrue(held.getCount() == 2, "one Fast Data Cable is used up, holds " + held.getCount());
            helper.assertTrue(dropped(helper, ServusLogisticsItems.DATA_CABLE.get()) == 1, "one Data Cable drops");
            helper.assertBlockNotPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), pos.north());
            helper.assertTrue(duct.hasArm(Line.DATA, Direction.EAST), "the link to the east duct stays");
            helper.assertTrue(duct.mode(Line.DATA, Direction.UP) == DuctMode.NONE,
                    "the side setting stays");
            helper.succeed();
        });
    }

    /** A Data Cable used on a Fast Data Cable duct changes nothing there and places a new duct beside it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noDowngrade(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(3, 1, 3);
        dataDuct(helper, pos, ServusLogisticsItems.FAST_DATA_CABLE.get());
        use(helper, player, ServusLogisticsItems.DATA_CABLE.get(), pos, Direction.EAST);
        use(helper, player, ServusLogisticsItems.FAST_DATA_CABLE.get(), pos, Direction.SOUTH);
        helper.runAfterDelay(1, () -> {
            UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
            helper.assertTrue(duct.dataTierId().equals(ServusLogisticsItems.FAST_DATA_CABLE.get().tierId()),
                    "the duct keeps the Fast Data Cable line, got " + duct.dataTierId());
            helper.assertTrue(dropped(helper, ServusLogisticsItems.FAST_DATA_CABLE.get()) == 0, "nothing drops");
            helper.assertTrue(helper.getBlockEntity(pos.east(), UtilityDuctBlockEntity.class).dataTierId()
                    .equals(ServusLogisticsItems.DATA_CABLE.get().tierId()), "the Data Cable is placed beside it");
            helper.assertTrue(helper.getBlockEntity(pos.south(), UtilityDuctBlockEntity.class).has(Line.DATA),
                    "an equal tier places a new duct beside it as well");
            helper.succeed();
        });
    }

    /** A Duplex Data Cable used on a bare duct side places a duct of its tier. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedCableKeepsTier(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        dataDuct(helper, new BlockPos(3, 1, 3), ServusLogisticsItems.DATA_CABLE.get());
        player.setShiftKeyDown(true);
        use(helper, player, ServusLogisticsItems.DUPLEX_DATA_CABLE.get(), new BlockPos(3, 1, 3), Direction.EAST);
        UtilityDuctBlockEntity placed = helper.getBlockEntity(new BlockPos(4, 1, 3), UtilityDuctBlockEntity.class);
        helper.assertTrue(placed.has(Line.DATA), "the new duct carries the data line");
        helper.assertTrue(placed.dataTierId().equals(ServusLogisticsItems.DUPLEX_DATA_CABLE.get().tierId()),
                "the new duct is a Duplex Data Cable, got " + placed.dataTierId());
        helper.succeed();
    }
}
