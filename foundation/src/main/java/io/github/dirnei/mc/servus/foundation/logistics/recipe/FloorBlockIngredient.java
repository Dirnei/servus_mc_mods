package io.github.dirnei.mc.servus.foundation.logistics.recipe;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * The ingredient {@code servus_foundation:floor_block}: any solid full block
 * without a block entity, from any mod. Facades and Fluid Tank floors use it,
 * and recipe viewers list every such block.
 */
public final class FloorBlockIngredient implements ICustomIngredient {

    /** The ingredient has no settings, so one instance serves every recipe. */
    public static final FloorBlockIngredient INSTANCE = new FloorBlockIngredient();

    private FloorBlockIngredient() {
    }

    /** A vanilla ingredient holding this one, for recipes and datagen. */
    public static Ingredient ingredient() {
        return INSTANCE.toVanilla();
    }

    /** Whether a block may be a facade or a tank floor: a solid full cube without a block entity. */
    public static boolean accepts(Block block) {
        BlockState state = block.defaultBlockState();
        return !state.hasBlockEntity()
                && state.isSolidRender()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** The block of a stack this ingredient accepts, or null. */
    public static Block blockOf(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && accepts(blockItem.getBlock()) ? blockItem.getBlock() : null;
    }

    @Override
    public boolean test(ItemStack stack) {
        return blockOf(stack) != null;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return BuiltInRegistries.ITEM.listElements()
                .filter(item -> item.value() instanceof BlockItem blockItem && accepts(blockItem.getBlock()))
                .<Holder<Item>>map(item -> item);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public IngredientType<?> getType() {
        return ServusLogisticsRecipes.FLOOR_BLOCK.get();
    }
}
