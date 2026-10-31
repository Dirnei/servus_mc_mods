package io.github.dirnei.mc.servus.foundation.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.data.UpkeepRequest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Saves an open {@link UpkeepRequest} with an upkeep holder's block. */
public final class UpkeepRequestCodec {

    public static final String KEY = "UpkeepRequest";

    public static final Codec<UpkeepRequest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("supplier").forGetter(UpkeepRequest::supplier),
            Codec.LONG.fieldOf("ticket").forGetter(UpkeepRequest::ticket),
            Codec.INT.fieldOf("remaining").forGetter(UpkeepRequest::remaining),
            Codec.INT.fieldOf("last_count").forGetter(UpkeepRequest::lastCount),
            Codec.LONG.fieldOf("until").forGetter(UpkeepRequest::until)
    ).apply(instance, UpkeepRequest::new));

    private UpkeepRequestCodec() {
    }

    /** Writes the request, nothing when it is null. */
    public static void save(ValueOutput output, UpkeepRequest request) {
        if (request != null) {
            output.store(KEY, CODEC, request);
        }
    }

    /** The saved request, null when there is none. */
    public static UpkeepRequest load(ValueInput input) {
        return input.read(KEY, CODEC).orElse(null);
    }
}
