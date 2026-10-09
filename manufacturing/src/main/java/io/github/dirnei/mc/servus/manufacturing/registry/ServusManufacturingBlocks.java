package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusManufacturingIds.MOD_ID);


    public static final DeferredBlock<CalcinerBlock> CALCINER =
            BLOCKS.registerBlock("calciner", CalcinerBlock::new, MachineContent.machine());

    public static final DeferredBlock<CoagulatorBlock> COAGULATOR =
            BLOCKS.registerBlock("coagulator", CoagulatorBlock::new, MachineContent.machine());

    public static final DeferredBlock<Block> SALT_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("salt_crystal_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.8F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    public static final DeferredBlock<AthanorBlock> ATHANOR = BLOCKS.registerBlock("athanor", AthanorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.STONE).lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0));

    private ServusManufacturingBlocks() {
    }
}
