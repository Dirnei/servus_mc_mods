package io.github.dirnei.mc.servus.shuttles.handheld;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What a right click with the handheld does. */
public enum HandheldMode implements StringRepresentable {
    /** Puts bays into the active zone. */
    ZONE("zone"),
    /** Lights up every bay holding an item. */
    FIND("find"),
    /** Shows what a bay holds. */
    INFO("info");

    public static final Codec<HandheldMode> CODEC = StringRepresentable.fromEnum(HandheldMode::values);
    public static final StreamCodec<ByteBuf, HandheldMode> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> values()[Math.floorMod(index, values().length)], HandheldMode::ordinal);

    private final String name;

    HandheldMode(String name) {
        this.name = name;
    }

    /** The mode {@code step} places further, wrapping around at both ends. */
    public HandheldMode step(int step) {
        HandheldMode[] all = values();
        return all[Math.floorMod(ordinal() + step, all.length)];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
