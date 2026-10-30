package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/**
 * The aquarium GUI: the feed, filter and catch slots, the stock, capacity, water quality and power as data, the auto
 * catch switch and the keep level, and the player inventory. Settings come back as {@link AquariumSettingsPayload}.
 */
public class AquariumMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 198;
    public static final int SLOT_Y = 62;
    public static final int FEED_X = 8;
    public static final int FILTER_X = 34;
    public static final int CATCH_X = 116;
    public static final int MURKY_X = 66;
    public static final int MURKY_WIDTH = 40;
    public static final int INVENTORY_Y = 116;

    private static final int DATA_STOCK = 0;
    private static final int DATA_CAPACITY = 1;
    private static final int DATA_QUALITY = 2;
    private static final int DATA_POWERED = 3;
    private static final int DATA_KEEP = 4;
    private static final int DATA_AUTO_CATCH = 5;
    private static final int DATA_SPECIES = 6;
    private static final int DATA_MURKY = 7;
    private static final int DATA_MURKY_CAPACITY = 8;
    private static final int DATA_DYING_BELOW = 9;
    private static final int DATA_COUNT = 10;
    /** Container data travels as shorts. */
    private static final int MAX_DATA = Short.MAX_VALUE;
    /** The master sits in a corner of a possibly large aquarium, so the GUI stays open farther than a chest's. */
    private static final double REACH = 24;

    private final AquariumPartBlockEntity master;
    private final ContainerData data;

    public AquariumMenu(int containerId, Inventory inventory, AquariumPartBlockEntity master) {
        this(containerId, inventory, master, new SimpleContainerData(DATA_COUNT));
    }

    private AquariumMenu(int containerId, Inventory inventory, AquariumPartBlockEntity master, ContainerData data) {
        super(ServusDelicaciesMenus.AQUARIUM.get(), containerId);
        this.master = master;
        this.data = data;
        ItemSlots slots = master.state().slots();
        addSlot(new AquariumSlot(slots, AquariumMasterState.FEED_SLOT, FEED_X, SLOT_Y));
        addSlot(new AquariumSlot(slots, AquariumMasterState.FILTER_SLOT, FILTER_X, SLOT_Y));
        for (int i = 0; i < AquariumMasterState.SLOT_COUNT - AquariumMasterState.FIRST_CATCH_SLOT; i++) {
            addSlot(new AquariumSlot(slots, AquariumMasterState.FIRST_CATCH_SLOT + i, CATCH_X + i * 18, SLOT_Y));
        }
        PlayerInventorySlots.add(this::addSlot, inventory, 8, INVENTORY_Y);
        addDataSlots(data);
        refresh();
    }

    public static AquariumMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof AquariumPartBlockEntity master) {
            return new AquariumMenu(containerId, inventory, master);
        }
        throw new IllegalStateException("No aquarium at " + pos);
    }

    /** Opens the GUI of {@code master}'s aquarium for {@code player}. */
    public static void open(ServerPlayer player, AquariumPartBlockEntity master) {
        BlockPos pos = master.getBlockPos();
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new AquariumMenu(id, inventory, master),
                Component.translatable("gui.servus_delicacies.aquarium")), buf -> buf.writeBlockPos(pos));
    }

    /** Applies settings a client sent, when its open menu shows that aquarium. */
    public static void applySettings(Player player, AquariumSettingsPayload payload) {
        if (player.containerMenu instanceof AquariumMenu menu && menu.master.getBlockPos().equals(payload.pos())
                && menu.stillValid(player)) {
            menu.master.setCatch(payload.autoCatch(), payload.keep());
            menu.refresh();
        }
    }

    public BlockPos masterPos() {
        return master.getBlockPos();
    }

    public int stock() {
        return data.get(DATA_STOCK);
    }

    public int capacity() {
        return data.get(DATA_CAPACITY);
    }

    /** Water quality in percent. */
    public int qualityPercent() {
        return data.get(DATA_QUALITY);
    }

    public boolean powered() {
        return data.get(DATA_POWERED) == 1;
    }

    public int keep() {
        return data.get(DATA_KEEP);
    }

    public boolean autoCatch() {
        return data.get(DATA_AUTO_CATCH) == 1;
    }

    /** mB of Murky Water waiting to be drained. */
    public int murky() {
        return data.get(DATA_MURKY);
    }

    public int murkyCapacity() {
        return data.get(DATA_MURKY_CAPACITY);
    }

    /** The water quality in percent below which fish die. */
    public int dyingBelowPercent() {
        return data.get(DATA_DYING_BELOW);
    }

    public @Nullable FishSpecies species() {
        return FishSpecies.byOrdinal(data.get(DATA_SPECIES) - 1);
    }

    private void refresh() {
        if (master.getLevel() == null || master.getLevel().isClientSide()) {
            return;
        }
        AquariumMasterState state = master.state();
        data.set(DATA_STOCK, Math.min(MAX_DATA, state.wholeFish()));
        data.set(DATA_CAPACITY, Math.min(MAX_DATA, master.capacity()));
        data.set(DATA_QUALITY, (int) Math.round(master.quality() * 100));
        data.set(DATA_POWERED, master.powered() ? 1 : 0);
        data.set(DATA_KEEP, Math.min(MAX_DATA, state.keep(AquariumStatsManager.INSTANCE.stats())));
        data.set(DATA_AUTO_CATCH, state.autoCatch() ? 1 : 0);
        data.set(DATA_SPECIES, state.species() == null ? 0 : state.species().ordinal() + 1);
        data.set(DATA_MURKY, Math.min(MAX_DATA, state.murky()));
        data.set(DATA_MURKY_CAPACITY, Math.min(MAX_DATA, AquariumStatsManager.INSTANCE.stats().murkyCapacity()));
        data.set(DATA_DYING_BELOW, (int) Math.round(AquariumStatsManager.INSTANCE.stats().dyingBelow() * 100));
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    /** Shift click: aquarium slots go to the inventory, inventory items into the feed or filter slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int aquariumSlots = AquariumMasterState.SLOT_COUNT;
        if (index < aquariumSlots) {
            if (!moveItemStackTo(stack, aquariumSlots, aquariumSlots + PlayerInventorySlots.COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, AquariumMasterState.FEED_SLOT, AquariumMasterState.FIRST_CATCH_SLOT, false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return master.isMaster() && !master.isRemoved()
                && player.distanceToSqr(master.getBlockPos().getCenter()) <= REACH * REACH;
    }

    /** A slot of the aquarium: players put feed and filters in and take everything out. */
    private static final class AquariumSlot extends ResourceHandlerSlot {

        AquariumSlot(ItemSlots slots, int index, int x, int y) {
            super(slots, slots::set, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AquariumMasterState.acceptsFromOutside(getSlotIndex(), stack);
        }
    }
}
