package io.github.dirnei.mc.servus.alchemy.hollow;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.HexColor;
import io.github.dirnei.mc.servus.alchemy.PrincipleCodecs;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth.Vein;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/** Loads the veins a Gnome Hollow can grow from {@code data/<namespace>/servus_alchemy/vein/<name>.json}. */
public final class VeinData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "vein_data");

    public static final String DIRECTORY = "servus_alchemy/vein";

    /**
     * A vein as data.
     *
     * @param yieldTemplate what a bound gnome digs
     * @param namedColor the RGB its material shows under Elemental Sight, if the JSON names one
     */
    public record VeinDef(ItemStackTemplate yieldTemplate, Vein growth, Optional<Integer> namedColor) {

        /** A new stack of what a bound gnome digs. */
        public ItemStack yield() {
            return yieldTemplate.create();
        }

        /** The RGB of its material under the sight: the named colour, else the yield block's map colour, else grey. */
        public int color() {
            return namedColor.orElseGet(() -> yieldTemplate.item().value() instanceof BlockItem block
                    ? block.getBlock().defaultMapColor().col
                    : DEFAULT_COLOR);
        }
    }

    /** The material colour of a vein whose yield names no colour of its own. */
    public static final int DEFAULT_COLOR = 0xB4B4B4;

    private static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            text -> HexColor.parse(text).stream().boxed().findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Not a #rrggbb colour: " + text)),
            HexColor::format);

    private static final Codec<VeinDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStackTemplate.CODEC.fieldOf("yield").forGetter(VeinDef::yieldTemplate),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("steps").forGetter(def -> def.growth().steps()),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("step_ticks").forGetter(def -> def.growth().stepTicks()),
            PrincipleCodecs.CODEC.optionalFieldOf("step_cost", Principles.NONE).forGetter(def -> def.growth().stepCost()),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("yield_ticks").forGetter(def -> def.growth().yieldTicks()),
            HEX_COLOR.optionalFieldOf("color").forGetter(VeinDef::namedColor)
    ).apply(instance, (yield, steps, stepTicks, stepCost, yieldTicks, color) ->
            new VeinDef(yield, new Vein(steps, stepTicks, stepCost, yieldTicks), color)));

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

    public Map<Identifier, VeinDef> veins() {
        return veins;
    }
}
