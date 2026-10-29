package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.minecraft.data.PackOutput;

final class ServusFoundationItemModelProvider extends ItemModelProvider {

    ServusFoundationItemModelProvider(PackOutput output) {
        super(output, ServusFoundationIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusFoundationItems.CONFIGURATION_CARROT.get());
        basicItem(ServusFoundationItems.DATA_POTATO.get());
        basicItem(ServusFoundationItems.GOGGLES.get());
        basicItem(ServusFoundationItems.SPEED_UPGRADE.get());
        basicItem(ServusFoundationItems.CAPACITY_UPGRADE.get());
        basicItem(ServusFoundationItems.HEATING_ELEMENT.get());
        basicItem(MachineContent.ANCHOR_UPGRADE.get());
        basicItem(ServusFoundationItems.CHARCOAL_POWDER.get());
        basicItem(ServusFoundationItems.ACTIVATED_CHARCOAL.get());
        basicItem(ServusFoundationItems.CARBON_FILTER.get());
        basicItem(ServusFoundationItems.SPENT_FILTER.get());
        LogisticsDataParts.itemModels(this);
    }
}
