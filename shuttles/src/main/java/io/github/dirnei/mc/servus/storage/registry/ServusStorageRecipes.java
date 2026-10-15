package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HeavyRackRecipe>> HEAVY_RACK =
            RECIPE_SERIALIZERS.register("heavy_rack", () -> HeavyRackRecipe.SERIALIZER);

    private ServusStorageRecipes() {
    }
}
