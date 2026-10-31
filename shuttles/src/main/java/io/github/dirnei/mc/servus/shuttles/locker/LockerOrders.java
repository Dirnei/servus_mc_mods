package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.shuttles.handheld.Basket;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldParcelPayload;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.ProductionLedger;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Orders to Parcel Lockers on the server: placing a basket as one order,
 * whether a locker can take one, and the warehouse's look at a locker's
 * orders, which turns delivered ones ready and old ones back to the racks.
 */
public final class LockerOrders {

    private static final float CHIME_PITCH = 1.5F;

    private LockerOrders() {
    }

    /**
     * What sending an order did.
     *
     * @param refusal  why nothing was ordered, empty when the order stands
     * @param orderId  the order's id, 0 when refused
     * @param ordered  how many of each item shuttles bring
     * @param shortBy  how many of each item the warehouse could not give
     */
    public record Placed(String refusal, int orderId, Map<ItemKey, Integer> ordered, Map<ItemKey, Integer> shortBy) {

        static Placed refused(String refusal, Map<ItemKey, Integer> shortBy) {
            return new Placed(refusal, 0, Map.of(), shortBy);
        }

        public boolean placed() {
            return refusal.isEmpty();
        }
    }

    /** Each basket entry cut to what the warehouse holds unlocked now; entries it lacks are left out. */
    public static Map<ItemKey, Integer> clamp(WarehouseNetwork network, Basket<ItemKey> basket) {
        Map<ItemKey, Integer> amounts = new LinkedHashMap<>();
        for (Basket.Entry<ItemKey> entry : basket.entries()) {
            int amount = (int) Math.min(entry.amount(), network.available(entry.key()));
            if (amount > 0) {
                amounts.put(entry.key(), amount);
            }
        }
        return amounts;
    }

    /** The stacks the amounts fill, each item rounded up to whole stacks. */
    public static int stacks(Map<ItemKey, Integer> amounts) {
        List<BoxAssignment.Need> needs = new ArrayList<>();
        amounts.forEach((key, amount) -> needs.add(new BoxAssignment.Need(amount, key.stack(1).getMaxStackSize())));
        return BoxAssignment.stacksNeeded(needs);
    }

    /** Whether the locker's free boxes take the basket as the warehouse could give it now. */
    public static boolean fits(WarehouseNetwork network, ParcelLockerBlockEntity locker, Basket<ItemKey> basket) {
        Map<ItemKey, Integer> amounts = clamp(network, basket);
        return !amounts.isEmpty() && BoxAssignment.assign(locker.freeBoxes(), stacks(amounts)).isPresent();
    }

    /**
     * Sends the basket as one order of the player to the locker: each entry
     * at most what the warehouse holds unlocked, in the boxes that fit it
     * best. Refused before anything is picked or locked when the warehouse
     * holds none of it, shuttles cannot reach the locker or its free boxes
     * cannot take it all.
     */
    public static Placed place(WarehouseNetwork network, ParcelLockerBlockEntity locker, UUID owner, String ownerName,
                               Basket<ItemKey> basket) {
        Map<ItemKey, Integer> amounts = clamp(network, basket);
        Map<ItemKey, Integer> shortBy = new LinkedHashMap<>();
        for (Basket.Entry<ItemKey> entry : basket.entries()) {
            int missing = entry.amount() - amounts.getOrDefault(entry.key(), 0);
            if (missing > 0) {
                shortBy.put(entry.key(), missing);
            }
        }
        if (amounts.isEmpty()) {
            return Placed.refused("out_of_stock", shortBy);
        }
        String reach = network.reachRefusal(locker.getBlockPos());
        if (!reach.isEmpty()) {
            return Placed.refused(reach, shortBy);
        }
        Optional<List<Integer>> boxes = BoxAssignment.assign(locker.freeBoxes(), stacks(amounts));
        if (boxes.isEmpty()) {
            return Placed.refused("locker_full", shortBy);
        }
        int orderId = ProductionLedger.get(network.level()).nextRequestId();
        locker.reserve(orderId, owner, ownerName, boxes.get());
        Map<ItemKey, Integer> ordered = new LinkedHashMap<>();
        amounts.forEach((key, amount) -> {
            int planned = network.pickFor(locker.getBlockPos(), key, amount, orderId);
            ordered.put(key, planned);
            if (planned < amount) {
                shortBy.merge(key, amount - planned, Integer::sum);
            }
        });
        if (ordered.values().stream().allMatch(planned -> planned <= 0)) {
            locker.cancel(orderId);
            return Placed.refused("out_of_stock", shortBy);
        }
        return new Placed("", orderId, ordered, shortBy);
    }

    /**
     * The warehouse's look at the locker's orders: one whose picks are all
     * in turns ready, with a chime and a note on the owner's Handheld, or
     * ends when nothing arrived; a ready one past its time turns back.
     *
     * @return whether a parcel turned back, so the locker is put away
     */
    public static boolean check(WarehouseNetwork network, ParcelLockerBlockEntity locker, long now) {
        boolean returning = false;
        long returnTicks = ShuttlesStatsManager.INSTANCE.lockerReturnTicks();
        for (LockerOrder order : locker.orders()) {
            switch (order.state()) {
                case RESERVED -> {
                    if (network.picksPending(order.id()) || locker.awaitsDelivery(order)) {
                        continue;
                    }
                    if (locker.holdsItems(order)) {
                        locker.markReady(order, now);
                        announce(network.level(), locker, order);
                    } else {
                        locker.cancel(order.id());
                    }
                }
                case READY -> {
                    if (LockerReturns.expired(order.state(), order.readySince(), now, returnTicks)) {
                        locker.markReturning(order);
                        returning = true;
                    }
                }
                case RETURNING -> {
                }
            }
        }
        return returning;
    }

    private static void announce(ServerLevel level, ParcelLockerBlockEntity locker, LockerOrder order) {
        level.playSound(null, locker.getBlockPos().above(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, CHIME_PITCH);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(order.owner());
        if (owner == null) {
            return;
        }
        String boxes = String.join(", ", order.boxes().stream().map(box -> LockerLayout.BOXES.get(box).id()).toList());
        owner.sendOverlayMessage(Component.translatable("message.servus_shuttles.parcel_locker.ready", locker.label(), boxes));
        PacketDistributor.sendToPlayer(owner, new HandheldParcelPayload(locker.label(), boxes));
    }
}
