package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.DelicaciesCropBlock;
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
