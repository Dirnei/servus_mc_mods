package io.github.dirnei.mc.servus.storage.rack;

import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Chest sized inventory of a rack bay. Clients only receive the sample: the
 * item types the rack holds the most of, computed on the server.
 */
public class HeavyRackBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    private static final int SYNC_INTERVAL = 10;

    private final ItemStackHandler items = new ItemStackHandler(StorageStatsManager.INSTANCE.rackSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    private List<ItemStack> displayed = List.of();
    private boolean syncPending;
    private int syncTimer;

    public HeavyRackBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.HEAVY_RACK.get(), pos, state);
    }

    @Override
    public boolean zonable() {
        return true;
    }

    /** Rails link and dock only on the front. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(getBlockState().getValue(HeavyRackBlock.FACING));
    }

    public ItemStackHandler items() {
        return items;
    }

    public boolean isEmpty() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The sample shown on the front: one item stack per displayed type, largest total first. */
    public List<ItemStack> displayed() {
        return displayed;
    }

    /** Empties every slot without dropping anything. */
    public void clear() {
        for (int i = 0; i < items.getSlots(); i++) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private void changed() {
        setChanged();
        syncPending = true;
        postDirty();
        if (level != null && !level.isClientSide()) {
            displayed = sample();
        }
    }

    private List<ItemStack> sample() {
        Map<ItemKey, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                counts.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemKey key : TopCounts.pick(counts, StorageStatsManager.INSTANCE.rackDisplayCount())) {
            result.add(key.stack(1));
        }
        return List.copyOf(result);
    }

    // Menu

    @Override
    public Component getDisplayName() {
        Component name = Component.translatable(getBlockState().getBlock().getDescriptionId());
        return zone().isEmpty() ? name : name.copy().append(" (" + zone() + ")");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new HeavyRackMenu(containerId, playerInventory, this);
    }

    // Ticking: throttle client syncs

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeavyRackBlockEntity rack) {
        if (!rack.syncPending) {
            return;
        }
        if (++rack.syncTimer < SYNC_INTERVAL) {
            return;
        }
        rack.syncTimer = 0;
        rack.syncPending = false;
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
            displayed = sample();
        }
        if (tag.contains("Display")) {
            List<ItemStack> list = new ArrayList<>();
            for (Tag entry : tag.getList("Display", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) entry);
                if (!stack.isEmpty()) {
                    list.add(stack);
                }
            }
            displayed = List.copyOf(list);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!zone().isEmpty()) {
            tag.putString("Zone", zone());
        }
        ListTag list = new ListTag();
        for (ItemStack stack : displayed) {
            list.add(stack.save(registries));
        }
        tag.put("Display", list);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
