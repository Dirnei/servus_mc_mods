package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusManufacturingItemModelProvider extends ItemModelProvider {

    ServusManufacturingItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusManufacturingIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusManufacturingItems.IRON_GEAR.get());
        basicItem(ServusManufacturingItems.COPPER_WIRE.get());
        basicItem(ServusManufacturingItems.RED_PACK.get());
        basicItem(ServusManufacturingItems.IRON_GRIT.get());
        basicItem(ServusManufacturingItems.COPPER_GRIT.get());
    }
}
