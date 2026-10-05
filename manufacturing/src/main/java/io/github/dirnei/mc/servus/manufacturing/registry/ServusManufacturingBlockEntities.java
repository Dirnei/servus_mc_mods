package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.cable.PowerCableBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.duct.ItemDuctBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.ElectricExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.generator.CoalGeneratorBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchLabBlockEntity>> RESEARCH_LAB =
            BLOCK_ENTITIES.register("research_lab", () -> BlockEntityType.Builder
                    .of(ResearchLabBlockEntity::new, ServusManufacturingBlocks.RESEARCH_LAB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerCableBlockEntity>> POWER_CABLE =
            BLOCK_ENTITIES.register("power_cable", () -> BlockEntityType.Builder
                    .of(PowerCableBlockEntity::new, ServusManufacturingBlocks.POWER_CABLE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemDuctBlockEntity>> ITEM_DUCT =
            BLOCK_ENTITIES.register("item_duct", () -> BlockEntityType.Builder
                    .of(ItemDuctBlockEntity::new, ServusManufacturingBlocks.ITEM_DUCT.get())
                    .build(null));

    private ServusManufacturingBlockEntities() {
    }
}
