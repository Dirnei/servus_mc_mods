package io.github.dirnei.mc.servus.alchemy.prelum;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The Prelum, the screw press of the Rubedo stage; it is lit while it presses. */
public class PrelumBlock extends MachineBlock {

    public static final MapCodec<PrelumBlock> CODEC = simpleCodec(PrelumBlock::new);

    /** Height of the press plate's bottom at rest, in pixels; the item model and the renderer share it. */
    public static final float PLATE_REST = 7.05F;
    /** Height of the press plate's bottom when it presses, in pixels: a twentieth of a pixel above the bed. */
    public static final float PLATE_DOWN = 4.05F;

    public PrelumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusAlchemyBlockEntities.PRELUM.get();
    }
}
