package io.github.dirnei.mc.servus.alchemy.hint;

import com.google.gson.JsonElement;
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

/** Loads the old gnome's settings from {@code data/<namespace>/servus_alchemy/old_gnome/old_gnome.json}. */
public final class OldGnomeData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "old_gnome_data");

    public static final String DIRECTORY = "servus_alchemy/old_gnome";
    /** The file that holds the settings. */
    public static final Identifier FILE = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "old_gnome");

    public static final OldGnomeData INSTANCE = new OldGnomeData();

    private volatile OldGnomeSettings settings = OldGnomeSettings.DEFAULTS;

    private OldGnomeData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(FILE);
        settings = json == null ? OldGnomeSettings.DEFAULTS : OldGnomeSettings.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid old gnome settings {}: {}", FILE, error))
                .orElse(OldGnomeSettings.DEFAULTS);
    }

    /** The loaded settings, the defaults without a file. */
    public OldGnomeSettings settings() {
        return settings;
    }
}
