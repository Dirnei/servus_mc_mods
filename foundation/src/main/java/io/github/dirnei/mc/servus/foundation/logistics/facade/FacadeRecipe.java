package io.github.dirnei.mc.servus.foundation.logistics.facade;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One full block plus one nugget (any item in {@code c:nuggets}) gives six facades of that block. */
public class FacadeRecipe extends CustomRecipe {

    public static final int RESULT_COUNT = 6;

    public FacadeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** Whether a block may be used as a facade: a solid full cube without a block entity. */
    public static boolean isFacadeable(Block block) {
        BlockState state = block.defaultBlockState();
        return !state.hasBlockEntity()
                && state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    private static Block facadeBlock(CraftingInput input) {
        Block block = null;
        boolean nugget = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Tags.Items.NUGGETS) && !nugget) {
                nugget = true;
            } else if (block == null && stack.getItem() instanceof BlockItem blockItem && isFacadeable(blockItem.getBlock())) {
                block = blockItem.getBlock();
            } else {
                return null;
            }
        }
        return nugget ? block : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return facadeBlock(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Block block = facadeBlock(input);
        return block == null ? ItemStack.EMPTY : FacadeItem.of(block, RESULT_COUNT);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ServusLogisticsRecipes.FACADE.get();
    }
}
