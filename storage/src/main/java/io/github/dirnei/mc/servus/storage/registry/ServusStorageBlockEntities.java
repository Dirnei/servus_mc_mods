package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder
                    .of(CrateBlockEntity::new, ServusStorageBlocks.CRATE.get())
                    .build(null));

    private ServusStorageBlockEntities() {
    }
}
