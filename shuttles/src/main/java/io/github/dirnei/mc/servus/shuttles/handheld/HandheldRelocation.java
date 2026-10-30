package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.Relocation;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The handheld's baskets and relocation on the server: edits of the
 * warehouse basket and the shelf basket, Relocate stashing the shelf basket
 * and putting the handheld into pick target mode, and the scan of the target
 * shelf that starts the shuttles.
 */
public final class HandheldRelocation {

    private static final String MESSAGE = "message.servus_shuttles.handheld.";
    private static final String SEPARATOR = "  ";
    private static final Component LIST_SEPARATOR = Component.literal(", ");

    private HandheldRelocation() {
    }

    /** The handheld's warehouse basket; empty when it has none. */
    public static Basket<ItemKey> basket(ItemStack handheld) {
        return BasketEntry.toBasket(handheld.getOrDefault(ServusShuttlesComponents.HANDHELD_BASKET.get(), List.of()));
    }

    /** The basket of the scanned shelf; empty when it has none. */
    public static Basket<ItemKey> shelfBasket(ItemStack handheld) {
        ShelfBasket basket = handheld.get(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get());
        return basket == null ? Basket.empty() : basket.basket();
    }

    /** The warehouse basket, or the shelf basket when the FIND list shows a shelf. */
    public static Basket<ItemKey> basket(ItemStack handheld, boolean shelfShown) {
        return shelfShown ? shelfBasket(handheld) : basket(handheld);
    }

    /** The relocation of pick target mode, or null when there is none. */
    public static @Nullable ShelfBasket relocation(ItemStack handheld) {
        return handheld.get(ServusShuttlesComponents.HANDHELD_RELOCATION.get());
    }

    private static void setBasket(ItemStack handheld, Basket<ItemKey> basket) {
        if (basket.isEmpty()) {
            handheld.remove(ServusShuttlesComponents.HANDHELD_BASKET.get());
        } else {
            handheld.set(ServusShuttlesComponents.HANDHELD_BASKET.get(), BasketEntry.of(basket));
        }
    }

    private static void setShelfBasket(ItemStack handheld, BlockPos shelf, Basket<ItemKey> basket) {
        if (basket.isEmpty()) {
            handheld.remove(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get());
        } else {
            handheld.set(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get(), ShelfBasket.of(shelf, basket));
        }
    }

    /** The FIND list now shows the shelf with that key: a shelf basket of another shelf is emptied. */
    public static void enterShelf(ItemStack handheld, BlockPos shelf) {
        ShelfBasket basket = handheld.get(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get());
        if (basket != null && !basket.shelf().equals(shelf)) {
            leaveShelf(handheld);
        }
    }

    /** The FIND list left the scanned shelf: the shelf basket is emptied. */
    public static void leaveShelf(ItemStack handheld) {
        handheld.remove(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get());
    }

    /** Whether the handheld waits for the target shelf of a relocation. */
    public static boolean pickingTarget(ItemStack handheld) {
        return handheld.getOrDefault(ServusShuttlesComponents.HANDHELD_PICK_TARGET.get(), false);
    }

    /** Ends pick target mode and drops the relocation; the baskets stay. */
    public static void stopPickingTarget(ItemStack handheld) {
        handheld.remove(ServusShuttlesComponents.HANDHELD_PICK_TARGET.get());
        handheld.remove(ServusShuttlesComponents.HANDHELD_RELOCATION.get());
    }

    /**
     * Applies a basket edit from the FIND screen to the basket of the list it
     * shows; a new item for a full basket is refused with a note.
     *
     * @param shelf the key of the shelf the list shows, null for the whole warehouse
     */
    public static void edit(Player player, ItemStack handheld, @Nullable BlockPos shelf, HandheldBasketPayload payload) {
        Basket<ItemKey> basket = shelf == null ? basket(handheld) : shelfBasketOf(handheld, shelf);
        Basket<ItemKey> edited = edited(player, basket, payload);
        if (edited.equals(basket)) {
            return;
        }
        if (shelf == null) {
            setBasket(handheld, edited);
        } else {
            setShelfBasket(handheld, shelf, edited);
        }
    }

    /** The shelf basket when it belongs to that shelf, else empty. */
    private static Basket<ItemKey> shelfBasketOf(ItemStack handheld, BlockPos shelf) {
        ShelfBasket basket = handheld.get(ServusShuttlesComponents.HANDHELD_SHELF_BASKET.get());
        return basket != null && basket.shelf().equals(shelf) ? basket.basket() : Basket.empty();
    }

    private static Basket<ItemKey> edited(Player player, Basket<ItemKey> basket, HandheldBasketPayload payload) {
        if (payload.op() == HandheldBasketPayload.Op.CLEAR) {
            return basket.clear();
        }
        if (payload.item().isEmpty()) {
            return basket;
        }
        ItemKey key = ItemKey.of(payload.item());
        return switch (payload.op()) {
            case ADD -> {
                if (!basket.canAdd(key)) {
                    player.sendOverlayMessage(Component.translatable(MESSAGE + "basket_full"));
                    yield basket;
                }
                yield basket.add(key, payload.amount());
            }
            case SET -> basket.set(key, payload.amount());
            case REMOVE -> basket.remove(key);
            case CLEAR -> basket.clear();
        };
    }

    /**
     * Relocate from the shelf the FIND list shows: when the handheld is in
     * FIND, bound to a warehouse and the shelf basket holds an entry, the
     * shelf basket becomes the relocation, is emptied, and pick target mode starts.
     *
     * @param shelf the key of the shelf the list shows
     * @return whether pick target mode started, so the caller closes the GUI
     */
    public static boolean relocate(Player player, ItemStack handheld, BlockPos shelf) {
        Basket<ItemKey> basket = shelfBasketOf(handheld, shelf);
        if (HandheldActions.mode(handheld) != HandheldMode.FIND || basket.isEmpty()
                || HandheldActions.boundNetwork(player.level().getServer(), handheld).isEmpty()) {
            return false;
        }
        handheld.set(ServusShuttlesComponents.HANDHELD_RELOCATION.get(), ShelfBasket.of(shelf, basket));
        leaveShelf(handheld);
        handheld.set(ServusShuttlesComponents.HANDHELD_PICK_TARGET.get(), true);
        return true;
    }

    /**
     * Pick target mode: relocates the stashed relocation from its source
     * shelf onto the shelf of the scanned bay. Success flashes the shelf
     * green and ends pick target mode; a refusal flashes it red and keeps it.
     */
    public static void scanTarget(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        List<BlockPos> shelf = ShelfZones.shelf(bay.getLevel(), bay.getBlockPos());
        Optional<WarehouseNetwork> network = HandheldActions.boundNetwork(player.level().getServer(), handheld);
        if (network.isEmpty()) {
            refuse(player, shelf, Component.translatable(MESSAGE + "not_bound"));
            return;
        }
        ShelfBasket stash = relocation(handheld);
        if (!(bay instanceof HeavyRackBlockEntity) || stash == null) {
            refuse(player, shelf, Component.translatable(MESSAGE + "relocate_not_target"));
            return;
        }
        Map<ItemKey, Integer> wanted = new LinkedHashMap<>();
        for (Basket.Entry<ItemKey> entry : stash.basket().entries()) {
            wanted.put(entry.key(), entry.amount());
        }
        List<BlockPos> source = ShelfZones.shelf(bay.getLevel(), stash.shelf());
        Relocation relocation = network.get().relocate(source, shelf, wanted);
        String code = bay.locationCode().isEmpty() ? "--" : bay.locationCode();
        switch (relocation.outcome()) {
            case STARTED -> {
                stopPickingTarget(handheld);
                player.sendOverlayMessage(withDetails(Component.translatable(MESSAGE + "relocate_started", code), relocation));
                send(player, new HandheldScanPayload(shelf, true, 0));
            }
            case NO_ROOM -> refuse(player, shelf, Component.translatable(MESSAGE + "relocate_no_room", code));
            case NOTHING_TO_MOVE -> refuse(player, shelf, withDetails(Component.translatable(MESSAGE + "relocate_nothing", code), relocation));
            case OFFLINE -> refuse(player, shelf, Component.translatable(MESSAGE + "relocate_offline"));
            case NOT_A_TARGET -> refuse(player, shelf, Component.translatable(MESSAGE + "relocate_not_target"));
        }
    }

    /** Pick target mode: a block that is no rack bay is refused with a buzz. */
    public static void refuseBlock(Player player, BlockPos pos) {
        refuse(player, List.of(pos), Component.translatable(MESSAGE + "relocate_not_target"));
    }

    private static void refuse(Player player, List<BlockPos> blocks, Component message) {
        player.sendOverlayMessage(message);
        send(player, new HandheldScanPayload(blocks, false, 0));
    }

    /** The message followed by the refused items and the items short, when there are any. */
    private static Component withDetails(MutableComponent message, Relocation relocation) {
        List<Component> refused = new ArrayList<>();
        List<Component> missing = new ArrayList<>();
        for (Relocation.Item item : relocation.items()) {
            Component name = item.key().stack(1).getHoverName();
            if (item.refused()) {
                refused.add(name);
            } else if (item.missing() > 0) {
                missing.add(Component.literal(item.missing() + " ").append(name));
            }
        }
        if (!refused.isEmpty()) {
            message.append(SEPARATOR).append(Component.translatable(MESSAGE + "relocate_refused", ComponentUtils.formatList(refused, LIST_SEPARATOR)));
        }
        if (!missing.isEmpty()) {
            message.append(SEPARATOR).append(Component.translatable(MESSAGE + "relocate_short", ComponentUtils.formatList(missing, LIST_SEPARATOR)));
        }
        return message;
    }

    private static void send(Player player, HandheldScanPayload payload) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }
}
