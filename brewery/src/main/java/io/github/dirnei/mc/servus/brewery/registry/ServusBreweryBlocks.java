package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.crop.BarleyBlock;
import io.github.dirnei.mc.servus.brewery.crop.HopPoleBlock;
import io.github.dirnei.mc.servus.brewery.malting.MaltingFloorBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The blocks of Servus: Brewery. */
public final class ServusBreweryBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusBreweryIds.MOD_ID);

    /** The barley crop, with the properties of vanilla wheat. */
    public static final DeferredBlock<BarleyBlock> BARLEY = BLOCKS.registerBlock("barley", BarleyBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(state -> state.getValue(CropBlock.AGE) >= 6 ? MapColor.COLOR_YELLOW : MapColor.PLANT)
                    .noCollision()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY));

    /** The pole that hops climb. */
    public static final DeferredBlock<HopPoleBlock> HOP_POLE = BLOCKS.registerBlock("hop_pole", HopPoleBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .forceSolidOff()
                    .randomTicks()
                    .pushReaction(PushReaction.DESTROY));

    /** The low spruce floor on which soaked grain sprouts. */
    public static final DeferredBlock<MaltingFloorBlock> MALTING_FLOOR = BLOCKS.registerBlock("malting_floor", MaltingFloorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    private ServusBreweryBlocks() {
    }
}
