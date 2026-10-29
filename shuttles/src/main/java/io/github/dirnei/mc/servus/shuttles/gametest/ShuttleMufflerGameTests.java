package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlockEntity;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** The Muffler: installed into a shuttle in the workshop, kept through builds and next to a tool, and in a garage's Muffler slot. */
public final class ShuttleMufflerGameTests {

    private static final BlockPos WORKSHOP = new BlockPos(5, 1, 5);
    /** Menu index of hotbar slot 0. */
    private static final int HOTBAR = ShuttleWorkshopMenu.INVENTORY_START + 27;

    private ShuttleMufflerGameTests() {
    }

    private static ItemStack mufflers(int count) {
        return new ItemStack(ServusShuttlesItems.MUFFLER.get(), count);
    }

    private static boolean muffled(ShuttleWorkshopMenu menu) {
        return ShuttleModules.of(menu.shuttle()).muffled();
    }

    /** Three Mufflers on a shuttle without one: one goes in, two stay with the player. */
    @ServusGameTest
    public static void installOneMufflerFromStack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, mufflers(3));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(muffled(menu), "the shuttle is muffled");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 2, "two Mufflers stay with the player, got "
                + player.getInventory().getItem(1).getCount());
        helper.assertTrue(menu.supplied("servus_shuttles:muffler") == 0, "no Muffler went to the supplies");
        helper.succeed();
    }

    /** A shuttle with a Muffler takes no second one, by click or by shift click. */
    @ServusGameTest
    public static void secondMufflerRefused(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        ItemStack shuttle = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withMuffled(true));
        player.getInventory().setItem(0, shuttle);
        menu.quickMoveStack(player, HOTBAR);
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).mayPlace(mufflers(1)), "the install slot refuses a second Muffler");
        helper.assertTrue(menu.install(mufflers(1)) == 0, "nothing installs");
        helper.succeed();
    }

    /** The remove button gives the Muffler back, and the bare shuttle stacks with a new one. */
    @ServusGameTest
    public static void removeMufflerGivesItBack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        menu.quickMoveStack(player, HOTBAR);
        menu.install(mufflers(1));
        helper.assertTrue(muffled(menu), "the Muffler is in");
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_MUFFLER), "the remove button works");
        helper.assertFalse(muffled(menu), "the shuttle is no longer muffled");
        helper.assertTrue(player.getInventory().countItem(ServusShuttlesItems.MUFFLER.get()) == 1, "the Muffler is back");
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_MUFFLER), "nothing left to take out");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.shuttle(), new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get())),
                "the shuttle stacks with a new Mk1 again");
        helper.succeed();
    }

    /** A muffled Mk1 built into a Mk2 keeps its Muffler. */
    @ServusGameTest
    public static void mufflerSurvivesBuild(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WORKSHOP, ShuttleWorkshopBlockEntity.class);
        ItemStack shuttle = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withMuffled(true));
        workshop.shuttle().setStackInSlot(0, shuttle);
        ShuttleModuleGameTests.supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP);
        helper.assertTrue(menu.shuttle().is(ServusShuttlesItems.SHUTTLE_MK2.get()), "the shuttle is a Mk2");
        helper.assertTrue(muffled(menu), "the Mk2 has the Muffler");
        helper.succeed();
    }

    /** A Muffler goes in next to a Harvester; the shuttle keeps both and its tooltip lists both. */
    @ServusGameTest
    public static void mufflerNextToHarvester(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        ItemStack shuttle = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, new ShuttleModules(Optional.of(ServusShuttlesItems.HARVESTER.get()), 0, 0));
        player.getInventory().setItem(0, shuttle);
        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.install(mufflers(1)) == 1, "the Muffler goes in");
        ShuttleModules modules = ShuttleModules.of(menu.shuttle());
        helper.assertTrue(modules.muffled() && modules.tool().equals(Optional.of(ServusShuttlesItems.HARVESTER.get())),
                "the shuttle has the Harvester and the Muffler");
        List<Component> lines = menu.shuttle().getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL);
        helper.assertTrue(hasLine(lines, "tooltip.servus_shuttles.shuttle.tool"), "the tooltip lists the tool");
        helper.assertTrue(hasLine(lines, "tooltip.servus_shuttles.shuttle.muffled"), "the tooltip lists the Muffler");
        helper.succeed();
    }

    /** The modules keep the Muffler through the save codec and the network codec; modules saved before it load unmuffled. */
    @ServusGameTest
    public static void modulesCodecKeepsMuffler(GameTestHelper helper) {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (boolean muffled : new boolean[] {true, false}) {
            ShuttleModules modules = new ShuttleModules(Optional.of(ServusShuttlesItems.HARVESTER.get()), 1, 2, muffled);
            Tag saved = ShuttleModules.CODEC.encodeStart(ops, modules).getOrThrow();
            helper.assertTrue(ShuttleModules.CODEC.parse(ops, saved).getOrThrow().equals(modules), "saved and loaded, muffled " + muffled);
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            ShuttleModules.STREAM_CODEC.encode(buf, modules);
            helper.assertTrue(ShuttleModules.STREAM_CODEC.decode(buf).equals(modules), "sent and received, muffled " + muffled);
        }
        CompoundTag old = new CompoundTag();
        old.putInt("speed", 1);
        helper.assertFalse(ShuttleModules.CODEC.parse(ops, old).getOrThrow().muffled(), "an old shuttle loads without a Muffler");
        helper.succeed();
    }

    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos GARAGE = new BlockPos(2, 1, 5);

    private static ShuttleGarageBlockEntity garage(GameTestHelper helper) {
        return WarehouseFixtures.garage(helper, GARAGE, Direction.SOUTH, 0);
    }

    /** The garage's Muffler slot takes a Muffler, one, and nothing else. */
    @ServusGameTest
    public static void onlyMufflerFitsGarageSlot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleGarageBlockEntity garage = garage(helper);
        ShuttleGarageMenu menu = new ShuttleGarageMenu(1, player.getInventory(), garage);
        Slot slot = menu.getSlot(menu.mufflerSlot());
        helper.assertFalse(slot.mayPlace(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())), "a Speed Upgrade does not fit");
        helper.assertFalse(slot.mayPlace(new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get())), "a shuttle does not fit");
        helper.assertTrue(slot.mayPlace(mufflers(1)), "a Muffler fits");
        player.getInventory().setItem(0, mufflers(3));
        menu.quickMoveStack(player, menu.inventoryStart() + 27);
        helper.assertTrue(garage.muffled(), "shift click puts a Muffler in");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 2, "one Muffler went in, got "
                + player.getInventory().getItem(0).getCount());
        menu.quickMoveStack(player, menu.mufflerSlot());
        helper.assertFalse(garage.muffled(), "shift click takes it out again");
        helper.assertTrue(player.getInventory().countItem(ServusShuttlesItems.MUFFLER.get()) == 3, "all three are back");
        helper.succeed();
    }

    /** The Muffler is saved with the garage and comes back on load. */
    @ServusGameTest
    public static void garageMufflerSurvivesSaveAndLoad(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = garage(helper);
        garage.muffler().setStackInSlot(0, mufflers(1));
        CompoundTag saved = garage.saveWithoutMetadata(helper.getLevel().registryAccess());
        garage.muffler().setStackInSlot(0, ItemStack.EMPTY);
        garage.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(garage.muffled(), "the Muffler is back after loading");
        helper.succeed();
    }

    /** Breaking an empty garage drops its Muffler. */
    @ServusGameTest
    public static void brokenGarageDropsMuffler(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = garage(helper);
        garage.muffler().setStackInSlot(0, mufflers(1));
        helper.destroyBlock(GARAGE);
        helper.assertItemEntityPresent(ServusShuttlesItems.MUFFLER.get(), GARAGE, 2);
        helper.succeed();
    }

    /** The sync of a garage with a Muffler says muffled, and stops saying so once the Muffler is out. */
    @ServusGameTest(timeoutTicks = 100)
    public static void garageSyncSaysMuffled(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        TerminalWiring.place(helper, TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 6, 4);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, GARAGE, Direction.SOUTH, 1);
        BlockPos at = helper.absolutePos(GARAGE);
        Waits.when(helper, () -> garage.network().isPresent(), 60, () -> "the garage joins the warehouse", () -> {
            WarehouseNetwork network = garage.network().orElseThrow();
            helper.assertFalse(network.shuttleSync(at).muffled(), "not muffled without a Muffler");
            garage.muffler().setStackInSlot(0, mufflers(1));
            helper.assertTrue(network.shuttleSync(at).muffled(), "muffled with a Muffler");
            garage.muffler().setStackInSlot(0, ItemStack.EMPTY);
            helper.assertFalse(network.shuttleSync(at).muffled(), "not muffled once it is out");
            helper.succeed();
        });
    }

    private static boolean hasLine(List<Component> lines, String key) {
        return lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key));
    }
}
