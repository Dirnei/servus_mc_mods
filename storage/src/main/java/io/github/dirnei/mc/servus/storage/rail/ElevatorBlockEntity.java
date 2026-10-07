package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connector node: joins the warehouse graph but holds no stock. The lowest
 * segment of a shaft also carries where its lift car is parked, so a player
 * who loads the chunk later sees the car there. That height is never saved.
 */
public class ElevatorBlockEntity extends StorageNodeBlockEntity {

    private static final String LIFT_Y = "LiftY";

    private float liftY = Float.NaN;

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.ELEVATOR.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.ELEVATOR;
    }

    /** Where the shaft's car is parked, or NaN when unknown. */
    public float liftY() {
        return liftY;
    }

    /** Server: the shaft's car parked at that height. */
    public void parkLift(float y) {
        liftY = y;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!Float.isNaN(liftY)) {
            tag.putFloat(LIFT_Y, liftY);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(LIFT_Y)) {
            liftY = tag.getFloat(LIFT_Y);
        }
    }
}
