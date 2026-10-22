package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.foundation.material.MaterialSlot;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Connector node: joins the warehouse graph but holds no stock. Remembers the
 * wood of its sleepers and, on a farm rail, the corrosion stage of its field
 * (see {@link FieldCorrosion}); the stage is never on the item.
 */
public class RailBlockEntity extends StorageNodeBlockEntity implements MaterialHolder {

    private static final String CORROSION = "Corrosion";
    private static final Identifier FRESH_HEAD = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/farm_rail_head");

    private final MaterialSlot materialSlot = new MaterialSlot(this);
    /** The corrosion stage of a farm rail, {@link FieldCorrosion#LOOSE} until it joins a field. */
    private int corrosion = FieldCorrosion.LOOSE;

    public RailBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.RAIL.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.RAIL;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        materialSlot.save(output);
        output.putInt(CORROSION, corrosion);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        materialSlot.load(input);
        int loaded = tag.contains(CORROSION) ? Math.max(FieldCorrosion.LOOSE, Math.min(FieldCorrosion.CORRODED, input.getIntOr(CORROSION, 0)))
                : FieldCorrosion.LOOSE;
        boolean changed = loaded != corrosion;
        corrosion = loaded;
        if (changed && level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The corrosion stage, {@link FieldCorrosion#LOOSE} for a rail in no field yet. */
    public int corrosion() {
        return corrosion;
    }

    /** Sets the stage of the rail's field and sends it to watching clients when it changed. Server side. */
    public void setCorrosion(int stage) {
        if (stage == corrosion) {
            return;
        }
        corrosion = stage;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public MaterialSlot materialSlot() {
        return materialSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        materialSlot.save(tag);
        tag.putInt(CORROSION, corrosion);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        materialSlot.apply(input.get(MaterialContent.MATERIAL.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        materialSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        MaterialSlot.removeFromTag(output);
    }

    /** The wood, and from exposed on the head texture of the stage in place of the fresh copper. */
    @Override
    public ModelData getModelData() {
        if (corrosion <= FieldCorrosion.COPPER) {
            return materialSlot.modelData();
        }
        Identifier stageHead = FRESH_HEAD.withSuffix("_" + FieldCorrosion.name(corrosion));
        return ModelData.builder().with(Material.PROPERTY, materialSlot.material()).with(Material.SWAPS, Map.of(FRESH_HEAD, stageHead)).build();
    }
}
