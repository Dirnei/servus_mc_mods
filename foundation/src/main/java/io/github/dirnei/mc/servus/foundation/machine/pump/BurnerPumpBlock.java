package io.github.dirnei.mc.servus.foundation.machine.pump;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BurnerPumpBlock extends PumpBlock {

    public static final MapCodec<BurnerPumpBlock> CODEC = simpleCodec(BurnerPumpBlock::new);

    public BurnerPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.BURNER_PUMP_ENTITY.get();
    }
}
