package io.github.dirnei.mc.servus.delicacies.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A Delicacies crop: a vanilla age crop on farmland that grows, takes bone
 * meal and drops by its loot table. It only adds which item plants it.
 */
public class DelicaciesCropBlock extends CropBlock {

    public static final MapCodec<DelicaciesCropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("planting_item").forGetter(block -> block.getBaseSeedId().asItem()),
                    propertiesCodec())
            .apply(instance, (item, properties) -> new DelicaciesCropBlock(() -> item, properties)));

    private final Supplier<? extends ItemLike> plantingItem;

    /** {@code plantingItem} is read lazily, so the item may be registered after the block. */
    public DelicaciesCropBlock(Supplier<? extends ItemLike> plantingItem, BlockBehaviour.Properties properties) {
        super(properties);
        this.plantingItem = plantingItem;
    }

    @Override
    public MapCodec<DelicaciesCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return plantingItem.get();
    }
}
