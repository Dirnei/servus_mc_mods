package io.github.dirnei.mc.servus.shuttles.handheld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What a right click with the handheld does. */
public enum HandheldMode implements StringRepresentable {
    /** Puts bays into the active zone. */
    ZONE("zone"),
    /** Scans a shelf: shows what it holds and opens the GUI on it. */
    INFO("info"),
    /** Lists the bound warehouse's stock; a bay scan switches to INFO. */
    STOCK("stock");

    /** A saved name that loads as INFO. */
    private static final String FIND_NAME = "find";

    /** Reads a mode by its name; the name "find" loads as INFO. */
    public static final Codec<HandheldMode> CODEC = Codec.STRING.comapFlatMap(HandheldMode::byName, HandheldMode::getSerializedName);
    public static final StreamCodec<ByteBuf, HandheldMode> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> values()[Math.floorMod(index, values().length)], HandheldMode::ordinal);

    private final String name;

    HandheldMode(String name) {
        this.name = name;
    }

    private static DataResult<HandheldMode> byName(String name) {
        if (FIND_NAME.equals(name)) {
            return DataResult.success(INFO);
        }
        for (HandheldMode mode : values()) {
            if (mode.name.equals(name)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unknown handheld mode: " + name);
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
