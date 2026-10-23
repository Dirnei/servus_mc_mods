package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.shuttles.garage.GarageStrandedPayload;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * One slot per shuttle place above the player inventory. The slot of a
 * shuttle that is out on a job cannot be emptied until it is back.
 */
public class ShuttleGarageMenu extends AbstractContainerMenu {

    public static final int SLOTS_Y = 30;
    public static final int INVENTORY_Y = 96;
    public static final int BUTTON_RECALL = 1;
    public static final int BUTTON_TOGGLE = 2;
    private static final int COLUMNS = 9;

    private final ShuttleGarageBlockEntity garage;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int capacity;
    private final int inventoryStart;
    private final DataSlot out = DataSlot.standalone();
    private final DataSlot active = DataSlot.standalone();
    private final DataSlot outSlots = DataSlot.standalone();
    private final PayloadSync<GarageStrandedPayload> strandedSync = new PayloadSync<>(5, true, this::stranded,
            payload -> payload.lines().stream().map(Component::getString).toList().hashCode() * 31L + payload.slots().hashCode());
    private List<Component> strandedLines = List.of();
    private List<Integer> strandedSlots = List.of();

    public ShuttleGarageMenu(int containerId, Inventory playerInventory, ShuttleGarageBlockEntity garage) {
        super(ServusShuttlesMenus.SHUTTLE_GARAGE.get(), containerId);
        this.garage = garage;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(garage.getLevel(), garage.getBlockPos());
        this.capacity = garage.capacity();
        for (int slot = 0; slot < capacity; slot++) {
            int index = slot;
            addSlot(new ResourceHandlerSlot(garage.shuttles(), garage.shuttles()::set, slot, slotX(slot), SLOTS_Y) {
                @Override
                public boolean mayPickup(Player player) {
                    return !locked(index);
                }
            });
        }
        inventoryStart = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        addDataSlot(out);
        addDataSlot(active);
        addDataSlot(outSlots);
    }

    public static ShuttleGarageMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ShuttleGarageBlockEntity garage) {
            return new ShuttleGarageMenu(containerId, playerInventory, garage);
        }
        throw new IllegalStateException("No shuttle garage at " + pos);
    }

    /** Where a shuttle slot sits: the row is centred. */
    public int slotX(int slot) {
        return 8 + (COLUMNS - capacity) * 9 + slot * 18;
    }

    public ShuttleGarageBlockEntity garage() {
        return garage;
    }

    public int capacity() {
        return capacity;
    }

    public int inventoryStart() {
        return inventoryStart;
    }

    /** Shuttles of this garage out on a job, as last synced. */
    public int out() {
        return out.get();
    }

    /** Whether the garage hands out shuttles, as last synced. */
    public boolean isActive() {
        return active.get() != 0;
    }

    /** Recall brings every shuttle inside; toggle pauses or resumes the garage. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_RECALL && id != BUTTON_TOGGLE) {
            return false;
        }
        if (!player.level().isClientSide()) {
            if (id == BUTTON_RECALL) {
                garage.network().ifPresent(network -> network.recallGarage(garage.getBlockPos()));
            } else {
                garage.setActive(!garage.isActive());
            }
            update();
        }
        return true;
    }

    private void update() {
        out.set(garage.outCount());
        outSlots.set(garage.outSlots());
        active.set(garage.isActive() ? 1 : 0);
    }

    /** Whether the slot's shuttle is away, as last synced. */
    public boolean locked(int slot) {
        return !garage.shuttles().getStackInSlot(slot).isEmpty() && slot < ShuttleFleet.MASK_SLOTS && (outSlots.get() & (1 << slot)) != 0;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            update();
        }
        super.broadcastChanges();
        strandedSync.tick(player);
    }

    /** The garage's shuttles parked with cargo that has nowhere to go, as lines for the GUI. Server side. */
    private GarageStrandedPayload stranded() {
        List<Component> lines = new ArrayList<>();
        List<Integer> slotIndexes = new ArrayList<>();
        garage.network().ifPresent(network -> {
            for (ShuttleFleet.Stranded stranded : network.stranded(garage.getBlockPos())) {
                lines.add(Component.translatable("gui.servus_shuttles.garage.stranded." + stranded.reason().name().toLowerCase(Locale.ROOT),
                        ShuttleItem.label(stranded.shuttle()), stranded.cargo().getCount(), stranded.cargo().getHoverName()));
                slotIndexes.add(stranded.slot());
            }
        });
        return new GarageStrandedPayload(containerId, lines, slotIndexes);
    }

    /** Client side: the lines the server sent. */
    public void acceptStranded(GarageStrandedPayload payload) {
        strandedLines = List.copyOf(payload.lines());
        strandedSlots = List.copyOf(payload.slots());
    }

    /** One line per shuttle parked with cargo that has nowhere to go, as last synced. */
    public List<Component> strandedLines() {
        return strandedLines;
    }

    /** Whether the slot's shuttle parks with cargo that has nowhere to go, as last synced. */
    public boolean stranded(int slot) {
        return strandedSlots.contains(slot);
    }

    /** Shift click: a Shuttle item in the inventory goes into a free place, a garage slot goes back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < capacity) {
            if (locked(index)) {
                return ItemStack.EMPTY;
            }
            moved = moveItemStackTo(stack, inventoryStart, slots.size(), true);
        } else {
            if (!ShuttleItem.isShuttle(stack)) {
                return ItemStack.EMPTY;
            }
            moved = moveItemStackTo(stack, 0, capacity, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.SHUTTLE_GARAGE.get());
    }
}
