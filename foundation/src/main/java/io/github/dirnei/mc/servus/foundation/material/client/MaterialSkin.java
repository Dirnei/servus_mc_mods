package io.github.dirnei.mc.servus.foundation.material.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jspecify.annotations.Nullable;

/**
 * How a family of blocks shows its material: the placeholder texture its
 * models use, the model data property and fallback that name the material of
 * a placed block, and how an item stack names its material.
 * {@link MaterialModel} and {@link MaterialItemModel} retexture placeholder
 * quads with it. Foundation's material blocks use {@link #of} per kind; other
 * mods (the rack) bring their own skin.
 */
public final class MaterialSkin {

    /** The item model type of Foundation's material blocks; its JSON names the kind. */
    public static final Identifier ITEM_MODEL_TYPE = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "material");

    /** {@code {"type": "servus_foundation:material", "model": "<model>", "kind": "wood"}}. */
    public static final MapCodec<MaterialItemModel.Unbaked> KIND_ITEM_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Identifier.CODEC.fieldOf("model").forGetter(MaterialItemModel.Unbaked::model),
                    MaterialKind.CODEC.fieldOf("kind").forGetter(unbaked -> unbaked.skin().kind()))
            .apply(i, (model, kind) -> new MaterialItemModel.Unbaked(model, of(kind))));

    private static final Map<MaterialKind, MaterialSkin> BY_KIND = new EnumMap<>(MaterialKind.class);

    static {
        for (MaterialKind kind : MaterialKind.values()) {
            BY_KIND.put(kind, new MaterialSkin(kind));
        }
    }

    private final Identifier itemModelType;
    private final Identifier placeholder;
    private final ModelProperty<Identifier> property;
    private final Identifier fallback;
    private final Function<ItemStack, Identifier> itemMaterial;
    private final boolean swaps;
    private final @Nullable MaterialKind kind;
    private final MapCodec<MaterialItemModel.Unbaked> itemCodec;

    private MaterialSkin(MaterialKind kind) {
        this.itemModelType = ITEM_MODEL_TYPE;
        this.placeholder = kind.placeholder();
        this.property = Material.PROPERTY;
        this.fallback = kind.defaultBlock();
        this.itemMaterial = Material::of;
        this.swaps = true;
        this.kind = kind;
        this.itemCodec = KIND_ITEM_CODEC;
    }

    /**
     * A skin with its own item model type, whose JSON holds only the model.
     *
     * @param itemModelType the item model type id item definitions name
     * @param swaps         whether placed blocks also read {@link Material#SWAPS}
     */
    public MaterialSkin(Identifier itemModelType, Identifier placeholder, ModelProperty<Identifier> property, Identifier fallback,
                        Function<ItemStack, Identifier> itemMaterial, boolean swaps) {
        this.itemModelType = itemModelType;
        this.placeholder = placeholder;
        this.property = property;
        this.fallback = fallback;
        this.itemMaterial = itemMaterial;
        this.swaps = swaps;
        this.kind = null;
        this.itemCodec = RecordCodecBuilder.mapCodec(i -> i.group(
                        Identifier.CODEC.fieldOf("model").forGetter(MaterialItemModel.Unbaked::model))
                .apply(i, model -> new MaterialItemModel.Unbaked(model, this)));
    }

    /** The skin of Foundation's material blocks of a kind. */
    public static MaterialSkin of(MaterialKind kind) {
        return BY_KIND.get(kind);
    }

    public Identifier itemModelType() {
        return itemModelType;
    }

    public Identifier placeholder() {
        return placeholder;
    }

    public ModelProperty<Identifier> property() {
        return property;
    }

    public Identifier fallback() {
        return fallback;
    }

    public Identifier itemMaterial(ItemStack stack) {
        return itemMaterial.apply(stack);
    }

    public boolean swaps() {
        return swaps;
    }

    /** The kind of a Foundation skin, null for a skin of its own. */
    public @Nullable MaterialKind kind() {
        return kind;
    }

    /** The codec to register for this skin's item model type in {@code RegisterItemModelsEvent}. */
    public MapCodec<MaterialItemModel.Unbaked> itemCodec() {
        return itemCodec;
    }
}
