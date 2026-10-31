package io.github.dirnei.mc.servus.shuttles.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The corrosion a farm rail item carries: its stage (see
 * {@link io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion}) and the
 * scrape progress in durability points.
 */
public record RailCorrosion(int stage, int progress) {

    public static final Codec<RailCorrosion> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("stage").forGetter(RailCorrosion::stage),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(RailCorrosion::progress))
            .apply(i, RailCorrosion::new));

    public static final StreamCodec<ByteBuf, RailCorrosion> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RailCorrosion::stage,
            ByteBufCodecs.VAR_INT, RailCorrosion::progress,
            RailCorrosion::new);
}
