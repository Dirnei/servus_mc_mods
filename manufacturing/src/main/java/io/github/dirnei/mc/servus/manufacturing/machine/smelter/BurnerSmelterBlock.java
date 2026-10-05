package io.github.dirnei.mc.servus.manufacturing.machine.smelter;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BurnerSmelterBlock extends MachineBlock {

    public static final MapCodec<BurnerSmelterBlock> CODEC = simpleCodec(BurnerSmelterBlock::new);

    public BurnerSmelterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.BURNER_SMELTER.get();
    }
}
