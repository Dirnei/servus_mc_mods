package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.shuttle.LiftUpgrades;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Connector node: joins the warehouse graph but holds no stock. The lowest
 * segment of a shaft also carries where its lift car is parked, so a player
 * who loads the chunk later sees the car there. That height is never saved.
 * The lowest segment also keeps the shaft's Speed upgrades; every segment has
 * the slots, but the others stay empty.
 */
public class ElevatorBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    private static final String LIFT_Y = "LiftY";
    private static final String UPGRADES = "Upgrades";

    private float liftY = Float.NaN;
    private boolean loading;
    private final LiftUpgradeSlots upgrades = new LiftUpgradeSlots(ShuttlesStatsManager.INSTANCE.liftUpgradeSlots(), this::upgradesChanged);

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.ELEVATOR.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.ELEVATOR;
    }

    /** The shaft's Speed upgrades; only the lowest segment holds any. */
    public LiftUpgradeSlots upgrades() {
        return upgrades;
    }

    /** How many Speed upgrades the slots hold. */
    public int upgradeCount() {
        int count = 0;
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            if (!upgrades.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void upgradesChanged() {
        if (loading) {
            return;
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            network().ifPresent(network -> network.post(new WarehouseEvent.LiftUpgradesChanged(worldPosition)));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElevatorMenu(containerId, playerInventory, this);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put(UPGRADES, upgrades.serializeNBT(registries));
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        if (tag.contains(LIFT_Y)) {
            liftY = input.getFloatOr(LIFT_Y, 0F);
        }
        if (tag.contains(UPGRADES)) {
            loading = true;
            upgrades.fillFrom(registries, tag.getCompoundOrEmpty(UPGRADES));
            loading = false;
        }
    }

    /** When the block is removed, its upgrades go to the heir segment of the shaft, or drop. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null || upgradeCount() <= 0) {
            return;
        }
        List<ItemStack> items = upgrades().takeAll();
        OptionalInt heir = LiftUpgrades.heirOnRemove(pos.getY(), ElevatorBlock.isSegment(level, pos));
        BlockPos heirPos = heir.isPresent() ? pos.atY(heir.getAsInt()) : null;
        if (heirPos != null && level.getBlockState(heirPos).canSurvive(level, heirPos)
                && level.getBlockEntity(heirPos) instanceof ElevatorBlockEntity next) {
            items = next.upgrades().insertAll(items);
        }
        items.forEach(stack -> Block.popResource(level, pos, stack));
    }
}
