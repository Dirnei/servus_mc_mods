package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLocksPayload;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The lockable slots of a menu: on the server it sends their locks to the
 * player, on the client it keeps what arrived and puts the locks onto the
 * menu's client inventories, so clicks the server refuses are not predicted.
 */
public final class MenuLocks {

    private record Target(int menuSlot, LockedSlots slots, int index) {
    }

    private final List<Target> targets = new ArrayList<>();
    private final Map<Integer, List<SlotLocksPayload.Entry>> shown = new HashMap<>();
    private PayloadSync<SlotLocksPayload> sync;

    private MenuLocks() {
    }

    /** The menu slots that show a lockable inventory. */
    public static MenuLocks of(List<Slot> slots) {
        MenuLocks locks = new MenuLocks();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) instanceof ResourceHandlerSlot slot && slot.getResourceHandler() instanceof LockedSlots lockable) {
                locks.targets.add(new Target(i, lockable, slot.getSlotIndex()));
            }
        }
        return locks;
    }

    /** Every slot of a lockable inventory a screen shows without menu slots, as virtual menu slots 0 to size - 1. */
    public static MenuLocks ofInventory(LockedSlots inventory) {
        MenuLocks locks = new MenuLocks();
        for (int i = 0; i < inventory.getSlots(); i++) {
            locks.targets.add(new Target(i, inventory, i));
        }
        return locks;
    }

    /** Server side, from {@code broadcastChanges}: sends the locks when they changed. */
    public void tick(Player player, int containerId, Supplier<Optional<WarehouseNetwork>> network) {
        if (sync == null) {
            sync = new PayloadSync<>(5, true, () -> payload(containerId, network.get()), MenuLocks::signature);
        }
        sync.tick(player);
    }

    private SlotLocksPayload payload(int containerId, Optional<WarehouseNetwork> network) {
        List<SlotLocksPayload.Entry> entries = new ArrayList<>();
        for (Target target : targets) {
            for (SlotLock lock : target.slots().locksOf(target.index())) {
                Optional<Component> shuttle = network.flatMap(warehouse -> warehouse.shuttleOnJob(lock.jobId())).map(ShuttleItem::label);
                entries.add(new SlotLocksPayload.Entry(target.menuSlot(), lock.kind() == SlotLock.Kind.IN, lock.key().stack(1), lock.amount(),
                        lock.jobId(), shuttle));
            }
        }
        return new SlotLocksPayload(containerId, entries);
    }

    private static long signature(SlotLocksPayload payload) {
        long hash = payload.containerId();
        for (SlotLocksPayload.Entry entry : payload.entries()) {
            hash = hash * 31 + Objects.hash(entry.slot(), entry.in(), entry.amount(), entry.jobId(), ItemStack.hashItemAndComponents(entry.item()),
                    entry.shuttle().map(Component::getString).orElse(""));
        }
        return hash;
    }

    /** Client side: takes the locks the server sent. */
    public void accept(List<SlotLocksPayload.Entry> entries) {
        shown.clear();
        Map<Integer, Target> byMenuSlot = new HashMap<>();
        Map<LockedSlots, List<LockedSlots.Locked>> perInventory = new LinkedHashMap<>();
        for (Target target : targets) {
            byMenuSlot.put(target.menuSlot(), target);
            perInventory.putIfAbsent(target.slots(), new ArrayList<>());
        }
        for (SlotLocksPayload.Entry entry : entries) {
            Target target = byMenuSlot.get(entry.slot());
            if (target == null) {
                continue;
            }
            shown.computeIfAbsent(entry.slot(), slot -> new ArrayList<>()).add(entry);
            SlotLock lock = new SlotLock(entry.in() ? SlotLock.Kind.IN : SlotLock.Kind.OUT, ItemKey.of(entry.item()), entry.amount(), entry.jobId());
            perInventory.get(target.slots()).add(new LockedSlots.Locked(target.index(), lock));
        }
        perInventory.forEach(LockedSlots::copyLocks);
    }

    /** The locks shown on a menu slot, empty when it is free. */
    public List<SlotLocksPayload.Entry> shownAt(int menuSlot) {
        return shown.getOrDefault(menuSlot, List.of());
    }
}
