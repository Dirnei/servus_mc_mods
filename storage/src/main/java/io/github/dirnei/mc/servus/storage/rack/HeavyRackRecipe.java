package io.github.dirnei.mc.servus.storage.rack;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * The shaped rack recipe that remembers the wood: the planks in the grid,
 * read in order, pick the wood through {@link RackWood}.
 */
public class HeavyRackRecipe extends ShapedRecipe {

    public HeavyRackRecipe(ShapedRecipe base) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        List<String> planks = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ItemTags.PLANKS) && stack.getItem() instanceof BlockItem block) {
                planks.add(BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
            }
        }
        String wood = RackWood.pick(planks);
        if (wood != null) {
            result.set(ServusStorageComponents.RACK_WOOD.get(), Identifier.parse(wood));
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusStorageRecipes.HEAVY_RACK.get();
    }

    /** Reads and writes the recipe exactly like a shaped recipe. */
    public static class Serializer implements RecipeSerializer<HeavyRackRecipe> {

        private static final MapCodec<HeavyRackRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(HeavyRackRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, HeavyRackRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(HeavyRackRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<HeavyRackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HeavyRackRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
