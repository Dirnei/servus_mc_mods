package io.github.dirnei.mc.servus.foundation.material;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The material shared by Servus blocks: the id of the block a block was
 * crafted from, such as a planks or a stone block, on the item and the block
 * entity, shown where the block's model draws its kind's placeholder texture.
 */
public final class Material {

    /** The material in a block entity's model data. */
    public static final ModelProperty<Identifier> PROPERTY = new ModelProperty<>();
    /**
     * Further textures a block swaps besides its material, from its model data:
     * texture name to the texture drawn in its place (block atlas), for
     * looks such as copper stages. Items never carry it.
     */
    public static final ModelProperty<Map<Identifier, Identifier>> SWAPS = new ModelProperty<>();

    private static final Map<Identifier, MaterialKind> BLOCKS = new LinkedHashMap<>();

    private Material() {
    }

    /** Marks a block whose block state and item models show its material of the given kind. Call during mod construction. */
    public static synchronized void register(Identifier blockId, MaterialKind kind) {
        BLOCKS.put(blockId, kind);
    }

    /** The kind of a registered material block, null for any other block. */
    public static synchronized @Nullable MaterialKind kindOf(Identifier blockId) {
        return BLOCKS.get(blockId);
    }

    /** The kind of a block, wood for a block that is not registered. */
    public static MaterialKind kindOf(Block block) {
        MaterialKind kind = kindOf(BuiltInRegistries.BLOCK.getKey(block));
        return kind != null ? kind : MaterialKind.WOOD;
    }

    /** The id itself when it names a block, else the kind's default. */
    public static Identifier or(@Nullable Identifier id, MaterialKind kind) {
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : kind.defaultBlock();
    }

    /** The kind of the block an item places, wood for any other item. */
    public static MaterialKind kindOf(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? kindOf(item.getBlock()) : MaterialKind.WOOD;
    }

    /** The material an item carries; its kind's default when none or unknown. */
    public static Identifier of(ItemStack stack) {
        return or(stack.get(MaterialContent.MATERIAL.get()), kindOf(stack));
    }

    /** The block of a material id. */
    public static Block block(@Nullable Identifier id, MaterialKind kind) {
        return BuiltInRegistries.BLOCK.getValue(or(id, kind));
    }

    /** Copies the material of a block entity onto a drop built by hand. */
    public static ItemStack stamp(ItemStack stack, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof MaterialHolder holder && holder.materialSlot().raw() != null) {
            stack.set(MaterialContent.MATERIAL.get(), holder.materialSlot().raw());
        }
        return stack;
    }
}
