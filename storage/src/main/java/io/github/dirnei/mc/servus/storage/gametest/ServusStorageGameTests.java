package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.production.StockSink;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.storage.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.storage.zone.ZoneFilter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusStorageGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);

    private static final BlockPos RAIL_CRATE = new BlockPos(2, 1, 3);
    private static final BlockPos RAIL_A = new BlockPos(3, 1, 3);
    private static final BlockPos RAIL_B = new BlockPos(4, 1, 3);
    private static final BlockPos RAIL_CONTROLLER = new BlockPos(5, 1, 3);
    private static final BlockPos RAIL_TERMINAL = new BlockPos(6, 1, 3);
    private static final BlockPos RAIL_CHEST = new BlockPos(3, 1, 4);

    private ServusStorageGameTests() {
    }

    /** Two rails link a crate to the controller; a chest beside a rail stays out of the index. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void railsLinkCrateAndController(GameTestHelper helper) {
        helper.setBlock(RAIL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(RAIL_A.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_B.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_A, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(RAIL_B, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(RAIL_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, RAIL_TERMINAL);
        helper.setBlock(RAIL_CHEST, Blocks.CHEST.defaultBlockState());
        CrateBlockEntity crate = helper.getBlockEntity(RAIL_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 40), false);
        ChestBlockEntity chest = helper.getBlockEntity(RAIL_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RAIL_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the rails");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 40, "stock should list 40 cobblestone from the crate");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 0, "bread beside a rail must not be indexed");
            helper.assertTrue(lit(helper, RAIL_CRATE), "crate lamp should be lit while online");

            helper.setBlock(RAIL_A, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                StorageTerminalMenu after = new StorageTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(stockOf(after, Items.COBBLESTONE) == 0, "crate stock should leave the index after the rail is broken");
                helper.assertTrue(!lit(helper, RAIL_CRATE), "crate lamp should go dark once cut off");
                helper.assertTrue(lit(helper, TerminalWiring.terminalPos(RAIL_TERMINAL)), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    private static final BlockPos LIFT_CRATE = new BlockPos(2, 1, 3);
    private static final BlockPos LIFT_RAIL_LOW = new BlockPos(3, 1, 3);
    private static final BlockPos LIFT_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos LIFT_MIDDLE = new BlockPos(4, 2, 3);
    private static final BlockPos LIFT_TOP = new BlockPos(4, 3, 3);
    private static final BlockPos LIFT_RAIL_HIGH = new BlockPos(5, 3, 3);
    private static final BlockPos LIFT_CONTROLLER = new BlockPos(6, 3, 3);
    private static final BlockPos LIFT_TERMINAL = new BlockPos(7, 3, 3);
    private static final BlockPos LIFT_CHEST = new BlockPos(4, 2, 4);
    private static final BlockPos LIFT_SIDE_CRATE = new BlockPos(4, 1, 4);

    /** A three segment shaft links a ground floor crate to a controller two floors up; a chest on the shaft stays out. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void elevatorLinksFloors(GameTestHelper helper) {
        helper.setBlock(LIFT_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(LIFT_RAIL_LOW.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_BOTTOM.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_LOW, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(LIFT_BOTTOM, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_MIDDLE, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_TOP, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_CONTROLLER.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_TERMINAL.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(LIFT_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, LIFT_TERMINAL);
        helper.setBlock(LIFT_CHEST, Blocks.CHEST.defaultBlockState());
        helper.setBlock(LIFT_SIDE_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(LIFT_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 30), false);
        CrateBlockEntity sideCrate = helper.getBlockEntity(LIFT_SIDE_CRATE);
        sideCrate.insert(new ItemStack(Items.APPLE, 7), false);
        ChestBlockEntity chest = helper.getBlockEntity(LIFT_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LIFT_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the shaft");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 30, "stock should list 30 cobblestone from the ground floor crate");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 0, "bread beside the shaft must not be indexed");
            helper.assertTrue(lit(helper, LIFT_CRATE), "crate lamp should be lit through the shaft");
            helper.assertTrue(stockOf(menu, Items.APPLE) == 0, "a crate touching only a segment must not join through it");
            helper.assertTrue(!lit(helper, LIFT_SIDE_CRATE), "that crate's lamp stays dark");

            helper.setBlock(LIFT_MIDDLE, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                StorageTerminalMenu after = new StorageTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(stockOf(after, Items.COBBLESTONE) == 0, "crate stock should leave the index after the shaft is cut");
                helper.assertTrue(!lit(helper, LIFT_CRATE), "crate lamp should go dark once the shaft is cut");
                helper.assertTrue(lit(helper, TerminalWiring.terminalPos(LIFT_TERMINAL)), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    private static final BlockPos NC_SEGMENT = new BlockPos(6, 1, 6);

    /** An entity above a segment stands on its top plate; one that descends or is inside passes. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void elevatorStandsFromAbove(GameTestHelper helper) {
        helper.setBlock(NC_SEGMENT.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(NC_SEGMENT, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        BlockState state = helper.getBlockState(NC_SEGMENT);
        BlockPos absolute = helper.absolutePos(NC_SEGMENT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        VoxelShape above = state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player));
        helper.assertTrue(!above.isEmpty() && above.max(Direction.Axis.Y) == 1.0, "an entity above should stand on the top plate");
        player.setShiftKeyDown(true);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "a sneaking player should sink through");
        player.setShiftKeyDown(false);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.2, absolute.getZ() + 0.5);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "an entity inside the segment should pass");
        helper.assertTrue(state.isScaffolding(player), "sneaking should not hold the player like a ladder");
        helper.succeed();
    }

    /** A segment has at most its top plate as collision, keeps a full outline to target it, and is climbable. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void elevatorHasNoCollision(GameTestHelper helper) {
        helper.setBlock(NC_SEGMENT.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(NC_SEGMENT, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        BlockState state = helper.getBlockState(NC_SEGMENT);
        BlockPos absolute = helper.absolutePos(NC_SEGMENT);
        VoxelShape plain = state.getCollisionShape(helper.getLevel(), absolute);
        helper.assertTrue(plain.min(Direction.Axis.Y) >= 14.0 / 16.0, "without an entity only the top plate collides, like scaffolding");
        helper.assertTrue(!state.getShape(helper.getLevel(), absolute).isEmpty(), "a segment keeps an outline");
        helper.assertTrue(state.is(BlockTags.CLIMBABLE), "a segment is climbable");
        helper.succeed();
    }

    private static final BlockPos QUAD = new BlockPos(8, 1, 8);
    private static final BlockPos QUAD_CONTROLLER = new BlockPos(9, 1, 8);
    private static final BlockPos QUAD_TERMINAL = new BlockPos(10, 1, 8);

    /** A quad crate keeps two types in two compartments, refuses a mismatch, and the terminal lists both. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void quadCrateCompartments(GameTestHelper helper) {
        helper.setBlock(QUAD, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(QUAD_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, QUAD_TERMINAL);
        CrateBlockEntity crate = helper.getBlockEntity(QUAD);
        helper.assertTrue(crate.compartments() == 4, "quad crate should have four compartments");
        helper.assertTrue(crate.insert(0, new ItemStack(Items.COBBLESTONE, 40), false) == 40, "compartment 0 takes cobblestone");
        helper.assertTrue(crate.insert(3, new ItemStack(Items.DIRT, 12), false) == 12, "compartment 3 takes dirt");
        helper.assertTrue(crate.insert(0, new ItemStack(Items.DIRT, 1), false) == 0, "compartment 0 refuses dirt");
        helper.assertTrue(crate.handler().getSlots() == 4, "handler exposes four slots");
        helper.assertTrue(crate.handler().getStackInSlot(0).getCount() == 40, "slot 0 reports 40 cobblestone");
        helper.assertTrue(crate.handler().getStackInSlot(3).is(Items.DIRT), "slot 3 reports dirt");
        helper.assertTrue(crate.handler().getStackInSlot(1).isEmpty(), "slot 1 is empty");
        helper.assertTrue(crate.insert(new ItemStack(Items.COBBLESTONE, 5), false) == 5 && crate.count(0) == 45,
                "untargeted insert joins the cobblestone compartment");
        helper.assertTrue(crate.capacityPerCompartment() * 4 == crate.capacity(), "compartments split the capacity");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, QUAD_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 45, "terminal lists 45 cobblestone");
            helper.assertTrue(stockOf(menu, Items.DIRT) == 12, "terminal lists 12 dirt");
            helper.succeed();
        });
    }

    private static final BlockPos RACK = new BlockPos(8, 1, 2);
    private static final BlockPos RACK_CONTROLLER = new BlockPos(9, 1, 2);
    private static final BlockPos RACK_TERMINAL = new BlockPos(10, 1, 2);

    /** A rack is indexed, lit, and shows the item types it holds the most of. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void heavyRackIndexesAndDisplays(GameTestHelper helper) {
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(RACK_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, RACK_TERMINAL);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        rack.items().insertItem(1, new ItemStack(Items.DIRT, 32), false);
        rack.items().insertItem(2, new ItemStack(Items.SAND, 16), false);
        rack.items().insertItem(3, new ItemStack(Items.GRAVEL, 8), false);
        rack.items().insertItem(4, new ItemStack(Items.APPLE, 1), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RACK_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 64 && stockOf(menu, Items.APPLE) == 1, "terminal lists the rack contents");
            List<ItemStack> sample = rack.displayed();
            helper.assertTrue(sample.size() == 4, "sample shows four types, had " + sample.size());
            helper.assertTrue(sample.get(0).is(Items.COBBLESTONE) && sample.get(3).is(Items.GRAVEL), "sample is ordered by count");

            rack.items().insertItem(5, new ItemStack(Items.APPLE, 64), false);
            rack.items().insertItem(6, new ItemStack(Items.APPLE, 36), false);
            helper.runAfterDelay(10, () -> {
                List<ItemStack> after = rack.displayed();
                helper.assertTrue(after.get(0).is(Items.APPLE), "apples lead the sample after the refill");
                helper.assertTrue(after.stream().noneMatch(stack -> stack.is(Items.GRAVEL)), "gravel dropped out of the sample");
                helper.succeed();
            });
        });
    }

    private static final BlockPos SH_CONTROLLER = new BlockPos(2, 1, 6);
    private static final BlockPos SH_TERMINAL = new BlockPos(3, 1, 7);
    private static final BlockPos SH_DIRECT_CRATE = new BlockPos(1, 1, 6);
    private static final BlockPos SH_FAR_CRATE = new BlockPos(7, 1, 7);
    private static final BlockPos SH_GARAGE = new BlockPos(3, 1, 5);

    /** A shuttle fetches from a crate at the end of the rail; a crate on the controller is still instant. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlePicksFromRail(GameTestHelper helper) {
        helper.setBlock(SH_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, SH_TERMINAL);
        helper.setBlock(SH_DIRECT_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 6), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 6), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SH_FAR_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity far = helper.getBlockEntity(SH_FAR_CRATE);
        far.insert(new ItemStack(Items.COBBLESTONE, 40), false);
        CrateBlockEntity direct = helper.getBlockEntity(SH_DIRECT_CRATE);
        direct.insert(new ItemStack(Items.DIRT, 9), false);
        helper.setBlock(SH_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SH_GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SH_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "cobblestone must not arrive at once");
            menu.request(ItemKey.of(new ItemStack(Items.DIRT)), 9);
            helper.assertTrue(direct.isEmpty(), "dirt from the crate on the controller is picked at once");
            helper.runAfterDelay(20, () -> helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0,
                    "shuttle is still on its way after 20 ticks"));
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 40 && terminal.outputCount(Items.DIRT) == 9
                            && menu.stock().requests().size() == 1 && menu.stock().requests().get(0).state().equals("MISSING"), 360,
                    () -> "40 cobblestone and 9 dirt should arrive, had " + terminal.outputCount(Items.COBBLESTONE) + " and "
                            + terminal.outputCount(Items.DIRT) + ", rows " + menu.stock().requests(), () -> {
                helper.assertTrue(far.isEmpty(), "far crate should be empty");
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).missing() == 24 && rows.get(0).state().equals("MISSING"),
                        "the dirt request is gone and the cobblestone request shows 24 missing, got " + rows);
                helper.succeed();
            });
        });
    }

    private static final BlockPos FR_RACK = new BlockPos(5, 1, 9);
    private static final BlockPos FR_SIDE_RAIL = new BlockPos(6, 1, 9);
    private static final BlockPos FR_SIDE_CONTROLLER = new BlockPos(7, 1, 9);
    private static final BlockPos FR_SIDE_TERMINAL = new BlockPos(8, 1, 9);
    private static final BlockPos FR_FRONT_RAIL = new BlockPos(5, 1, 8);
    private static final BlockPos FR_FRONT_CONTROLLER = new BlockPos(4, 1, 8);
    private static final BlockPos FR_FRONT_TERMINAL = new BlockPos(3, 1, 8);

    /** A rail at the side of a rack does not link it; a rail at its front does. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void rackLinksRailOnlyFromFront(GameTestHelper helper) {
        helper.setBlock(FR_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(FR_SIDE_RAIL.below(), Blocks.STONE.defaultBlockState());
        placeRail(helper, FR_SIDE_RAIL);
        helper.setBlock(FR_SIDE_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, FR_SIDE_TERMINAL);
        HeavyRackBlockEntity rack = helper.getBlockEntity(FR_RACK);
        rack.items().insertItem(0, new ItemStack(Items.SAND, 20), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity sideTerminal = TerminalWiring.at(helper, FR_SIDE_TERMINAL);
            StorageTerminalMenu sideMenu = new StorageTerminalMenu(1, player.getInventory(), sideTerminal);
            helper.assertTrue(sideMenu.stock().online(), "side warehouse is online");
            helper.assertTrue(stockOf(sideMenu, Items.SAND) == 0, "a rail at the rack's side must not link it");
            helper.assertTrue(!helper.getBlockState(FR_SIDE_RAIL).getValue(RailBlock.DOCK_WEST), "no dock onto the rack's side");

            helper.setBlock(FR_FRONT_RAIL.below(), Blocks.STONE.defaultBlockState());
            placeRail(helper, FR_FRONT_RAIL);
            helper.setBlock(FR_FRONT_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                    .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
            TerminalWiring.place(helper, FR_FRONT_TERMINAL);
            helper.runAfterDelay(10, () -> {
                StorageTerminalBlockEntity frontTerminal = TerminalWiring.at(helper, FR_FRONT_TERMINAL);
                StorageTerminalMenu frontMenu = new StorageTerminalMenu(1, player.getInventory(), frontTerminal);
                helper.assertTrue(frontMenu.stock().online(), "front warehouse is online");
                helper.assertTrue(stockOf(frontMenu, Items.SAND) == 20, "a rail at the rack's front links it");
                helper.assertTrue(helper.getBlockState(FR_FRONT_RAIL).getValue(RailBlock.DOCK_SOUTH), "dock onto the rack's front");
                helper.succeed();
            });
        });
    }

    /** Places a rail the way a player would: its connection and dock flags come from the neighbours. */
    private static void placeRail(GameTestHelper helper, BlockPos pos) {
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    private static final BlockPos STACK_BASE = new BlockPos(10, 1, 10);

    /** A rack placed on top of a rack takes the lower rack's facing, whatever way the player looks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackOnRackCopiesFacing(GameTestHelper helper) {
        helper.setBlock(STACK_BASE, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.EAST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        BlockPos above = helper.absolutePos(STACK_BASE.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(above), Direction.UP, above, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        net.minecraft.world.InteractionResult placed = rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.runAfterDelay(2, () -> {
            BlockState top = helper.getBlockState(STACK_BASE.above());
            helper.assertTrue(top.getBlock() instanceof HeavyRackBlock, "a rack should have been placed on top, got " + top.getBlock() + " result " + placed);
            helper.assertTrue(top.getValue(HeavyRackBlock.FACING) == Direction.EAST, "the top rack should face east like the one below");
            helper.succeed();
        });
    }

    private static final BlockPos WOOD_RACK = new BlockPos(2, 1, 2);

    /** Assembles the rack recipe from a grid whose middle row holds the given planks. */
    private static ItemStack craftRack(GameTestHelper helper, Item left, Item middle, Item right) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                iron, ItemStack.EMPTY, iron,
                new ItemStack(left), new ItemStack(middle), new ItemStack(right),
                iron, ItemStack.EMPTY, iron));
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "heavy_rack")).orElseThrow();
        @SuppressWarnings("unchecked")
        CraftingRecipe recipe = ((RecipeHolder<CraftingRecipe>) holder).value();
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the rack recipe should match the grid");
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    private static ResourceLocation idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    /** The recipe remembers spruce, the majority, and the middle of three different woods. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void rackRecipePicksWood(GameTestHelper helper) {
        ItemStack spruce = craftRack(helper, Items.SPRUCE_PLANKS, Items.SPRUCE_PLANKS, Items.SPRUCE_PLANKS);
        helper.assertTrue(spruce.is(ServusStorageItems.HEAVY_RACK.get()), "the result is a rack");
        helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(spruce.get(ServusStorageComponents.RACK_WOOD.get())), "three spruce planks give spruce");
        ItemStack majority = craftRack(helper, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.OAK_PLANKS);
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(majority.get(ServusStorageComponents.RACK_WOOD.get())), "oak, spruce, oak gives oak");
        ItemStack middle = craftRack(helper, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS, Items.OAK_PLANKS);
        helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(middle.get(ServusStorageComponents.RACK_WOOD.get())), "birch, spruce, oak gives spruce");
        helper.succeed();
    }

    /** A placed spruce rack keeps spruce, and its drops and dismantled item carry it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackKeepsWood(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(WOOD_RACK);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        rackItem.set(ServusStorageComponents.RACK_WOOD.get(), idOf(Blocks.SPRUCE_PLANKS));
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.runAfterDelay(2, () -> {
            HeavyRackBlockEntity rack = helper.getBlockEntity(WOOD_RACK);
            helper.assertTrue(idOf(Blocks.SPRUCE_PLANKS).equals(rack.wood()), "the placed rack remembers spruce, had " + rack.wood());
            List<ItemStack> drops = Block.getDrops(helper.getBlockState(WOOD_RACK), helper.getLevel(), absolute, rack);
            helper.assertTrue(drops.size() == 1 && HeavyRackBlockEntity.woodOf(drops.get(0)).equals(idOf(Blocks.SPRUCE_PLANKS)),
                    "the dropped rack carries spruce, drops " + drops);
            ItemStack dismantled = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
            rack.saveToItem(dismantled, helper.getLevel().registryAccess());
            helper.assertTrue(HeavyRackBlockEntity.woodOf(dismantled).equals(idOf(Blocks.SPRUCE_PLANKS)), "the dismantled rack carries spruce");
            helper.succeed();
        });
    }

    /** A rack without a stored wood shows oak. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void rackWithoutWoodIsOak(GameTestHelper helper) {
        helper.setBlock(WOOD_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(WOOD_RACK);
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(rack.wood()), "a rack without wood is oak");
        helper.assertTrue(idOf(Blocks.OAK_PLANKS).equals(HeavyRackBlockEntity.woodOf(new ItemStack(ServusStorageBlocks.HEAVY_RACK.get()))),
                "a rack item without wood is oak");
        helper.succeed();
    }

    private static final int WOOD_Z = 4;

    /** Places a rack of that wood by hand with the mock player looking south, so it faces north unless a rack is below. */
    private static void placeWoodRack(GameTestHelper helper, Player player, BlockPos pos, Block planks) {
        BlockPos absolute = helper.absolutePos(pos);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        rackItem.set(ServusStorageComponents.RACK_WOOD.get(), idOf(planks));
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    /** Spruce beside oak stays apart, spruce beside spruce joins, and a rack on top copies the facing whatever its wood. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackJoinsOnlySameWood(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        BlockPos oak = new BlockPos(1, 1, WOOD_Z);
        BlockPos spruce = new BlockPos(2, 1, WOOD_Z);
        BlockPos spruce2 = new BlockPos(3, 1, WOOD_Z);
        BlockPos top = new BlockPos(1, 2, WOOD_Z);
        placeWoodRack(helper, player, oak, Blocks.OAK_PLANKS);
        placeWoodRack(helper, player, spruce, Blocks.SPRUCE_PLANKS);
        placeWoodRack(helper, player, spruce2, Blocks.SPRUCE_PLANKS);
        placeWoodRack(helper, player, top, Blocks.SPRUCE_PLANKS);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(spruce).getValue(HeavyRackBlock.FACING) == Direction.NORTH, "racks face north");
            helper.assertTrue(!helper.getBlockState(spruce).getValue(HeavyRackBlock.LEFT), "spruce does not join the oak beside it");
            helper.assertTrue(!helper.getBlockState(oak).getValue(HeavyRackBlock.RIGHT), "oak does not join the spruce beside it");
            helper.assertTrue(helper.getBlockState(spruce).getValue(HeavyRackBlock.RIGHT)
                    && helper.getBlockState(spruce2).getValue(HeavyRackBlock.LEFT), "two spruce racks join");
            helper.assertTrue(helper.getBlockState(top).getValue(HeavyRackBlock.FACING) == Direction.NORTH, "the stacked rack copies the facing");
            helper.succeed();
        });
    }

    private static final int SEC_Z = 11;

    /** Places a rack through its item with the mock player looking south, so the rack faces north. */
    private static void placeRackByHand(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static boolean joined(GameTestHelper helper, int x, BooleanProperty side) {
        return helper.getBlockState(new BlockPos(x, 1, SEC_Z)).getValue(side);
    }

    /** Sections form in placement order and never move: three join, a fourth at either end stands alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSectionsStayPut(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 4; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "bays 4 and 5 join");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "bays 5 and 6 join");
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 6, HeavyRackBlock.RIGHT), "the section has posts at both ends");

        placeRackByHand(helper, player, new BlockPos(7, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 6, HeavyRackBlock.RIGHT) && !joined(helper, 7, HeavyRackBlock.LEFT), "a fourth bay on the right stands alone");
        placeRackByHand(helper, player, new BlockPos(3, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 3, HeavyRackBlock.RIGHT), "a fourth bay on the left stands alone");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "the first three are unchanged");

        placeRackByHand(helper, player, new BlockPos(8, 1, SEC_Z));
        helper.assertTrue(joined(helper, 7, HeavyRackBlock.RIGHT) && joined(helper, 8, HeavyRackBlock.LEFT), "a fifth bay joins the lone fourth");
        helper.assertTrue(!joined(helper, 7, HeavyRackBlock.LEFT), "the fourth still does not join the full section");
        helper.succeed();
    }

    private static final int UP_Z = 11;

    /** An upper row placed shifted by one splits above the same post as the row below. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void upperRowFollowsLowerSections(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 1; x <= 5; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, UP_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT), "lower row splits after the third bay");
        for (int x = 2; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 2, UP_Z));
        }
        BlockState up2 = helper.getBlockState(new BlockPos(2, 2, UP_Z));
        BlockState up3 = helper.getBlockState(new BlockPos(3, 2, UP_Z));
        BlockState up4 = helper.getBlockState(new BlockPos(4, 2, UP_Z));
        BlockState up5 = helper.getBlockState(new BlockPos(5, 2, UP_Z));
        BlockState up6 = helper.getBlockState(new BlockPos(6, 2, UP_Z));
        helper.assertTrue(up2.getValue(HeavyRackBlock.RIGHT) && up3.getValue(HeavyRackBlock.LEFT), "upper 2 and 3 join like below");
        helper.assertTrue(!up3.getValue(HeavyRackBlock.RIGHT) && !up4.getValue(HeavyRackBlock.LEFT), "upper row splits above the lower post");
        helper.assertTrue(up4.getValue(HeavyRackBlock.RIGHT) && up5.getValue(HeavyRackBlock.LEFT), "upper 4 and 5 join like below");
        helper.assertTrue(!up5.getValue(HeavyRackBlock.RIGHT) && !up6.getValue(HeavyRackBlock.LEFT), "upper 6 has no rack below and the rack below 5 ends its section");
        helper.succeed();
    }

    /** Four in a row, a gap, one more, then the gap filled: the filler joins the lone bays on both sides. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gapFillerJoinsBothLoneBays(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x : new int[] {1, 2, 3, 4, 6, 5}) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT) && !joined(helper, 4, HeavyRackBlock.LEFT), "the fourth bay stands apart from the first three");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "filler joins the fourth");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "filler joins the sixth");
        helper.succeed();
    }

    private static final int SM_Z = 6;

    /** Places a row of racks by hand from x {@code first} to {@code last}; they face north and join. */
    private static Player rackRow(GameTestHelper helper, int first, int last) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = first; x <= last; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SM_Z));
        }
        // Stand in front of the row so the menu's reach check holds.
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(first + 1, 1, SM_Z - 1))));
        return player;
    }

    private static HeavyRackBlockEntity rackAt(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 1, SM_Z));
    }

    /** The middle bay of a section of three opens all three bays, left to right, and shift click spills into the next bay. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSectionSharesMenu(GameTestHelper helper) {
        Player player = rackRow(helper, 2, 4);
        HeavyRackBlockEntity left = rackAt(helper, 2);
        HeavyRackBlockEntity middle = rackAt(helper, 3);
        HeavyRackBlockEntity right = rackAt(helper, 4);
        int bay = left.items().getSlots();
        for (int slot = 0; slot < bay; slot++) {
            left.items().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
        }
        middle.items().setStackInSlot(0, new ItemStack(Items.DIRT, 5));
        right.items().setStackInSlot(0, new ItemStack(Items.SAND, 7));

        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), middle);
        helper.assertTrue(menu.rackSlots() == 3 * bay, "a section of three shows " + 3 * bay + " slots, had " + menu.rackSlots());
        helper.assertTrue(menu.rows() == (3 * bay + 8) / 9, "the GUI has a row per nine slots, had " + menu.rows());
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.STONE), "the left bay comes first");
        helper.assertTrue(menu.getSlot(bay).getItem().is(Items.DIRT), "the middle bay follows the left one");
        helper.assertTrue(menu.getSlot(2 * bay).getItem().is(Items.SAND), "the right bay comes last");
        HeavyRackMenu fromLeft = HeavyRackMenu.forSection(2, player.getInventory(), left);
        helper.assertTrue(fromLeft.bays().equals(menu.bays()), "every bay of the section opens the same bays");
        helper.assertTrue(menu.stillValid(player), "the menu is valid while the section stands");

        player.getInventory().setItem(0, new ItemStack(Items.APPLE, 30));
        menu.quickMoveStack(player, menu.rackSlots() + 27);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the apples left the hotbar");
        helper.assertTrue(middle.items().getStackInSlot(1).is(Items.APPLE) && middle.items().getStackInSlot(1).getCount() == 30,
                "with the left bay full, the apples land in the middle bay, had " + middle.items().getStackInSlot(1));
        helper.succeed();
    }

    /** Breaking the right bay of a section spills only its items; the left bay keeps its own. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackBayBreaksAlone(GameTestHelper helper) {
        rackRow(helper, 2, 3);
        rackAt(helper, 2).items().setStackInSlot(0, new ItemStack(Items.DIRT, 5));
        rackAt(helper, 3).items().setStackInSlot(0, new ItemStack(Items.SAND, 7));
        helper.destroyBlock(new BlockPos(3, 1, SM_Z));
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(Items.SAND, new BlockPos(3, 1, SM_Z), 2.0);
            helper.assertItemEntityNotPresent(Items.DIRT, new BlockPos(3, 1, SM_Z), 4.0);
            ItemStack kept = rackAt(helper, 2).items().getStackInSlot(0);
            helper.assertTrue(kept.is(Items.DIRT) && kept.getCount() == 5, "the left bay keeps its dirt, had " + kept);
            helper.succeed();
        });
    }

    /** The section menu turns invalid once a bay of the section is removed. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackMenuClosesWhenSectionChanges(GameTestHelper helper) {
        Player player = rackRow(helper, 2, 4);
        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), rackAt(helper, 2));
        helper.assertTrue(menu.stillValid(player), "the menu is valid while the section stands");
        helper.setBlock(new BlockPos(4, 1, SM_Z), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!menu.stillValid(player), "the menu turns invalid once a bay is gone");
        helper.succeed();
    }

    private static final BlockPos FG_RACK = new BlockPos(2, 1, 11);

    /** With a rack in hand, the side of a rack takes a new rack; the front opens the GUI instead. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackPlacesAgainstRackSide(GameTestHelper helper) {
        helper.setBlock(FG_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        useRackOn(helper, player, FG_RACK, Direction.EAST);
        helper.assertTrue(helper.getBlockState(FG_RACK.east()).getBlock() instanceof HeavyRackBlock, "clicking the side places a rack beside it");
        useRackOn(helper, player, FG_RACK, Direction.NORTH);
        helper.assertTrue(!(helper.getBlockState(FG_RACK.north()).getBlock() instanceof HeavyRackBlock), "clicking the front opens the rack instead of placing");
        helper.succeed();
    }

    /** Uses a rack item on the given face of the block at pos, through the block's own interaction. */
    private static void useRackOn(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        BlockState state = helper.getBlockState(pos);
        ItemInteractionResult result = state.useItemOn(rackItem, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        } else if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }

    private static final BlockPos NR_RACK = new BlockPos(1, 1, 1);

    /** The carrot leaves a rack's facing and joins alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void carrotDoesNotTurnRack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        placeRackByHand(helper, player, NR_RACK);
        placeRackByHand(helper, player, NR_RACK.east());
        BlockPos absolute = helper.absolutePos(NR_RACK);
        ItemStack carrot = new ItemStack(io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        BlockState after = helper.getBlockState(NR_RACK);
        helper.assertTrue(after.getBlock() instanceof HeavyRackBlock, "the rack is still there");
        helper.assertTrue(after.getValue(HeavyRackBlock.FACING) == Direction.NORTH, "the carrot must not turn the rack");
        helper.assertTrue(after.getValue(HeavyRackBlock.RIGHT), "the join to the neighbour survives the click");
        helper.succeed();
    }

    private static final BlockPos CLIMB_RACK = new BlockPos(6, 2, 6);

    /** A player pressed against a rack side climbs or slides without fall damage; the front stays a wall. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSideClimbsAndFrontDoesNot(GameTestHelper helper) {
        helper.setBlock(CLIMB_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos rack = helper.absolutePos(CLIMB_RACK);
        double gravity = player.getGravity();

        // West side: the player's east face rests on the rack's west face, feet a little above the rack bottom.
        player.setPos(rack.getX() - 0.3, rack.getY() + 0.3, rack.getZ() + 0.5);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = false;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 0.0F, "hanging on the side clears the fall distance");
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.15) < 1.0E-9, "hanging slides at the ladder speed, got " + player.getDeltaMovement().y);

        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - (0.2 - gravity) * 0.98) < 1.0E-9, "pushing climbs at the ladder speed, got " + player.getDeltaMovement().y);

        // Front: the player's south face rests on the rack's north face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() - 0.3);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the front is not climbable");
        helper.assertTrue(player.getDeltaMovement().y == -0.5, "the front leaves the motion alone");

        // Back: the player's north face rests on the rack's south face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() + 1.3);
        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the back is not climbable");
        helper.succeed();
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static long stockOf(StorageTerminalMenu menu, Item item) {
        long total = 0;
        for (StockEntry entry : menu.stock().entries()) {
            if (entry.stack().is(item)) {
                total += entry.count();
            }
        }
        return total;
    }

    /** A request without shuttles lands in the output slots at once, the player collects it, and a push stores a stack back. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void terminalRequestsAndPushes(GameTestHelper helper) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, TERMINAL);
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 50), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online with one controller");
            helper.assertTrue(menu.stock().entries().size() == 1 && menu.stock().entries().get(0).count() == 50,
                    "stock should list 50 cobblestone");

            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(crate.isEmpty(), "crate should be empty after the request");
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 50
                            && menu.stock().requests().size() == 1 && menu.stock().requests().get(0).state().equals("MISSING"), 300,
                    () -> "50 cobblestone should arrive as packets, had " + terminal.outputCount(Items.COBBLESTONE) + ", rows " + menu.stock().requests(), () -> {
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "nothing goes into the inventory by itself");
            List<RequestRow> rows = menu.stock().requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 50 && rows.get(0).missing() == 14 && rows.get(0).state().equals("MISSING"),
                    "the request should show 50 delivered and 14 missing, got " + rows);
            menu.quickMoveStack(player, 0);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 50, "shift click should collect the delivery");
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the output slot should be empty");

            menu.cancel(rows.get(0).id());
            helper.assertTrue(menu.stock().requests().isEmpty(), "the cancelled request leaves the list");

            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.COBBLESTONE));
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 20));
            int menuIndex = menu.inventoryStart() + (slot < 9 ? 27 + slot : slot - 9);
            menu.quickMoveStack(player, menuIndex);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "inventory should be empty of cobblestone");
            // The shift click goes into the Input row, which stores it on the next tick.
            TerminalWiring.when(helper, () -> crate.count() == 20, 40,
                    () -> "crate should hold 20 cobblestone after the push, had " + crate.count(), () -> {
                menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
                helper.assertTrue(menu.stock().message().equals("out_of_stock"), "a request for nothing in stock is refused, got " + menu.stock().message());
                helper.succeed();
            });
            });
        });
    }

    private static final BlockPos ZL_CRATE = new BlockPos(2, 1, 9);
    private static final BlockPos ZL_RACK = new BlockPos(2, 1, 7);

    /** Uses the held label on the block's north face. */
    private static void useLabel(GameTestHelper helper, Player player, ItemStack label, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0, -0.5), Direction.NORTH, absolute, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, label);
        label.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ItemStack label(String name, ZoneFilter filter) {
        ItemStack label = new ItemStack(ServusStorageItems.ZONE_LABEL.get());
        if (name != null) {
            label.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        if (filter != null) {
            label.set(ServusStorageComponents.ZONE_FILTER.get(), filter);
        }
        return label;
    }

    /** A named label copies name and filter onto crates and racks, a restamp replaces, sneaking clears, an unnamed label does nothing, and the label stays. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void zoneLabelStampsAndClears(GameTestHelper helper) {
        helper.setBlock(ZL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(ZL_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ZL_CRATE);
        HeavyRackBlockEntity rack = helper.getBlockEntity(ZL_RACK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack unnamed = label(null, new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, unnamed, ZL_CRATE);
        helper.assertTrue(crate.zone().isEmpty(), "an unnamed label must not zone the crate");
        helper.assertTrue(crate.isEmpty(), "the label must not be inserted into the crate");

        ItemStack aisle = label(" Aisle 3 ", new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, aisle, ZL_CRATE);
        useLabel(helper, player, aisle, ZL_RACK);
        helper.assertTrue(crate.zone().equals("Aisle 3"), "crate zone should be trimmed to Aisle 3, was '" + crate.zone() + "'");
        helper.assertTrue(rack.zone().equals("Aisle 3"), "rack zone should be Aisle 3, was '" + rack.zone() + "'");
        helper.assertTrue(crate.zoneKey().equals("aisle 3"), "zone key should be lower case");
        helper.assertTrue(crate.zoneFilter().tags().equals(List.of("c:ores")), "the crate should carry the label's tags, had " + crate.zoneFilter().tags());
        helper.assertTrue(rack.getDisplayName().getString().endsWith("(Aisle 3)"), "rack title should carry the zone, was " + rack.getDisplayName().getString());
        helper.assertTrue(!aisle.isEmpty() && aisle.getCount() == 1, "the label must not be consumed");

        ItemStack food = label("Food", new ZoneFilter(List.of(new ItemStack(Items.BREAD)), List.of()));
        useLabel(helper, player, food, ZL_RACK);
        helper.assertTrue(rack.zone().equals("Food"), "restamp should replace the zone");
        helper.assertTrue(rack.zoneFilter().items().size() == 1 && rack.zoneFilter().items().get(0).is(Items.BREAD), "restamp should replace the filter");

        player.setShiftKeyDown(true);
        useLabel(helper, player, food, ZL_CRATE);
        player.setShiftKeyDown(false);
        helper.assertTrue(crate.zone().isEmpty() && crate.zoneFilter().isEmpty(), "sneak click should clear the zone");

        ItemStack drop = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        rack.saveToItem(drop, helper.getLevel().registryAccess());
        helper.setBlock(ZL_RACK, Blocks.AIR.defaultBlockState());
        helper.setBlock(ZL_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        net.minecraft.world.item.BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(ZL_RACK), drop);
        HeavyRackBlockEntity placed = helper.getBlockEntity(ZL_RACK);
        helper.assertTrue(placed.zone().equals("Food"), "a dismantled and placed rack keeps its zone, was '" + placed.zone() + "'");
        helper.assertTrue(placed.zoneFilter().items().size() == 1, "a dismantled and placed rack keeps its filter");
        helper.succeed();
    }

    /** The label menu names the label, parses tags and takes ghost items; the result stamps. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void zoneLabelMenuEditsDefinition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusStorageItems.ZONE_LABEL.get()));
        ZoneLabelMenu menu = new ZoneLabelMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a fresh label defines no zone");

        menu.setName("  Ores ");
        menu.setTags("c:ores, #minecraft:logs, not a tag!!");
        menu.setCarried(new ItemStack(Items.BREAD, 5));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        ItemStack labelStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ZoneDefinition definition = ZoneDefinition.of(labelStack);
        helper.assertTrue(definition != null && definition.name().equals("Ores"), "the label should be named Ores");
        helper.assertTrue(definition.filter().tags().equals(List.of("c:ores", "minecraft:logs")), "tags should be parsed and cleaned, were " + definition.filter().tags());
        helper.assertTrue(definition.filter().items().size() == 1 && definition.filter().items().get(0).is(Items.BREAD)
                && definition.filter().items().get(0).getCount() == 1, "the ghost slot should put one bread on the label");
        helper.assertTrue(menu.getCarried().getCount() == 5, "the cursor keeps its bread");
        helper.assertTrue(labelStack.getHoverName().getString().equals("Ores"), "the item shows the zone name");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)).filter().items().isEmpty(), "an empty cursor clears the ghost slot");

        menu.setName("");
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a blank name unnames the label");
        menu.setName("Ores");

        helper.setBlock(ZL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ZL_CRATE);
        useLabel(helper, player, player.getItemInHand(InteractionHand.MAIN_HAND), ZL_CRATE);
        helper.assertTrue(crate.zone().equals("Ores") && crate.zoneFilter().tags().equals(List.of("c:ores", "minecraft:logs")),
                "the edited label should stamp name and tags, got " + crate.zone() + " " + crate.zoneFilter().tags());
        helper.succeed();
    }

    private static final BlockPos IZ_CONTROLLER = new BlockPos(6, 1, 9);
    private static final BlockPos IZ_ORES = new BlockPos(5, 1, 9);
    private static final BlockPos IZ_PLAIN = new BlockPos(7, 1, 9);
    private static final BlockPos IZ_SPARE = new BlockPos(6, 1, 10);

    private static final BlockPos IZ_IRON = new BlockPos(6, 1, 8);

    private static ZoneDefinition zone(String name, List<ItemStack> items, List<String> tags) {
        return new ZoneDefinition(name, new ZoneFilter(items, tags));
    }

    /** Inserts follow the zones: exact items beat tags, filtered blocks refuse other items, unzoned storage catches full zones. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void insertRespectsZones(GameTestHelper helper) {
        helper.setBlock(IZ_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(IZ_ORES, IZ_PLAIN, IZ_SPARE, IZ_IRON)) {
            helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        CrateBlockEntity ores = helper.getBlockEntity(IZ_ORES);
        CrateBlockEntity iron = helper.getBlockEntity(IZ_IRON);
        CrateBlockEntity plain = helper.getBlockEntity(IZ_PLAIN);
        CrateBlockEntity spare = helper.getBlockEntity(IZ_SPARE);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(IZ_CONTROLLER);
        ores.setZone(zone("Ores", List.of(), List.of("c:ores")));
        iron.setZone(zone("Iron", List.of(new ItemStack(Items.IRON_ORE)), List.of()));
        plain.insert(new ItemStack(Items.BREAD, 1), false);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "warehouse should be online");
            helper.assertTrue(network.zoneBlockCounts().equals(java.util.Map.of("ores", 1, "iron", 1)), "two zones with one block each, got " + network.zoneBlockCounts());

            ItemStack left = network.insert(new ItemStack(Items.IRON_ORE, 10));
            helper.assertTrue(left.isEmpty(), "iron ore should fit");
            helper.assertTrue(iron.count() == 10, "iron ore should land in the Iron crate (exact beats tag), had " + iron.count());
            helper.assertTrue(ores.isEmpty(), "the Ores crate should not take it while the exact match has room");

            left = network.insert(new ItemStack(Items.COPPER_ORE, 10));
            helper.assertTrue(left.isEmpty(), "copper ore should fit");
            helper.assertTrue(ores.count() == 10, "copper ore should land in the Ores crate by tag, had " + ores.count());
            helper.assertTrue(plain.count() == 1 && spare.isEmpty(), "unzoned crates must not take ore while a zone has room");

            left = network.insert(new ItemStack(Items.BREAD, 5));
            helper.assertTrue(left.isEmpty(), "bread should fit");
            helper.assertTrue(plain.count() == 6, "bread should join the unzoned crate already holding bread, had " + plain.count());

            iron.insert(new ItemStack(Items.IRON_ORE, 5000), false);
            ores.insert(new ItemStack(Items.COPPER_ORE, 5000), false);
            helper.assertTrue(iron.count() == iron.capacity() && ores.count() == ores.capacity(), "both zoned crates should be full");
            left = network.insert(new ItemStack(Items.IRON_ORE, 7));
            helper.assertTrue(left.isEmpty(), "with the zones full the unzoned crate takes the ore");
            helper.assertTrue(spare.count() == 7, "the ore should be in the unzoned spare crate, had " + spare.count());

            spare.insert(new ItemStack(Items.IRON_ORE, 5000), false);
            left = network.insert(new ItemStack(Items.IRON_ORE, 7));
            helper.assertTrue(left.getCount() == 7, "with everything full nothing is stored, left " + left.getCount());
            helper.succeed();
        });
    }

    private static final BlockPos ST_CONTROLLER = new BlockPos(9, 1, 3);
    private static final BlockPos ST_STATION = new BlockPos(10, 1, 3);
    private static final BlockPos ST_CRATE = new BlockPos(9, 1, 4);
    private static final BlockPos ST_CRATE_2 = new BlockPos(8, 1, 3);
    private static final BlockPos ST_CHEST = new BlockPos(11, 1, 3);

    private static boolean blocked(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ACTIVITY) == StorageNodeBlocks.Activity.BLOCKED;
    }

    private static InboundStationBlockEntity stationSetup(GameTestHelper helper) {
        helper.setBlock(ST_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(ST_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(ST_STATION);
    }

    /** A stack fed into the station lands in a crate within a few ticks when there are no shuttles. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationPutsAwayInstantly(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ST_CRATE);
        helper.runAfterDelay(5, () -> {
            ItemStack left = ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            helper.assertTrue(left.isEmpty(), "the intake should take a stack");
            helper.assertTrue(station.intake().extractItem(0, 64, false).isEmpty(), "the intake must refuse extraction");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(crate.count() == 64, "cobblestone should be in the crate, had " + crate.count());
                helper.assertTrue(station.isEmpty(), "the buffer should be empty");
                helper.assertTrue(!blocked(helper, ST_STATION), "lamp must not be red");
                helper.assertTrue(helper.getBlockState(ST_STATION).getValue(StorageNodeBlocks.ONLINE), "station shows the online lamp");
                helper.succeed();
            });
        });
    }

    /** With its zone full and no unzoned storage the stack waits with a red lamp, and leaves once an unzoned crate appears. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationBacksUpWhenZoneFull(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity full = helper.getBlockEntity(ST_CRATE);
        full.setZone(zone("Ores", List.of(), List.of("c:ores")));
        full.insert(new ItemStack(Items.IRON_ORE, 5000), false);
        helper.runAfterDelay(5, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.IRON_ORE, 7), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty(), "the ore should wait in the buffer");
                helper.assertTrue(blocked(helper, ST_STATION), "the lamp should be red");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_ROOM,
                        "reason should be no room, was " + station.blockedReason());
                helper.assertTrue("Ores".equals(station.blockedZone()), "blocked zone should read Ores, was " + station.blockedZone());
                helper.setBlock(ST_CRATE_2, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
                CrateBlockEntity fresh = helper.getBlockEntity(ST_CRATE_2);
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(fresh.count() == 7, "the waiting ore should spill into the new unzoned crate, had " + fresh.count());
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty again");
                    helper.assertTrue(!blocked(helper, ST_STATION), "the lamp should be back to normal");
                    helper.succeed();
                });
            });
        });
    }

    /** Neither a chest feeding the station nor the buffer itself counts as warehouse stock. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void chestOnStationIsNotStock(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CHEST, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(ST_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.runAfterDelay(5, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(15, () -> {
                WarehouseControllerBlockEntity controller = helper.getBlockEntity(ST_CONTROLLER);
                WarehouseNetwork network = controller.network().orElseThrow();
                helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0,
                        "bread beside or inside the station must not be indexed");
                helper.assertTrue(!station.isEmpty(), "with no storage the bread waits in the buffer");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_STORAGE, "reason should be no storage");
                helper.succeed();
            });
        });
    }

    private static final BlockPos DK_STATION = new BlockPos(5, 1, 5);

    /** Rails dock onto a station on every side, and the carrot dismantles it with its buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stationDocksAndDismantles(GameTestHelper helper) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(DK_STATION.relative(direction).below(), Blocks.STONE.defaultBlockState());
        }
        helper.setBlock(DK_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            placeRail(helper, DK_STATION.relative(direction));
        }
        InboundStationBlockEntity station = helper.getBlockEntity(DK_STATION);
        ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
        helper.runAfterDelay(2, () -> {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState rail = helper.getBlockState(DK_STATION.relative(direction));
                helper.assertTrue(rail.getValue(RailBlock.dock(direction.getOpposite())),
                        "rail " + direction + " of the station should dock towards it");
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
            player.setShiftKeyDown(true);
            BlockPos absolute = helper.absolutePos(DK_STATION);
            carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
            helper.runAfterDelay(2, () -> {
                helper.assertBlockPresent(Blocks.AIR, DK_STATION);
                helper.assertItemEntityPresent(ServusStorageItems.INBOUND_STATION.get(), DK_STATION, 2.0);
                helper.assertItemEntityPresent(Items.BREAD, DK_STATION, 2.0);
                helper.succeed();
            });
        });
    }

    private static final BlockPos PA_CONTROLLER = new BlockPos(2, 1, 1);
    private static final BlockPos PA_STATION = new BlockPos(4, 1, 2);
    private static final BlockPos PA_CRATE = new BlockPos(8, 1, 2);
    private static final BlockPos PA_GARAGE = new BlockPos(3, 1, 2);

    /** A shuttle fetches a stack from the station and carries it along the rail to the crate. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttleCarriesPutAway(GameTestHelper helper) {
        helper.setBlock(PA_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(PA_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        helper.setBlock(PA_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(PA_STATION);
        CrateBlockEntity crate = helper.getBlockEntity(PA_CRATE);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(PA_CONTROLLER);
        helper.setBlock(PA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(PA_GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");

        helper.runAfterDelay(10, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.runAfterDelay(2, () -> helper.assertTrue(!station.isEmpty(), "the stack waits in the buffer until the shuttle arrives"));
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(station.isEmpty(), "the shuttle should have loaded the stack by now");
                helper.assertTrue(crate.isEmpty(), "the crate must still be empty while the shuttle travels");
            });
            helper.runAfterDelay(70, () -> {
                helper.assertTrue(crate.count() == 40, "the shuttle should have delivered 40 cobblestone, crate has " + crate.count());
                helper.assertTrue(station.isEmpty(), "nothing should ride back");
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("0 busy"),
                        "the shuttle should be home and idle: " + controller.network().orElseThrow().summary().getString());
                helper.succeed();
            });
        });
    }

    /** A crate on the station's own rail node: the shuttle's next leg has one node and must still complete. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlePutAwayOnStationRail(GameTestHelper helper) {
        helper.setBlock(PA_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        BlockPos crateBeside = new BlockPos(4, 1, 0);
        helper.setBlock(PA_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        helper.setBlock(crateBeside, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.SOUTH));
        InboundStationBlockEntity station = helper.getBlockEntity(PA_STATION);
        CrateBlockEntity crate = helper.getBlockEntity(crateBeside);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(PA_CONTROLLER);
        helper.setBlock(PA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(PA_GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");

        helper.runAfterDelay(10, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.runAfterDelay(70, () -> {
                helper.assertTrue(crate.count() == 40, "the shuttle should have delivered 40 cobblestone, crate has " + crate.count());
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("0 busy"),
                        "the shuttle should be home and idle: " + controller.network().orElseThrow().summary().getString());
                helper.succeed();
            });
        });
    }

    /** The station menu reports the warehouse state and shift click feeds the buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationMenuFeedsBuffer(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity ores = helper.getBlockEntity(ST_CRATE);
        ores.setZone(zone("Ores", List.of(), List.of("c:ores")));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            InboundStationMenu menu = new InboundStationMenu(1, player.getInventory(), station);
            helper.assertTrue(menu.state().online(), "the station should see an online warehouse");
            helper.assertTrue(menu.state().idle(), "an empty buffer reads idle");

            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 20 && station.buffer().getStackInSlot(0).is(Items.COBBLESTONE),
                    "shift click should move the cobblestone into the buffer");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the inventory slot should be empty");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!menu.state().blockedReason().isEmpty() && menu.state().blockedStack().is(Items.COBBLESTONE),
                        "cobblestone has no unzoned storage and the state should say so: " + menu.state());
                helper.succeed();
            });
        });
    }

    private static final BlockPos GA_GARAGE = new BlockPos(5, 1, 11);

    /** A garage takes shuttle items up to its capacity, refuses to be dismantled while it holds them, and breaks when empty. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void garageHoldsShuttles(GameTestHelper helper) {
        helper.setBlock(GA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(GA_GARAGE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int capacity = garage.capacity();
        io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu menu = new io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu(1, player.getInventory(), garage);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get(), capacity + 1));
        for (int i = 0; i <= capacity; i++) {
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
        }
        helper.assertTrue(garage.shuttleCount() == capacity, "one shift click per shuttle should fill every slot, filled " + garage.shuttleCount());
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 1, "one shuttle item should not fit");
        helper.assertTrue(!garage.shuttles().isItemValid(0, new ItemStack(Items.BREAD)), "only shuttles fit the slots");
        helper.assertTrue(garage.takeShuttle() && garage.takeShuttle(), "two shuttles can be taken back");
        helper.assertTrue(garage.shuttleCount() == capacity - 2, "count should drop by two");

        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(GA_GARAGE);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.assertTrue(helper.getBlockState(GA_GARAGE).getBlock() instanceof ShuttleGarageBlock, "an occupied garage must not be dismantled");
        while (garage.takeShuttle()) {
            // empty it
        }
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, GA_GARAGE);
            helper.assertItemEntityPresent(ServusStorageItems.SHUTTLE_GARAGE.get(), GA_GARAGE, 2.0);
            helper.succeed();
        });
    }

    private static final BlockPos AR_GARAGE = new BlockPos(5, 1, 6);

    /** The rail behind a garage links to it as a plain arm; a rail in front neither links nor docks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void garageRailIsAnArm(GameTestHelper helper) {
        helper.setBlock(AR_GARAGE.north().below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(AR_GARAGE.south().below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(AR_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        placeRail(helper, AR_GARAGE.north());
        placeRail(helper, AR_GARAGE.south());
        helper.runAfterDelay(2, () -> {
            BlockState behind = helper.getBlockState(AR_GARAGE.south());
            helper.assertTrue(behind.getValue(ConnectableBlock.NORTH), "the rail behind should connect to the garage");
            helper.assertTrue(!behind.getValue(RailBlock.DOCK_NORTH), "the rail behind should show an arm, not a dock");
            BlockState front = helper.getBlockState(AR_GARAGE.north());
            helper.assertTrue(!front.getValue(ConnectableBlock.SOUTH), "the rail in front must not connect to the garage");
            helper.assertTrue(!front.getValue(RailBlock.DOCK_SOUTH), "the rail in front must not dock onto the garage");
            helper.succeed();
        });
    }

    private static final BlockPos TG_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos TG_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos TG_GARAGE_A = new BlockPos(2, 1, 5);
    private static final BlockPos TG_GARAGE_B = new BlockPos(10, 1, 5);
    private static final BlockPos TG_CRATE = new BlockPos(9, 1, 5);

    /** With two garages the nearer shuttle serves and each shuttle returns to its own garage. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlesParkAtTheirGarage(GameTestHelper helper) {
        helper.setBlock(TG_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, TG_TERMINAL);
        for (int x = 2; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(TG_GARAGE_A, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(TG_GARAGE_B, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(TG_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garageA = helper.getBlockEntity(TG_GARAGE_A);
        ShuttleGarageBlockEntity garageB = helper.getBlockEntity(TG_GARAGE_B);
        helper.assertTrue(garageA.addShuttle() && garageB.addShuttle(), "each garage takes a shuttle");
        CrateBlockEntity crate = helper.getBlockEntity(TG_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 30), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(TG_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.summary().getString().contains("2 garages, 2 shuttles"), "summary should count both garages: " + network.summary().getString());
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TG_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.runAfterDelay(6, () -> {
                List<ShuttleView> a = network.shuttleViews(helper.absolutePos(TG_GARAGE_A));
                List<ShuttleView> b = network.shuttleViews(helper.absolutePos(TG_GARAGE_B));
                helper.assertTrue(a.isEmpty(), "garage A's shuttle should stay inside its garage and not be drawn");
                helper.assertTrue(b.size() == 1 && b.get(0).moving(), "garage B's shuttle, the nearer one, should be on its way");
            });
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the cobblestone should have been delivered into the output slots");
                helper.assertTrue(network.shuttleViews(helper.absolutePos(TG_GARAGE_B)).isEmpty(), "garage B's shuttle should be back inside its garage");
                helper.assertTrue(network.summary().getString().contains("2 shuttles, 0 busy"), "both shuttles idle: " + network.summary().getString());
                helper.succeed();
            });
        });
    }

    private static final BlockPos RS_CONTROLLER = new BlockPos(1, 1, 8);
    private static final BlockPos RS_TERMINAL = new BlockPos(1, 1, 9);
    private static final BlockPos RS_RACK = new BlockPos(1, 1, 7);

    /** A rack is never served instantly: extract skips it, insert and terminal pushes leave it alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackNeedsShuttle(GameTestHelper helper) {
        helper.setBlock(RS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, RS_TERMINAL);
        helper.setBlock(RS_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RS_RACK);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 40), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(RS_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 40, "the rack's bread is indexed");
            helper.assertTrue(network.extract(ItemKey.of(new ItemStack(Items.BREAD)), 64).isEmpty(), "instant extract must skip the rack");
            helper.assertTrue(network.insert(new ItemStack(Items.COBBLESTONE, 20)).getCount() == 20, "instant insert must skip the rack");

            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RS_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, 27);
            helper.assertTrue(player.getInventory().getItem(0).getCount() == 20, "a terminal push must not fill the rack");
            helper.assertTrue(rack.items().getStackInSlot(0).getCount() == 40 && rack.items().getStackInSlot(1).isEmpty(), "the rack is untouched");
            helper.succeed();
        });
    }

    private static final BlockPos SW_CONTROLLER = new BlockPos(1, 1, 11);
    private static final BlockPos SW_STATION = new BlockPos(3, 1, 10);
    private static final BlockPos SW_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos SW_GARAGE = new BlockPos(6, 1, 10);

    /** A station stack whose only room is a rack waits with the needs a shuttle reason until a garage with a shuttle can reach the rack. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void stationWaitsForShuttleOnRacks(GameTestHelper helper) {
        helper.setBlock(SW_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 1; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE.defaultBlockState());
        }
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 11), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SW_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        // The rack faces south, towards the rail behind it at z 11.
        helper.setBlock(SW_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        InboundStationBlockEntity station = helper.getBlockEntity(SW_STATION);
        HeavyRackBlockEntity rack = helper.getBlockEntity(SW_RACK);
        helper.runAfterDelay(10, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty(), "the bread should wait: only a rack has room");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_SHUTTLE, "reason should be no shuttle, was " + station.blockedReason());
                helper.assertTrue(blocked(helper, SW_STATION), "the lamp should be red");
                helper.setBlock(SW_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
                ShuttleGarageBlockEntity garage = helper.getBlockEntity(SW_GARAGE);
                helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
                helper.runAfterDelay(120, () -> {
                    int inRack = 0;
                    for (int i = 0; i < rack.items().getSlots(); i++) {
                        inRack += rack.items().getStackInSlot(i).is(Items.BREAD) ? rack.items().getStackInSlot(i).getCount() : 0;
                    }
                    helper.assertTrue(inRack == 10, "the shuttle should have carried the bread into the rack, rack has " + inRack);
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty");
                    helper.assertTrue(!blocked(helper, SW_STATION), "the lamp should be normal again");
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos RP_CONTROLLER = new BlockPos(8, 1, 10);
    private static final BlockPos RP_TERMINAL = new BlockPos(9, 1, 11);
    private static final BlockPos RP_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos RP_GARAGE = new BlockPos(3, 1, 10);

    /** A rack request is refused without a garage and delivered into the output slots once a shuttle can reach the rack. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void rackPickByShuttle(GameTestHelper helper) {
        helper.setBlock(RP_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 11), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        TerminalWiring.place(helper, RP_TERMINAL);
        helper.setBlock(RP_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RP_RACK);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RP_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "a rack request without a garage is refused, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");

            helper.setBlock(RP_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
            ShuttleGarageBlockEntity garage = helper.getBlockEntity(RP_GARAGE);
            helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
            helper.runAfterDelay(5, () -> {
                menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
                helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through now, got " + menu.stock().message());
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "nothing arrives at once from a rack");
                helper.runAfterDelay(120, () -> {
                    helper.assertTrue(terminal.outputCount(Items.BREAD) == 30, "the shuttle should have brought 30 bread, output has " + terminal.outputCount(Items.BREAD));
                    helper.assertTrue(rack.isEmpty(), "the rack should be empty");
                    helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled");
                    helper.succeed();
                });
            });
        });
    }

    private static final int SR_Z = 5;
    private static final BlockPos SR_CONTROLLER = new BlockPos(8, 1, 7);
    private static final BlockPos SR_TERMINAL = new BlockPos(9, 1, 8);
    private static final BlockPos SR_GARAGE = new BlockPos(6, 1, 7);

    /**
     * A section of three racks facing south at x 2 to 4 (left bay at x 4), a
     * rail only in front of the left bay leading to a terminal, and a garage
     * with one shuttle on that rail.
     */
    private static Player sectionRailSetup(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180.0F);
        for (int x = 2; x <= 4; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SR_Z));
        }
        List<BlockPos> rails = List.of(new BlockPos(4, 1, 6), new BlockPos(4, 1, 7), new BlockPos(4, 1, 8),
                new BlockPos(5, 1, 8), new BlockPos(6, 1, 8), new BlockPos(7, 1, 8), new BlockPos(8, 1, 8));
        for (BlockPos rail : rails) {
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            helper.setBlock(rail, ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SR_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, SR_TERMINAL);
        helper.setBlock(SR_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SR_GARAGE);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        return player;
    }

    /** Bread in the right bay of a section is fetched through the rail in front of the left bay. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void rackSectionServedFromOneRail(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, SR_Z))).size() == 3,
                "the three racks form one section");
        HeavyRackBlockEntity right = helper.getBlockEntity(new BlockPos(2, 1, SR_Z));
        right.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SR_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 30, "the terminal lists the right bay's bread");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through, got " + menu.stock().message());
            helper.runAfterDelay(160, () -> {
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 30, "the shuttle should have brought 30 bread, output has " + terminal.outputCount(Items.BREAD));
                helper.assertTrue(right.isEmpty(), "the right bay should be empty");
                helper.succeed();
            });
        });
    }

    /** A lone rack beside a section with a front rail, but without one of its own, needs a shuttle. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackBesideSectionNotServed(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        BlockPos lonePos = new BlockPos(1, 1, SR_Z);
        placeRackByHand(helper, player, lonePos);
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(lonePos)).size() == 1,
                "the fourth rack stands alone");
        HeavyRackBlockEntity lone = helper.getBlockEntity(lonePos);
        lone.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SR_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(stockOf(menu, Items.BREAD) == 30, "the terminal lists the lone rack's bread");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "the lone rack is out of reach, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");
            helper.succeed();
        });
    }

    private static final BlockPos SS_CONTROLLER = new BlockPos(5, 1, 3);
    private static final BlockPos SS_RACK = new BlockPos(6, 1, 3);
    private static final BlockPos SS_TERMINAL = new BlockPos(9, 1, 4);
    private static final BlockPos SS_GARAGE = new BlockPos(3, 1, 3);

    /** A rack touching the controller with a member rail only along its side is refused as needing a shuttle. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSideRailDoesNotReach(GameTestHelper helper) {
        for (int x = 2; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, SS_TERMINAL);
        helper.setBlock(SS_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SS_GARAGE);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        // Faces west onto the controller; the rail at z 4 runs along its south side.
        helper.setBlock(SS_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.WEST));
        HeavyRackBlockEntity rack = helper.getBlockEntity(SS_RACK);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SS_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 30, "the terminal lists the rack's bread");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "a side rail must not reach the rack, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");
            helper.succeed();
        });
    }

    private static final BlockPos RQ_CONTROLLER = new BlockPos(1, 1, 1);
    private static final BlockPos RQ_TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos RQ_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos RQ_CRATE = new BlockPos(6, 1, 2);

    private static StorageTerminalMenu requestSetup(GameTestHelper helper, Player player, int cobblestone) {
        helper.setBlock(RQ_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        TerminalWiring.place(helper, RQ_TERMINAL);
        helper.setBlock(RQ_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(RQ_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RQ_GARAGE);
        garage.addShuttle();
        CrateBlockEntity crate = helper.getBlockEntity(RQ_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, cobblestone), false);
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RQ_TERMINAL);
        return new StorageTerminalMenu(1, player.getInventory(), terminal);
    }

    /** A request over a stack splits into two loads and shows its progress until both arrived. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void requestSplitsAndDelivers(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = requestSetup(helper, player, 200);
        StorageTerminalBlockEntity terminal = menu.terminal();
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 100);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through");
            helper.runAfterDelay(6, () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).requested() == 100 && rows.get(0).delivered() == 0, "one open request for 100, got " + rows);
                helper.assertTrue(rows.get(0).state().equals("PICKING") || rows.get(0).state().equals("SENDING"), "state should be picking or sending, was " + rows.get(0).state());
            });
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 100 && menu.stock().requests().isEmpty(), 360,
                    () -> "100 cobblestone should arrive and the request settle, had " + terminal.outputCount(Items.COBBLESTONE)
                            + ", rows " + menu.stock().requests(), helper::succeed);
        });
    }

    /** When the output slots are full the shuttle waits beside the terminal and unloads once a slot frees up. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void outputFullShuttleWaits(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = requestSetup(helper, player, 64);
        StorageTerminalBlockEntity terminal = menu.terminal();
        for (int i = 0; i < terminal.output().getSlots(); i++) {
            terminal.output().setStackInSlot(i, new ItemStack(Items.DIRT, 64));
        }
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            TerminalWiring.when(helper, () -> !menu.stock().requests().isEmpty() && menu.stock().requests().get(0).state().equals("WAITING_ROOM"), 300,
                    () -> "the packets should be waiting for room, got " + menu.stock().requests(), () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 0, "nothing delivered yet, got " + rows);
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "nothing can land yet");
                terminal.output().setStackInSlot(0, ItemStack.EMPTY);
                TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 64 && menu.stock().requests().isEmpty(), 300,
                        () -> "the waiting packets should land once a slot is free, had " + terminal.outputCount(Items.COBBLESTONE)
                                + ", rows " + menu.stock().requests(), helper::succeed);
            });
        });
    }

    private static final BlockPos CS_CONTROLLER = new BlockPos(6, 1, 6);
    private static final BlockPos CS_FULL = new BlockPos(5, 1, 6);
    private static final BlockPos CS_EMPTY = new BlockPos(7, 1, 6);

    /** The controller exposes the warehouse through the Lib stock capabilities. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void controllerExposesStock(GameTestHelper helper) {
        helper.setBlock(CS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CS_FULL, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CS_EMPTY, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity full = helper.getBlockEntity(CS_FULL);
        CrateBlockEntity empty = helper.getBlockEntity(CS_EMPTY);
        full.insert(new ItemStack(Items.COBBLESTONE, 80), false);
        helper.runAfterDelay(10, () -> {
            BlockPos absolute = helper.absolutePos(CS_CONTROLLER);
            StockProvider provider = helper.getLevel().getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, absolute, Direction.UP);
            StockSink sink = helper.getLevel().getCapability(ServusFoundationCapabilities.STOCK_SINK, absolute, null);
            helper.assertTrue(provider != null && sink != null, "the controller should expose both stock capabilities");
            helper.assertTrue(provider.count(new ItemStack(Items.COBBLESTONE)) == 80, "count should be 80, was " + provider.count(new ItemStack(Items.COBBLESTONE)));
            ItemStack taken = provider.extract(new ItemStack(Items.COBBLESTONE), 20);
            helper.assertTrue(taken.getCount() == 20 && full.count() == 60, "extract should take 20 and leave 60");
            ItemStack left = sink.insert(new ItemStack(Items.BREAD, 20));
            helper.assertTrue(left.isEmpty() && empty.count() == 20 && empty.type(0).is(Items.BREAD), "the sink should put the bread into the empty crate");
            helper.succeed();
        });
    }

    private static final BlockPos OS_CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos OS_STATION = new BlockPos(3, 1, 2);
    private static final BlockPos OS_FRONT = new BlockPos(4, 1, 2);
    private static final BlockPos OS_CRATE = new BlockPos(1, 1, 2);

    private static OutboundStationBlockEntity outboundSetup(GameTestHelper helper) {
        helper.setBlock(OS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        // Facing west: the station's back, and so its machine, is the block east of it.
        helper.setBlock(OS_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(OS_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(OS_STATION);
    }

    /** A keep slot pulls its amount out of storage into the buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void outboundKeepsBufferStocked(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.STICK, 50), false);
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        helper.runAfterDelay(StorageStatsManager.INSTANCE.outboundCheckInterval() + 20, () -> {
            helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, "16 sticks should be in the buffer, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))));
            helper.assertTrue(crate.count() == 34, "34 sticks should remain in the crate, had " + crate.count());
            helper.assertTrue(station.shortStack().isEmpty(), "nothing should be short");
            helper.succeed();
        });
    }

    /** What a hopper takes from an extract side is topped up again. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void outboundToppedUpAfterExtraction(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.STICK, 50), false);
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        int interval = StorageStatsManager.INSTANCE.outboundCheckInterval();
        helper.runAfterDelay(interval + 20, () -> {
            helper.assertTrue(station.handlerFor(Direction.DOWN) == null, "the bottom is off by default");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.PUSH, "the first cycle turns the bottom to push");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.OUTPUT, "the second cycle turns the bottom to output");
            IItemHandler bottom = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(OS_STATION), Direction.DOWN);
            helper.assertTrue(bottom != null, "the extract side exposes a handler");
            helper.assertTrue(bottom.extractItem(0, 5, false).getCount() == 5, "5 sticks can be taken");
            helper.assertTrue(bottom.insertItem(1, new ItemStack(Items.COBBLESTONE, 3), false).getCount() == 3, "nothing goes in");
            helper.runAfterDelay(interval + 20, () -> {
                helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, "the buffer should be topped up to 16, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))));
                helper.assertTrue(crate.count() == 29, "29 sticks should remain in the crate, had " + crate.count());
                helper.succeed();
            });
        });
    }

    /** The buffer is pushed into the block in front. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundPushesIntoFront(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.setBlock(OS_FRONT, Blocks.CHEST.defaultBlockState());
        station.buffer().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.runAfterDelay(20, () -> {
            ChestBlockEntity chest = helper.getBlockEntity(OS_FRONT);
            helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 8, "the chest should hold the 8 ingots, had " + chest.countItem(Items.IRON_INGOT));
            helper.assertTrue(station.isEmpty(), "the buffer should be empty");
            helper.succeed();
        });
    }

    /** Sides cycle push, output, off; off sides expose nothing, the back starts as push and the front is fixed off. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundSideModes(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.assertTrue(station.sideMode(Direction.EAST) == SideMode.PUSH, "the back starts as push");
        helper.assertTrue(station.relativeSide(Direction.EAST) == RelativeSide.BACK, "east is the back of a station facing west");
        helper.assertTrue(station.handlerFor(Direction.EAST) != null, "a push side also lets neighbours extract");
        helper.assertTrue(station.sideMode(Direction.WEST) == SideMode.DISABLED && station.cycleFace(Direction.WEST) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED && station.handlerFor(Direction.UP) == null, "other sides start off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PUSH, "off cycles to push");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.OUTPUT, "push cycles to output");
        helper.assertTrue(station.handlerFor(Direction.UP) != null, "an output side exposes the buffer");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "output cycles to off");
        helper.assertTrue(station.handlerFor(Direction.UP) == null, "an off side exposes nothing");
        station.sideConfig().set(RelativeSide.TOP, SideMode.BOTH);
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED, "an unsupported mode falls back to off");
        helper.succeed();
    }

    /** Feeding a station from a crate touching the controller is instant. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void feedOutboundFromCrate(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.IRON_INGOT, 10), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(OS_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            String refused = network.feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 4);
            helper.assertTrue(refused.isEmpty(), "the feed should go through, got " + refused);
            helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 4, "4 ingots should be in the buffer at once");
            helper.assertTrue(crate.count() == 6, "the crate should have 6 left");
            helper.assertTrue(network.feedStation(station, ItemKey.of(new ItemStack(Items.BREAD)), 4).equals("out_of_stock"), "nothing in stock is refused");
            helper.succeed();
        });
    }

    private static final BlockPos FD_CONTROLLER = new BlockPos(1, 1, 7);
    private static final BlockPos FD_RACK = new BlockPos(4, 1, 8);
    private static final BlockPos FD_GARAGE = new BlockPos(2, 1, 8);
    private static final BlockPos FD_STATION = new BlockPos(7, 1, 8);

    /** Feeding a station from a rack rides a shuttle. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void feedOutboundByShuttle(GameTestHelper helper) {
        helper.setBlock(FD_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 7), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 7), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(FD_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(FD_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(FD_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.SOUTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(FD_RACK);
        rack.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(FD_GARAGE);
        garage.addShuttle();
        OutboundStationBlockEntity station = helper.getBlockEntity(FD_STATION);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(FD_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            String refused = network.feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 4);
            helper.assertTrue(refused.isEmpty(), "the feed should go through, got " + refused);
            helper.assertTrue(station.isEmpty(), "nothing arrives at once from a rack");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 4, "the shuttle should have brought 4 ingots, buffer has " + station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))));
                helper.succeed();
            });
        });
    }

    private static final BlockPos IS_STATION = new BlockPos(5, 1, 10);

    /** An inbound side switched off takes nothing until it is switched on again. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inboundSideOff(GameTestHelper helper) {
        helper.setBlock(IS_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(IS_STATION);
        helper.assertTrue(station.sideMode(Direction.NORTH) == SideMode.DISABLED && station.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PULL, "input cycles to pull");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "pull cycles to off");
        helper.setBlock(IS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(IS_STATION.above());
        hopper.setItem(0, new ItemStack(Items.BREAD, 10));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(station.isEmpty() && hopper.getItem(0).getCount() == 10, "the hopper should not get through an off side");
            helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.INPUT, "off cycles to input");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(!station.isEmpty(), "the hopper should feed the accepting side");
                helper.succeed();
            });
        });
    }

    private static final BlockPos IP_STATION = new BlockPos(8, 1, 10);

    /** A Pull side empties a chest into the buffer without a hopper. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inboundPullsFromChest(GameTestHelper helper) {
        helper.setBlock(IP_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(IP_STATION);
        helper.setBlock(IP_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(IP_STATION.south());
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.assertTrue(station.relativeSide(Direction.SOUTH) == RelativeSide.BACK, "south is the back of a station facing north");
        station.sideConfig().set(RelativeSide.BACK, SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(chest.countItem(Items.BREAD) == 0, "the chest should be emptied, has " + chest.countItem(Items.BREAD));
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 30, "the bread should be in the buffer");
            helper.succeed();
        });
    }

    private static final BlockPos CS_STATION = new BlockPos(9, 1, 2);

    /** A Crafting Station side set to Off refuses a hopper; Pull takes used items from a chest. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftingStationSideOff(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CS_STATION);
        station.sideConfig().set(RelativeSide.TOP, SideMode.DISABLED);
        helper.setBlock(CS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(CS_STATION.above());
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.setBlock(CS_STATION.west(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(CS_STATION.west());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 6));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 2));
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper above an off side keeps its planks");
            helper.assertTrue(chest.countItem(Items.OAK_PLANKS) == 0 && chest.countItem(Items.COBBLESTONE) == 2, "the pull side takes planks and leaves cobblestone");
            helper.succeed();
        });
    }

    private static final BlockPos CR_STATION = new BlockPos(9, 1, 9);

    private static CraftingStationBlockEntity plainStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    /** A station with a long sticks job, so it takes planks and crafts them. */
    private static CraftingStationBlockEntity craftingStationWithSticks(GameTestHelper helper, BlockPos pos) {
        CraftingStationBlockEntity station = plainStation(helper, pos);
        StationJobs.sticksJob(station, 64);
        return station;
    }

    /** A sticks job is crafted from the inputs, one craft per work time, and ends with no crafts left. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftingStationCraftsJob(GameTestHelper helper) {
        CraftingStationBlockEntity station = plainStation(helper, CR_STATION);
        station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 6));
        Object owner = StationJobs.sticksJob(station, 2);
        helper.assertTrue(station.working() && station.jobLeft() == 2, "the job should have 2 crafts left");
        int ticks = station.workTicks();
        helper.runAfterDelay(ticks + 5, () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).is(Items.STICK) && station.outputs().getStackInSlot(0).getCount() == 4,
                    "four sticks should be in the outputs after one work time, got " + station.outputs().getStackInSlot(0));
            helper.assertTrue(station.inputs().getStackInSlot(0).getCount() == 4, "four planks should remain");
            helper.runAfterDelay(ticks * 2 + 4, () -> {
                helper.assertTrue(station.outputs().getStackInSlot(0).getCount() == 8, "eight sticks after the second craft and no third, got "
                        + station.outputs().getStackInSlot(0).getCount());
                helper.assertTrue(!station.working() && station.jobLeft() == 0, "no crafts left");
                helper.assertTrue(station.inputs().getStackInSlot(0).getCount() == 2, "the leftover planks stay");
                station.release(owner);
                helper.assertTrue(station.job().isEmpty(), "released, the station is idle");
                helper.succeed();
            });
        });
    }

    /** Without a job the station crafts nothing, reports no recipes and takes nothing from automation. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void craftingStationIdleWithoutJob(GameTestHelper helper) {
        CraftingStationBlockEntity station = plainStation(helper, CR_STATION);
        station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        ProductionProvider provider = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(CR_STATION), null);
        helper.assertTrue(provider != null && provider.recipes().isEmpty() && provider.active().isEmpty(), "no recipes and nothing active");
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CR_STATION), Direction.UP);
        helper.assertTrue(ItemHandlerHelper.insertItem(handler, new ItemStack(Items.OAK_PLANKS, 5), false).getCount() == 5, "planks are refused without a job");
        helper.runAfterDelay(station.workTicks() + 5, () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).isEmpty(), "nothing is crafted without a job");
            StationJobs.sticksJob(station, 1);
            ProductionProvider working = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(CR_STATION), null);
            helper.assertTrue(working.recipes().isEmpty() && working.active().map(id -> id.equals(ResourceLocation.withDefaultNamespace("stick"))).orElse(false),
                    "with a job the sticks recipe is active");
            helper.succeed();
        });
    }

    /** Automation can insert only used items into the inputs and extract only from the outputs. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void craftingStationRefusesUnusedItems(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CR_STATION);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CR_STATION), Direction.UP);
        helper.assertTrue(handler != null, "the station should expose an item handler");
        helper.assertTrue(ItemHandlerHelper.insertItem(handler, new ItemStack(Items.COBBLESTONE, 5), false).getCount() == 5, "cobblestone is refused");
        helper.assertTrue(ItemHandlerHelper.insertItem(handler, new ItemStack(Items.OAK_PLANKS, 5), false).isEmpty(), "planks are accepted");
        helper.assertTrue(handler.extractItem(0, 5, false).isEmpty(), "inputs cannot be extracted");
        station.outputs().setStackInSlot(0, new ItemStack(Items.STICK, 3));
        helper.assertTrue(handler.extractItem(CraftingStationBlockEntity.INPUT_SLOTS, 3, false).getCount() == 3, "outputs can be extracted");
        helper.succeed();
    }

    private static final BlockPos DS_CONTROLLER = new BlockPos(2, 1, 5);
    private static final BlockPos DS_CRATE_A = new BlockPos(1, 1, 5);
    private static final BlockPos DS_OUTBOUND = new BlockPos(3, 1, 5);
    private static final BlockPos DS_STATION = new BlockPos(4, 1, 5);
    private static final BlockPos DS_CRATE_B = new BlockPos(3, 1, 6);
    private static final BlockPos DS_INBOUND = new BlockPos(4, 1, 6);

    /** An outbound station pushes planks into the station, an inbound station collects the sticks, which end in the crates. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void outboundServesCraftingStation(GameTestHelper helper) {
        helper.setBlock(DS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_CRATE_A, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_CRATE_B, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_OUTBOUND, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(DS_INBOUND, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = plainStation(helper, DS_STATION);
        Object owner = StationJobs.sticksJob(station, 2);
        OutboundStationBlockEntity outbound = helper.getBlockEntity(DS_OUTBOUND);
        InboundStationBlockEntity inbound = helper.getBlockEntity(DS_INBOUND);
        CrateBlockEntity crateA = helper.getBlockEntity(DS_CRATE_A);
        CrateBlockEntity crateB = helper.getBlockEntity(DS_CRATE_B);
        outbound.buffer().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        // A held job keeps its outputs for the CPU; the sticks leave once the job ends.
        TerminalWiring.when(helper, () -> !station.working(), station.workTicks() * 2 + 80, () -> "both crafts should be done", () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).getCount() == 8, "the sticks wait in the outputs while the job is held");
            station.release(owner);
        });
        helper.runAfterDelay(station.workTicks() * 2 + 140, () -> {
            int sticks = (crateA.count() > 0 && crateA.type(0).is(Items.STICK) ? crateA.count() : 0)
                    + (crateB.count() > 0 && crateB.type(0).is(Items.STICK) ? crateB.count() : 0);
            helper.assertTrue(sticks == 8, "eight sticks should have flowed into the crates, found " + sticks);
            helper.assertTrue(outbound.isEmpty() && inbound.isEmpty(), "both station buffers should be empty");
            helper.succeed();
        });
    }

    private static final BlockPos RC_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos RC_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos RC_GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos RC_RACK = new BlockPos(9, 1, 5);
    private static final BlockPos RC_CRATE = new BlockPos(1, 1, 5);

    /** Controller, terminal, a rail along z 4, a garage behind the rail with one shuttle and a crate beside it, a rack with cobblestone at the far end. */
    private static WarehouseControllerBlockEntity recallSetup(GameTestHelper helper) {
        helper.setBlock(RC_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, RC_TERMINAL);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(RC_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        // The crate keeps the garage in the warehouse when the rail behind it is broken.
        helper.setBlock(RC_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(RC_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        HeavyRackBlockEntity rack = helper.getBlockEntity(RC_RACK);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 30), false);
        return helper.getBlockEntity(RC_CONTROLLER);
    }

    /** Recall brings a shuttle that is out on a pick back inside at once; the pick is queued again. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void garageRecallBringsShuttlesHome(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                network.recallGarage(garagePos);
                helper.assertTrue(network.garageOut(garagePos) == 0, "after the recall nothing is out");
                helper.assertTrue(network.shuttleViews(garagePos).isEmpty(), "the recalled shuttle sits inside and is not drawn");
                helper.runAfterDelay(160, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the queued pick is taken up again and delivered, got " + terminal.outputCount(Items.COBBLESTONE));
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos PG_GARAGE_B = new BlockPos(8, 1, 5);

    /** A paused garage hands out no shuttle; the active one serves instead. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pausedGarageTakesNoJobs(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        helper.setBlock(PG_GARAGE_B, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garageB = helper.getBlockEntity(PG_GARAGE_B);
        helper.assertTrue(garageB.addShuttle(), "garage B takes a shuttle");
        garageB.setActive(false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.garageSummary(helper.absolutePos(PG_GARAGE_B)).getString().contains("paused"), "the summary says paused");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(PG_GARAGE_B)) == 0, "the paused garage, though nearer, sends nothing");
                helper.assertTrue(network.garageOut(helper.absolutePos(RC_GARAGE)) == 1, "the active garage sends its shuttle");
                garageB.setActive(true);
                helper.assertTrue(!network.garageSummary(helper.absolutePos(PG_GARAGE_B)).getString().contains("paused"), "resumed");
                helper.succeed();
            });
        });
    }

    /** Breaking the rail behind a garage while its shuttle is out brings the shuttle inside instead of freezing it. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void losingTheRailRecalls(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                helper.setBlock(RC_GARAGE.north(), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    // The rail line split off, so the controller's part is a rebuilt network object.
                    WarehouseNetwork after = controller.network().orElseThrow();
                    ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE);
                    helper.assertTrue(garage.shuttleCount() == 1, "the garage still holds its shuttle");
                    helper.assertTrue(after.garageOut(garagePos) == 0, "without a home the shuttle is recalled, out is " + after.garageOut(garagePos));
                    helper.assertTrue(after.shuttleViews(garagePos).isEmpty(), "nothing is drawn on the rails");
                    helper.assertTrue(after.garageSummary(garagePos).getString().contains("no rail"), "the summary says no rail: " + after.garageSummary(garagePos).getString());
                    helper.succeed();
                });
            });
        });
    }

    // Lift car: a shaft at x 4, z 3 from y 1 to 3. Ground rails x 1 to 3, garage north of x 3,
    // controller north of x 1, terminal south of x 2.

    private static final BlockPos LC_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos LC_MIDDLE = new BlockPos(4, 2, 3);
    private static final BlockPos LC_TOP = new BlockPos(4, 3, 3);
    private static final BlockPos LC_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos LC_CONTROLLER = new BlockPos(1, 1, 2);
    private static final BlockPos LC_TERMINAL = new BlockPos(2, 1, 4);

    /** Ground floor rails, garage with the given shuttles, controller, terminal and a three segment shaft. */
    private static void liftBase(GameTestHelper helper, int shuttles) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(LC_BOTTOM, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LC_MIDDLE, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LC_TOP, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LC_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, LC_TERMINAL);
        helper.setBlock(LC_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(LC_GARAGE);
        for (int i = 0; i < shuttles; i++) {
            helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        }
    }

    /** A rail beside a shaft floor with a rack behind it facing the rail, both standing on stone. */
    private static HeavyRackBlockEntity liftRack(GameTestHelper helper, BlockPos rail, Item item, int count) {
        BlockPos rackPos = rail.south();
        helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(rackPos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(rail, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(rackPos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.items().insertItem(0, new ItemStack(item, count), false);
        return rack;
    }

    private static WarehouseNetwork liftNetwork(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(LC_CONTROLLER);
        return controller.network().orElseThrow();
    }

    /** A ground floor shuttle rides the car up to a rack two floors up and back down to the terminal. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttleRidesCarToUpperRack(GameTestHelper helper) {
        liftBase(helper, 1);
        HeavyRackBlockEntity rack = liftRack(helper, new BlockPos(5, 3, 3), Items.BREAD, 20);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        double[] highest = {Double.NEGATIVE_INFINITY};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(LC_CONTROLLER) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().map(network -> network.lift(bottom)).ifPresent(car -> highest[0] = Math.max(highest[0], car.y()));
            }
        });
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = liftNetwork(helper);
            helper.assertTrue(network.lift(bottom) != null, "the shaft has a car");
            helper.assertTrue(network.lift(bottom).y() == bottom.getY(), "the car starts at the lowest segment");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 20);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 20, "the shuttle should have brought 20 bread, output has " + terminal.outputCount(Items.BREAD));
                helper.assertTrue(rack.isEmpty(), "the rack should be empty");
                helper.assertTrue(highest[0] == bottom.getY() + 2, "the car went up to the top floor, highest was " + highest[0]);
                helper.assertTrue(network.lift(bottom).y() == bottom.getY(), "the car brought the shuttle back down");
                helper.assertTrue(network.garageOut(helper.absolutePos(LC_GARAGE)) == 0, "the shuttle is home");
                helper.succeed();
            });
        });
    }

    /** Two shuttles call the car at the same floor: the first caller rides first, the second waits for the car. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlesQueueForCar(GameTestHelper helper) {
        liftBase(helper, 2);
        HeavyRackBlockEntity top = liftRack(helper, new BlockPos(3, 3, 3), Items.BREAD, 10);
        HeavyRackBlockEntity middle = liftRack(helper, new BlockPos(5, 2, 3), Items.APPLE, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 10);
            menu.request(ItemKey.of(new ItemStack(Items.APPLE)), 10);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(top.isEmpty(), "the first caller rode up first and loaded the top rack");
                helper.assertTrue(!middle.isEmpty(), "the second caller is still waiting for the car, although its floor is nearer");
                helper.runAfterDelay(200, () -> {
                    helper.assertTrue(terminal.outputCount(Items.BREAD) == 10 && terminal.outputCount(Items.APPLE) == 10,
                            "both loads arrive, output has " + terminal.outputCount(Items.BREAD) + " bread and " + terminal.outputCount(Items.APPLE) + " apples");
                    helper.succeed();
                });
            });
        });
    }

    /** A shuttle crosses the shaft on the ground floor deck while the car stays parked at the top. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void shuttleCrossesShaftWithoutCar(GameTestHelper helper) {
        liftBase(helper, 1);
        BlockPos crossRail = new BlockPos(5, 1, 3);
        BlockPos cratePos = new BlockPos(6, 1, 3);
        helper.setBlock(crossRail.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(crossRail, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(cratePos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(cratePos);
        crate.insert(new ItemStack(Items.COBBLESTONE, 16), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = liftNetwork(helper);
            // Park the car at the top floor with a call nobody answers, then take the call back.
            network.lift(bottom).call(-1, bottom.getY() + 2);
            helper.runAfterDelay(40, () -> {
                network.lift(bottom).remove(-1);
                helper.assertTrue(network.lift(bottom).y() == bottom.getY() + 2, "the car is parked at the top");
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
                StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
                menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 16);
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the crate behind the shaft is served by the shuttle");
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 16,
                            "the shuttle crossed both ways without waiting, output has " + terminal.outputCount(Items.COBBLESTONE));
                    helper.assertTrue(network.lift(bottom).y() == bottom.getY() + 2 && !network.lift(bottom).moving(), "the car did not move");
                    helper.succeed();
                });
            });
        });
    }

    /** Breaking the middle segment while the car carries a loaded shuttle drops the load at its garage. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shaftBreakDropsRiderCargo(GameTestHelper helper) {
        liftBase(helper, 1);
        liftRack(helper, new BlockPos(5, 3, 3), Items.BREAD, 20);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        BlockPos garagePos = helper.absolutePos(LC_GARAGE);
        long[] brokeAt = {-1};
        StorageTerminalMenu[] menu = {null};
        // One top level tick hook: scheduling from inside a scheduled task is not safe.
        helper.onEachTick(() -> {
            if (helper.getTick() == 10) {
                menu[0] = new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, LC_TERMINAL));
                menu[0].request(ItemKey.of(new ItemStack(Items.BREAD)), 20);
            }
            if (menu[0] == null) {
                return;
            }
            WarehouseNetwork network = liftNetwork(helper);
            if (brokeAt[0] < 0) {
                var car = network.lift(bottom);
                boolean loaded = network.shuttleViews(garagePos).stream().anyMatch(view -> !view.cargo().isEmpty());
                if (car != null && car.carrying() && loaded && car.y() < bottom.getY() + 1.5) {
                    brokeAt[0] = helper.getTick();
                    helper.setBlock(LC_MIDDLE, Blocks.AIR.defaultBlockState());
                }
            } else if (helper.getTick() == brokeAt[0] + 40) {
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
                List<RequestRow> rows = menu[0].stock().requests();
                helper.assertItemEntityPresent(Items.BREAD, LC_GARAGE, 3.0);
                helper.assertTrue(rows.size() == 1 && rows.get(0).missing() == 20, "the request shows the bread missing, got " + rows);
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "no bread arrived");
                helper.assertTrue(network.garageOut(garagePos) == 0, "the shuttle is back in its garage");
                helper.succeed();
            }
        });
    }

    private static final int HANG_Y = 3;

    private static void hangRack(GameTestHelper helper, int x, int z, Direction facing) {
        helper.setBlock(new BlockPos(x, HANG_Y, z), ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, facing));
    }

    private static void hangRail(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, HANG_Y, z), ServusStorageBlocks.RAIL.get().defaultBlockState());
    }

    private static boolean railAt(GameTestHelper helper, int x, int z) {
        return helper.getBlockState(new BlockPos(x, HANG_Y, z)).getBlock() instanceof RailBlock;
    }

    /** Uses a rail item on the given face of a block, like a player clicking it. */
    private static void clickRail(GameTestHelper helper, Player player, BlockPos clicked, Direction face) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack rail = new ItemStack(ServusStorageBlocks.RAIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rail);
        rail.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false)));
    }

    /** A rail hangs on a raised rack front; the rack side, a crate and open air take none. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void railHangsOnRackFrontOnly(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 2, 2, Direction.SOUTH);
        helper.setBlock(new BlockPos(8, HANG_Y, 2), ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        clickRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.SOUTH);
        clickRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.EAST);
        clickRail(helper, player, new BlockPos(8, HANG_Y, 2), Direction.SOUTH);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(railAt(helper, 2, 3), "a rail should hang on the rack front");
            helper.assertFalse(railAt(helper, 3, 2), "the rack side is no anchor");
            helper.assertFalse(railAt(helper, 8, 3), "a crate is no anchor");
            helper.succeed();
        });
    }

    /** A row of rails hangs along five raised racks at any length. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void hungRowAlongRacksHolds(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            hangRack(helper, x, 2, Direction.SOUTH);
            hangRail(helper, x, 3);
        }
        helper.runAfterDelay(5, () -> {
            for (int x = 1; x <= 5; x++) {
                helper.assertTrue(railAt(helper, x, 3), "rail " + x + " of the row should hang");
            }
            helper.succeed();
        });
    }

    /** Three rails bridge two elevator shafts, ends first, and link them. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bridgeBetweenElevatorsHolds(GameTestHelper helper) {
        for (int x : new int[] {1, 5}) {
            helper.setBlock(new BlockPos(x, HANG_Y - 1, 2), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, HANG_Y, 2), ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        }
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 4, 2), "a rail should hang on each shaft");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.succeed();
            });
        });
    }

    /** Three rails bridge two raised racks facing each other; breaking one rack drops the bridge. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bridgeBetweenRacksFallsWithItsRack(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.setBlock(new BlockPos(5, HANG_Y, 2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    for (int x = 2; x <= 4; x++) {
                        helper.assertFalse(railAt(helper, x, 2), "bridge rail " + x + " should fall with its rack");
                    }
                    helper.succeed();
                });
            });
        });
    }

    /** A fourth rail beside the middle of a three rail bridge drops all four. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void fourthRailCollapsesBridge(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 3, 2) && railAt(helper, 4, 2), "three rails should hold");
                hangRail(helper, 3, 3);
                helper.runAfterDelay(5, () -> {
                    helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2) || railAt(helper, 4, 2) || railAt(helper, 3, 3),
                            "all four rails should have collapsed");
                    helper.succeed();
                });
            });
        });
    }

    /** In a four block gap the first free rail drops with the hung rail it touches; the far hung rail stays. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void fourBlockGapCannotBeBridged(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 6, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 5, 2);
        helper.runAfterDelay(3, () -> {
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop");
                helper.assertTrue(railAt(helper, 5, 2), "the far hung rail should stay");
                helper.succeed();
            });
        });
    }

    /** Two floor edges three blocks apart are bridged by one rail off each edge and one in the middle. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bridgeBetweenFloorsHolds(GameTestHelper helper) {
        for (int x : new int[] {1, 5}) {
            helper.setBlock(new BlockPos(x, HANG_Y - 1, 2), Blocks.STONE.defaultBlockState());
            hangRail(helper, x, 2);
        }
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2) && railAt(helper, 4, 2), "a rail should hang off each floor edge");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                for (int x = 1; x <= 5; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "rail " + x + " should stay");
                }
                helper.setBlock(new BlockPos(5, HANG_Y - 1, 2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(railAt(helper, 1, 2), "the standing rail should stay");
                    for (int x = 2; x <= 5; x++) {
                        helper.assertFalse(railAt(helper, x, 2), "rail " + x + " should fall with the floor");
                    }
                    helper.succeed();
                });
            });
        });
    }

    /** A free rail beside a hung rail and nothing else drops both. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void overhangCollapses(GameTestHelper helper) {
        hangRack(helper, 1, 2, Direction.EAST);
        hangRail(helper, 2, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(railAt(helper, 2, 2), "the hung rail should stay on its own");
            hangRail(helper, 3, 2);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop both rails");
                helper.succeed();
            });
        });
    }

    /** Breaking the floor under a rail leaves it hung on a rack front and drops a lone rail. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void floorRemovedUnderRails(GameTestHelper helper) {
        hangRack(helper, 2, 2, Direction.SOUTH);
        helper.setBlock(new BlockPos(2, HANG_Y - 1, 3), Blocks.STONE.defaultBlockState());
        hangRail(helper, 2, 3);
        helper.setBlock(new BlockPos(8, HANG_Y - 1, 3), Blocks.STONE.defaultBlockState());
        hangRail(helper, 8, 3);
        helper.runAfterDelay(3, () -> {
            helper.setBlock(new BlockPos(2, HANG_Y - 1, 3), Blocks.AIR.defaultBlockState());
            helper.setBlock(new BlockPos(8, HANG_Y - 1, 3), Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(railAt(helper, 2, 3), "the rail in front of the rack should stay hung");
                helper.assertFalse(railAt(helper, 8, 3), "the lone rail should drop");
                helper.succeed();
            });
        });
    }

    /** Uses a rail item while sneaking, hitting the given face at a local point (top and bottom faces use x and z). */
    private static ItemStack sneakRail(GameTestHelper helper, Player player, BlockPos clicked, Direction face, double x, double z) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack rail = new ItemStack(ServusStorageBlocks.RAIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rail);
        player.setShiftKeyDown(true);
        Vec3 location = face.getAxis().isVertical()
                ? new Vec3(absolute.getX() + x, absolute.getY() + (face == Direction.UP ? 2.0 / 16.0 : 0.0), absolute.getZ() + z)
                : Vec3.atCenterOf(absolute).relative(face, 0.5);
        rail.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(location, face, absolute, false)));
        player.setShiftKeyDown(false);
        return rail;
    }

    private static boolean railAt(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof RailBlock;
    }

    private static void stoneFloor(GameTestHelper helper, int z) {
        for (int x = 0; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        }
    }

    private static void floorRails(GameTestHelper helper, int fromX, int toX, int z) {
        stoneFloor(helper, z);
        for (int x = fromX; x <= toX; x++) {
            placeRail(helper, new BlockPos(x, 2, z));
        }
    }

    /** Sneaking with a rail on the east half of a floor run places past its end; a plain click still turns. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailExtendsStraight(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE.defaultBlockState());
        sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.9, 0.5);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the rail should land past the fourth rail");
        helper.assertFalse(railAt(helper, 1, 3, 3), "nothing should land on top of the clicked rail");
        clickRail(helper, player, new BlockPos(1, 2, 3), Direction.NORTH);
        helper.assertTrue(railAt(helper, 1, 2, 2), "a plain click should place against the north face");
        helper.assertFalse(railAt(helper, 6, 2, 3), "a plain click should not walk");
        helper.succeed();
    }

    /** On lone rails the top face quadrant and side faces pick the direction; the walk passes a junction. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailDirectionAndJunction(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        stoneFloor(helper, 1);
        stoneFloor(helper, 2);
        placeRail(helper, new BlockPos(2, 2, 2));
        sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.85, 0.4);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the walk should pass the junction and land past the line");
        placeRail(helper, new BlockPos(8, 2, 3));
        sneakRail(helper, player, new BlockPos(8, 2, 3), Direction.UP, 0.5, 0.1);
        helper.assertTrue(railAt(helper, 8, 2, 2), "a hit near the north edge of a lone rail should place north");
        placeRail(helper, new BlockPos(0, 2, 1));
        sneakRail(helper, player, new BlockPos(0, 2, 1), Direction.SOUTH, 0.5, 0.5);
        helper.assertTrue(railAt(helper, 0, 2, 2), "a side face hit on a lone rail should walk towards that face");
        helper.succeed();
    }

    /** A hit near the north edge of a rail inside an east west line still extends the line by the half it lands in. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailFollowsStraightRun(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 4, 3);
        stoneFloor(helper, 2);
        sneakRail(helper, player, new BlockPos(2, 2, 3), Direction.UP, 0.6, 0.1);
        helper.assertTrue(railAt(helper, 5, 2, 3), "the rail should land past the east end of the line");
        helper.assertFalse(railAt(helper, 2, 2, 2), "no rail should land north of the clicked rail");
        sneakRail(helper, player, new BlockPos(3, 2, 3), Direction.NORTH, 0.5, 0.5);
        helper.assertTrue(railAt(helper, 6, 2, 3), "a side face hit across the line follows the line too");
        helper.succeed();
    }

    /** A run that ends at a stone block places nothing and keeps the stack. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailBlockedPlacesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        floorRails(helper, 1, 3, 3);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = sneakRail(helper, player, new BlockPos(1, 2, 3), Direction.UP, 0.9, 0.5);
        helper.assertTrue(held.getCount() == 1, "the stack should keep its rail");
        helper.assertFalse(railAt(helper, 1, 3, 3) || railAt(helper, 1, 2, 2) || railAt(helper, 1, 2, 4),
                "no rail should be placed anywhere");
        helper.succeed();
    }

    /** Sneak clicking a rail hung on a rack towards the other rack fills the middle of the gap and the bridge holds. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailBridgesRacks(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 1, 2, Direction.EAST);
        hangRack(helper, 5, 2, Direction.WEST);
        hangRail(helper, 2, 2);
        hangRail(helper, 4, 2);
        helper.runAfterDelay(3, () -> {
            sneakRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.UP, 0.9, 0.5);
            helper.runAfterDelay(5, () -> {
                for (int x = 2; x <= 4; x++) {
                    helper.assertTrue(railAt(helper, x, 2), "bridge rail " + x + " should stay");
                }
                helper.succeed();
            });
        });
    }

    /** Sneak extending a lone hung rail into the air drops both, as a hand placed overhang would. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakRailOverhangCollapses(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        hangRack(helper, 1, 2, Direction.EAST);
        hangRail(helper, 2, 2);
        helper.runAfterDelay(3, () -> {
            sneakRail(helper, player, new BlockPos(2, HANG_Y, 2), Direction.UP, 0.9, 0.5);
            helper.assertTrue(railAt(helper, 3, 2), "the rail should be placed beyond the hung rail");
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(railAt(helper, 2, 2) || railAt(helper, 3, 2), "the overhang should drop both rails");
                helper.succeed();
            });
        });
    }

    /** Uses an elevator item while sneaking on the east face of a segment. */
    private static ItemStack sneakElevator(GameTestHelper helper, Player player, BlockPos clicked) {
        return useElevator(helper, player, clicked, true);
    }

    /** Uses an elevator item on the east face of a block, sneaking or not. */
    private static ItemStack useElevator(GameTestHelper helper, Player player, BlockPos clicked, boolean sneak) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack elevator = new ItemStack(ServusStorageBlocks.ELEVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elevator);
        player.setShiftKeyDown(sneak);
        elevator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.EAST, 0.5), Direction.EAST, absolute, false)));
        player.setShiftKeyDown(false);
        return elevator;
    }

    /** A plain click on the side of the lowest segment places a new segment on top of the shaft, like scaffolding. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void plainElevatorBuildsUp(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 4);
        ItemStack held = useElevator(helper, player, new BlockPos(3, 1, 3), false);
        helper.assertBlockPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(3, 5, 3));
        helper.assertBlockNotPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.assertTrue(held.isEmpty(), "one elevator should be used");
        helper.succeed();
    }

    /** A plain click on a shaft capped by stone places nothing and keeps the stack. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void plainElevatorBlockedTop(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 2);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = useElevator(helper, player, new BlockPos(3, 2, 3), false);
        helper.assertTrue(held.getCount() == 1, "the stack should keep its elevator");
        helper.assertBlockNotPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(4, 2, 3));
        helper.succeed();
    }

    /** A plain click on the top of a stone block places a segment on it as with any block. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void plainElevatorOnGround(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE.defaultBlockState());
        BlockPos absolute = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack elevator = new ItemStack(ServusStorageBlocks.ELEVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elevator);
        elevator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false)));
        helper.assertBlockPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(3, 2, 3));
        helper.succeed();
    }

    private static void shaft(GameTestHelper helper, int x, int z, int height) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= height; y++) {
            helper.setBlock(new BlockPos(x, y, z), ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        }
    }

    /** A sneak click on the side of the lowest segment places a new segment on top of the shaft. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakElevatorBuildsUp(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 4);
        sneakElevator(helper, player, new BlockPos(3, 1, 3));
        helper.assertBlockPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(3, 5, 3));
        helper.assertBlockNotPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.succeed();
    }

    /** A shaft capped by stone takes no new segment and the stack keeps its count. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void sneakElevatorBlockedTop(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 2);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = sneakElevator(helper, player, new BlockPos(3, 1, 3));
        helper.assertTrue(held.getCount() == 1, "the stack should keep its elevator");
        helper.assertBlockNotPresent(ServusStorageBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.succeed();
    }
}
