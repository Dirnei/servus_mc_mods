package io.github.dirnei.mc.servus.shuttles.gantry;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * What a Fluid Gantry is for. Storage: no pipe access, its tanks are the
 * warehouse's fluid stock. Import: pipes fill it and get nothing back.
 * Export: pipes drain it and put nothing in.
 */
public enum GantryMode implements StringRepresentable {
    STORAGE("storage"), IMPORT("import"), EXPORT("export");

    public static final Codec<GantryMode> CODEC = StringRepresentable.fromEnum(GantryMode::values);
    public static final StreamCodec<ByteBuf, GantryMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(GantryMode::byId, GantryMode::ordinal);

    private final String name;

    GantryMode(String name) {
        this.name = name;
    }

    /** The mode with that ordinal, Storage for an unknown one. */
    public static GantryMode byId(int id) {
        GantryMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : STORAGE;
    }

    /** The mode after this one: Storage, Import, Export and round again. */
    public GantryMode next() {
        return byId((ordinal() + 1) % values().length);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
