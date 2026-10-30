package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Fluid Rack as a block: shelves, the port, buckets, pipe access while
 * no rail stands at the port, dismantling and breaking, and zones. Racks in
 * these tests face north, so a player in front sees east on the left.
 */
public final class FluidRackGameTests {

    private static final Direction FRONT = Direction.NORTH;
    private static final int PER_BAY = 16000;

    private FluidRackGameTests() {
    }

    /** Three bays from west to east, at z, as a row facing north. */
    private static BlockPos[] three(int z) {
        return new BlockPos[] {new BlockPos(3, 1, z), new BlockPos(4, 1, z), new BlockPos(5, 1, z)};
    }

    /** Uses a fluid container on the front of the bay, as a right click there does: the block takes the click first. */
    private static void useBucket(GameTestHelper helper, Player player, ItemStack bucket, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(FRONT, 0.5), FRONT, absolute, false));
    }

    private static ResourceHandler<FluidResource> capability(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side);
    }

    /** Three bays placed side by side form one shelf with the port on the middle bay only. */
    @ServusGameTest(timeoutTicks = 40)
    public static void threeBaysOneShelfPortInMiddle(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FluidRackFixtures.shelf(helper, bays[0]).size() == 3, "one shelf of three, got "
                    + FluidRackFixtures.shelf(helper, bays[0]));
            helper.assertTrue(!FluidRackFixtures.port(helper, bays[0]) && FluidRackFixtures.port(helper, bays[1])
                    && !FluidRackFixtures.port(helper, bays[2]), "only the middle bay carries the port");
            helper.assertTrue(FluidRackFixtures.bay(helper, bays[0]).shelfHandler().content().capacity() == 3 * PER_BAY,
                    "the shelf holds three bays' worth");
            helper.succeed();
        });
    }

    /** A shelf of two has its port on the bay to the left of a player in front of it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void portOfTwoOnTheLeft(GameTestHelper helper) {
        BlockPos west = new BlockPos(3, 1, 3);
        BlockPos east = west.east();
        FluidRackFixtures.row(helper, FRONT, west, east);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FluidRackFixtures.shelf(helper, west).equals(List.of(east, west)), "east is the left bay seen from the north, got "
                    + FluidRackFixtures.shelf(helper, west));
            helper.assertTrue(FluidRackFixtures.port(helper, east) && !FluidRackFixtures.port(helper, west), "the port is on the left bay");
            helper.succeed();
        });
    }

    /** Breaking the right bay of three leaves two bays whose port moves to their left bay. */
    @ServusGameTest(timeoutTicks = 40)
    public static void portMovesWhenBayBreaks(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(FluidRackFixtures.port(helper, bays[1]), "the middle bay carries the port first");
            helper.destroyBlock(bays[0]);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(FluidRackFixtures.shelf(helper, bays[1]).size() == 2, "two bays are left in the shelf");
                helper.assertTrue(FluidRackFixtures.port(helper, bays[2]) && !FluidRackFixtures.port(helper, bays[1]),
                        "the port moved to the left bay of the two");
                helper.succeed();
            });
        });
    }

    /** A rack item carrying lava placed beside a bay holding water stands as a shelf of its own. */
    @ServusGameTest(timeoutTicks = 40)
    public static void lavaDoesNotJoinWater(GameTestHelper helper) {
        BlockPos water = new BlockPos(3, 1, 3);
        BlockPos lava = water.east();
        FluidRackFixtures.place(helper, water, FRONT, FluidRackFixtures.carrying(Fluids.WATER, 4000));
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.place(helper, lava, FRONT, FluidRackFixtures.carrying(Fluids.LAVA, 2000));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(FluidRackFixtures.shelf(helper, lava).size() == 1, "the lava bay stands alone");
                helper.assertTrue(FluidRackFixtures.content(helper, water).is(Fluids.WATER) && FluidRackFixtures.amount(helper, water) == 4000,
                        "the water bay keeps its water");
                helper.assertTrue(FluidRackFixtures.content(helper, lava).is(Fluids.LAVA) && FluidRackFixtures.amount(helper, lava) == 2000,
                        "the lava bay keeps its lava");
                helper.succeed();
            });
        });
    }

    /** A row of empty bays on top of a row holding water takes the same boundaries and holds no water. */
    @ServusGameTest(timeoutTicks = 40)
    public static void stackedRowsStayApart(GameTestHelper helper) {
        BlockPos[] bottom = three(3);
        FluidRackFixtures.row(helper, FRONT, bottom);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bottom[0], Fluids.WATER, 30000);
            BlockPos[] top = {bottom[0].above(), bottom[1].above(), bottom[2].above()};
            FluidRackFixtures.row(helper, Direction.EAST, top);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(helper.getBlockState(top[0]).getValue(FluidRackBlock.FACING) == FRONT, "the top row copies the facing below");
                helper.assertTrue(FluidRackFixtures.shelf(helper, top[0]).size() == 3, "the top row is one shelf of three");
                helper.assertTrue(FluidRackFixtures.content(helper, top[0]).isEmpty(), "the top row holds no water");
                helper.assertTrue(FluidRackFixtures.amount(helper, bottom[2]) == 30000, "the bottom row keeps its water");
                helper.assertTrue(FluidRackFixtures.port(helper, top[1]), "the top row has its own port");
                helper.succeed();
            });
        });
    }

    /** A bay with no rail in front is a tank on every side: a duct draws its fluid arm and filling through it works. */
    @ServusGameTest(timeoutTicks = 40)
    public static void standaloneBayFilledByDuct(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        BlockPos duct = rack.east();
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            GameTestSteps.duct(helper, duct, Line.FLUID);
            helper.runAfterDelay(2, () -> {
                for (Direction side : Direction.values()) {
                    helper.assertTrue(capability(helper, rack, side) != null, "the bay offers its tank on " + side);
                }
                helper.assertBlockProperty(duct, ConnectableBlock.WEST, true);
                int filled = Transfer.fill(capability(helper, rack, Direction.EAST), new FluidStack(Fluids.WATER, 3000), false);
                helper.assertTrue(filled == 3000 && FluidRackFixtures.amount(helper, rack) == 3000, "the pipe fills the bay, got " + filled);
                helper.succeed();
            });
        });
    }

    /** A rail placed in front of the port closes every bay to pipes and the duct loses its arm. */
    @ServusGameTest(timeoutTicks = 40)
    public static void railAtPortClosesPipeAccess(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        BlockPos duct = bays[2].east();
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            GameTestSteps.duct(helper, duct, Line.FLUID);
            helper.runAfterDelay(2, () -> {
                helper.assertBlockProperty(duct, ConnectableBlock.WEST, true);
                WarehouseFixtures.railOnStone(helper, bays[1].relative(FRONT));
                helper.runAfterDelay(2, () -> {
                    for (BlockPos bay : bays) {
                        helper.assertTrue(capability(helper, bay, Direction.EAST) == null, "no bay offers a tank, " + bay + " did");
                    }
                    helper.assertBlockProperty(duct, ConnectableBlock.WEST, false);
                    helper.succeed();
                });
            });
        });
    }

    /** A rail in front of a bay without the port shows no dock and leaves the shelf open to pipes. */
    @ServusGameTest(timeoutTicks = 40)
    public static void railAtOtherBayDoesNotJoin(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        FluidRackFixtures.row(helper, FRONT, bays);
        BlockPos rail = bays[0].relative(FRONT);
        helper.runAfterDelay(2, () -> {
            WarehouseFixtures.railOnStone(helper, rail);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!helper.getBlockState(rail).getValue(RailBlock.dock(FRONT.getOpposite())), "the rail shows no dock");
                helper.assertTrue(capability(helper, bays[0], Direction.UP) != null, "the shelf is still a tank for pipes");
                helper.assertTrue(FluidRackFixtures.bay(helper, bays[0]).standalone(), "the shelf is not docked");
                helper.succeed();
            });
        });
    }

    /** Rails hang in front of every bay of a raised shelf; only the port bay's rail docks. */
    @ServusGameTest(timeoutTicks = 40)
    public static void railsHangAlongRaisedShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(3, 4, 3), new BlockPos(4, 4, 3), new BlockPos(5, 4, 3)};
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            for (BlockPos bay : bays) {
                WarehouseFixtures.rail(helper, bay.relative(FRONT));
            }
            helper.runAfterDelay(5, () -> {
                for (int i = 0; i < bays.length; i++) {
                    BlockPos rail = bays[i].relative(FRONT);
                    helper.assertTrue(helper.getBlockState(rail).getBlock() instanceof RailBlock, "rail " + i + " should hang");
                    helper.assertTrue(helper.getBlockState(rail).getValue(RailBlock.dock(FRONT.getOpposite())) == (i == 1),
                            "only the middle rail docks");
                }
                helper.succeed();
            });
        });
    }

    /** A water bucket on the front fills the shelf and leaves an empty bucket; the empty bucket takes it back. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bucketFillsAndEmpties(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), rack);
            helper.assertTrue(FluidRackFixtures.amount(helper, rack) == 1000, "the shelf holds 1000 mB of water");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the player holds an empty bucket");
            useBucket(helper, player, player.getMainHandItem(), rack);
            helper.assertTrue(FluidRackFixtures.amount(helper, rack) == 0, "the bucket took the water back");
            helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the player holds a water bucket again");
            helper.succeed();
        });
    }

    /** An empty hand on the front opens nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void emptyHandOpensNothing(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos absolute = helper.absolutePos(rack);
            BlockState state = helper.getBlockState(rack);
            InteractionResult result = state.useWithoutItem(helper.getLevel(), player,
                    new BlockHitResult(Vec3.atCenterOf(absolute).relative(FRONT, 0.5), FRONT, absolute, false));
            helper.assertTrue(result == InteractionResult.PASS, "the click passes, got " + result);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "no menu is open");
            helper.succeed();
        });
    }

    /** Sneaking with the carrot dismantles a bay into an item that keeps its lava; placing it brings the lava back. */
    @ServusGameTest(timeoutTicks = 60)
    public static void dismantleKeepsShare(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        BlockPos again = new BlockPos(7, 1, 7);
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, rack, Fluids.LAVA, 12000);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            GameTestSteps.useCarrot(helper, player, rack, FRONT);
            helper.assertTrue(helper.getBlockState(rack).isAir(), "the bay is gone");
            List<ItemStack> drops = FluidRackFixtures.dropped(helper, rack);
            helper.assertTrue(drops.size() == 1, "one rack item drops, got " + drops);
            SimpleFluidContent share = drops.getFirst().get(ServusShuttlesComponents.RACK_FLUID.get());
            helper.assertTrue(share != null && share.is(Fluids.LAVA) && share.getAmount() == 12000, "the item carries 12000 mB of lava");
            FluidRackFixtures.place(helper, again, FRONT, drops.getFirst().copy());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(FluidRackFixtures.content(helper, again).is(Fluids.LAVA) && FluidRackFixtures.amount(helper, again) == 12000,
                        "the placed bay holds 12000 mB of lava, got " + FluidRackFixtures.content(helper, again));
                helper.succeed();
            });
        });
    }

    /** Breaking the right bay of a shelf holding 30000 mB drops 10000 mB with it and leaves 20000 mB. */
    @ServusGameTest(timeoutTicks = 60)
    public static void breakingBayDropsShare(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[1], Fluids.WATER, 30000);
            helper.destroyBlock(bays[0]);
            helper.runAfterDelay(1, () -> {
                List<ItemStack> drops = FluidRackFixtures.dropped(helper, bays[0]);
                helper.assertTrue(drops.size() == 1, "one rack item drops, got " + drops);
                SimpleFluidContent share = drops.getFirst().get(ServusShuttlesComponents.RACK_FLUID.get());
                helper.assertTrue(share != null && share.getAmount() == 10000, "the item carries 10000 mB of water");
                helper.assertTrue(FluidRackFixtures.amount(helper, bays[1]) == 20000, "the two bays hold 20000 mB, got "
                        + FluidRackFixtures.amount(helper, bays[1]));
                helper.succeed();
            });
        });
    }

    /** A handheld scan of one bay zones all three bays; a scan with No zone active clears the whole shelf. */
    @ServusGameTest(timeoutTicks = 40)
    public static void scanZonesShelfAndNoZoneClears(GameTestHelper helper) {
        BlockPos[] bays = three(3);
        FluidRackFixtures.row(helper, FRONT, bays);
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(helper, "Cooling", ZoneFilter.EMPTY), bays[0], FRONT);
            for (BlockPos bay : bays) {
                helper.assertTrue(FluidRackFixtures.bay(helper, bay).zone().equals("Cooling"), "every bay is in Cooling, " + bay + " is not");
            }
            GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(ZoneList.NONE), bays[2], FRONT);
            for (BlockPos bay : bays) {
                helper.assertTrue(FluidRackFixtures.bay(helper, bay).zone().isEmpty(), "every bay is unzoned again, " + bay + " is not");
            }
            helper.succeed();
        });
    }

    /** A bay placed beside a zoned shelf joins it and takes its zone. */
    @ServusGameTest(timeoutTicks = 40)
    public static void joiningBayTakesZone(GameTestHelper helper) {
        BlockPos first = new BlockPos(3, 1, 3);
        BlockPos second = first.east();
        FluidRackFixtures.place(helper, first, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(helper, "Cooling", ZoneFilter.EMPTY), first, FRONT);
            FluidRackFixtures.place(helper, second, FRONT, FluidRackFixtures.item());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(FluidRackFixtures.shelf(helper, first).size() == 2, "the bays joined");
                helper.assertTrue(FluidRackFixtures.bay(helper, second).zone().equals("Cooling"), "the new bay took the zone");
                helper.succeed();
            });
        });
    }

    /** A shelf zoned with a water bucket refuses a lava bucket; the player keeps it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void waterZoneRefusesLavaBucket(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameTestSteps.useOn(helper, player,
                    ZoneFixtures.handheld(helper, "Cooling", new ZoneFilter(List.of(new ItemStack(Items.WATER_BUCKET)), List.of())), rack, FRONT);
            useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), rack);
            helper.assertTrue(FluidRackFixtures.content(helper, rack).isEmpty(), "the shelf takes no lava");
            helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET), "the player keeps the lava bucket");
            useBucket(helper, player, new ItemStack(Items.WATER_BUCKET), rack);
            helper.assertTrue(FluidRackFixtures.amount(helper, rack) == 1000, "the shelf takes water");
            helper.succeed();
        });
    }

    /** A zone whose filter lists bread and an item tag but no fluid takes any fluid. */
    @ServusGameTest(timeoutTicks = 40)
    public static void zoneWithoutFluidsTakesAnything(GameTestHelper helper) {
        BlockPos rack = new BlockPos(3, 1, 3);
        FluidRackFixtures.place(helper, rack, FRONT, FluidRackFixtures.item());
        helper.runAfterDelay(2, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameTestSteps.useOn(helper, player,
                    ZoneFixtures.handheld(helper, "Back", new ZoneFilter(List.of(new ItemStack(Items.BREAD)), List.of("c:ores"))), rack, FRONT);
            FluidRackBlockEntity bay = FluidRackFixtures.bay(helper, rack);
            helper.assertTrue(bay.zone().equals("Back") && bay.zoneFluids().isEmpty(), "the zone lists no fluids");
            helper.assertTrue(FluidRackFixtures.fill(helper, rack, Fluids.LAVA, 2000) == 2000, "the shelf takes lava");
            helper.succeed();
        });
    }
}
