package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.cable.PowerCableBlock;
import io.github.dirnei.mc.servus.manufacturing.duct.ItemDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.generator.CoalGeneratorBlock;
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

    public static final DeferredBlock<PowerCableBlock> POWER_CABLE =
            BLOCKS.registerBlock("power_cable", PowerCableBlock::new, connectable(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<ItemDuctBlock> ITEM_DUCT =
            BLOCKS.registerBlock("item_duct", ItemDuctBlock::new, connectable(MapColor.METAL));

    /** Shared properties of cables and ducts: thin, quick to break, no occlusion. */
    private static BlockBehaviour.Properties connectable(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private ServusManufacturingBlocks() {
    }

    /** Deposits are permanent: unbreakable in survival and they drop nothing. */
    private static DeferredBlock<Block> deposit(String name, MapColor color) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE));
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
