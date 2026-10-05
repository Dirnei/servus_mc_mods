package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusManufacturingIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerExtractorBlockEntity>> BURNER_EXTRACTOR =
            BLOCK_ENTITIES.register("burner_extractor", () -> BlockEntityType.Builder
                    .of(BurnerExtractorBlockEntity::new, ServusManufacturingBlocks.BURNER_EXTRACTOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerSmelterBlockEntity>> BURNER_SMELTER =
            BLOCK_ENTITIES.register("burner_smelter", () -> BlockEntityType.Builder
                    .of(BurnerSmelterBlockEntity::new, ServusManufacturingBlocks.BURNER_SMELTER.get())
                    .build(null));

    private ServusManufacturingBlockEntities() {
    }
}
