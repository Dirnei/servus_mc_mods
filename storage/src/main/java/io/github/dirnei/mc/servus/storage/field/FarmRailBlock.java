package io.github.dirnei.mc.servus.storage.field;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * A Warehouse Rail for fields: it is a rail in every way, and a connected run
 * of farm rails is one field that a Field Head waters and watches.
 */
public class FarmRailBlock extends RailBlock {

    public static final MapCodec<FarmRailBlock> CODEC = simpleCodec(FarmRailBlock::new);

    public FarmRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
