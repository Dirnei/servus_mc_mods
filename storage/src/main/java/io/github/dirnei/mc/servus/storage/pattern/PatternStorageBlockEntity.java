package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.world.level.block.Block;

/**
 * Keeps patterns as data, {@code patterns_per_block} places of them. A link
 * member of the data line; the Production Terminal reads and writes its
 * places. Patterns kept beyond a smaller configured size stay kept. The
 * patterns travel with the dropped block item.
 */
public class PatternStorageBlockEntity extends BlockEntity implements WoodHolder, DataLink {

    private final WoodSlot woodSlot = new WoodSlot(this);

    public static final int MAX_SLOTS = 27;

    private EncodedPattern[] patterns = new EncodedPattern[slots()];

    public PatternStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PATTERN_STORAGE.get(), pos, state);
    }

    /** The configured number of pattern places. */
    public static int slots() {
        return Math.max(1, Math.min(MAX_SLOTS, StorageStatsManager.INSTANCE.patternsPerBlock()));
    }

    /** How many places exist, the configured ones plus any kept beyond them. */
    public int size() {
        return Math.max(slots(), patterns.length);
    }

    /** How many places new patterns may use. */
    public int capacity() {
        return slots();
    }

    /** The pattern at a place, if any. */
    public Optional<EncodedPattern> pattern(int slot) {
        return slot >= 0 && slot < patterns.length ? Optional.ofNullable(patterns[slot]) : Optional.empty();
    }

    /** Every place holding a pattern, in order. */
    public List<Integer> usedSlots() {
        List<Integer> used = new ArrayList<>();
        for (int slot = 0; slot < patterns.length; slot++) {
            if (patterns[slot] != null) {
                used.add(slot);
            }
        }
        return used;
    }

    /** The first configured place without a pattern, or -1. */
    public int firstFree() {
        for (int slot = 0; slot < capacity(); slot++) {
            if (pattern(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Puts a pattern at a place, replacing what is there. */
    public void set(int slot, EncodedPattern pattern) {
        grow(slot + 1);
        patterns[slot] = pattern;
        setChanged();
        updateLights();
    }

    public void clear(int slot) {
        if (slot >= 0 && slot < patterns.length) {
            patterns[slot] = null;
            setChanged();
            updateLights();
        }
    }

    /** Sets the front lights to the stored patterns; only a changed count is written. */
    public void updateLights() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(PatternStorageBlock.LIGHTS)) {
            return;
        }
        int lit = PatternLights.lit(usedSlots().size(), capacity());
        if (state.getValue(PatternStorageBlock.LIGHTS) != lit) {
            level.setBlock(worldPosition, state.setValue(PatternStorageBlock.LIGHTS, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void grow(int length) {
        if (patterns.length < length) {
            patterns = Arrays.copyOf(patterns, length);
        }
    }

    private StoredPatterns stored() {
        List<StoredPatterns.Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < patterns.length; slot++) {
            if (patterns[slot] != null) {
                entries.add(new StoredPatterns.Entry(slot, patterns[slot]));
            }
        }
        return new StoredPatterns(entries);
    }

    private void restore(StoredPatterns stored) {
        patterns = new EncodedPattern[slots()];
        if (stored == null) {
            return;
        }
        for (StoredPatterns.Entry entry : stored.entries()) {
            if (entry.slot() >= 0 && entry.slot() < MAX_SLOTS) {
                grow(entry.slot() + 1);
                patterns[entry.slot()] = entry.pattern();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        woodSlot.save(tag);
        StoredPatterns.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stored())
                .ifSuccess(saved -> tag.put("Patterns", saved));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        woodSlot.load(tag);
        StoredPatterns saved = tag.contains("Patterns")
                ? StoredPatterns.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Patterns"))
                        .result().orElse(null)
                : null;
        restore(saved);
    }

    // Item components: the patterns travel with the dropped block

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
        restore(input.get(ServusStorageComponents.PATTERNS.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
        StoredPatterns stored = stored();
        if (!stored.isEmpty()) {
            builder.set(ServusStorageComponents.PATTERNS.get(), stored);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        WoodSlot.removeFromTag(tag);
        tag.remove("Patterns");
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }
}
