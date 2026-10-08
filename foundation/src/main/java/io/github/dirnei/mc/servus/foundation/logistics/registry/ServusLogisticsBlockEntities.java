package io.github.dirnei.mc.servus.foundation.logistics.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusFoundationIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UtilityDuctBlockEntity>> UTILITY_DUCT =
            BLOCK_ENTITIES.register("utility_duct", () -> BlockEntityType.Builder
                    .of(UtilityDuctBlockEntity::new, ServusLogisticsBlocks.UTILITY_DUCT.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register("fluid_tank", () -> BlockEntityType.Builder
                    .of(FluidTankBlockEntity::new, ServusLogisticsBlocks.FLUID_TANK.get())
                    .build(null));

    private ServusLogisticsBlockEntities() {
    }
}
