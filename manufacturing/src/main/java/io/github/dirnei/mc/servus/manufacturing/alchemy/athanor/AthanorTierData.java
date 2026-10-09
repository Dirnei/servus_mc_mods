package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.TierDef;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Athanor tiers from {@code data/<namespace>/servus_manufacturing/athanor_tier/<name>.json}. */
public final class AthanorTierData extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "servus_manufacturing/athanor_tier";

    private static final Codec<BlockAt> BLOCK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf(3, 3).fieldOf("offset").forGetter(block -> List.of(block.dx(), block.dy(), block.dz())),
            Codec.STRING.fieldOf("block").forGetter(BlockAt::block)
    ).apply(instance, (offset, block) -> new BlockAt(offset.get(0), offset.get(1), offset.get(2), block)));

    /** A tier as written in JSON; the name comes from the file. */
    private record TierJson(int tier, String color, List<BlockAt> blocks) {

        static final Codec<TierJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 3).fieldOf("tier").forGetter(TierJson::tier),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(TierJson::color),
                BLOCK_CODEC.listOf().fieldOf("blocks").forGetter(TierJson::blocks)
        ).apply(instance, TierJson::new));
    }

    private static final Gson GSON = new GsonBuilder().create();

    public static final AthanorTierData INSTANCE = new AthanorTierData();

    private volatile List<TierDef> tiers = List.of();

    private AthanorTierData() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<TierDef> loaded = files.entrySet().stream()
                .flatMap(entry -> TierJson.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> ServusManufacturing.LOGGER.error("Invalid Athanor tier {}: {}", entry.getKey(), error))
                        .map(json -> new TierDef(entry.getKey().getPath(), json.tier(), parseColor(json.color()), json.blocks()))
                        .stream())
                .sorted(Comparator.comparingInt(TierDef::number))
                .toList();
        tiers = loaded;
        ServusManufacturing.LOGGER.info("Loaded {} Athanor tiers", loaded.size());
    }

    private static int parseColor(String color) {
        try {
            return Integer.parseInt(color.startsWith("#") ? color.substring(1) : color, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public List<TierDef> tiers() {
        return tiers;
    }
}
