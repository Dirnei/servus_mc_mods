package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLogisticsItemModelProvider extends ItemModelProvider {

    ServusLogisticsItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusLogisticsItems.ITEM_PIPE.get());
        basicItem(ServusLogisticsItems.POWER_CABLE.get());
        basicItem(ServusLogisticsItems.FLUID_PIPE.get());
        basicItem(ServusLogisticsItems.FACADE.get());
    }
}
