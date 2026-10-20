package io.github.dirnei.mc.servus.foundation.machine.anchor;

import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineTestSupport;
import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea.Shift;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/** The Ender Anchor's area: upgrades, offset, FE per chunk and the team cap in chunks. */
public final class AnchorGameTests {

    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private AnchorGameTests() {
    }

    /** Places a powered anchor of {@code team} with {@code upgrades} Anchor Upgrades. */
    static EnderAnchorBlockEntity anchor(GameTestHelper helper, BlockPos pos, UUID team, int upgrades) {
        helper.setBlock(pos, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
        EnderAnchorBlockEntity anchor = helper.getBlockEntity(pos, EnderAnchorBlockEntity.class);
        anchor.setOwner(team);
        for (int slot = 0; slot < upgrades; slot++) {
            anchor.upgrades().insertItem(slot, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        }
        anchor.energyStorage().receiveEnergy(20000, false);
        return anchor;
    }

    /** The square the anchor at {@code pos} should hold, from its own chunk. */
    private static Set<Long> expected(GameTestHelper helper, BlockPos pos, int radius, int offsetX, int offsetZ) {
        ChunkPos own = ChunkPos.containing(helper.absolutePos(pos));
        return new HashSet<>(AnchorArea.of(radius, offsetX, offsetZ).chunks(own.x(), own.z()));
    }

    /** A stand-in loader of {@code team} holding {@code chunks} chunks far away from the test. */
    private static void standIn(GameTestHelper helper, UUID team, int chunks) {
        ServerLevel level = helper.getLevel();
        List<Long> packed = new ArrayList<>();
        for (int i = 0; i < chunks; i++) {
            packed.add(AnchorArea.pack(200000 + i % 64, 200000 + i / 64));
        }
        data(helper).register(team, "standin@" + team, EnderAnchorData.chunks(level.dimension(), packed));
    }

    private static EnderAnchorData data(GameTestHelper helper) {
        return EnderAnchorData.get(helper.getLevel().getServer());
    }

    private static int cap(GameTestHelper helper) {
        int cap = ServusFoundationConfig.maxLoadedChunksPerTeam();
        helper.assertTrue(cap >= 12, "the test needs a cap of at least 12 chunks, got " + cap);
        return cap;
    }

    /** No upgrade loads its own chunk, one a 3 by 3 square, two a 5 by 5 square; the team load follows. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorLoadsItsSquare(GameTestHelper helper) {
        BlockPos[] at = {ANCHOR, ANCHOR.above(), ANCHOR.above(2)};
        UUID[] teams = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        EnderAnchorBlockEntity[] anchors = new EnderAnchorBlockEntity[3];
        for (int upgrades = 0; upgrades < 3; upgrades++) {
            anchors[upgrades] = anchor(helper, at[upgrades], teams[upgrades], upgrades);
        }
        helper.runAfterDelay(5, () -> {
            int[] sizes = {1, 9, 25};
            for (int upgrades = 0; upgrades < 3; upgrades++) {
                EnderAnchorBlockEntity anchor = anchors[upgrades];
                helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "active, status " + anchor.status());
                helper.assertTrue(anchor.area().size() == sizes[upgrades], "size " + anchor.area().size());
                helper.assertTrue(anchor.heldChunks().equals(expected(helper, at[upgrades], upgrades, 0, 0)),
                        upgrades + " upgrades hold their square, held " + anchor.heldChunks().size());
                helper.assertTrue(data(helper).load(teams[upgrades]) == sizes[upgrades], "team load " + data(helper).load(teams[upgrades]));
            }
            helper.succeed();
        });
    }

    /** The offset stays within the radius, keeps the own chunk inside, moves the tickets and shrinks with the upgrades. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorOffsetKeepsOwnChunk(GameTestHelper helper) {
        EnderAnchorBlockEntity north = anchor(helper, ANCHOR, UUID.randomUUID(), 1);
        helper.assertTrue(north.shift(Shift.NORTH), "one chunk north is allowed");
        helper.assertFalse(north.shift(Shift.NORTH), "two chunks north is refused");
        helper.assertTrue(north.area().offsetZ() == -1, "offset stays one north, was " + north.area().offsetZ());
        EnderAnchorBlockEntity east = anchor(helper, ANCHOR.above(), UUID.randomUUID(), 1);
        EnderAnchorBlockEntity shrink = anchor(helper, ANCHOR.above(2), UUID.randomUUID(), 2);
        shrink.shift(Shift.EAST);
        shrink.shift(Shift.EAST);
        helper.assertTrue(shrink.area().offsetX() == 2, "two east with two upgrades");
        shrink.upgrades().extractItem(1, 1, false);
        helper.assertTrue(shrink.area().offsetX() == 1 && shrink.area().size() == 9,
                "losing an upgrade shrinks the offset to one east, was " + shrink.area());
        helper.runAfterDelay(5, () -> {
            ChunkPos own = ChunkPos.containing(helper.absolutePos(ANCHOR));
            helper.assertTrue(north.heldChunks().equals(expected(helper, ANCHOR, 1, 0, -1)), "the square north of the anchor is held");
            helper.assertTrue(north.heldChunks().contains(ChunkPos.pack(own.x(), own.z())), "the own chunk is in the southern row");
            helper.assertTrue(east.heldChunks().equals(expected(helper, ANCHOR.above(), 1, 0, 0)), "centred before the shift");
            east.shift(Shift.EAST);
            helper.runAfterDelay(3, () -> {
                Set<Long> held = east.heldChunks();
                helper.assertTrue(held.equals(expected(helper, ANCHOR.above(), 1, 1, 0)), "the tickets moved east");
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertFalse(held.contains(ChunkPos.pack(own.x() - 1, own.z() + dz)), "the west edge is released");
                }
                helper.succeed();
            });
        });
    }

    /** An anchor whose area would push the load over the cap stays inactive and loads nothing. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorRefusedOverCap(GameTestHelper helper) {
        int cap = cap(helper);
        UUID nearlyFull = UUID.randomUUID();
        standIn(helper, nearlyFull, cap - 9);
        EnderAnchorBlockEntity big = anchor(helper, ANCHOR, nearlyFull, 2);
        UUID full = UUID.randomUUID();
        standIn(helper, full, cap);
        EnderAnchorBlockEntity small = anchor(helper, ANCHOR.above(), full, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(big.status() == EnderAnchorBlockEntity.STATUS_TEAM_LIMIT, "team limit, status " + big.status());
            helper.assertTrue(!big.ticketHeld(), "none of the area is loaded");
            helper.assertTrue(data(helper).load(nearlyFull) == cap - 9, "the load stays, was " + data(helper).load(nearlyFull));
            helper.assertTrue(small.status() == EnderAnchorBlockEntity.STATUS_TEAM_LIMIT, "a full team refuses even one chunk");
            helper.assertTrue(data(helper).load(full) == cap, "the full load stays");
            helper.assertFalse(helper.getBlockState(ANCHOR).getValue(EnderAnchorBlock.LIT), "the refused anchor's eye stays shut");
            helper.succeed();
        });
    }

    /** Two overlapping 3 by 3 squares of one team count 12 chunks, not 18. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorOverlapCountedOnce(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        EnderAnchorBlockEntity first = anchor(helper, ANCHOR, team, 1);
        EnderAnchorBlockEntity second = anchor(helper, ANCHOR.above(), team, 1);
        second.shift(Shift.EAST);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(first.status() == EnderAnchorBlockEntity.STATUS_ACTIVE
                    && second.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "both active");
            helper.assertTrue(data(helper).load(team) == 12, "load 12, was " + data(helper).load(team));
            helper.succeed();
        });
    }

    /** An active anchor that grows past the cap is checked without its old area, then lets go of everything. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorGrowOverCapReleases(GameTestHelper helper) {
        int cap = cap(helper);
        UUID team = UUID.randomUUID();
        standIn(helper, team, cap - 12);
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, team, 1);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "active with one upgrade");
            helper.assertTrue(data(helper).load(team) == cap - 3, "load " + data(helper).load(team));
            anchor.upgrades().insertItem(1, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_TEAM_LIMIT, "team limit, status " + anchor.status());
                helper.assertTrue(!anchor.ticketHeld(), "the area is released");
                helper.assertTrue(data(helper).load(team) == cap - 12, "only the stand-in is left, load " + data(helper).load(team));
                helper.succeed();
            });
        });
    }

    /** A 5 by 5 anchor spends its energy per tick 25 times over. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorPaysPerChunk(GameTestHelper helper) {
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, UUID.randomUUID(), 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "active");
            int before = anchor.energyStorage().getEnergyStored();
            helper.runAfterDelay(10, () -> {
                int spent = before - anchor.energyStorage().getEnergyStored();
                int perTick = anchor.stats().energyPerTick() * 25;
                helper.assertTrue(spent == 10 * perTick, "10 ticks cost " + 10 * perTick + " FE, spent " + spent);
                helper.succeed();
            });
        });
    }

    /**
     * With {@code enderAnchorRespectsFtbChunks} on, an anchor next to FTB Chunks stays inactive and loads nothing;
     * without FTB Chunks the option changes nothing. Only this anchor sees the option on; the config stays as it is.
     */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorStaysInactiveForFtbChunks(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, team, 1);
        anchor.respectFtbChunksAs(true);
        helper.runAfterDelay(5, () -> {
            if (ModList.get().isLoaded(ServusFoundationConfig.FTB_CHUNKS)) {
                helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_FTB_CHUNKS, "use FTB Chunks, status " + anchor.status());
                helper.assertTrue(!anchor.ticketHeld(), "no chunk is loaded");
                helper.assertTrue(data(helper).load(team) == 0, "no team load, was " + data(helper).load(team));
            } else {
                helper.assertTrue(anchor.status() == EnderAnchorBlockEntity.STATUS_ACTIVE, "active without FTB Chunks, status " + anchor.status());
                helper.assertTrue(anchor.heldChunks().size() == 9, "the square is held");
            }
            helper.succeed();
        });
    }

    /** Only Anchor Upgrades go in, one per slot, and automation sees no slot. */
    @ServusGameTest(timeoutTicks = 20)
    public static void anchorSlotsTakeOnlyAnchorUpgrades(GameTestHelper helper) {
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, UUID.randomUUID(), 0);
        helper.assertTrue(anchor.upgrades().getSlots() == 2, "two slots");
        helper.assertFalse(anchor.upgrades().isItemValid(0, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())), "no Speed Upgrade");
        ItemStack rest = anchor.upgrades().insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get(), 2), false);
        helper.assertTrue(rest.getCount() == 1 && anchor.upgrades().count() == 1, "one upgrade per slot");
        for (Direction side : Direction.values()) {
            helper.assertTrue(anchor.sideHandler(side).size() == 0, "automation reaches no slot from " + side);
        }
        helper.succeed();
    }

    /** Breaking an active anchor releases its whole square and its share of the team load, and drops the upgrades. */
    @ServusGameTest(timeoutTicks = 60)
    public static void anchorBreakReleasesArea(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, team, 1);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(anchor.heldChunks().size() == 9, "nine chunks held");
            helper.setBlock(ANCHOR, Blocks.AIR.defaultBlockState());
            helper.assertTrue(!anchor.ticketHeld(), "breaking releases every chunk");
            helper.assertTrue(data(helper).load(team) == 0, "the team load is free again");
            List<ItemEntity> drops = GameTestSteps.drops(helper, ANCHOR, 1);
            int upgrades = MachineTestSupport.count(drops, MachineContent.ANCHOR_UPGRADE.get());
            helper.assertTrue(upgrades == 1, "the upgrade drops, got " + upgrades);
            drops.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    /** The menu's arrow buttons shift the area on the server, the GUI data follows and stops at the limit. */
    @ServusGameTest(timeoutTicks = 20)
    public static void anchorMenuButtonsShift(GameTestHelper helper) {
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, UUID.randomUUID(), 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EnderAnchorMenu menu = new EnderAnchorMenu(0, player.getInventory(), anchor);
        helper.assertTrue(menu.clickMenuButton(player, Shift.EAST.ordinal()), "the east arrow is a button");
        helper.assertTrue(anchor.area().offsetX() == 1, "one chunk east, was " + anchor.area());
        menu.clickMenuButton(player, Shift.EAST.ordinal());
        helper.assertTrue(anchor.area().offsetX() == 1, "the limit holds");
        helper.assertTrue(menu.area().equals(anchor.area()) && !menu.area().canShift(Shift.EAST), "the GUI greys the east arrow");
        helper.assertTrue(menu.teamCap() == ServusFoundationConfig.maxLoadedChunksPerTeam(), "the GUI shows the cap");
        helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(MachineContent.ANCHOR_UPGRADE.get())), "the first slot is an upgrade slot");
        helper.assertFalse(menu.slots.get(0).mayPlace(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())), "no Speed Upgrade");
        helper.succeed();
    }

    /** Sneaking with the carrot dismantles the anchor with its upgrades and offset on the item. */
    @ServusGameTest(timeoutTicks = 20)
    public static void anchorDismantleKeepsUpgradesAndOffset(GameTestHelper helper) {
        EnderAnchorBlockEntity anchor = anchor(helper, ANCHOR, UUID.randomUUID(), 2);
        anchor.shift(Shift.EAST);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, ANCHOR, Direction.NORTH);
        helper.assertBlockNotPresent(MachineContent.ENDER_ANCHOR.get(), ANCHOR);
        List<ItemEntity> drops = GameTestSteps.drops(helper, ANCHOR, 1);
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(MachineContent.ANCHOR_UPGRADE.get())), "no upgrade drops on its own");
        ItemStack item = drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(MachineContent.ENDER_ANCHOR_ITEM.get())).findFirst().orElseThrow();
        BlockPos again = ANCHOR.east(4);
        helper.setBlock(again, MachineContent.ENDER_ANCHOR.get().defaultBlockState());
        EnderAnchorBlockEntity placed = helper.getBlockEntity(again, EnderAnchorBlockEntity.class);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(again), item);
        helper.assertTrue(placed.upgrades().count() == 2, "the upgrades travel, got " + placed.upgrades().count());
        helper.assertTrue(placed.area().offsetX() == 1, "the offset travels, was " + placed.area());
        helper.assertTrue(!placed.ticketHeld(), "a placed anchor holds no tickets of its old place");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }
}
