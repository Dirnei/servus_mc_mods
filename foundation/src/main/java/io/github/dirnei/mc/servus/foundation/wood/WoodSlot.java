package io.github.dirnei.mc.servus.foundation.wood;

import java.util.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The wood of one block entity. The owner forwards its component, NBT,
 * update tag and model data calls here.
 */
public final class WoodSlot {

    private static final String KEY = "Wood";

    private final BlockEntity owner;
    private @Nullable Identifier wood;

    public WoodSlot(BlockEntity owner) {
        this.owner = owner;
    }

    /** The wood shown, oak when none is set. */
    public Identifier wood() {
        return Wood.or(wood);
    }

    /** The wood as set, null when none. */
    public @Nullable Identifier raw() {
        return wood;
    }

    public void set(@Nullable Identifier id) {
        wood = id;
        owner.setChanged();
    }

    /** From {@code applyImplicitComponents}, with {@code input.get(WoodContent.WOOD.get())}: the placed item's wood. */
    public void apply(@Nullable Identifier id) {
        wood = id;
    }

    /** From {@code collectImplicitComponents}: the wood goes with drops. */
    public void collect(DataComponentMap.Builder builder) {
        if (wood != null) {
            builder.set(WoodContent.WOOD.get(), wood);
        }
    }

    /** From {@code removeComponentsFromTag}. */
    public static void removeFromTag(ValueOutput output) {
        output.discard(KEY);
    }

    /** From {@code saveAdditional} and {@code getUpdateTag}. */
    public void save(ValueOutput output) {
        if (wood != null) {
            output.putString(KEY, wood.toString());
        }
    }

    /** For update tags built by hand; read back by {@link #load}. */
    public void save(net.minecraft.nbt.CompoundTag tag) {
        if (wood != null) {
            tag.putString(KEY, wood.toString());
        }
    }

    /** From {@code loadAdditional}; a changed wood redraws the block on the client. */
    public void load(ValueInput input) {
        Identifier loaded = input.getString(KEY).map(Identifier::tryParse).orElse(null);
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
