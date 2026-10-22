package io.github.dirnei.mc.servus.foundation.material;

import java.util.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The material of one block entity. The owner forwards its component, NBT,
 * update tag and model data calls here.
 */
public final class MaterialSlot {

    private static final String KEY = "Material";

    private final BlockEntity owner;
    private @Nullable Identifier material;

    public MaterialSlot(BlockEntity owner) {
        this.owner = owner;
    }

    /** The material shown, the default of the owner block's kind when none is set. */
    public Identifier material() {
        return Material.or(material, Material.kindOf(owner.getBlockState().getBlock()));
    }

    /** The material as set, null when none. */
    public @Nullable Identifier raw() {
        return material;
    }

    public void set(@Nullable Identifier id) {
        material = id;
        owner.setChanged();
    }

    /** From {@code applyImplicitComponents}, with {@code input.get(MaterialContent.MATERIAL.get())}: the placed item's material. */
    public void apply(@Nullable Identifier id) {
        material = id;
    }

    /** From {@code collectImplicitComponents}: the material goes with drops. */
    public void collect(DataComponentMap.Builder builder) {
        if (material != null) {
            builder.set(MaterialContent.MATERIAL.get(), material);
        }
    }

    /** From {@code removeComponentsFromTag}. */
    public static void removeFromTag(ValueOutput output) {
        output.discard(KEY);
    }

    /** From {@code saveAdditional} and {@code getUpdateTag}. */
    public void save(ValueOutput output) {
        if (material != null) {
            output.putString(KEY, material.toString());
        }
    }

    /** For update tags built by hand; read back by {@link #load}. */
    public void save(CompoundTag tag) {
        if (material != null) {
            tag.putString(KEY, material.toString());
        }
    }

    /** From {@code loadAdditional}; a changed material redraws the block on the client. */
    public void load(ValueInput input) {
        Identifier loaded = input.getString(KEY).map(Identifier::tryParse).orElse(null);
        boolean changed = !Objects.equals(loaded, material);
        material = loaded;
        Level level = owner.getLevel();
        if (changed && level != null && level.isClientSide()) {
            owner.requestModelDataUpdate();
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** From {@code getModelData}. */
    public ModelData modelData() {
        return ModelData.builder().with(Material.PROPERTY, material()).build();
    }
}
