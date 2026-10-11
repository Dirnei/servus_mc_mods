package io.github.dirnei.mc.servus.foundation.wood;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The wood material shared by Servus blocks: a planks block id on the item
 * and the block entity, shown as the board panels of the block's model.
 */
public final class Wood {

    /** Boards of a block that remembers no wood. */
    public static final Identifier DEFAULT = BuiltInRegistries.BLOCK.getKey(Blocks.OAK_PLANKS);
    /** The wood in a block entity's model data. */
    public static final ModelProperty<Identifier> PROPERTY = new ModelProperty<>();
    /** The board texture of layered models; quads with this sprite show the wood. */
    public static final Identifier BOARD = Identifier.fromNamespaceAndPath("servus_foundation", "block/wood_board");
    /**
     * Further textures a block swaps besides its wood, from its model data:
     * texture name to the texture drawn in its place (block atlas), for
     * material looks such as copper stages. Items never carry it.
     */
    public static final ModelProperty<Map<Identifier, Identifier>> SWAPS = new ModelProperty<>();

    private static final Set<Identifier> BLOCKS = new LinkedHashSet<>();

    private Wood() {
    }

    /** Marks a block whose block state and item models show its wood. Call during mod construction. */
    public static synchronized void register(Identifier blockId) {
        BLOCKS.add(blockId);
    }

    public static synchronized boolean isWoodBlock(Identifier blockId) {
        return BLOCKS.contains(blockId);
    }

    /** The id itself when it names a block, else oak. */
    public static Identifier or(@Nullable Identifier id) {
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : DEFAULT;
    }

    /** The wood an item carries; oak when none or unknown. */
    public static Identifier of(ItemStack stack) {
        return or(stack.get(WoodContent.WOOD.get()));
    }

    /** The planks block of a wood id. */
    public static Block block(Identifier wood) {
        return BuiltInRegistries.BLOCK.get(or(wood));
    }

    /** Copies the wood of a block entity onto a drop built by hand. */
    public static ItemStack stamp(ItemStack stack, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof WoodHolder holder && holder.woodSlot().raw() != null) {
            stack.set(WoodContent.WOOD.get(), holder.woodSlot().raw());
        }
        return stack;
    }
}
