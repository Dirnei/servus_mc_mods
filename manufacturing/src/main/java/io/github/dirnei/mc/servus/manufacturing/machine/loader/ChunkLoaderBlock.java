package io.github.dirnei.mc.servus.manufacturing.machine.loader;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ChunkLoaderBlock extends MachineBlock {

    public static final MapCodec<ChunkLoaderBlock> CODEC = simpleCodec(ChunkLoaderBlock::new);

    public ChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.CHUNK_LOADER.get();
    }
}
