package io.github.dirnei.mc.servus.brewery.datagen;

import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** Flat item models from the item textures; block items use their block models, wort buckets the NeoForge fluid container. */
final class ServusBreweryItemModelProvider extends ItemModelProvider {

    /** Block items whose item model is their block model. */
    private static final Set<DeferredItem<? extends Item>> BLOCK_MODELS =
            Set.of(ServusBreweryItems.MALTING_FLOOR, ServusBreweryItems.BREWHOUSE_BRICKS, ServusBreweryItems.BREW_KETTLE);

    ServusBreweryItemModelProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        ServusBreweryItems.CREATIVE_TAB_ORDER.stream()
                .filter(item -> !BLOCK_MODELS.contains(item) && !(item.get() instanceof BucketItem))
                .forEach(item -> basicItem(item.get()));
        // NeoForge's fluid container model with the textures of its own bucket model, without a cover layer.
        for (BreweryFluids.Wort wort : BreweryFluids.WORTS) {
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", "minecraft:item/bucket");
            textures.addProperty("base", "minecraft:item/bucket");
            textures.addProperty("fluid", "neoforge:item/mask/bucket_fluid");
            JsonObject model = new JsonObject();
            model.addProperty("type", "neoforge:fluid_container");
            model.add("textures", textures);
            model.addProperty("fluid", wort.source().getId().toString());
            itemModelDefinition(wort.bucket().getId(), model);
        }
    }
}
