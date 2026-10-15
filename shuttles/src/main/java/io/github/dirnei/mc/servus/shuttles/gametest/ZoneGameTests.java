package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Zone labels and put-away by zone. */
public final class ZoneGameTests {

    private static final BlockPos ZL_OTHER = new BlockPos(2, 1, 9);
    private static final BlockPos ZL_RACK = new BlockPos(2, 1, 7);
    private static final BlockPos IZ_CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos IZ_ORES = new BlockPos(4, 1, 10);
    private static final BlockPos IZ_PLAIN = new BlockPos(5, 1, 10);
    private static final BlockPos IZ_SPARE = new BlockPos(7, 1, 10);
    private static final BlockPos IZ_IRON = new BlockPos(8, 1, 10);

    private ZoneGameTests() {
    }

    /** Uses the held label on the block's north face. */
    private static void useLabel(GameTestHelper helper, Player player, ItemStack label, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0, -0.5), Direction.NORTH, absolute, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, label);
        label.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ItemStack label(String name, ZoneFilter filter) {
        ItemStack label = new ItemStack(ServusShuttlesItems.ZONE_LABEL.get());
        if (name != null) {
            label.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        if (filter != null) {
            label.set(ServusShuttlesComponents.ZONE_FILTER.get(), filter);
        }
        return label;
    }

    /** A named label copies name and filter onto racks, a restamp replaces, sneaking clears, an unnamed label does nothing, and the label stays. */
    @ServusGameTest(timeoutTicks = 100)
    public static void zoneLabelStampsAndClears(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, ZL_OTHER, Direction.NORTH);
        WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        HeavyRackBlockEntity crate = helper.getBlockEntity(ZL_OTHER, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity rack = helper.getBlockEntity(ZL_RACK, HeavyRackBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack unnamed = label(null, new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, unnamed, ZL_OTHER);
        helper.assertTrue(crate.zone().isEmpty(), "an unnamed label must not zone the rack");
        helper.assertTrue(crate.isEmpty(), "the label must not be inserted into the rack");

        ItemStack aisle = label(" Aisle 3 ", new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, aisle, ZL_OTHER);
        useLabel(helper, player, aisle, ZL_RACK);
        helper.assertTrue(crate.zone().equals("Aisle 3"), "first rack zone should be trimmed to Aisle 3, was '" + crate.zone() + "'");
        helper.assertTrue(rack.zone().equals("Aisle 3"), "rack zone should be Aisle 3, was '" + rack.zone() + "'");
        helper.assertTrue(crate.zoneKey().equals("aisle 3"), "zone key should be lower case");
        helper.assertTrue(crate.zoneFilter().tags().equals(List.of("c:ores")), "the first rack should carry the label's tags, had " + crate.zoneFilter().tags());
        helper.assertTrue(rack.getDisplayName().getString().endsWith("(Aisle 3)"), "rack title should carry the zone, was " + rack.getDisplayName().getString());
        helper.assertTrue(!aisle.isEmpty() && aisle.getCount() == 1, "the label must not be consumed");

        ItemStack food = label("Food", new ZoneFilter(List.of(new ItemStack(Items.BREAD)), List.of()));
        useLabel(helper, player, food, ZL_RACK);
        helper.assertTrue(rack.zone().equals("Food"), "restamp should replace the zone");
        helper.assertTrue(rack.zoneFilter().items().size() == 1 && rack.zoneFilter().items().get(0).is(Items.BREAD), "restamp should replace the filter");

        player.setShiftKeyDown(true);
        useLabel(helper, player, food, ZL_OTHER);
        player.setShiftKeyDown(false);
        helper.assertTrue(crate.zone().isEmpty() && crate.zoneFilter().isEmpty(), "sneak click should clear the zone");

        ItemStack drop = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        BlockEntityItems.saveToItem(rack, drop, helper.getLevel().registryAccess());
        helper.setBlock(ZL_RACK, Blocks.AIR.defaultBlockState());
        WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(ZL_RACK), drop);
        HeavyRackBlockEntity placed = helper.getBlockEntity(ZL_RACK, HeavyRackBlockEntity.class);
        helper.assertTrue(placed.zone().equals("Food"), "a dismantled and placed rack keeps its zone, was '" + placed.zone() + "'");
        helper.assertTrue(placed.zoneFilter().items().size() == 1, "a dismantled and placed rack keeps its filter");
        helper.succeed();
    }

    /** A named label zones nothing on the controller; on a rack it zones the rack. */
    @ServusGameTest(timeoutTicks = 40)
    public static void zoneLabelOnlyOnRacks(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(5, 1, 9);
        WarehouseFixtures.controller(helper, controllerPos);
        WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack ores = label("Ores", new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, ores, controllerPos);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(controllerPos, WarehouseControllerBlockEntity.class);
        helper.assertTrue(controller.zone().isEmpty() && controller.zoneFilter().isEmpty(), "the controller takes no zone, got '" + controller.zone() + "'");
        useLabel(helper, player, ores, ZL_RACK);
        HeavyRackBlockEntity rack = helper.getBlockEntity(ZL_RACK, HeavyRackBlockEntity.class);
        helper.assertTrue(rack.zone().equals("Ores") && rack.zoneFilter().tags().equals(List.of("c:ores")), "the rack takes the zone, got '" + rack.zone() + "'");
        helper.succeed();
    }

    /** The label menu names the label, parses tags and takes ghost items; the result stamps. */
    @ServusGameTest(timeoutTicks = 100)
    public static void zoneLabelMenuEditsDefinition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusShuttlesItems.ZONE_LABEL.get()));
        ZoneLabelMenu menu = new ZoneLabelMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a fresh label defines no zone");

        menu.setName("  Ores ");
        menu.setTags("c:ores, #minecraft:logs, not a tag!!");
        menu.setCarried(new ItemStack(Items.BREAD, 5));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        ItemStack labelStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ZoneDefinition definition = ZoneDefinition.of(labelStack);
        helper.assertTrue(definition != null && definition.name().equals("Ores"), "the label should be named Ores");
        helper.assertTrue(definition.filter().tags().equals(List.of("c:ores", "minecraft:logs")), "tags should be parsed and cleaned, were " + definition.filter().tags());
        helper.assertTrue(definition.filter().items().size() == 1 && definition.filter().items().get(0).is(Items.BREAD)
                && definition.filter().items().get(0).getCount() == 1, "the ghost slot should put one bread on the label");
        helper.assertTrue(menu.getCarried().getCount() == 5, "the cursor keeps its bread");
        helper.assertTrue(labelStack.getHoverName().getString().equals("Ores"), "the item shows the zone name");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)).filter().items().isEmpty(), "an empty cursor clears the ghost slot");

        menu.setName("");
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a blank name unnames the label");
        menu.setName("Ores");

        HeavyRackBlockEntity crate = WarehouseFixtures.rack(helper, ZL_OTHER, Direction.NORTH);
        useLabel(helper, player, player.getItemInHand(InteractionHand.MAIN_HAND), ZL_OTHER);
        helper.assertTrue(crate.zone().equals("Ores") && crate.zoneFilter().tags().equals(List.of("c:ores", "minecraft:logs")),
                "the edited label should stamp name and tags, got " + crate.zone() + " " + crate.zoneFilter().tags());
        helper.succeed();
    }

    private static ZoneDefinition zone(String name, List<ItemStack> items, List<String> tags) {
        return new ZoneDefinition(name, new ZoneFilter(items, tags));
    }

    /** Stores a stack the way a shuttle put-away routes it, straight into each chosen rack; returns what found no room. */
    private static ItemStack store(WarehouseNetwork network, ItemStack stack) {
        ItemStack left = stack.copy();
        while (!left.isEmpty()) {
            Optional<BlockPos> target = network.putAwayTarget(left, pos -> true);
            if (target.isEmpty()) {
                break;
            }
            left = network.insertAt(target.get(), left);
        }
        return left;
    }

    /** Put-away follows the zones: exact items beat tags, filtered racks refuse other items, unzoned racks catch full zones. */
    @ServusGameTest(timeoutTicks = 200)
    public static void insertRespectsZones(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseFixtures.controller(helper, IZ_CONTROLLER);
        for (BlockPos pos : List.of(IZ_ORES, IZ_PLAIN, IZ_SPARE, IZ_IRON)) {
            WarehouseFixtures.rack(helper, pos, Direction.NORTH);
        }
        HeavyRackBlockEntity ores = helper.getBlockEntity(IZ_ORES, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity iron = helper.getBlockEntity(IZ_IRON, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity plain = helper.getBlockEntity(IZ_PLAIN, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity spare = helper.getBlockEntity(IZ_SPARE, HeavyRackBlockEntity.class);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(IZ_CONTROLLER, WarehouseControllerBlockEntity.class);
        ores.setZone(zone("Ores", List.of(), List.of("c:ores")));
        iron.setZone(zone("Iron", List.of(new ItemStack(Items.IRON_ORE)), List.of()));
        plain.items().insertItem(0, new ItemStack(Items.BREAD, 1), false);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "warehouse should be online");
            helper.assertTrue(network.zoneBlockCounts().equals(Map.of("ores", 1, "iron", 1)), "two zones with one block each, got " + network.zoneBlockCounts());

            ItemStack left = store(network, new ItemStack(Items.IRON_ORE, 10));
            helper.assertTrue(left.isEmpty(), "iron ore should fit");
            helper.assertTrue(WarehouseFixtures.count(iron, Items.IRON_ORE) == 10, "iron ore should land in the Iron rack (exact beats tag), had " + WarehouseFixtures.count(iron, Items.IRON_ORE));
            helper.assertTrue(ores.isEmpty(), "the Ores rack should not take it while the exact match has room");

            left = store(network, new ItemStack(Items.COPPER_ORE, 10));
            helper.assertTrue(left.isEmpty(), "copper ore should fit");
            helper.assertTrue(WarehouseFixtures.count(ores, Items.COPPER_ORE) == 10, "copper ore should land in the Ores rack by tag");
            helper.assertTrue(WarehouseFixtures.count(plain, Items.COPPER_ORE) == 0 && spare.isEmpty(), "unzoned racks must not take ore while a zone has room");

            helper.runAfterDelay(10, () -> {
                ItemStack bread = store(network, new ItemStack(Items.BREAD, 5));
                helper.assertTrue(bread.isEmpty(), "bread should fit");
                helper.assertTrue(WarehouseFixtures.count(plain, Items.BREAD) == 6, "bread should join the unzoned rack already holding bread, had " + WarehouseFixtures.count(plain, Items.BREAD));

                WarehouseFixtures.fill(iron, Items.IRON_ORE);
                WarehouseFixtures.fill(ores, Items.COPPER_ORE);
                ItemStack ore = store(network, new ItemStack(Items.IRON_ORE, 7));
                helper.assertTrue(ore.isEmpty(), "with the zones full an unzoned rack takes the ore");
                helper.assertTrue(WarehouseFixtures.count(plain, Items.IRON_ORE) + WarehouseFixtures.count(spare, Items.IRON_ORE) == 7,
                        "the ore should be in the unzoned racks");

                WarehouseFixtures.fill(plain, Items.DIRT);
                WarehouseFixtures.fill(spare, Items.DIRT);
                ore = store(network, new ItemStack(Items.IRON_ORE, 7));
                helper.assertTrue(ore.getCount() == 7, "with everything full nothing is stored, left " + ore.getCount());
                helper.succeed();
            });
        });
    }

    /** With its zone full and no unzoned storage the stack waits with a red lamp, and leaves once an unzoned rack appears. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stationBacksUpWhenZoneFull(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 4));
        HeavyRackBlockEntity full = warehouse.rack();
        full.setZone(zone("Ores", List.of(), List.of("c:ores")));
        WarehouseFixtures.fill(full, Items.IRON_ORE);
        InboundStationBlockEntity station = warehouse.withInbound();
        BlockPos stationPos = warehouse.inboundPos();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.IRON_ORE, 7), false);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!station.isEmpty(), "the ore should wait in the buffer");
                helper.assertTrue(WarehouseFixtures.blocked(helper, stationPos), "the lamp should be red");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_ROOM,
                        "reason should be no room, was " + station.blockedReason());
                helper.assertTrue("Ores".equals(station.blockedZone()), "blocked zone should read Ores, was " + station.blockedZone());
                BlockPos freshPos = warehouse.railPos(0).south();
                HeavyRackBlockEntity fresh = WarehouseFixtures.rack(helper, freshPos, Direction.NORTH);
                Waits.when(helper, () -> WarehouseFixtures.count(fresh, Items.IRON_ORE) == 7, MiniWarehouse.TRIP_TICKS * 2,
                        () -> "the waiting ore should go into the new unzoned rack, had " + WarehouseFixtures.count(fresh, Items.IRON_ORE), () -> {
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty again");
                    helper.assertTrue(!WarehouseFixtures.blocked(helper, stationPos), "the lamp should be back to normal");
                    helper.succeed();
                });
            });
        });
    }
}
