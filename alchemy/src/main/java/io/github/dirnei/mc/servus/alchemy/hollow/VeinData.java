package io.github.dirnei.mc.servus.alchemy.hollow;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.PrincipleCodecs;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Vein;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/** Loads the veins a Gnome Hollow can grow from {@code data/<namespace>/servus_manufacturing/vein/<name>.json}. */
public final class VeinData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "vein_data");

    public static final String DIRECTORY = "servus_manufacturing/vein";

    /**
     * A vein as data.
     *
     * @param samples the item tags whose items seed this vein
     * @param yieldTemplate what a bound gnome digs
     * @param chapter the journal chapter a team must have finished to seed it, if any
     */
    public record VeinDef(List<TagKey<Item>> samples, ItemStackTemplate yieldTemplate, Optional<Identifier> chapter, Vein growth) {

        /** A new stack of what a bound gnome digs. */
        public ItemStack yield() {
            return yieldTemplate.create();
        }

        public boolean matches(ItemStack stack) {
            return samples.stream().anyMatch(stack::is);
        }
    }

    private static final Codec<VeinDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.ITEM).listOf().fieldOf("samples").forGetter(VeinDef::samples),
            ItemStackTemplate.CODEC.fieldOf("yield").forGetter(VeinDef::yieldTemplate),
            Identifier.CODEC.optionalFieldOf("chapter").forGetter(VeinDef::chapter),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("steps").forGetter(def -> def.growth().steps()),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("step_ticks").forGetter(def -> def.growth().stepTicks()),
            PrincipleCodecs.CODEC.optionalFieldOf("step_cost", Principles.NONE).forGetter(def -> def.growth().stepCost()),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("yield_ticks").forGetter(def -> def.growth().yieldTicks())
    ).apply(instance, (samples, yield, chapter, steps, stepTicks, stepCost, yieldTicks) ->
            new VeinDef(samples, yield, chapter, new Vein(steps, stepTicks, stepCost, yieldTicks))));

    public static final VeinData INSTANCE = new VeinData();

    private volatile Map<Identifier, VeinDef> veins = Map.of();

    private VeinData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, VeinDef> loaded = new LinkedHashMap<>();
        new TreeMap<>(files).forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid vein {}: {}", id, error))
                .ifPresent(def -> loaded.put(id, def)));
        veins = Map.copyOf(loaded);
        ServusAlchemy.LOGGER.info("Loaded {} veins", loaded.size());
    }

    public Optional<VeinDef> get(Identifier id) {
        return Optional.ofNullable(veins.get(id));
    }

    /** The vein a sample seeds: the first by id whose sample tags hold the item. */
    public Optional<Map.Entry<Identifier, VeinDef>> forSample(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return new TreeMap<>(veins).entrySet().stream().filter(entry -> entry.getValue().matches(stack)).findFirst();
    }

    public Map<Identifier, VeinDef> veins() {
        return veins;
    }
}
