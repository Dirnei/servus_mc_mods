package io.github.dirnei.mc.servus.storage.crate;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** Crate with four compartments in a two by two grid. */
public class QuadCrateBlock extends CrateBlock {

    public static final MapCodec<QuadCrateBlock> CODEC = simpleCodec(QuadCrateBlock::new);

    public QuadCrateBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
