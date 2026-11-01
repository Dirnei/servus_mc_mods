package io.github.dirnei.mc.servus.foundation.logistics.tank;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.logistics.recipe.FloorBlockIngredient;
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
import net.minecraft.world.level.block.Block;

/**
 * The Fluid Tank recipe: copper ingots in the corners, glass on the top, left and right
 * edges, an empty centre, and in the bottom middle any floor block, which becomes the
 * tank's floor. It reads and writes like a shaped recipe, so recipe viewers show it as one.
 */
public class FluidTankRecipe extends ShapedRecipe {

    private static final int FLOOR_SLOT = 7;

    /** Same fields as a shaped recipe. */
    public static final MapCodec<FluidTankRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result))
            .apply(i, FluidTankRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidTankRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            FluidTankRecipe::new);
    public static final RecipeSerializer<FluidTankRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;

    public FluidTankRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tank = super.assemble(input);
        Block floor = FloorBlockIngredient.blockOf(input.getItem(FLOOR_SLOT));
        if (floor != null) {
            tank.set(ServusLogisticsComponents.TANK_FLOOR.get(), floor);
        }
        return tank;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe narrows the return type to its own serializer; ours decodes to the subclass.
        return (RecipeSerializer) ServusLogisticsRecipes.FLUID_TANK.get();
    }
}
