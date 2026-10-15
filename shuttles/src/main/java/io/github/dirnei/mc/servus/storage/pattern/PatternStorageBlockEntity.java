package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.storage.production.DataLineBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Keeps patterns as data, {@code patterns_per_block} places of them. A link
 * member of the data line; the Production Terminal reads and writes its
 * places. Patterns kept beyond a smaller configured size stay kept. The
 * patterns travel with the dropped block item.
 */
public class PatternStorageBlockEntity extends DataLineBlockEntity implements DataLink {

    public static final int MAX_SLOTS = 27;

    private EncodedPattern[] patterns = new EncodedPattern[slots()];

    /** Whether the kept patterns carry the result their recipe really makes; cleared by every restore. */
    private boolean refreshed;

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
        refresh();
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
        patterns[slot] = level instanceof ServerLevel server ? pattern.assembled(server) : pattern;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, PatternStorageBlockEntity storage) {
        storage.tickOnlineLamp((ServerLevel) level, state);
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

    /**
     * Patterns kept from before get the result their recipe really makes from
     * the template, once per restore and only on a server level: loading has no
     * level yet, and a placed item applies its patterns after the block loaded.
     */
    private void refresh() {
        if (refreshed || !(level instanceof ServerLevel server)) {
            return;
        }
        refreshed = true;
        boolean changed = false;
        for (int slot = 0; slot < patterns.length; slot++) {
            if (patterns[slot] != null) {
                EncodedPattern real = patterns[slot].assembled(server);
                if (real != patterns[slot]) {
                    patterns[slot] = real;
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
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
        refreshed = false;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        StoredPatterns.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stored())
                .ifSuccess(saved -> tag.put("Patterns", saved));
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        StoredPatterns saved = tag.contains("Patterns")
                ? StoredPatterns.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Patterns"))
                        .result().orElse(null)
                : null;
        restore(saved);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        restore(input.get(ServusStorageComponents.PATTERNS.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        StoredPatterns stored = stored();
        if (!stored.isEmpty()) {
            builder.set(ServusStorageComponents.PATTERNS.get(), stored);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Patterns");
    }
}
