package io.github.dirnei.mc.servus.brewery.crop;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Barley: a vanilla age crop like wheat, planted from Barley Seeds. */
public class BarleyBlock extends CropBlock {

    public static final MapCodec<BarleyBlock> CODEC = simpleCodec(BarleyBlock::new);

    public BarleyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ServusBreweryItems.BARLEY_SEEDS.get();
    }
}
