package io.github.dirnei.mc.servus.foundation.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Loads machine profiles from {@code data/<ns>/machine_profiles/*.json} and finds the one of a block. */
public final class MachineProfiles extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "machine_profiles";
    public static final MachineProfiles INSTANCE = new MachineProfiles();

    private static final Gson GSON = new GsonBuilder().create();

    private volatile List<MachineProfile> profiles = List.of();
    private final Map<Block, Optional<MachineProfile>> byBlock = new ConcurrentHashMap<>();

    private MachineProfiles() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<MachineProfile> read = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                read.add(MachineProfile.parse((JsonObject) json));
            } catch (RuntimeException e) {
                ServusFoundation.LOGGER.warn("Skipping machine profile {}: {}", id, e.getMessage());
            }
        });
        profiles = List.copyOf(read);
        byBlock.clear();
        ServusFoundation.LOGGER.info("Machine profiles: {}", read.size());
    }

    /** The first profile that names the block or one of its tags. */
    public Optional<MachineProfile> forState(BlockState state) {
        if (state.isAir()) {
            return Optional.empty();
        }
        return byBlock.computeIfAbsent(state.getBlock(), block -> find(state));
    }

    private Optional<MachineProfile> find(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (MachineProfile profile : profiles) {
            for (String entry : profile.blocks()) {
                boolean match = entry.startsWith("#")
                        ? state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(entry.substring(1))))
                        : id.equals(ResourceLocation.tryParse(entry));
                if (match) {
                    return Optional.of(profile);
                }
            }
        }
        return Optional.empty();
    }
}
