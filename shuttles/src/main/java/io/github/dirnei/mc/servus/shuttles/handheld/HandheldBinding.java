package io.github.dirnei.mc.servus.shuttles.handheld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The warehouse a handheld is bound to: its controller and the warehouse
 * name when it was bound, for the panel.
 */
public record HandheldBinding(GlobalPos controller, String name) {

    public static final Codec<HandheldBinding> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("controller").forGetter(HandheldBinding::controller),
            Codec.STRING.fieldOf("name").forGetter(HandheldBinding::name))
            .apply(i, HandheldBinding::new));

    public static final StreamCodec<ByteBuf, HandheldBinding> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, HandheldBinding::controller,
            ByteBufCodecs.STRING_UTF8, HandheldBinding::name,
            HandheldBinding::new);
}
