package io.github.dirnei.mc.servus.foundation.save;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
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

    /**
     * Everything saved in {@code input} as one compound tag, for loading code
     * that still reads tags (store with {@code output.store(tag)}).
     */
    @SuppressWarnings("deprecation")
    public static CompoundTag whole(ValueInput input) {
        return input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
    }

    /** The registries for saving code that still writes tags with item stacks: those of the block entity's level. */
    public static HolderLookup.Provider registries(BlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) {
            throw new IllegalStateException("Saving " + blockEntity + " without a level");
        }
        return blockEntity.getLevel().registryAccess();
    }

    /** An item stack as a tag (empty compound for an empty stack), what {@code ItemStack#save} did. */
    public static CompoundTag saveStack(HolderLookup.Provider registries, ItemStack stack) {
        return (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow();
    }

    /** The item stack in a tag written by {@link #saveStack}, empty when missing or broken. */
    public static ItemStack parseStack(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result()
                .orElse(ItemStack.EMPTY);
    }

    /** A block position as a tag, what {@code NbtUtils#writeBlockPos} did. */
    public static Tag posTag(BlockPos pos) {
        return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
    }

    /** The block position under {@code key}, what {@code NbtUtils#readBlockPos} did. */
    public static java.util.Optional<BlockPos> readPos(CompoundTag tag, String key) {
        return tag.read(key, BlockPos.CODEC);
    }

    public static void putTag(ValueOutput output, String key, CompoundTag tag) {
        output.store(key, CompoundTag.CODEC, tag);
    }

    public static CompoundTag getTag(ValueInput input, String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
