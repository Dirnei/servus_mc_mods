package io.github.dirnei.mc.servus.alchemy.calciner;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads how much faster the eternal flame calcines from {@code data/<namespace>/servus_alchemy/eternal_fire/calciner.json},
 * one field {@code speed} from 1 to 64; 1 without the file.
 */
public final class EternalFireData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "eternal_fire_data");

    public static final String DIRECTORY = "servus_alchemy/eternal_fire";
    /** The file that holds the Calciner's speed. */
    public static final Identifier FILE = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calciner");
    private static final int DEFAULT_SPEED = 1;

    private static final Codec<Integer> CODEC = Codec.intRange(1, 64).fieldOf("speed").codec();

    public static final EternalFireData INSTANCE = new EternalFireData();

    private volatile int speed = DEFAULT_SPEED;

    private EternalFireData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(FILE);
        speed = json == null ? DEFAULT_SPEED : CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid eternal fire settings {}: {}", FILE, error))
                .orElse(DEFAULT_SPEED);
    }

    /** How many times faster a Calciner calcines over the eternal flame, at least 1. */
    public int speed() {
        return speed;
    }
}
