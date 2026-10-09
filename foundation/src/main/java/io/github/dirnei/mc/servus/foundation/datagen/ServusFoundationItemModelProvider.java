package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusFoundationItemModelProvider extends ItemModelProvider {

    ServusFoundationItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusFoundationItems.CONFIGURATION_CARROT.get());
        basicItem(ServusFoundationItems.GOGGLES.get());
        basicItem(ServusFoundationItems.SPEED_UPGRADE.get());
        LogisticsDataParts.itemModels(this);
    }
}
