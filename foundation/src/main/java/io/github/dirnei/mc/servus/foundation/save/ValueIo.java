package io.github.dirnei.mc.servus.foundation.save;

import com.mojang.logging.LogUtils;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Small helpers for the 26.1 value input and output: the old tag methods for
 * byte arrays, UUIDs and whole compound tags have no direct counterpart.
 */
public final class ValueIo {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ValueIo() {
    }

    /** Stores bytes as an int array (one int per byte). */
    public static void putBytes(ValueOutput output, String key, byte[] bytes) {
        output.putIntArray(key, toInts(bytes));
    }

    /** One int per byte, the form {@link #putBytes} stores; for update tags written by hand. */
    public static int[] toInts(byte[] bytes) {
        int[] ints = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i];
        }
        return ints;
    }

    /** Reads bytes stored by {@link #putBytes}; empty when missing. */
    public static byte[] getBytes(ValueInput input, String key) {
        int[] ints = input.getIntArray(key).orElse(new int[0]);
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    public static void putUuid(ValueOutput output, String key, UUID uuid) {
        output.store(key, UUIDUtil.CODEC, uuid);
    }

    public static UUID getUuid(ValueInput input, String key) {
        return input.read(key, UUIDUtil.CODEC).orElse(null);
    }

    /**
     * Builds a compound tag through a value output, for update tags that are
     * read back on the client by {@code loadAdditional(ValueInput)}.
     */
    public static CompoundTag write(BlockEntity blockEntity, HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            writer.accept(output);
            return output.buildResult();
        }
    }

    public static void putTag(ValueOutput output, String key, CompoundTag tag) {
        output.store(key, CompoundTag.CODEC, tag);
    }

    public static CompoundTag getTag(ValueInput input, String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
