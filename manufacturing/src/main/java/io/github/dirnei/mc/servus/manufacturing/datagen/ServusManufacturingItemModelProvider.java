package io.github.dirnei.mc.servus.manufacturing.datagen;

import com.google.gson.JsonObject;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.data.PackOutput;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;

final class ServusManufacturingItemModelProvider extends ItemModelProvider {

    ServusManufacturingItemModelProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID);
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
        // Buckets draw their principle with NeoForge's fluid container item model, tinted like the fluid,
        // with the same textures as NeoForge's own bucket model (no cover layer).
        for (Principle principle : Principle.values()) {
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", "minecraft:item/bucket");
            textures.addProperty("base", "minecraft:item/bucket");
            textures.addProperty("fluid", "neoforge:item/mask/bucket_fluid");
            JsonObject model = new JsonObject();
            model.addProperty("type", "neoforge:fluid_container");
            model.add("textures", textures);
            model.addProperty("fluid", entry.source().getId().toString());
            itemModelDefinition(entry.bucket().getId(), model);
        }
    }
}
