package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusStorageIds.MOD_ID);

    public static final DeferredBlock<CrateBlock> CRATE = BLOCKS.registerBlock("crate", CrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    private ServusStorageBlocks() {
    }
}
