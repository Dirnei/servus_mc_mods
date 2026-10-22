package io.github.dirnei.mc.servus.foundation.material;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;

/** The stone material kind: its default, its tag and a stone kind recipe. */
public final class MaterialGameTests {

    private MaterialGameTests() {
    }

    /** A stone kind block without a stone shows Bricks. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stoneKindDefaultsToBricks(GameTestHelper helper) {
        helper.assertTrue(Identifier.withDefaultNamespace("bricks").equals(Material.or(null, MaterialKind.STONE)),
                "the stone default is bricks, got " + Material.or(null, MaterialKind.STONE));
        helper.assertTrue(Material.block(null, MaterialKind.STONE) == Blocks.BRICKS, "the stone default block is Bricks");
        helper.succeed();
    }

    /** The stone tag holds full cubes and no slabs. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stoneTagHoldsFullCubes(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.STONE_BRICKS).is(MaterialKind.STONES), "Stone Bricks are a stone");
        helper.assertTrue(new ItemStack(Items.POLISHED_DEEPSLATE).is(MaterialKind.STONES), "Polished Deepslate is a stone");
        helper.assertTrue(new ItemStack(Items.CUT_SANDSTONE).is(MaterialKind.STONES), "Cut Sandstone is a stone");
        helper.assertFalse(new ItemStack(Items.STONE_BRICK_SLAB).is(MaterialKind.STONES), "a Stone Brick Slab is no stone");
        helper.succeed();
    }

    /** Two Stone Bricks and one Bricks in a stone kind recipe give a result carrying Stone Bricks. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stoneRecipePicksMajority(GameTestHelper helper) {
        Ingredient stone = Ingredient.of(helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM).getOrThrow(MaterialKind.STONES));
        MaterialRecipe recipe = new MaterialRecipe(new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, ""),
                ShapedRecipePattern.of(Map.of('S', stone), "SSS"), new ItemStackTemplate(Items.BRICK_WALL), MaterialKind.STONE);
        CraftingInput input = CraftingInput.of(3, 1,
                List.of(new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.BRICKS), new ItemStack(Items.STONE_BRICKS)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the grid matches the stone recipe");
        Identifier carried = recipe.assemble(input).get(MaterialContent.MATERIAL.get());
        helper.assertTrue(Identifier.withDefaultNamespace("stone_bricks").equals(carried),
                "the result carries stone bricks, got " + carried);
        helper.succeed();
    }
}
