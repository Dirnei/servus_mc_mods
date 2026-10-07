package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Sink GUI: one trash slot that voids whatever is put in, at once and
 * without a rate limit, plus the chimney height and rates. Always belongs to
 * the controller of a formed Sink.
 */
public class SinkMenu extends AbstractContainerMenu {

    public static final int TRASH_X = 152;
    public static final int TRASH_Y = 35;
    private static final int TRASH_SLOTS = 1;

    private final SinkBlockEntity sink;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public SinkMenu(int containerId, Inventory playerInventory, SinkBlockEntity sink) {
        this(containerId, playerInventory, sink, sink.containerData());
    }

    private SinkMenu(int containerId, Inventory playerInventory, SinkBlockEntity sink, ContainerData data) {
        super(ServusManufacturingMenus.SINK.get(), containerId);
        this.sink = sink;
        this.data = data;
        this.access = ContainerLevelAccess.create(sink.getLevel(), sink.getBlockPos());
        addSlot(new TrashSlot(new TrashContainer(sink), TRASH_X, TRASH_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public static SinkMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof SinkBlockEntity sink) {
            return new SinkMenu(containerId, playerInventory, sink, new SimpleContainerData(SinkBlockEntity.DATA_COUNT));
        }
        throw new IllegalStateException("No Sink at " + pos);
    }

    public SinkBlockEntity sink() {
        return sink;
    }

    public int chimneyHeight() {
        return data.get(SinkBlockEntity.DATA_HEIGHT);
    }

    /** Items per second, or -1 for Unlimited. */
    public int itemRate() {
        return data.get(SinkBlockEntity.DATA_ITEMS);
    }

    /** mB per tick, or -1 for Unlimited. */
    public int fluidRate() {
        return data.get(SinkBlockEntity.DATA_FLUID);
    }

    /** FE per tick, or -1 for Unlimited. */
    public int energyRate() {
        return data.get(SinkBlockEntity.DATA_ENERGY);
    }

    /** Valid while the player is near and the Sink is still formed. */
    @Override
    public boolean stillValid(Player player) {
        return SinkBlock.isFormed(sink.getBlockState()) && stillValid(access, player, sink.getBlockState().getBlock());
    }

    /** Shift click from the inventory throws the whole stack away; the trash slot never holds anything. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < TRASH_SLOTS) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        slots.get(0).set(slot.getItem().copy());
        slot.setByPlayer(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    /** A one slot container that destroys every stack set into it. */
    private static final class TrashContainer extends SimpleContainer {

        private final SinkBlockEntity sink;

        TrashContainer(SinkBlockEntity sink) {
            super(1);
            this.sink = sink;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty()) {
                sink.markVoided();
            }
            super.setItem(slot, ItemStack.EMPTY);
        }
    }

    /** The trash slot: takes any stack and never gives anything back. */
    private static final class TrashSlot extends Slot {

        TrashSlot(SimpleContainer container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
