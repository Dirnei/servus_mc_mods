package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.TierDef;
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

/** Loads the Athanor tiers from {@code data/<namespace>/servus_manufacturing/athanor_tier/<name>.json}. */
public final class AthanorTierData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("servus_manufacturing", "athanor_tier_data");

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

    public static final AthanorTierData INSTANCE = new AthanorTierData();

    /** The tiers Manufacturing ships, by file name; the journal draws a schematic for each. */
    public static final List<String> SHIPPED = List.of("albedo");

    /** Reads a shipped tier straight from the mod's own resources, for the data generator. */
    public static TierDef readShipped(String name) {
        String path = "/data/servus_manufacturing/" + DIRECTORY + "/" + name + ".json";
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
