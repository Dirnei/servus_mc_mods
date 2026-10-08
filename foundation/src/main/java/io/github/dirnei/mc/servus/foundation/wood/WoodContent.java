package io.github.dirnei.mc.servus.foundation.wood;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the wood component and the wood recipe serializer. */
public final class WoodContent {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusFoundationIds.MOD_ID);

    /** The planks block a wood block was crafted from. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> WOOD =
            COMPONENTS.registerComponentType("wood", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, WoodRecipe.Serializer> WOOD_SHAPED =
            RECIPE_SERIALIZERS.register("wood_shaped", WoodRecipe.Serializer::new);

    private WoodContent() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
