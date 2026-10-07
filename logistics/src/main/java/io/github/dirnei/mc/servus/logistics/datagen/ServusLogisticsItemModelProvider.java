package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLogisticsItemModelProvider extends ItemModelProvider {

    ServusLogisticsItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusLogisticsIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusLogisticsItems.ITEM_PIPE.get());
        basicItem(ServusLogisticsItems.POWER_CABLE.get());
        basicItem(ServusLogisticsItems.FACADE.get());
    }
}
