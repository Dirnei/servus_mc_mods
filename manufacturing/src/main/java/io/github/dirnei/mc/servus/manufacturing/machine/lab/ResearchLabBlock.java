package io.github.dirnei.mc.servus.manufacturing.machine.lab;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ResearchLabBlock extends MachineBlock {

    public static final MapCodec<ResearchLabBlock> CODEC = simpleCodec(ResearchLabBlock::new);

    public ResearchLabBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.RESEARCH_LAB.get();
    }
}
