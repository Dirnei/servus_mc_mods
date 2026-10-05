package io.github.dirnei.mc.servus.manufacturing.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A tech as written in {@code data/<ns>/servus_manufacturing/tech/<name>.json}.
 *
 * @param icon         stack shown in the tree
 * @param parents      techs that must be completed first
 * @param cost         packs to consume
 * @param ticksPerPack lab time per pack
 * @param unlocks      recipe ids this tech unlocks
 */
public record TechDefinition(ItemStack icon, List<ResourceLocation> parents, List<ItemStack> cost,
                             int ticksPerPack, List<ResourceLocation> unlocks) {

    public static final int DEFAULT_TICKS_PER_PACK = 100;

    public static final Codec<TechDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("icon").forGetter(TechDefinition::icon),
            ResourceLocation.CODEC.listOf().optionalFieldOf("parents", List.of()).forGetter(TechDefinition::parents),
            ItemStack.CODEC.listOf().fieldOf("cost").forGetter(TechDefinition::cost),
            Codec.INT.optionalFieldOf("ticks_per_pack", DEFAULT_TICKS_PER_PACK).forGetter(TechDefinition::ticksPerPack),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(TechDefinition::unlocks)
    ).apply(instance, TechDefinition::new));
}
