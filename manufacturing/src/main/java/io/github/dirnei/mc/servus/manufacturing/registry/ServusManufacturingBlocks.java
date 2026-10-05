package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
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

    private ServusManufacturingBlocks() {
    }

    /** Deposits are permanent: unbreakable in survival and they drop nothing. */
    private static DeferredBlock<Block> deposit(String name, MapColor color) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE));
    }
}
