package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.upgrade.AnchorUpgradeSlots;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.site.SiteCapacity;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteName;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.site.WakeMachine;
import io.github.dirnei.mc.servus.storage.site.WirelessCost;
import io.github.dirnei.mc.servus.storage.site.WirelessReach;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.model.data.ModelData;

/** Exactly one per network makes the warehouse online. Shuttles live in garages. */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity implements WoodHolder, MenuProvider {

    private final WoodSlot woodSlot = new WoodSlot(this);

    /** Shuttles an older save kept in the controller; dropped as items on load. */
    private int legacyShuttles;
    private final StockRules rules = new StockRules();

    private String siteName = "";
    private UUID team;
    private final Set<Long> span = new LinkedHashSet<>();
    private final AnchorUpgradeSlots upgrades =
            new AnchorUpgradeSlots(StorageStatsManager.INSTANCE.anchorUpgradeSlots(), this::upgradesChanged);
    /** The Wireless Upgrade slot: with it, and powered, the controller is the site's antenna. */
    private final WirelessUpgradeSlots wireless = new WirelessUpgradeSlots(this::wirelessUpgradeChanged);
    /** The site's FE buffer, filled through the energy capability, spent by the controller. */
    private final ControllerEnergy energy = new ControllerEnergy(StorageStatsManager.INSTANCE.controllerEnergyCapacity(), this::energyChanged);
    /** Whether the controller was wireless capable at the last upgrade or energy change; a flip tells the warehouse. */
    private boolean wasCapable;
    /** Whether the controller was powered for wireless at the last energy change; the site record keeps it. */
    private boolean wasPowered;
    /** For GameTests only, not saved: draws of shuttles, wakes and stations are paid without taking FE. */
    private boolean freeDraws;
    /** Whether the controller was in the no power state when last looked at; a flip refreshes the lamp. */
    private boolean wasNoPower = true;
    /** Set when the chunk unloads, so the removal that follows keeps the site's record. */
    private boolean unloading;

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
        rules.onChanged(this::setChanged);
        span.add(homeChunk());
    }

    /** The name the player gave the site, empty when unnamed. */
    public String siteName() {
        return siteName;
    }

    /** The name to show: the player's name or "Warehouse at x, z". */
    public String displayName() {
        return SiteName.display(siteName, worldPosition.getX(), worldPosition.getZ());
    }

    /** Sets the name after cleaning it; returns the kept name. */
    public String setSiteName(String typed) {
        String cleaned = SiteName.clean(typed);
        if (!cleaned.equals(siteName)) {
            siteName = cleaned;
            setChanged();
            syncRecord();
        }
        return siteName;
    }

    /** The team of the player who placed the controller, empty when placed by no player. */
    public Optional<UUID> team() {
        return Optional.ofNullable(team);
    }

    public void setTeam(UUID team) {
        this.team = team;
        setChanged();
        syncRecord();
    }

    /** Writes the site's record; only on the server and only while the controller is in the world. */
    private void syncRecord() {
        if (level instanceof ServerLevel server && !isRemoved()) {
            SiteRegistry.of(server).update(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        if (level instanceof ServerLevel server) {
            SiteRegistry.of(server).updateEnergy(this);
        }
        super.onChunkUnloaded();
    }

    /** A broken or dismantled controller takes its site with it; an unloading one keeps it. */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && !unloading && server.isLoaded(worldPosition)
                && !(server.getBlockState(worldPosition).getBlock() instanceof WarehouseControllerBlock)) {
            SiteRegistry.of(server).remove(server.dimension(), worldPosition);
            SiteWakes.forget(server, worldPosition);
        }
        super.setRemoved();
    }

    /** The key of the chunk the controller stands in. */
    public long homeChunk() {
        return SiteChunks.ofBlock(worldPosition.getX(), worldPosition.getZ());
    }

    /** The chunks the site spans, kept by the network from its events. */
    public Set<Long> span() {
        return Collections.unmodifiableSet(span);
    }

    /** Takes a new span from the network. */
    public void setSpan(Set<Long> chunks) {
        if (span.equals(chunks)) {
            return;
        }
        span.clear();
        span.addAll(chunks);
        span.add(homeChunk());
        setChanged();
        refreshSiteLamp();
        syncRecord();
    }

    /** A placed controller starts with its own chunk; a span carried in an item would point at the old place. */
    public void resetSpan() {
        setSpan(Set.of());
    }

    public AnchorUpgradeSlots upgrades() {
        return upgrades;
    }

    /** Chunks the upgrades cover. */
    public int capacity() {
        return SiteCapacity.capacity(upgrades.count(), StorageStatsManager.INSTANCE.chunksPerUpgrade());
    }

    /** Whether the site's upgrades cover its span. */
    public SiteCapacity.State siteState() {
        return SiteCapacity.state(span.size(), upgrades.count(), StorageStatsManager.INSTANCE.chunksPerUpgrade());
    }

    /** The wake line of the GUI; empty while nobody wakes the site. */
    public Component wakeLine() {
        if (level == null) {
            return Component.empty();
        }
        return SiteWakes.line(level.dimension(), worldPosition)
                .orElse(Component.empty());
    }

    /** Whether the wake line explains a problem: the site waits to wake. */
    public boolean wakeAlert() {
        return level != null && SiteWakes.status(level.dimension(), worldPosition)
                .map(status -> status.state() == WakeMachine.State.WAITING).orElse(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(displayName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WarehouseControllerMenu(containerId, playerInventory, this);
    }

    public WirelessUpgradeSlots wirelessUpgrade() {
        return wireless;
    }

    /** Whether the controller holds a Wireless Upgrade. */
    public boolean hasWirelessUpgrade() {
        return wireless.held();
    }

    public ControllerEnergy energy() {
        return energy;
    }

    /** FE stored. */
    public int energyStored() {
        return energy.stored();
    }

    /** For GameTests and tools: sets the FE stored. */
    public void setEnergy(int amount) {
        energy.setStored(amount);
    }

    /** Spends FE from the buffer, for wireless transfers this controller receives. */
    public void spendEnergy(long amount) {
        energy.spend(gameTime(), amount);
    }

    /** A wireless transfer found nothing it could pay: counts as a refused draw. */
    public void refuseEnergy() {
        energy.refused(gameTime());
        powerStateChanged();
    }

    /**
     * Draws FE for one of the warehouse's consumers: shuttles, wakes, Crafting
     * Stations. Paid in full or not at all.
     *
     * @return whether the draw was paid
     */
    public boolean drawEnergy(long amount) {
        if (freeDraws) {
            return true;
        }
        boolean paid = energy.draw(gameTime(), amount);
        if (!paid) {
            powerStateChanged();
        }
        return paid;
    }

    /** Whether the warehouse is in the no power state: an empty buffer or a draw refused during the last second. */
    public boolean noPower() {
        return energy.noPower(gameTime());
    }

    /** FE drawn during the last second. */
    public long usedLastSecond() {
        return energy.usedLastSecond(gameTime());
    }

    /**
     * For GameTests that count other FE (wireless): shuttles, wakes and
     * Crafting Stations of this controller draw nothing. Not saved.
     */
    public void setFreeDrawsForTests(boolean free) {
        freeDraws = free;
    }

    /** Once per tick from the warehouse: a no power state whose second has passed ends here. */
    public void tickPower() {
        powerStateChanged();
    }

    private long gameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    /** Compares the no power state with the last one; a flip refreshes the lamp. */
    private void powerStateChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean now = noPower();
        if (now != wasNoPower) {
            wasNoPower = now;
            refreshSiteLamp();
        }
    }

    /** Whether the controller holds the FE of one item at the smallest wireless cost. */
    public boolean poweredForWireless() {
        return WirelessCost.powered(energy.stored(), StorageStatsManager.INSTANCE.wirelessFePerItem());
    }

    /**
     * Whether the controller is the site's antenna: it holds a Wireless Upgrade
     * and is powered. A site whose controller is not capable neither sends nor receives.
     */
    public boolean capable() {
        return WirelessReach.capable(hasWirelessUpgrade(), energy.stored(),
                StorageStatsManager.INSTANCE.wirelessFePerItem());
    }

    /** A dismantled or newly placed controller carries no FE in its item. */
    public void clearEnergyForItem() {
        energy.load(0);
        wasCapable = capable();
        wasPowered = poweredForWireless();
    }

    private void energyChanged() {
        setChanged();
        wirelessStateChanged();
        powerStateChanged();
    }

    private void wirelessUpgradeChanged() {
        setChanged();
        wasCapable = capable();
        tellNetworkWireless();
    }

    /** Compares the capable state with the last one; on a flip the warehouse and the site record learn it. */
    private void wirelessStateChanged() {
        boolean now = capable();
        boolean powered = poweredForWireless();
        if (now == wasCapable && powered == wasPowered) {
            return;
        }
        wasCapable = now;
        wasPowered = powered;
        tellNetworkWireless();
    }

    private void tellNetworkWireless() {
        if (level != null && !level.isClientSide()) {
            network().ifPresent(WarehouseNetwork::wirelessChanged);
        }
    }

    private void upgradesChanged() {
        setChanged();
        refreshSiteLamp();
        syncRecord();
    }

    /** What the activity lamp shows: red over capacity or without power, orange while awake, green otherwise. */
    protected StorageNodeBlocks.Activity lampActivity() {
        if (siteState() == SiteCapacity.State.OVER || noPower()) {
            return StorageNodeBlocks.Activity.BLOCKED;
        }
        boolean awake = level != null && SiteWakes.isAwake(level.dimension(), worldPosition);
        return awake ? StorageNodeBlocks.Activity.WORKING : StorageNodeBlocks.Activity.IDLE;
    }

    /** Writes the activity lamp into the block state when it changed. */
    public void refreshSiteLamp() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        StorageNodeBlocks.Activity activity = lampActivity();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity
                && level.getBlockState(worldPosition).getBlock() == state.getBlock()) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity), Block.UPDATE_CLIENTS);
        }
    }

    /** The stock rules of this warehouse. */
    public StockRules rules() {
        return rules;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        unloading = false;
        wasNoPower = noPower();
        refreshSiteLamp();
        syncRecord();
        if (legacyShuttles > 0 && level instanceof ServerLevel) {
            Block.popResource(level, worldPosition, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get(), legacyShuttles));
            legacyShuttles = 0;
            setChanged();
        }
    }

    private Optional<WarehouseNetwork> online() {
        return network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE);
    }

    /**
     * The Foundation stock provider view: counts from the index of the racks.
     * Taking gives nothing, since items leave the warehouse only by shuttle
     * through a Packing Station. Offline it has nothing.
     */
    public StockProvider stockProvider() {
        return new StockProvider() {
            @Override
            public long count(ItemStack like) {
                return like.isEmpty() ? 0 : online().map(network -> network.index().count(ItemKey.of(like))).orElse(0L);
            }

            @Override
            public ItemStack extract(ItemStack like, int amount) {
                return ItemStack.EMPTY;
            }
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        tag.put("StockRules", rules.save(registries));
        output.putString("SiteName", siteName);
        if (team != null) {
            tag.store("Team", UUIDUtil.CODEC, team);
        }
        tag.putLongArray("Span", span.stream().mapToLong(Long::longValue).toArray());
        tag.put("AnchorUpgrades", upgrades.serializeNBT(registries));
        tag.put("WirelessUpgrade", wireless.serializeNBT(registries));
        output.putInt("Energy", energy.stored());
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        legacyShuttles = input.getIntOr("Shuttles", 0);
        if (tag.contains("StockRules")) {
            rules.load(tag.getCompoundOrEmpty("StockRules"), registries);
        }
        siteName = SiteName.clean(input.getStringOr("SiteName", ""));
        team = tag.read("Team", UUIDUtil.CODEC).orElse(null);
        span.clear();
        for (long key : tag.getLongArray("Span").orElse(new long[0])) {
            span.add(key);
        }
        span.add(homeChunk());
        if (tag.contains("AnchorUpgrades")) {
            AnchorUpgradeSlots saved = new AnchorUpgradeSlots(0, null);
            saved.deserializeNBT(registries, tag.getCompoundOrEmpty("AnchorUpgrades"));
            for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                upgrades.setStackInSlot(slot, slot < saved.getSlots() ? saved.getStackInSlot(slot) : ItemStack.EMPTY);
            }
        }
        if (tag.contains("WirelessUpgrade")) {
            WirelessUpgradeSlots saved = new WirelessUpgradeSlots(null);
            saved.deserializeNBT(registries, tag.getCompoundOrEmpty("WirelessUpgrade"));
            wireless.setStackInSlot(0, saved.getSlots() > 0 ? saved.getStackInSlot(0) : ItemStack.EMPTY);
        }
        energy.load(input.getIntOr("Energy", 0));
        wasCapable = capable();
        wasPowered = poweredForWireless();
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            dropUpgrades(level, pos);
        }
    }

    /** Drops the Anchor and Wireless Upgrades at the position. */
    public void dropUpgrades(Level level, BlockPos pos) {
        upgrades.takeAll().forEach(stack -> Block.popResource(level, pos, stack));
        wireless.takeAll().forEach(stack -> Block.popResource(level, pos, stack));
    }
}
