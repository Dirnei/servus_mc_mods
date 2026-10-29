package io.github.dirnei.mc.servus.foundation.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;

/**
 * What makes an item a filter: its cleaning {@code points} and the {@code spent} item a worn out filter turns into.
 * Read from the {@link FilterDataMaps#FILTER} data map, for example {@code {"points": 512, "spent": "servus_foundation:spent_filter"}}.
 */
public record FilterSpec(int points, Item spent) {

    public static final Codec<FilterSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("points").forGetter(FilterSpec::points),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("spent").forGetter(FilterSpec::spent)
    ).apply(instance, FilterSpec::new));
}
