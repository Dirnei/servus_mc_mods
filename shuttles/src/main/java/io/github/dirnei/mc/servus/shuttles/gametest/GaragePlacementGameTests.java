package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** A placed garage turns its doorway (the side opposite its front) towards a rail at hand. */
public final class GaragePlacementGameTests {

    private static final BlockPos GARAGE = new BlockPos(3, 1, 3);
    /** Yaw of a player looking north. */
    private static final float NORTH = 180;

    private GaragePlacementGameTests() {
    }

    /** Clicking the east side of a rail puts the garage east of it with the doorway west. */
    @ServusGameTest
    public static void garageClickedOnRailSideFacesIt(GameTestHelper helper) {
        BlockPos rail = GARAGE.west();
        floor(helper);
        WarehouseFixtures.rail(helper, rail);
        place(helper, rail, Direction.EAST);
        assertDoorway(helper, Direction.WEST);
        helper.succeed();
    }

    /** On the floor with a rail only to the west, the doorway turns west. */
    @ServusGameTest
    public static void garageTurnsToOnlyRailBeside(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, GARAGE.west());
        placeOnFloor(helper);
        assertDoorway(helper, Direction.WEST);
        helper.succeed();
    }

    /** Rails north and west: the one where the player looks wins. */
    @ServusGameTest
    public static void railInLookDirectionWins(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, GARAGE.west());
        WarehouseFixtures.rail(helper, GARAGE.north());
        placeOnFloor(helper);
        assertDoorway(helper, Direction.NORTH);
        helper.succeed();
    }

    /** Rails east and west, none where the player looks: the front faces the player. */
    @ServusGameTest
    public static void severalRailsFaceThePlayer(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, GARAGE.west());
        WarehouseFixtures.rail(helper, GARAGE.east());
        placeOnFloor(helper);
        assertDoorway(helper, Direction.NORTH);
        helper.succeed();
    }

    /** No rail beside it: the front faces the player as before. */
    @ServusGameTest
    public static void noRailFacesThePlayer(GameTestHelper helper) {
        floor(helper);
        placeOnFloor(helper);
        assertDoorway(helper, Direction.NORTH);
        helper.succeed();
    }

    /** The rail behind a placed garage draws its arm towards it. */
    @ServusGameTest
    public static void railShowsArmToPlacedGarage(GameTestHelper helper) {
        BlockPos rail = GARAGE.west();
        floor(helper);
        WarehouseFixtures.rail(helper, rail);
        place(helper, rail, Direction.EAST);
        helper.assertBlockProperty(rail, ConnectableBlock.EAST, true);
        helper.succeed();
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                WarehouseFixtures.stone(helper, new BlockPos(x, 0, z));
            }
        }
    }

    private static void placeOnFloor(GameTestHelper helper) {
        place(helper, GARAGE.below(), Direction.UP);
    }

    /** A player looking north uses a garage item on that face of the clicked block. */
    private static void place(GameTestHelper helper, BlockPos clicked, Direction face) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 away = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 1, 8)));
        player.snapTo(away);
        player.setYRot(NORTH);
        ItemStack stack = new ItemStack(ServusShuttlesItems.SHUTTLE_GARAGE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(clicked);
        Vec3 hit = Vec3.atCenterOf(absolute).relative(face, 0.5);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, absolute, false)));
    }

    private static void assertDoorway(GameTestHelper helper, Direction doorway) {
        helper.assertBlockProperty(GARAGE, ShuttleGarageBlock.FACING, doorway.getOpposite());
    }
}
