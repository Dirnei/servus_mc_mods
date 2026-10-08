package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.PatternStorageMenu;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds encoded Patterns, {@code patterns_per_block} of them. A link member of
 * the data line; the Production Terminal reads and writes its slots. Patterns
 * saved beyond a smaller configured size stay saved and drop with the block.
 */
public class PatternStorageBlockEntity extends BlockEntity implements DataLink, MenuProvider {

    public static final int MAX_SLOTS = 27;

    private ItemStackHandler patterns = handler(slots());

    public PatternStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PATTERN_STORAGE.get(), pos, state);
    }

    /** The configured number of pattern slots. */
    public static int slots() {
        return Math.max(1, Math.min(MAX_SLOTS, StorageStatsManager.INSTANCE.patternsPerBlock()));
    }

    private ItemStackHandler handler(int size) {
        return new ItemStackHandler(size) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return PatternItem.encoded(stack).isPresent();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    /** All slots, the configured ones first. */
    public ItemStackHandler patterns() {
        return patterns;
    }

    /** How many slots the menu shows and new patterns may use. */
    public int capacity() {
        return Math.min(slots(), patterns.getSlots());
    }

    /** The pattern in a slot, if any. */
    public Optional<EncodedPattern> pattern(int slot) {
        return slot >= 0 && slot < patterns.getSlots() ? PatternItem.encoded(patterns.getStackInSlot(slot)) : Optional.empty();
    }

    /** Every slot holding a pattern, in order. */
    public List<Integer> usedSlots() {
        List<Integer> used = new ArrayList<>();
        for (int slot = 0; slot < patterns.getSlots(); slot++) {
            if (pattern(slot).isPresent()) {
                used.add(slot);
            }
        }
        return used;
    }

    /** The first configured slot without a pattern, or -1. */
    public int firstFree() {
        for (int slot = 0; slot < capacity(); slot++) {
            if (patterns.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Puts a pattern into a slot, replacing what is there. */
    public void set(int slot, EncodedPattern pattern) {
        patterns.setStackInSlot(slot, PatternItem.of(pattern));
    }

    public void clear(int slot) {
        patterns.setStackInSlot(slot, ItemStack.EMPTY);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < patterns.getSlots(); slot++) {
            ItemStack stack = patterns.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                patterns.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Patterns", patterns.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag saved = tag.getCompound("Patterns");
        int savedSize = saved.contains("Size") ? saved.getInt("Size") : 0;
        patterns = handler(Math.max(slots(), savedSize));
        patterns.deserializeNBT(registries, saved);
        if (patterns.getSlots() < slots()) {
            ItemStackHandler grown = handler(slots());
            for (int slot = 0; slot < patterns.getSlots(); slot++) {
                grown.setStackInSlot(slot, patterns.getStackInSlot(slot));
            }
            patterns = grown;
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PatternStorageMenu(containerId, playerInventory, this);
    }
}
