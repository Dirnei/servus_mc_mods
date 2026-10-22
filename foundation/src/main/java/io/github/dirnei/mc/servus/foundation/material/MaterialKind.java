package io.github.dirnei.mc.servus.foundation.material;

import com.mojang.serialization.Codec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * A kind of material a block can be crafted from: the items that count as
 * the material in its recipe, the block shown when it has none, the
 * placeholder texture its models draw the material with and its tooltip line.
 */
public enum MaterialKind implements StringRepresentable {

    WOOD("wood", ItemTags.PLANKS, Identifier.withDefaultNamespace("oak_planks"),
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/wood_board"), "tooltip.servus_foundation.wood"),
    STONE("stone", TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "stones")),
            Identifier.withDefaultNamespace("bricks"), Identifier.withDefaultNamespace("block/bricks"),
            "tooltip.servus_foundation.stone");

    /** The stones a stone kind recipe accepts, {@code #servus_foundation:stones}; datapacks may extend it. */
    public static final TagKey<Item> STONES = STONE.ingredients;

    public static final Codec<MaterialKind> CODEC = StringRepresentable.fromEnum(MaterialKind::values);
    public static final StreamCodec<ByteBuf, MaterialKind> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> values()[id], MaterialKind::ordinal);

    private final String name;
    private final TagKey<Item> ingredients;
    private final Identifier defaultBlock;
    private final Identifier placeholder;
    private final String tooltipKey;

    MaterialKind(String name, TagKey<Item> ingredients, Identifier defaultBlock, Identifier placeholder, String tooltipKey) {
        this.name = name;
        this.ingredients = ingredients;
        this.defaultBlock = defaultBlock;
        this.placeholder = placeholder;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The items a recipe of this kind takes as its material. */
    public TagKey<Item> ingredients() {
        return ingredients;
    }

    /** The block shown by a block of this kind that has no material. */
    public Identifier defaultBlock() {
        return defaultBlock;
    }

    /** The texture whose quads show the material (block atlas). */
    public Identifier placeholder() {
        return placeholder;
    }

    /** The tooltip line, with the material block's name as its argument. */
    public String tooltipKey() {
        return tooltipKey;
    }
}
