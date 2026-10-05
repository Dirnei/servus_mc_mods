package io.github.dirnei.mc.servus.logistics.registry;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusLogisticsIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UtilityDuctBlockEntity>> UTILITY_DUCT =
            BLOCK_ENTITIES.register("utility_duct", () -> BlockEntityType.Builder
                    .of(UtilityDuctBlockEntity::new, ServusLogisticsBlocks.UTILITY_DUCT.get())
                    .build(null));

    private ServusLogisticsBlockEntities() {
    }
}
