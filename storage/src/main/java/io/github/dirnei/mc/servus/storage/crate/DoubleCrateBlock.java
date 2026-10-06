package io.github.dirnei.mc.servus.storage.crate;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** Crate with two compartments side by side. */
public class DoubleCrateBlock extends CrateBlock {

    public static final MapCodec<DoubleCrateBlock> CODEC = simpleCodec(DoubleCrateBlock::new);

    public DoubleCrateBlock(Properties properties) {
        super(properties, 2);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
