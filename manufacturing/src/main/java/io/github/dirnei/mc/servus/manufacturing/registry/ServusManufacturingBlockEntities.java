package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.crusher.CrusherBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.loader.ChunkLoaderBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.ElectricExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.ElectricPumpBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
            BLOCK_ENTITIES.register("coal_generator", () -> BlockEntityType.Builder
                    .of(CoalGeneratorBlockEntity::new, ServusManufacturingBlocks.COAL_GENERATOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
            BLOCK_ENTITIES.register("assembler", () -> BlockEntityType.Builder
                    .of(AssemblerBlockEntity::new, ServusManufacturingBlocks.ASSEMBLER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricExtractorBlockEntity>> ELECTRIC_EXTRACTOR =
            BLOCK_ENTITIES.register("electric_extractor", () -> BlockEntityType.Builder
                    .of(ElectricExtractorBlockEntity::new, ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () -> BlockEntityType.Builder
                    .of(CrusherBlockEntity::new, ServusManufacturingBlocks.CRUSHER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER =
            BLOCK_ENTITIES.register("chunk_loader", () -> BlockEntityType.Builder
                    .of(ChunkLoaderBlockEntity::new, ServusManufacturingBlocks.CHUNK_LOADER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchLabBlockEntity>> RESEARCH_LAB =
            BLOCK_ENTITIES.register("research_lab", () -> BlockEntityType.Builder
                    .of(ResearchLabBlockEntity::new, ServusManufacturingBlocks.RESEARCH_LAB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerPumpBlockEntity>> BURNER_PUMP =
            BLOCK_ENTITIES.register("burner_pump", () -> BlockEntityType.Builder
                    .of(BurnerPumpBlockEntity::new, ServusManufacturingBlocks.BURNER_PUMP.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP =
            BLOCK_ENTITIES.register("electric_pump", () -> BlockEntityType.Builder
                    .of(ElectricPumpBlockEntity::new, ServusManufacturingBlocks.ELECTRIC_PUMP.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SinkBlockEntity>> SINK =
            BLOCK_ENTITIES.register("sink", () -> BlockEntityType.Builder
                    .of(SinkBlockEntity::new, ServusManufacturingBlocks.SINK.get())
                    .build(null));

    private ServusManufacturingBlockEntities() {
    }
}
