package io.github.dirnei.mc.servus.foundation.logistics.facade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.logistics.recipe.FloorBlockIngredient;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;

/**
 * One floor block plus one nugget (any item in {@code c:nuggets}) gives six facades of that block.
 * It reads and writes like a shapeless recipe, so recipe viewers show it as one.
 */
public class FacadeRecipe extends ShapelessRecipe {

    public static final int RESULT_COUNT = 6;

    /** Same fields as a shapeless recipe. */
    public static final MapCodec<FacadeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
                    Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients))
            .apply(i, FacadeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
            FacadeRecipe::new);
    public static final RecipeSerializer<FacadeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;

    public FacadeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
            List<Ingredient> ingredients) {
        super(commonInfo, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
    }

    /** The first floor block in the grid, or null. */
    private static Block facadeBlock(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            Block block = FloorBlockIngredient.blockOf(input.getItem(i));
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack facades = super.assemble(input);
        Block block = facadeBlock(input);
        if (block != null) {
            facades.set(ServusLogisticsComponents.FACADE_BLOCK.get(), block);
        }
        return facades;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        // ShapelessRecipe narrows the return type to its own serializer; ours decodes to the subclass.
        return (RecipeSerializer) ServusLogisticsRecipes.FACADE.get();
    }
}
