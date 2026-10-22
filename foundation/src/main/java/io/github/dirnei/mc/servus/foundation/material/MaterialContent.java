package io.github.dirnei.mc.servus.foundation.material;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the material component and the material recipe serializer. */
public final class MaterialContent {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusFoundationIds.MOD_ID);

    /** The block a material block was crafted from, such as a planks or a stone block. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> MATERIAL =
            COMPONENTS.registerComponentType("material", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaterialRecipe>> MATERIAL_SHAPED =
            RECIPE_SERIALIZERS.register("material_shaped", () -> MaterialRecipe.SERIALIZER);

    private MaterialContent() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
