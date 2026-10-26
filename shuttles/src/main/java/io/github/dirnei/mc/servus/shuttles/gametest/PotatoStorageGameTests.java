package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Data Potato on storage: the section report of a rack, the tank report of a Fluid Gantry and the station report. */
public final class PotatoStorageGameTests {

    private static final String KEY = "message.servus_shuttles.potato.";
    private static final int ROW_Z = 4;
    private static final BlockPos RAIL = new BlockPos(4, 1, 4);
    private static final int LOCK_JOB = 9001;

    private PotatoStorageGameTests() {
    }

    /**
     * A section of three stamped "Ores" with cobblestone over 29 slots, 40 iron ore and one empty slot a put-away
     * locked reports its zone, 31 slots in use and its top items.
     */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsRackSection(GameTestHelper helper) {
        HeavyRackBlockEntity[] bays = section(helper, 2, 3);
        for (HeavyRackBlockEntity bay : bays) {
            bay.setZone(new ZoneDefinition("Ores", ZoneFilter.EMPTY));
        }
        for (int slot = 0; slot < 27; slot++) {
            bays[0].items().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        bays[1].items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        bays[1].items().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 8));
        bays[2].items().setStackInSlot(5, new ItemStack(Items.IRON_ORE, 40));
        bays[2].items().lock(10, new SlotLock(SlotLock.Kind.IN, ItemKey.of(new ItemStack(Items.STONE)), 64, LOCK_JOB));
        int total = 3 * bays[0].items().getSlots();

        List<Component> chat = new ArrayList<>();
        ServerPlayer player = potatoPlayer(helper, new BlockPos(3, 1, ROW_Z), chat);
        PlayerClicks.use(helper, player, new BlockPos(3, 1, ROW_Z));

        helper.assertTrue(chat.size() == 4, "zone, slots and two items, got " + chat);
        helper.assertTrue(args(helper, chat.get(0), "rack_zone")[0].equals("Ores"), "the zone is Ores");
        Object[] slots = args(helper, chat.get(1), "rack_slots");
        helper.assertTrue(slots[0].equals(3) && slots[1].equals(31) && slots[2].equals(total),
                "3 bays and 31 of " + total + " slots with the locked one, got " + List.of(slots));
        Object[] first = args(helper, chat.get(2), "rack_item");
        helper.assertTrue(text(first[0]).equals(Items.COBBLESTONE.getDescriptionId()) && first[1].equals(27 * 64 + 72),
                "cobblestone comes first with " + (27 * 64 + 72) + ", got " + List.of(first));
        Object[] second = args(helper, chat.get(3), "rack_item");
        helper.assertTrue(text(second[0]).equals(Items.IRON_ORE.getDescriptionId()) && second[1].equals(40), "then 40 iron ore, got " + List.of(second));
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
        helper.assertTrue(bays[2].items().getStackInSlot(5).getCount() == 40, "the items are unchanged");
        helper.succeed();
    }

    /** A full single bay is marked full; an empty unzoned bay says so. */
    @ServusGameTest(timeoutTicks = 40)
    public static void potatoReportsFullAndEmptyBays(GameTestHelper helper) {
        HeavyRackBlockEntity full = section(helper, 1, 1)[0];
        HeavyRackBlockEntity empty = section(helper, 5, 1)[0];
        int bay = full.items().getSlots();
        for (int slot = 0; slot < bay; slot++) {
            full.items().setStackInSlot(slot, new ItemStack(Items.DIRT, 1));
        }

        List<Component> chat = new ArrayList<>();
        PlayerClicks.use(helper, potatoPlayer(helper, new BlockPos(1, 1, ROW_Z), chat), new BlockPos(1, 1, ROW_Z));
        args(helper, chat.get(0), "rack_no_zone");
        Object[] slots = args(helper, chat.get(1), "rack_slots_full");
        helper.assertTrue(slots[1].equals(bay) && slots[2].equals(bay), "every slot is used, got " + List.of(slots));

        chat.clear();
        PlayerClicks.use(helper, potatoPlayer(helper, new BlockPos(5, 1, ROW_Z), chat), new BlockPos(5, 1, ROW_Z));
        helper.assertTrue(chat.size() == 3, "zone, slots and empty, got " + chat);
        args(helper, chat.get(0), "rack_no_zone");
        Object[] none = args(helper, chat.get(1), "rack_slots");
        helper.assertTrue(none[0].equals(1) && none[1].equals(0) && none[2].equals(bay), "1 bay, 0 of " + bay + ", got " + List.of(none));
        args(helper, chat.get(2), "rack_empty");
        helper.assertTrue(empty.isEmpty(), "the bay stays empty");
        helper.succeed();
    }

    /** A Storage gantry with one upgrade, water locked left and an empty right tank on Any, clicked on the beam and on a leg. */
    @ServusGameTest(timeoutTicks = 60)
    public static void potatoReportsGantry(GameTestHelper helper) {
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 6);
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, RAIL);
        gantry.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        gantry.setLock(0, Fluids.WATER);
        gantry.tank(0).fill(new FluidStack(Fluids.WATER, 12000), false);
        int capacity = gantry.tank(0).getCapacity();

        List<Component> chat = new ArrayList<>();
        BlockPos beam = RAIL.above();
        ServerPlayer player = potatoPlayer(helper, beam, chat);
        PlayerClicks.use(helper, player, beam);
        helper.assertTrue(chat.size() == 3, "mode and two tanks, got " + chat);
        helper.assertTrue(text(args(helper, chat.get(0), "gantry_mode")[0]).equals("gui.servus_shuttles.fluid_gantry.mode.storage"), "the mode is Storage");
        Object[] left = args(helper, chat.get(1), "gantry_tank");
        Object[] content = ((TranslatableContents) ((Component) left[1]).getContents()).getArgs();
        helper.assertTrue(content[1].equals(12000) && content[2].equals(capacity), "left holds 12,000 of " + capacity + ", got " + List.of(content));
        helper.assertTrue(text(left[2]).equals(Fluids.WATER.getFluidType().getDescriptionId()), "left is locked to water, got " + text(left[2]));
        Object[] right = args(helper, chat.get(2), "gantry_tank");
        helper.assertTrue(((TranslatableContents) ((Component) right[1]).getContents()).getKey().equals("message.servus_foundation.fluid_tank.empty"),
                "the right tank is empty");
        helper.assertTrue(text(right[2]).equals(KEY + "gantry_any"), "the right tank is on Any");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens from the beam");

        chat.clear();
        BlockPos leg = RAIL.east();
        ServerPlayer legPlayer = potatoPlayer(helper, leg, chat);
        PlayerClicks.use(helper, legPlayer, leg);
        helper.assertTrue(chat.size() == 3, "a leg gives the same report, got " + chat);
        helper.assertTrue(legPlayer.containerMenu == legPlayer.inventoryMenu, "no GUI opens from a leg");
        helper.assertTrue(gantry.mode() == GantryMode.STORAGE && gantry.lock(0) == Fluids.WATER && gantry.lock(1) == null
                && gantry.tank(0).getFluidAmount() == 12000, "nothing changed");
        helper.succeed();
    }

    /** An Outbound Station of the site "North Mine" reports its label, the site and that it is online; no GUI opens. */
    @ServusGameTest(timeoutTicks = 100)
    public static void potatoReportsOnlineStation(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 1, 6, 2);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, new BlockPos(0, 1, 2));
        controller.setSiteName("North Mine");
        BlockPos at = new BlockPos(3, 1, 3);
        OutboundStationBlockEntity station = WarehouseFixtures.outbound(helper, at, Direction.NORTH);
        Waits.when(helper, () -> station.number() > 0, 60, () -> "the station should get a number", () -> {
            station.rename("Compost Dock");
            List<Component> chat = new ArrayList<>();
            ServerPlayer player = potatoPlayer(helper, at, chat);
            PlayerClicks.use(helper, player, at);
            helper.assertTrue(chat.size() == 3, "label, site and online, got " + chat);
            Component label = chat.get(0);
            helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(NamedMachine.NAMED_KEY)
                    && contents.getArgs()[0].equals("Compost Dock") && contents.getArgs()[1] instanceof Component numbered
                    && numbered.getContents() instanceof TranslatableContents kind && kind.getKey().equals("machine.servus_shuttles.outbound_station")
                    && kind.getArgs()[0].equals(1), "the label is Compost Dock (Outbound #1), got " + label);
            helper.assertTrue(args(helper, chat.get(1), "station_site")[0].equals("North Mine"), "the site is North Mine");
            args(helper, chat.get(2), "station_online");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
            helper.succeed();
        });
    }

    /** A station whose rail reaches no controller reports its label and that it has no controller. */
    @ServusGameTest(timeoutTicks = 60)
    public static void potatoReportsOfflineStation(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 1, 6, 2);
        BlockPos at = new BlockPos(3, 1, 3);
        WarehouseFixtures.inbound(helper, at, Direction.NORTH);
        helper.runAfterDelay(5, () -> {
            List<Component> chat = new ArrayList<>();
            ServerPlayer player = potatoPlayer(helper, at, chat);
            PlayerClicks.use(helper, player, at);
            helper.assertTrue(chat.size() == 2, "label and offline reason, got " + chat);
            helper.assertTrue(text(chat.get(0)).equals("block.servus_shuttles.inbound_station"),
                    "an unnumbered station reads Inbound Station");
            helper.assertTrue(text(args(helper, chat.get(1), "station_offline")[0]).equals("gui.servus_shuttles.terminal.no_controller"),
                    "the station has no controller");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "no GUI opens");
            helper.succeed();
        });
    }

    /** A row of {@code count} joined racks facing north from x {@code first}, left (west) to right. */
    private static HeavyRackBlockEntity[] section(GameTestHelper helper, int first, int count) {
        HeavyRackBlockEntity[] bays = new HeavyRackBlockEntity[count];
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(first + i, 1, ROW_Z);
            helper.setBlock(pos, ServusShuttlesBlocks.HEAVY_RACK.get().defaultBlockState()
                    .setValue(HeavyRackBlock.FACING, Direction.NORTH)
                    .setValue(HeavyRackBlock.LEFT, i > 0)
                    .setValue(HeavyRackBlock.RIGHT, i < count - 1));
            bays[i] = helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
        }
        return bays;
    }

    private static ServerPlayer potatoPlayer(GameTestHelper helper, BlockPos pos, List<Component> chat) {
        return PlayerClicks.player(helper, pos, new ItemStack(ServusFoundationItems.DATA_POTATO.get()), chat);
    }

    private static Object[] args(GameTestHelper helper, Component line, String key) {
        helper.assertTrue(line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(KEY + key),
                "expected " + KEY + key + ", got " + line);
        return ((TranslatableContents) line.getContents()).getArgs();
    }

    /** The lang key of a translatable argument, or the text of a literal one. */
    private static String text(Object argument) {
        Component component = (Component) argument;
        return component.getContents() instanceof TranslatableContents contents ? contents.getKey() : component.getString();
    }
}
