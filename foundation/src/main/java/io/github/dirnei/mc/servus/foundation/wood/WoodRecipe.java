package io.github.dirnei.mc.servus.foundation.wood;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A shaped recipe that records the wood of the planks in the grid on its
 * result, picked by {@link WoodPick}. It reads and writes like a shaped
 * recipe, so recipe viewers show it as one.
 */
public class WoodRecipe extends ShapedRecipe {

    public WoodRecipe(ShapedRecipe base) {
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
        String wood = WoodPick.pick(planks);
        if (wood != null) {
            result.set(WoodContent.WOOD.get(), ResourceLocation.parse(wood));
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WoodContent.WOOD_SHAPED.get();
    }

    /** Reads and writes the recipe exactly like a shaped recipe. */
    public static class Serializer implements RecipeSerializer<WoodRecipe> {

        private static final MapCodec<WoodRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(WoodRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, WoodRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(WoodRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<WoodRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WoodRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
