package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.crusher.CrusherBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.loader.ChunkLoaderBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.ElectricExtractorBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.generator.CoalGeneratorBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.BurnerPumpBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.ElectricPumpBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusManufacturingIds.MOD_ID);

    public static final DeferredBlock<Block> IRON_DEPOSIT = deposit("iron_deposit", MapColor.STONE);
    public static final DeferredBlock<Block> COPPER_DEPOSIT = deposit("copper_deposit", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<Block> COAL_DEPOSIT = deposit("coal_deposit", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> STONE_DEPOSIT = deposit("stone_deposit", MapColor.STONE);

    public static final DeferredBlock<BurnerExtractorBlock> BURNER_EXTRACTOR =
            BLOCKS.registerBlock("burner_extractor", BurnerExtractorBlock::new, machine());
    public static final DeferredBlock<BurnerSmelterBlock> BURNER_SMELTER =
            BLOCKS.registerBlock("burner_smelter", BurnerSmelterBlock::new, machine());
    public static final DeferredBlock<CoalGeneratorBlock> COAL_GENERATOR =
            BLOCKS.registerBlock("coal_generator", CoalGeneratorBlock::new, machine());
    public static final DeferredBlock<AssemblerBlock> ASSEMBLER =
            BLOCKS.registerBlock("assembler", AssemblerBlock::new, machine());
    public static final DeferredBlock<ElectricExtractorBlock> ELECTRIC_EXTRACTOR =
            BLOCKS.registerBlock("electric_extractor", ElectricExtractorBlock::new, machine());
    public static final DeferredBlock<CrusherBlock> CRUSHER =
            BLOCKS.registerBlock("crusher", CrusherBlock::new, machine());
    public static final DeferredBlock<ChunkLoaderBlock> CHUNK_LOADER =
            BLOCKS.registerBlock("chunk_loader", ChunkLoaderBlock::new, machine());
    public static final DeferredBlock<ResearchLabBlock> RESEARCH_LAB =
            BLOCKS.registerBlock("research_lab", ResearchLabBlock::new, machine());
    public static final DeferredBlock<BurnerPumpBlock> BURNER_PUMP =
            BLOCKS.registerBlock("burner_pump", BurnerPumpBlock::new, machine().noOcclusion());
    public static final DeferredBlock<ElectricPumpBlock> ELECTRIC_PUMP =
            BLOCKS.registerBlock("electric_pump", ElectricPumpBlock::new, machine().noOcclusion());
    public static final DeferredBlock<SinkBlock> SINK =
            BLOCKS.registerBlock("sink", SinkBlock::new, brick().noOcclusion());
    public static final DeferredBlock<ChimneyBlock> CHIMNEY =
            BLOCKS.registerBlock("chimney", ChimneyBlock::new, brick());

    private ServusManufacturingBlocks() {
    }

    /** Deposits are permanent: unbreakable in survival and they drop nothing. */
    private static DeferredBlock<Block> deposit(String name, MapColor color) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE));
    }

    /** Brick blocks of the Sink family: like vanilla bricks. */
    private static BlockBehaviour.Properties brick() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(2.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    /** Shared properties of machine blocks: metal, pickaxe, glow when lit. */
    public static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0);
    }
}
