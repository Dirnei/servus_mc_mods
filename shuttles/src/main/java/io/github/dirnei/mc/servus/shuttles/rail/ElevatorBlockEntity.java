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
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
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
public class ElevatorBlockEntity extends StorageNodeBlockEntity implements MenuProvider, WoodHolder {

    private static final String LIFT_Y = "LiftY";
    private static final String UPGRADES = "Upgrades";
    private static final String REPLACED_RAIL = "ReplacedRail";
    private static final String RAIL_WOOD = "RailWood";

    private float liftY = Float.NaN;
    private boolean loading;
    private final WoodSlot woodSlot = new WoodSlot(this);
    /** The segment took the place of a Warehouse Rail, which comes back when the segment goes. */
    private boolean replacedRail;
    private @Nullable Identifier railWood;
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
        woodSlot.save(tag);
        if (!Float.isNaN(liftY)) {
            tag.putFloat(LIFT_Y, liftY);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The segment stands where a Warehouse Rail with that sleeper wood lay; the rail comes back when it goes. */
    public void rememberRail(@Nullable Identifier wood) {
        replacedRail = true;
        railWood = wood;
        setChanged();
    }

    public boolean replacedRail() {
        return replacedRail;
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put(UPGRADES, upgrades.serializeNBT(registries));
        woodSlot.save(tag);
        if (replacedRail) {
            tag.putBoolean(REPLACED_RAIL, true);
            if (railWood != null) {
                tag.putString(RAIL_WOOD, railWood.toString());
            }
        }
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        replacedRail = tag.getBooleanOr(REPLACED_RAIL, false);
        railWood = tag.getString(RAIL_WOOD).map(Identifier::tryParse).orElse(null);
        if (tag.contains(LIFT_Y)) {
            liftY = input.getFloatOr(LIFT_Y, 0F);
        }
        if (tag.contains(UPGRADES)) {
            loading = true;
            upgrades.fillFrom(registries, tag.getCompoundOrEmpty(UPGRADES));
            loading = false;
        }
    }

    /**
     * When the block is removed, its upgrades go to the heir segment of the shaft, or drop,
     * and a rail it replaced is put back once the removal is done.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (replacedRail && level instanceof ServerLevel server) {
            replacedRail = false;
            Identifier wood = railWood;
            MinecraftServer minecraft = server.getServer();
            // After the removal: the level still sends the segment's own neighbour updates as air.
            minecraft.schedule(new TickTask(minecraft.getTickCount(), () -> ElevatorBlock.putRailBack(server, pos, wood)));
        }
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
