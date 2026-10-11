package io.github.dirnei.mc.servus.foundation.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

/** Saves a block entity onto its item, what {@code BlockEntity#saveToItem} did before 1.21.5. */
public final class BlockEntityItems {

    private static final Logger LOGGER = LogUtils.getLogger();

    private BlockEntityItems() {
    }

    /** Writes the block entity data and its components onto {@code stack}, as a picked or dismantled block keeps them. */
    public static void saveToItem(BlockEntity blockEntity, ItemStack stack, HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            blockEntity.saveCustomOnly(output);
            blockEntity.removeComponentsFromTag(output);
            BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
            stack.applyComponents(blockEntity.collectComponents());
        }
    }
}
