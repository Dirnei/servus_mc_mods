package io.github.dirnei.mc.servus.storage.cpu;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * A CPU Buffer block: joins a Crafting CPU box like a CPU block and adds
 * buffer room instead of order slots. Shares the CPU's block entity type.
 */
public class CpuBufferBlock extends CpuBlock {

    public static final MapCodec<CpuBufferBlock> CODEC = simpleCodec(CpuBufferBlock::new);

    public CpuBufferBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
