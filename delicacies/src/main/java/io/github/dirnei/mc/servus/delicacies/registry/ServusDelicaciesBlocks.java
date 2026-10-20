package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.DelicaciesCropBlock;
import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingOvenBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.PanBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The blocks of Servus: Delicacies. */
public final class ServusDelicaciesBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusDelicaciesIds.MOD_ID);

    /** One crop block per crop, with the properties of vanilla carrots. */
    public static final Map<CropKind, DeferredBlock<DelicaciesCropBlock>> CROPS = cropBlocks();

    /** The Stove Tile, loose or part of a Tiled Stove. */
    public static final DeferredBlock<StoveTileBlock> STOVE_TILE = BLOCKS.registerBlock("stove_tile", StoveTileBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(StoveTileBlock::lightLevel));

    /** The Pot, family steel. */
    public static final DeferredBlock<PotBlock> POT = BLOCKS.registerBlock("pot", PotBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** The Pan, dark iron with a wooden handle. */
    public static final DeferredBlock<PanBlock> PAN = BLOCKS.registerBlock("pan", PanBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** The Baking Oven, family steel with a window in its iron door. */
    public static final DeferredBlock<BakingOvenBlock> BAKING_OVEN = BLOCKS.registerBlock("baking_oven", BakingOvenBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(BakingOvenBlock::lightLevel));

    /** The Cutting Board. */
    public static final DeferredBlock<CuttingBoardBlock> CUTTING_BOARD = BLOCKS.registerBlock("cutting_board", CuttingBoardBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    private ServusDelicaciesBlocks() {
    }

    /** The crop block of {@code crop}. */
    public static DelicaciesCropBlock crop(CropKind crop) {
        return CROPS.get(crop).get();
    }

    private static Map<CropKind, DeferredBlock<DelicaciesCropBlock>> cropBlocks() {
        Map<CropKind, DeferredBlock<DelicaciesCropBlock>> blocks = new EnumMap<>(CropKind.class);
        for (CropKind crop : CropKind.values()) {
            blocks.put(crop, BLOCKS.registerBlock(crop.blockId(),
                    properties -> new DelicaciesCropBlock(() -> ServusDelicaciesItems.planting(crop), properties),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.CROP)
                            .pushReaction(PushReaction.DESTROY)));
        }
        return Collections.unmodifiableMap(blocks);
    }
}
