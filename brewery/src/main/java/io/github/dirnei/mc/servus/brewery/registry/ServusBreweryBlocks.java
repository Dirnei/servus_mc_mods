package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.CaskBlock;
import io.github.dirnei.mc.servus.brewery.beer.FermentingVatBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewKettleBlock;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBricksBlock;
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

    /** The firebox bricks of the Brewhouse. */
    public static final DeferredBlock<BrewhouseBricksBlock> BREWHOUSE_BRICKS = BLOCKS.registerBlock("brewhouse_bricks",
            BrewhouseBricksBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(BrewhouseBricksBlock::lightLevel));

    /** A copper block of the Brew Kettle. */
    public static final DeferredBlock<BrewKettleBlock> BREW_KETTLE = BLOCKS.registerBlock("brew_kettle",
            BrewKettleBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)
                    .noOcclusion());

    /** The open spruce vat that ferments wort into beer. */
    public static final DeferredBlock<FermentingVatBlock> FERMENTING_VAT = BLOCKS.registerBlock("fermenting_vat",
            FermentingVatBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    /** The wooden barrel with a brass tap that stores beer and fills glasses. */
    public static final DeferredBlock<CaskBlock> CASK = BLOCKS.registerBlock("cask", CaskBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    private ServusBreweryBlocks() {
    }
}
