package io.github.dirnei.mc.servus.alchemy.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.client.hollow.VeinColorTint;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import net.minecraft.data.PackOutput;

final class ServusAlchemyItemModelProvider extends ItemModelProvider {

    ServusAlchemyItemModelProvider(PackOutput output) {
        super(output, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusAlchemyItems.ASH.get());
        basicItem(ServusAlchemyItems.CAPUT_MORTUUM.get());
        basicItem(ServusAlchemyItems.SALT_CRYSTAL.get());
        basicItem(ServusAlchemyItems.SULFUR.get());
        basicItem(ServusAlchemyItems.QUICKSILVER.get());
        basicItem(ServusAlchemyItems.SAL_AMMONIAC.get());
        basicItem(ServusAlchemyItems.IGNIS_PERPETUUS.get());
        basicItem(ServusAlchemyItems.SEALED_FLASK.get());
        basicItem(ServusAlchemyItems.PRELUM_SCREW.get());
        basicItem(ServusAlchemyItems.TINY_PICKAXE.get());
        basicItem(ServusAlchemyItems.RED_FELT_SCRAP.get());
        basicItem(ServusAlchemyItems.SALT_OFFERING.get());
        basicItem(ServusAlchemyItems.SALAMANDER_SPAWN_EGG.get());
        seed();
        basicItem(ElementalSight.ELIXIR.get());
        basicItem(ServusAlchemyItems.PARACELSUS_JOURNAL.get());
        // NeoForge's fluid container model with the textures of its own bucket model, without a cover layer.
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

    /** The greyscale seed tinted in its vein's colour, with the untinted husk drawn over it. */
    private void seed() {
        basicItem(ServusAlchemyItems.SEED.get()).texture("layer1", modLoc("item/seed_husk"));
        JsonObject tint = new JsonObject();
        tint.addProperty("type", VeinColorTint.ID.toString());
        JsonArray tints = new JsonArray();
        tints.add(tint);
        itemTints(ServusAlchemyItems.SEED.getId(), tints);
    }
}
