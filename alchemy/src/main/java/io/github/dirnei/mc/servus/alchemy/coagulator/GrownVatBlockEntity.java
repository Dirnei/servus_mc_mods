package io.github.dirnei.mc.servus.alchemy.coagulator;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A Principle Vat with a grown crystal; it holds nothing and only gives the cluster its renderer. */
public class GrownVatBlockEntity extends BlockEntity {

    public GrownVatBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.GROWN_VAT.get(), pos, state);
    }

    public VatCrystal crystal() {
        BlockState state = getBlockState();
        return state.hasProperty(VatBlock.CRYSTAL) ? state.getValue(VatBlock.CRYSTAL) : VatCrystal.NONE;
    }
}
