package io.github.dirnei.mc.servus.foundation.logistics.facade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Eight Facades of one block around an empty centre give eight Hollow Facades of that block.
 * It reads and writes like a shaped recipe, so recipe viewers show it as one.
 */
public class HollowFacadeRecipe extends ShapedRecipe {

    public static final int RESULT_COUNT = 8;

    /** Same fields as a shaped recipe. */
    public static final MapCodec<HollowFacadeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result))
            .apply(i, HollowFacadeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HollowFacadeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            HollowFacadeRecipe::new);
    public static final RecipeSerializer<HollowFacadeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;

    public HollowFacadeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    /** The one block of every Facade in the grid, or null when they differ or there are none. */
    private static Block ringBlock(CraftingInput input) {
        Block block = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Block slotBlock = FacadeItem.blockOf(stack);
            if (block != null && block != slotBlock) {
                return null;
            }
            block = slotBlock;
        }
        return block;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && ringBlock(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack hollow = super.assemble(input);
        Block block = ringBlock(input);
        if (block != null) {
            hollow.set(ServusLogisticsComponents.FACADE_BLOCK.get(), block);
        }
        return hollow;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe narrows the return type to its own serializer; ours decodes to the subclass.
        return (RecipeSerializer) ServusLogisticsRecipes.HOLLOW_FACADE.get();
    }
}
