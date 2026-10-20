package io.github.dirnei.mc.servus.foundation.wood;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that records the wood of the planks in the grid on its
 * result, picked by {@link WoodPick}; an ingredient that carries a wood
 * itself (a rail) counts like planks of that wood. It reads and writes like a shaped
 * recipe, so recipe viewers show it as one.
 */
public class WoodRecipe extends ShapedRecipe {

    /** Same fields as a shaped recipe. */
    public static final MapCodec<WoodRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result))
            .apply(i, WoodRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WoodRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            WoodRecipe::new);
    public static final RecipeSerializer<WoodRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;

    public WoodRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        List<String> planks = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            Identifier carried = stack.get(WoodContent.WOOD.get());
            if (carried != null) {
                planks.add(carried.toString());
            } else if (stack.is(ItemTags.PLANKS) && stack.getItem() instanceof BlockItem block) {
                planks.add(BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
            }
        }
        String wood = WoodPick.pick(planks);
        if (wood != null) {
            result.set(WoodContent.WOOD.get(), Identifier.parse(wood));
        }
        return result;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe narrows the return type to its own serializer; ours decodes to the subclass.
        return (RecipeSerializer) WoodContent.WOOD_SHAPED.get();
    }
}
