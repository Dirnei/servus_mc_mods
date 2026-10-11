package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Shuttle Workshop, shuttle modules and how the fleet uses them. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShuttleModuleGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos WS_POS = new BlockPos(5, 1, 5);
    /** Menu index of hotbar slot 0. */
    private static final int HOTBAR = ShuttleWorkshopMenu.INVENTORY_START + 27;

    private ShuttleModuleGameTests() {
    }

    private static ShuttleWorkshopMenu workshop(GameTestHelper helper, Player player) {
        helper.setBlock(WS_POS, ServusStorageBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        return new ShuttleWorkshopMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(WS_POS)));
    }

    private static boolean hasCargoHold(ItemStack shuttle) {
        return shuttle.is(ServusStorageItems.SHUTTLE.get())
                && ShuttleModules.of(shuttle).utility().equals(Optional.of(ServusStorageItems.CARGO_HOLD.get()));
    }

    /** Fit a Cargo Hold, carry the shuttle out and back in, take the module out again. */
    @GameTest(template = EMPTY)
    public static void workshopFitsCargoHold(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.CARGO_HOLD.get()));

        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.shuttle().is(ServusStorageItems.SHUTTLE.get()), "the shuttle goes into the shuttle slot");
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(hasCargoHold(menu.shuttle()), "the Cargo Hold is fitted into the shuttle");
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "the Cargo Hold item left the inventory");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().is(ServusStorageItems.CARGO_HOLD.get()), "the utility slot shows it");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.TOOL_SLOT).getItem().isEmpty(), "the tool slot stays empty");

        menu.quickMoveStack(player, ShuttleWorkshopMenu.SHUTTLE_SLOT);
        helper.assertTrue(menu.shuttle().isEmpty(), "the shuttle left the workshop");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().isEmpty(), "no shuttle, no modules shown");
        int at = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (hasCargoHold(player.getInventory().getItem(i))) {
                at = i;
            }
        }
        helper.assertTrue(at >= 0, "the player holds a shuttle with a Cargo Hold");

        int menuIndex = at < 9 ? HOTBAR + at : ShuttleWorkshopMenu.INVENTORY_START + at - 9;
        menu.quickMoveStack(player, menuIndex);
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().is(ServusStorageItems.CARGO_HOLD.get()),
                "back in the workshop the utility slot shows the Cargo Hold");

        menu.quickMoveStack(player, ShuttleWorkshopMenu.UTILITY_SLOT);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).isEmpty(), "the module is removed from the shuttle");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.shuttle(), new ItemStack(ServusStorageItems.SHUTTLE.get())),
                "a shuttle without modules is a plain shuttle again");
        helper.assertTrue(player.getInventory().countItem(ServusStorageItems.CARGO_HOLD.get()) == 1, "the Cargo Hold is back in the inventory");
        helper.succeed();
    }

    /** Modules need a shuttle and the slot of their kind; only one shuttle goes in. */
    @GameTest(template = EMPTY)
    public static void workshopRefusesWrongModules(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ItemStack cargo = new ItemStack(ServusStorageItems.CARGO_HOLD.get());
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).mayPlace(cargo), "no module without a shuttle");
        player.getInventory().setItem(1, cargo);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 1, "shift click leaves the module in the inventory without a shuttle");

        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get(), 5));
        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.shuttle().getCount() == 1, "one shuttle goes in");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 4, "four stay in the inventory");
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.TOOL_SLOT).mayPlace(cargo), "a Cargo Hold does not fit the tool slot");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).mayPlace(cargo), "it fits the utility slot");
        helper.succeed();
    }

    /** The workshop keeps nothing: closing hands the shuttle back with its modules. */
    @GameTest(template = EMPTY)
    public static void workshopReturnsShuttleOnClose(GameTestHelper helper) {
        // Vanilla hands container items back only to a server player.
        Player player = helper.makeMockServerPlayerInLevel();
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.CARGO_HOLD.get()));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        menu.removed(player);
        boolean back = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            back |= hasCargoHold(player.getInventory().getItem(i));
        }
        helper.assertTrue(back, "the shuttle with its Cargo Hold is back in the inventory");
        helper.assertTrue(menu.shuttle().isEmpty(), "the workshop is empty");
        helper.succeed();
    }
}
