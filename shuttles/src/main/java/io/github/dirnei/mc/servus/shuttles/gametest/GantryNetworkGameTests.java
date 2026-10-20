package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid Gantries in the warehouse: joining by rail and the fluid stock. */
public final class GantryNetworkGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 1);
    private static final BlockPos RAIL = new BlockPos(4, 1, 4);
    private static final BlockPos SECOND = new BlockPos(4, 1, 7);
    private static final FluidKey WATER = FluidKey.of(new FluidStack(Fluids.WATER, 1));

    private GantryNetworkGameTests() {
    }

    /** A controller and a north to south rail from it, z 2 to 9 on x 4. */
    private static WarehouseControllerBlockEntity warehouse(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 9);
        return controller;
    }

    private static boolean sameWarehouse(WarehouseControllerBlockEntity controller, FluidGantryBlockEntity gantry) {
        return gantry.network().isPresent() && gantry.network().equals(controller.network());
    }

    private static WarehouseNetwork network(WarehouseControllerBlockEntity controller) {
        return controller.network().orElseThrow();
    }

    /** A gantry over a rail of an online warehouse joins it and lights its lamp on the beam. */
    @ServusGameTest
    public static void gantryJoinsByItsRail(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        Waits.when(helper, () -> helper.getBlockState(RAIL.above()).getValue(StorageNodeBlocks.ONLINE), 20,
                () -> "the beam shows the connection lamp", () -> {
            helper.assertTrue(sameWarehouse(controller, gantry), "the gantry is part of the warehouse");
            helper.assertTrue(WarehouseFixtures.lit(helper, RAIL.west()), "the master is online");
            helper.succeed();
        });
    }

    /** A leg touching the controller does not join when the rail belongs to no warehouse. */
    @ServusGameTest
    public static void gantryTouchingControllerStaysOut(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, RAIL.west(2));
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!sameWarehouse(controller, gantry), "touching the controller is not enough");
            helper.assertTrue(!WarehouseFixtures.lit(helper, RAIL.west()), "the lamp stays dark");
            helper.succeed();
        });
    }

    /** The rail through the gantry keeps its straight track: no arm and no dock toward the legs. */
    @ServusGameTest
    public static void railShowsNoDockArmToLegs(GameTestHelper helper) {
        warehouse(helper);
        GantryFixtures.place(helper, RAIL);
        BlockState rail = helper.getBlockState(RAIL);
        helper.assertTrue(rail.getValue(ConnectableBlock.NORTH) && rail.getValue(ConnectableBlock.SOUTH), "the rail runs on");
        helper.assertTrue(!rail.getValue(ConnectableBlock.EAST) && !rail.getValue(ConnectableBlock.WEST), "no arm toward the legs");
        helper.assertTrue(!rail.getValue(RailBlock.DOCK_EAST) && !rail.getValue(RailBlock.DOCK_WEST), "no dock toward the legs");
        helper.succeed();
    }

    /** The tanks of two Storage gantries add up to the warehouse's water stock. */
    @ServusGameTest
    public static void storageGantriesAreFluidStock(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        GantryFixtures.place(helper, RAIL).tank(0).fill(new FluidStack(Fluids.WATER, 3000), false);
        GantryFixtures.place(helper, SECOND).tank(1).fill(new FluidStack(Fluids.WATER, 5000), false);
        Waits.when(helper, () -> network(controller).fluidIndex().count(WATER) == 8000, 20,
                () -> "the warehouse counts 8000 mB of water, got " + network(controller).fluidIndex().count(WATER), () -> {
            helper.assertTrue(network(controller).gantries().size() == 2, "both gantries are listed");
            helper.assertTrue(network(controller).fluidIndex().sourcesHolding(WATER).size() == 2, "two tanks hold water");
            helper.succeed();
        });
    }

    /** An Import gantry's water is no stock, and the gantry is listed as Import. */
    @ServusGameTest
    public static void importGantryIsNotStock(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.setMode(GantryMode.IMPORT);
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 4000), false);
        Waits.when(helper, () -> !network(controller).tankViews(gantry.getBlockPos()).isEmpty()
                        && network(controller).tankViews(gantry.getBlockPos()).getFirst().fluid().getAmount() == 4000, 20,
                () -> "the warehouse reads the Import gantry's tanks", () -> {
            helper.assertTrue(network(controller).fluidIndex().count(WATER) == 0, "Import water is no stock");
            helper.assertTrue(network(controller).tankViews(gantry.getBlockPos()).getFirst().mode() == GantryMode.IMPORT, "listed as Import");
            helper.succeed();
        });
    }

    /** A bucket emptied by hand into a Storage gantry reaches the fluid stock within a second. */
    @ServusGameTest
    public static void bucketChangeReachesIndexWithinASecond(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        GantryFixtures.place(helper, RAIL);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(network(controller).fluidIndex().count(WATER) == 0, "no water yet");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            BlockPos leg = helper.absolutePos(RAIL.east().above());
            helper.getLevel().getBlockState(leg).useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(leg), Direction.EAST, leg, false));
            Waits.when(helper, () -> network(controller).fluidIndex().count(WATER) == 1000, 20,
                    () -> "the stock shows the bucket's 1000 mB within a second", helper::succeed);
        });
    }

    /** {@code /servus_warehouse fluids} lists the water total and every gantry with its mode. */
    @ServusGameTest
    public static void fluidsCommandListsGantries(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        GantryFixtures.place(helper, RAIL).tank(0).fill(new FluidStack(Fluids.WATER, 3000), false);
        FluidGantryBlockEntity export = GantryFixtures.place(helper, SECOND);
        export.setMode(GantryMode.EXPORT);
        Waits.when(helper, () -> network(controller).fluidIndex().count(WATER) == 3000 && network(controller).tankViews(export.getBlockPos()).size() == 2,
                20, () -> "the warehouse read both gantries", () -> {
            List<String> lines = new ArrayList<>();
            CommandSource collector = new CommandSource() {
                @Override
                public void sendSystemMessage(Component message) {
                    lines.add(message.getString());
                }

                @Override
                public boolean acceptsSuccess() {
                    return true;
                }

                @Override
                public boolean acceptsFailure() {
                    return true;
                }

                @Override
                public boolean shouldInformAdmins() {
                    return false;
                }
            };
            CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withSource(collector);
            BlockPos at = helper.absolutePos(CONTROLLER);
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                    "servus_warehouse fluids " + at.getX() + " " + at.getY() + " " + at.getZ());
            String text = String.join("\n", lines);
            helper.assertTrue(text.contains("Water: 3000 mB"), "the total is listed: " + text);
            helper.assertTrue(text.contains("storage") && text.contains("export"), "both gantries are listed with their modes: " + text);
            helper.succeed();
        });
    }
}
