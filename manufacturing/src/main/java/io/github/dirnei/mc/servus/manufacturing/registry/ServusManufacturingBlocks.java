package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusManufacturingIds.MOD_ID);

    public static final DeferredBlock<ResearchLabBlock> RESEARCH_LAB =
            BLOCKS.registerBlock("research_lab", ResearchLabBlock::new, MachineContent.machine());

    private ServusManufacturingBlocks() {
    }
}
