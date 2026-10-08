package io.github.dirnei.mc.servus.lib.registry;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.data.DataCableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLibBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusLibIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataCableBlockEntity>> DATA_CABLE =
            BLOCK_ENTITIES.register("data_cable", () -> BlockEntityType.Builder
                    .of(DataCableBlockEntity::new, ServusLibBlocks.DATA_CABLE.get())
                    .build(null));

    private ServusLibBlockEntities() {
    }
}
