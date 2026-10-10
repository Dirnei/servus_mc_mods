package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusManufacturingItemModelProvider extends ItemModelProvider {

    ServusManufacturingItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusManufacturingIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusManufacturingItems.ASH.get());
        basicItem(ServusManufacturingItems.CAPUT_MORTUUM.get());
        basicItem(ServusManufacturingItems.SALT_CRYSTAL.get());
        basicItem(ServusManufacturingItems.SULFUR.get());
        basicItem(ServusManufacturingItems.QUICKSILVER.get());
        basicItem(ElementalSight.ELIXIR.get());
        basicItem(ServusManufacturingItems.PARACELSUS_JOURNAL.get());
        // Buckets draw their principle with NeoForge's fluid container model, tinted like the fluid.
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            getBuilder(entry.bucket().getId().getPath())
                    .parent(new ModelFile.UncheckedModelFile("neoforge:item/bucket"))
                    .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(entry.source().get())
                    .end();
        }
    }
}
