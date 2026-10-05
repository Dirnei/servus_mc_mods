package io.github.dirnei.mc.servus.manufacturing.machine.generator;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CoalGeneratorBlock extends MachineBlock {

    public static final MapCodec<CoalGeneratorBlock> CODEC = simpleCodec(CoalGeneratorBlock::new);

    public CoalGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.COAL_GENERATOR.get();
    }
}
