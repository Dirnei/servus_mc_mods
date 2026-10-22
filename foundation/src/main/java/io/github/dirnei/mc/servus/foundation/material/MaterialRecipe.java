package io.github.dirnei.mc.servus.foundation.material;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
 * A shaped recipe that records the material of its kind in the grid on its
 * result, picked by {@link MaterialPick}: every block item of the kind's tag
 * counts, and an ingredient that carries a material itself (a rail) counts
 * like a block of that material. It reads and writes like a shaped recipe
 * plus a {@code kind} field, so recipe viewers show it as one.
 */
public class MaterialRecipe extends ShapedRecipe {

    /** The fields of a shaped recipe and the material kind. */
    public static final MapCodec<MaterialRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
                    MaterialKind.CODEC.fieldOf("kind").forGetter(o -> o.kind))
            .apply(i, MaterialRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            MaterialKind.STREAM_CODEC.cast(), o -> o.kind,
            MaterialRecipe::new);
    public static final RecipeSerializer<MaterialRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;
    private final MaterialKind kind;

    public MaterialRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result, MaterialKind kind) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
        this.kind = kind;
    }

    public MaterialKind kind() {
        return kind;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        List<String> materials = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            Identifier carried = stack.get(MaterialContent.MATERIAL.get());
            if (carried != null) {
                materials.add(carried.toString());
            } else if (stack.is(kind.ingredients()) && stack.getItem() instanceof BlockItem block) {
                materials.add(BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
            }
        }
        String material = MaterialPick.pick(materials);
        if (material != null) {
            result.set(MaterialContent.MATERIAL.get(), Identifier.parse(material));
        }
        return result;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe narrows the return type to its own serializer; ours decodes to the subclass.
        return (RecipeSerializer) MaterialContent.MATERIAL_SHAPED.get();
    }
}
