package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.ItemStack;

/**
 * The chat report of the Data Potato on a Parcel Locker, after its label:
 * free boxes per size, then each parcel with its owner, boxes, state, items
 * and the time left until it returns. Changes nothing. Server thread only.
 */
public final class ParcelLockerReport {

    private static final String KEY = "message.servus_shuttles.potato.parcel_locker.";
    private static final int TICKS_PER_MINUTE = 1200;

    private ParcelLockerReport() {
    }

    public static List<Component> of(ParcelLockerBlockEntity locker) {
        List<Component> report = new ArrayList<>();
        if (locker.network().isEmpty()) {
            report.add(Component.translatable(KEY + "offline"));
        }
        report.add(Component.translatable(KEY + "free", locker.freeCount(LockerLayout.Size.S), locker.freeCount(LockerLayout.Size.M),
                locker.freeCount(LockerLayout.Size.L)));
        long now = locker.getLevel() == null ? 0 : locker.getLevel().getGameTime();
        for (LockerOrder order : locker.orders()) {
            String boxes = String.join(", ", order.boxes().stream().map(box -> LockerLayout.BOXES.get(box).id()).toList());
            report.add(Component.translatable(KEY + "parcel", order.ownerName(), boxes, state(order, now)));
            List<Component> items = new ArrayList<>();
            for (ItemStack stack : locker.contents(order)) {
                items.add(Component.translatable(KEY + "item", stack.getCount(), stack.getHoverName()));
            }
            if (!items.isEmpty()) {
                report.add(Component.literal("  ").append(ComponentUtils.formatList(items, Component.literal(", "))));
            }
        }
        if (locker.blockedReason() != null) {
            report.add(Component.translatable(KEY + "stuck." + locker.blockedReason().name().toLowerCase(Locale.ROOT),
                    locker.blockedStack().getHoverName()));
        }
        return report;
    }

    private static Component state(LockerOrder order, long now) {
        return switch (order.state()) {
            case RESERVED -> Component.translatable(KEY + "state.reserved");
            case RETURNING -> Component.translatable(KEY + "state.returning");
            case READY -> {
                long left = LockerReturns.ticksLeft(order.readySince(), now, ShuttlesStatsManager.INSTANCE.lockerReturnTicks());
                yield Component.translatable(KEY + "state.ready", (left + TICKS_PER_MINUTE - 1) / TICKS_PER_MINUTE);
            }
        };
    }
}
