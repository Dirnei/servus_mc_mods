package io.github.dirnei.mc.servus.foundation.data.packet;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the data line limits (retry interval, grant hold time) from {@code data/<ns>/data/packets.json}. */
public final class PacketLimitsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "packet_limits_manager");

    public static final String DIRECTORY = "data";

    private static final Identifier PACKETS = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "packets");

    public static final PacketLimitsManager INSTANCE = new PacketLimitsManager();

    private volatile PacketLimits limits = PacketLimits.DEFAULTS;

    private PacketLimitsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        PacketLimits read = PacketLimits.DEFAULTS;
        if (files.get(PACKETS) instanceof JsonObject json) {
            read = new PacketLimits(json.has("retry_interval") ? json.get("retry_interval").getAsInt() : read.retryTicks(),
                    json.has("grant_hold_ticks") ? json.get("grant_hold_ticks").getAsInt() : read.grantHoldTicks());
        }
        limits = read;
        ServusFoundation.LOGGER.info("Data packets: retry every {} ticks, grants held {} ticks", read.retryTicks(), read.grantHoldTicks());
    }

    /** The limits of the loaded datapacks, the defaults before any is loaded. */
    public PacketLimits limits() {
        return limits;
    }
}
