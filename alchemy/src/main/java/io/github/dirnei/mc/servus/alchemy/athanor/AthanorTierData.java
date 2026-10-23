package io.github.dirnei.mc.servus.alchemy.athanor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.HexColor;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.TierDef;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Athanor tiers from {@code data/<namespace>/servus_alchemy/athanor_tier/<name>.json}. */
public final class AthanorTierData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor_tier_data");

    public static final String DIRECTORY = "servus_alchemy/athanor_tier";

    /** The block state properties a block must have, by name, each one value or a list of accepted values; optional in JSON. */
    private static final MapCodec<Map<String, List<String>>> PROPERTIES = Codec.unboundedMap(Codec.STRING,
            ExtraCodecs.compactListCodec(Codec.STRING, ExtraCodecs.nonEmptyList(Codec.STRING.listOf())))
            .optionalFieldOf("properties", Map.of());

    /** A block id and its properties, at offset 0: {@code {"block": ..., "properties": {...}}}. */
    public static final Codec<BlockAt> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("block").forGetter(BlockAt::block),
            PROPERTIES.forGetter(BlockAt::properties)
    ).apply(instance, (block, properties) -> new BlockAt(0, 0, 0, block, properties)));

    /** The three numbers of an offset from the Athanor. */
    public static final MapCodec<List<Integer>> OFFSET = Codec.INT.listOf(3, 3).fieldOf("offset");

    private static final Codec<BlockAt> BLOCK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OFFSET.forGetter(block -> List.of(block.dx(), block.dy(), block.dz())),
            Codec.STRING.fieldOf("block").forGetter(BlockAt::block),
            PROPERTIES.forGetter(BlockAt::properties)
    ).apply(instance, (offset, block, properties) -> new BlockAt(offset.get(0), offset.get(1), offset.get(2), block, properties)));

    /** A tier as written in JSON; the name comes from the file. */
    private record TierJson(int tier, String color, List<BlockAt> blocks) {

        static final Codec<TierJson> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 3).fieldOf("tier").forGetter(TierJson::tier),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(TierJson::color),
                BLOCK_CODEC.listOf().fieldOf("blocks").forGetter(TierJson::blocks)
        ).apply(instance, TierJson::new));
    }

    public static final AthanorTierData INSTANCE = new AthanorTierData();

    /** The tiers Alchemy ships, by file name; the journal draws a schematic for each. */
    public static final List<String> SHIPPED = List.of("albedo", "citrinitas", "rubedo");

    /** Reads a shipped tier straight from the mod's own resources, for the data generator. */
    public static TierDef readShipped(String name) {
        String path = "/data/servus_alchemy/" + DIRECTORY + "/" + name + ".json";
        try (var stream = AthanorTierData.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shipped Athanor tier " + path);
            }
            JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            TierJson tier = TierJson.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            return new TierDef(name, tier.tier(), parseColor(tier.color()), tier.blocks());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read shipped Athanor tier " + path, e);
        }
    }

    private volatile List<TierDef> tiers = List.of();

    private AthanorTierData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<TierDef> loaded = files.entrySet().stream()
                .flatMap(entry -> TierJson.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid Athanor tier {}: {}", entry.getKey(), error))
                        .map(json -> new TierDef(entry.getKey().getPath(), json.tier(), parseColor(json.color()), json.blocks()))
                        .stream())
                .sorted(Comparator.comparingInt(TierDef::number))
                .toList();
        tiers = loaded;
        ServusAlchemy.LOGGER.info("Loaded {} Athanor tiers", loaded.size());
    }

    static int parseColor(String color) {
        return HexColor.parse(color).orElse(0xFFFFFF);
    }

    public List<TierDef> tiers() {
        return tiers;
    }
}
