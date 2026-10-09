package io.github.dirnei.mc.servus.foundation.data.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the data line limits (the retry interval) from {@code data/<ns>/data/packets.json}. */
public final class PacketLimitsManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "data";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation PACKETS = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "packets");

    public static final PacketLimitsManager INSTANCE = new PacketLimitsManager();

    private volatile PacketLimits limits = PacketLimits.DEFAULTS;

    private PacketLimitsManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        PacketLimits read = PacketLimits.DEFAULTS;
        if (files.get(PACKETS) instanceof JsonObject json) {
            read = new PacketLimits(json.has("retry_interval") ? json.get("retry_interval").getAsInt() : read.retryTicks());
        }
        limits = read;
        ServusFoundation.LOGGER.info("Data packets: retry every {} ticks", read.retryTicks());
    }

    public PacketLimits limits() {
        return limits;
    }

    /** For tests: sets the limits as if a datapack had been loaded. */
    public void set(PacketLimits limits) {
        this.limits = limits;
    }
}
