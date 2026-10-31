package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerOrders;
import io.github.dirnei.mc.servus.shuttles.locker.OrderRange;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Handheld's orders on the server: the lockers the STOCK basket could
 * go to, and SEND, which orders the basket to one of them and empties it.
 * An HH-1 orders only within the bound warehouse's site and a margin around it.
 */
public final class HandheldOrders {

    private static final String MESSAGE = "message.servus_shuttles.handheld.order.";

    private HandheldOrders() {
    }

    /** What the locker picker lists for the player: every locker of the bound warehouse, or why nothing can be sent. */
    public static HandheldLockersPayload lockers(Player player, ItemStack handheld) {
        Optional<WarehouseNetwork> network = HandheldActions.boundNetwork(player.level().getServer(), handheld);
        String problem = problem(player, network);
        if (!problem.isEmpty()) {
            return HandheldLockersPayload.problem(problem);
        }
        List<ParcelLockerBlockEntity> lockers = network.get().parcelLockers();
        if (lockers.isEmpty()) {
            return HandheldLockersPayload.problem("no_locker");
        }
        Basket<ItemKey> basket = HandheldRelocation.basket(handheld);
        List<HandheldLockersPayload.Row> rows = new ArrayList<>();
        for (ParcelLockerBlockEntity locker : lockers) {
            List<Integer> free = List.of(locker.freeCount(LockerLayout.Size.S), locker.freeCount(LockerLayout.Size.M),
                    locker.freeCount(LockerLayout.Size.L));
            rows.add(new HandheldLockersPayload.Row(locker.label(), locker.getBlockPos(), free, LockerOrders.fits(network.get(), locker, basket)));
        }
        return new HandheldLockersPayload("", rows);
    }

    /**
     * SEND: orders the STOCK basket to the locker whose dock is at {@code dock}
     * and empties the basket; the action bar names the locker and what was
     * short, or why nothing was ordered.
     *
     * @return whether the order stands
     */
    public static boolean request(Player player, ItemStack handheld, BlockPos dock) {
        Optional<WarehouseNetwork> network = HandheldActions.boundNetwork(player.level().getServer(), handheld);
        String problem = problem(player, network);
        if (!problem.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + problem));
            return false;
        }
        Basket<ItemKey> basket = HandheldRelocation.basket(handheld);
        Optional<ParcelLockerBlockEntity> locker = network.get().parcelLockers().stream()
                .filter(found -> found.getBlockPos().equals(dock)).findFirst();
        if (basket.isEmpty() || locker.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + (basket.isEmpty() ? "empty" : "no_locker")));
            return false;
        }
        LockerOrders.Placed placed = LockerOrders.place(network.get(), locker.get(), player.getUUID(), player.getName().getString(), basket);
        if (!placed.placed()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + placed.refusal(), locker.get().label()));
            return false;
        }
        HandheldRelocation.edit(player, handheld, null, HandheldBasketPayload.clear());
        player.sendOverlayMessage(placed.shortBy().isEmpty()
                ? Component.translatable(MESSAGE + "sent", locker.get().label())
                : Component.translatable(MESSAGE + "sent_short", locker.get().label(), shortList(placed.shortBy())));
        return true;
    }

    /** Why the player cannot order from the bound warehouse now, empty when they can. */
    private static String problem(Player player, Optional<WarehouseNetwork> network) {
        if (network.isEmpty()) {
            return "not_bound";
        }
        Optional<WarehouseControllerBlockEntity> controller = network.get().controller();
        if (controller.isEmpty()) {
            return "offline";
        }
        boolean sameDimension = network.get().level().dimension().equals(player.level().dimension());
        BlockPos at = player.blockPosition();
        if (!OrderRange.inRange(controller.get().span(), sameDimension, at.getX(), at.getZ(),
                ShuttlesStatsManager.INSTANCE.handheldOrderMargin())) {
            return "out_of_range";
        }
        return "";
    }

    private static Component shortList(Map<ItemKey, Integer> shortBy) {
        List<Component> parts = new ArrayList<>();
        shortBy.forEach((key, count) -> parts.add(Component.translatable(MESSAGE + "short_item", count, key.stack(1).getHoverName())));
        return ComponentUtils.formatList(parts, Component.literal(", "));
    }
}
