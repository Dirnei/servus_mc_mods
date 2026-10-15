package io.github.dirnei.mc.servus.alchemy.hollow;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
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

/** Loads what gnomes like as gifts from {@code data/<namespace>/servus_alchemy/gnome_gift/<name>.json}. */
public final class GnomeGiftData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome_gift_data");

    public static final String DIRECTORY = "servus_alchemy/gnome_gift";

    /**
     * A gift as data.
     *
     * @param items         liked items
     * @param tags          liked item tags
     * @param factor        how many times faster the gnome digs, at least 1
     * @param durationTicks how long the boost lasts
     */
    public record GiftDef(List<Item> items, List<TagKey<Item>> tags, double factor, int durationTicks) {

        public boolean matches(ItemStack stack) {
            return items.contains(stack.getItem()) || tags.stream().anyMatch(stack::is);
        }
    }

    private static final Codec<GiftDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("items", List.of()).forGetter(GiftDef::items),
            TagKey.codec(Registries.ITEM).listOf().optionalFieldOf("tags", List.of()).forGetter(GiftDef::tags),
            Codec.doubleRange(1, 100).fieldOf("factor").forGetter(GiftDef::factor),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration_ticks").forGetter(GiftDef::durationTicks)
    ).apply(instance, GiftDef::new));

    public static final GnomeGiftData INSTANCE = new GnomeGiftData();

    private volatile Map<Identifier, GiftDef> gifts = Map.of();

    private GnomeGiftData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, GiftDef> loaded = new LinkedHashMap<>();
        new TreeMap<>(files).forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid gnome gift {}: {}", id, error))
                .ifPresent(def -> loaded.put(id, def)));
        gifts = Map.copyOf(loaded);
        ServusAlchemy.LOGGER.info("Loaded {} gnome gifts", loaded.size());
    }

    /** The gift {@code stack} is: the first by id that likes it, empty when gnomes do not want it. */
    public Optional<GiftDef> forItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return new TreeMap<>(gifts).values().stream().filter(def -> def.matches(stack)).findFirst();
    }

    public Map<Identifier, GiftDef> gifts() {
        return gifts;
    }
}
