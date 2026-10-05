package io.github.dirnei.mc.servus.manufacturing.deposit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

/**
 * Loads deposit definitions from datapacks and answers what a block yields.
 */
public final class DepositManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "deposit";

    // Declared before INSTANCE: static fields initialize in order and the constructor needs it.
    private static final Gson GSON = new GsonBuilder().create();

    public static final DepositManager INSTANCE = new DepositManager();

    private Map<Block, DepositDefinition> byBlock = Map.of();
    private Map<ResourceLocation, DepositDefinition> byId = Map.of();

    private DepositManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Block, DepositDefinition> blocks = new HashMap<>();
        Map<ResourceLocation, DepositDefinition> ids = new HashMap<>();
        files.forEach((id, json) -> DepositDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusManufacturing.LOGGER.error("Invalid deposit {}: {}", id, error))
                .ifPresent(definition -> {
                    DepositDefinition previous = blocks.put(definition.block(), definition);
                    if (previous != null) {
                        ServusManufacturing.LOGGER.warn("Deposit {} replaces another definition for block {}", id, definition.block());
                    }
                    ids.put(id, definition);
                }));
        byBlock = Collections.unmodifiableMap(blocks);
        byId = Collections.unmodifiableMap(ids);
        ServusManufacturing.LOGGER.info("Loaded {} deposits", ids.size());
    }

    public Optional<DepositDefinition> forBlock(Block block) {
        return Optional.ofNullable(byBlock.get(block));
    }

    public boolean isDeposit(Block block) {
        return byBlock.containsKey(block);
    }

    public Map<ResourceLocation, DepositDefinition> all() {
        return byId;
    }
}
