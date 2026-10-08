package io.github.dirnei.mc.servus.storage.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The patterns a Pattern Storage keeps, by place, as saved with the block and
 * carried by its dropped item.
 */
public record StoredPatterns(List<Entry> entries) {

    /** One kept pattern and its place. */
    public record Entry(int slot, EncodedPattern pattern) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(Entry::slot),
                EncodedPattern.CODEC.fieldOf("pattern").forGetter(Entry::pattern)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                EncodedPattern.STREAM_CODEC, Entry::pattern,
                Entry::new);
    }

    public static final Codec<StoredPatterns> CODEC = Entry.CODEC.listOf().xmap(StoredPatterns::new, StoredPatterns::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredPatterns> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StoredPatterns::new, StoredPatterns::entries);

    public StoredPatterns {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
