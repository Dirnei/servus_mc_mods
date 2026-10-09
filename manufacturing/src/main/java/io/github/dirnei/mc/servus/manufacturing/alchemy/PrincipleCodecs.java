package io.github.dirnei.mc.servus.manufacturing.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codecs of {@link Principles}: {@code {"salt": 100, "mercury": 10}}, missing keys are 0. */
public final class PrincipleCodecs {

    public static final Codec<Principles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("salt", 0).forGetter(Principles::salt),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("sulfur", 0).forGetter(Principles::sulfur),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("mercury", 0).forGetter(Principles::mercury)
    ).apply(instance, Principles::new));

    public static final StreamCodec<ByteBuf, Principles> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Principles::salt,
            ByteBufCodecs.VAR_INT, Principles::sulfur,
            ByteBufCodecs.VAR_INT, Principles::mercury,
            Principles::new);

    private PrincipleCodecs() {
    }
}
