package io.github.dirnei.mc.servus.foundation.wood.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * How a family of blocks shows its wood: the board texture its models use,
 * the model data property and fallback that name the wood of a placed block,
 * and how an item stack names its wood. {@link WoodModel} and
 * {@link WoodItemModel} retexture board quads with it; other mods (the rack)
 * bring their own skin.
 */
public final class WoodSkin {

    /** The Servus wood blocks: {@link Wood#BOARD}, {@link Wood#PROPERTY}, {@link Wood#of}. */
    public static final WoodSkin WOOD = new WoodSkin(Identifier.fromNamespaceAndPath("servus_foundation", "wood"),
            Wood.BOARD, Wood.PROPERTY, Wood.DEFAULT, Wood::of, true);

    private final Identifier itemModelType;
    private final Identifier board;
    private final ModelProperty<Identifier> property;
    private final Identifier fallback;
    private final Function<ItemStack, Identifier> itemWood;
    private final boolean swaps;
    private final MapCodec<WoodItemModel.Unbaked> itemCodec;

    /**
     * @param itemModelType the item model type id item definitions name
     * @param swaps         whether placed blocks also read {@link Wood#SWAPS}
     */
    public WoodSkin(Identifier itemModelType, Identifier board, ModelProperty<Identifier> property, Identifier fallback,
                    Function<ItemStack, Identifier> itemWood, boolean swaps) {
        this.itemModelType = itemModelType;
        this.board = board;
        this.property = property;
        this.fallback = fallback;
        this.itemWood = itemWood;
        this.swaps = swaps;
        this.itemCodec = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("model").forGetter(WoodItemModel.Unbaked::model))
                .apply(i, model -> new WoodItemModel.Unbaked(model, this)));
    }

    public Identifier itemModelType() {
        return itemModelType;
    }

    public Identifier board() {
        return board;
    }

    public ModelProperty<Identifier> property() {
        return property;
    }

    public Identifier fallback() {
        return fallback;
    }

    public Identifier itemWood(ItemStack stack) {
        return itemWood.apply(stack);
    }

    public boolean swaps() {
        return swaps;
    }

    /** The codec to register for {@link #itemModelType()} in {@code RegisterItemModelsEvent}. */
    public MapCodec<WoodItemModel.Unbaked> itemCodec() {
        return itemCodec;
    }
}
