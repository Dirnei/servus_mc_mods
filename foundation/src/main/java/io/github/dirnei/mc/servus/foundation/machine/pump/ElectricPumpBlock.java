package io.github.dirnei.mc.servus.foundation.machine.pump;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ElectricPumpBlock extends PumpBlock {

    public static final MapCodec<ElectricPumpBlock> CODEC = simpleCodec(ElectricPumpBlock::new);

    public ElectricPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.ELECTRIC_PUMP_ENTITY.get();
    }
}
