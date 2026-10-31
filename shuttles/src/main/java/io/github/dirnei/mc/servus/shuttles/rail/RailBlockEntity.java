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
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
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
 * wood of its sleepers and, on a farm rail, its corrosion stage and scrape
 * progress (see {@link FieldCorrosion}), both carried by its dropped item.
 */
public class RailBlockEntity extends StorageNodeBlockEntity implements MaterialHolder {

    private static final String CORROSION = "Corrosion";
    private static final String SCRAPE_PROGRESS = "ScrapeProgress";
    private static final Identifier FRESH_HEAD = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/farm_rail_head");

    private final MaterialSlot materialSlot = new MaterialSlot(this);
    private int corrosion = FieldCorrosion.COPPER;
    /** Durability points a scrape already put into the next stage back. */
    private int scrapeProgress;

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
        output.putInt(SCRAPE_PROGRESS, scrapeProgress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        materialSlot.load(input);
        int loaded = FieldCorrosion.clamp(tag.contains(CORROSION) ? input.getIntOr(CORROSION, 0) : FieldCorrosion.COPPER);
        scrapeProgress = Math.max(0, input.getIntOr(SCRAPE_PROGRESS, 0));
        boolean changed = loaded != corrosion;
        corrosion = loaded;
        if (changed && level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The corrosion stage. */
    public int corrosion() {
        return corrosion;
    }

    /** The durability points a scrape put toward the next stage back. */
    public int scrapeProgress() {
        return scrapeProgress;
    }

    /** Sets the stage and clears the scrape progress; sends the stage to watching clients when it changed. Server side. */
    public void setCorrosion(int stage) {
        setCorrosion(stage, 0);
    }

    /** Sets the stage and scrape progress; sends them to watching clients when the stage changed. Server side. */
    public void setCorrosion(int stage, int progress) {
        int clamped = FieldCorrosion.clamp(stage);
        int held = Math.max(0, progress);
        if (clamped == corrosion && held == scrapeProgress) {
            return;
        }
        boolean stageChanged = clamped != corrosion;
        corrosion = clamped;
        scrapeProgress = held;
        setChanged();
        if (!stageChanged) {
            return;
        }
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
        tag.putInt(SCRAPE_PROGRESS, scrapeProgress);
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
        RailCorrosion carried = input.get(ServusShuttlesComponents.RAIL_CORROSION.get());
        if (carried != null) {
            corrosion = FieldCorrosion.clamp(carried.stage());
            scrapeProgress = Math.max(0, carried.progress());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        materialSlot.collect(builder);
        if (corrosion > FieldCorrosion.COPPER || scrapeProgress > 0) {
            builder.set(ServusShuttlesComponents.RAIL_CORROSION.get(), new RailCorrosion(corrosion, scrapeProgress));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        MaterialSlot.removeFromTag(output);
        output.discard(CORROSION);
        output.discard(SCRAPE_PROGRESS);
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
