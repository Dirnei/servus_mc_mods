package io.github.dirnei.mc.servus.foundation.wood;

import java.util.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The wood of one block entity. The owner forwards its component, NBT,
 * update tag and model data calls here.
 */
public final class WoodSlot {

    private static final String KEY = "Wood";

    private final BlockEntity owner;
    private @Nullable ResourceLocation wood;

    public WoodSlot(BlockEntity owner) {
        this.owner = owner;
    }

    /** The wood shown, oak when none is set. */
    public ResourceLocation wood() {
        return Wood.or(wood);
    }

    /** The wood as set, null when none. */
    public @Nullable ResourceLocation raw() {
        return wood;
    }

    public void set(@Nullable ResourceLocation id) {
        wood = id;
        owner.setChanged();
    }

    /** From {@code applyImplicitComponents}, with {@code input.get(WoodContent.WOOD.get())}: the placed item's wood. */
    public void apply(@Nullable ResourceLocation id) {
        wood = id;
    }

    /** From {@code collectImplicitComponents}: the wood goes with drops. */
    public void collect(DataComponentMap.Builder builder) {
        if (wood != null) {
            builder.set(WoodContent.WOOD.get(), wood);
        }
    }

    /** From {@code removeComponentsFromTag}. */
    public static void removeFromTag(CompoundTag tag) {
        tag.remove(KEY);
    }

    /** From {@code saveAdditional} and {@code getUpdateTag}. */
    public void save(CompoundTag tag) {
        if (wood != null) {
            tag.putString(KEY, wood.toString());
        }
    }

    /** From {@code loadAdditional}; a changed wood redraws the block on the client. */
    public void load(CompoundTag tag) {
        ResourceLocation loaded = tag.contains(KEY, Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString(KEY)) : null;
        boolean changed = !Objects.equals(loaded, wood);
        wood = loaded;
        Level level = owner.getLevel();
        if (changed && level != null && level.isClientSide()) {
            owner.requestModelDataUpdate();
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** From {@code getModelData}. */
    public ModelData modelData() {
        return ModelData.builder().with(Wood.PROPERTY, wood()).build();
    }
}
