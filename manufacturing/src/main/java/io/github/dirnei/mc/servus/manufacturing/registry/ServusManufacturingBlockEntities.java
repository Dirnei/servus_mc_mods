package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusManufacturingIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchLabBlockEntity>> RESEARCH_LAB =
            BLOCK_ENTITIES.register("research_lab", () -> BlockEntityType.Builder
                    .of(ResearchLabBlockEntity::new, ServusManufacturingBlocks.RESEARCH_LAB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalcinerBlockEntity>> CALCINER =
            BLOCK_ENTITIES.register("calciner", () -> BlockEntityType.Builder
                    .of(CalcinerBlockEntity::new, ServusManufacturingBlocks.CALCINER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoagulatorBlockEntity>> COAGULATOR =
            BLOCK_ENTITIES.register("coagulator", () -> BlockEntityType.Builder
                    .of(CoagulatorBlockEntity::new, ServusManufacturingBlocks.COAGULATOR.get())
                    .build(null));

    private ServusManufacturingBlockEntities() {
    }
}
