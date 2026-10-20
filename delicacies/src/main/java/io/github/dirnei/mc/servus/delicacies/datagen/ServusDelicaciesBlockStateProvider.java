package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropStages;
import io.github.dirnei.mc.servus.delicacies.crop.DelicaciesCropBlock;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** Block states and block models of the crops and the kitchen, and the item models of every Delicacies item. */
final class ServusDelicaciesBlockStateProvider extends BlockStateProvider {

    ServusDelicaciesBlockStateProvider(PackOutput output) {
        super(output, ServusDelicaciesIds.MOD_ID);
    }

    @Override
    protected void registerStatesAndModels() {
        for (CropKind crop : CropKind.values()) {
            crop(ServusDelicaciesBlocks.crop(crop), crop);
        }
        KitchenModels.register(this);
        for (DeferredItem<? extends Item> item : ServusDelicaciesItems.CREATIVE_TAB_ORDER) {
            if (!ServusDelicaciesItems.BLOCK_MODEL_ITEMS.contains(item)) {
                itemModels().basicItem(item.get());
            }
        }
        itemModels().basicItem(ServusDelicaciesItems.KITCHEN_KNIFE.get()).parent(new ModelFile.UncheckedModelFile("item/handheld"));
    }

    /** A vanilla crop model per growth look and one variant per age. */
    private void crop(DelicaciesCropBlock block, CropKind crop) {
        List<ModelFile> stages = new ArrayList<>();
        for (int stage = 0; stage < CropKind.STAGES; stage++) {
            String name = crop.stageName(stage);
            stages.add(models().withExistingParent(name, mcLoc("block/crop")).texture("crop", modLoc("block/" + name)));
        }
        getVariantBuilder(block).forAllStates(state ->
                new ConfiguredModel[] {new ConfiguredModel(stages.get(CropStages.stageOf(block.getAge(state))))});
    }
}
