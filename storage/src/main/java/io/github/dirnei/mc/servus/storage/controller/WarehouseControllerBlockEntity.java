package io.github.dirnei.mc.servus.storage.controller;

import net.minecraft.world.level.Level;
import net.minecraft.core.UUIDUtil;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.production.StockSink;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.site.SiteCapacity;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteName;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.model.data.ModelData;

/** Exactly one per network makes the warehouse online. Shuttles live in garages. */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity implements WoodHolder, net.minecraft.world.MenuProvider {

    private final WoodSlot woodSlot = new WoodSlot(this);

    /** Shuttles saved by an older build, dropped as items on the first load. */
    private int legacyShuttles;
    private final StockRules rules = new StockRules();

    // The site: its name, its team, the chunks it spans and the upgrades that let it wake.
    private String siteName = "";
    private UUID team;
    private final Set<Long> span = new LinkedHashSet<>();
    private final ChunkLoaderUpgradeSlots upgrades =
            new ChunkLoaderUpgradeSlots(StorageStatsManager.INSTANCE.chunkLoaderUpgradeSlots(), this::upgradesChanged);
    /** Set when the chunk unloads, so the removal that follows keeps the site's record. */
    private boolean unloading;

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
        rules.onChanged(this::setChanged);
        span.add(homeChunk());
    }

    // Site

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
            io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(server).update(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        super.onChunkUnloaded();
    }

    /** A broken or dismantled controller takes its site with it; an unloading one keeps it. */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && !unloading && server.isLoaded(worldPosition)
                && !(server.getBlockState(worldPosition).getBlock() instanceof WarehouseControllerBlock)) {
            io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(server).remove(server.dimension(), worldPosition);
            io.github.dirnei.mc.servus.storage.site.SiteWakes.forget(server, worldPosition);
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
        span.clear();
        span.add(homeChunk());
        setChanged();
        refreshSiteLamp();
        syncRecord();
    }

    public ChunkLoaderUpgradeSlots upgrades() {
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
    public net.minecraft.network.chat.Component wakeLine() {
        if (level == null) {
            return net.minecraft.network.chat.Component.empty();
        }
        return io.github.dirnei.mc.servus.storage.site.SiteWakes.line(level.dimension(), worldPosition)
                .orElse(net.minecraft.network.chat.Component.empty());
    }

    /** Whether the wake line explains a problem: the site waits to wake. */
    public boolean wakeAlert() {
        return level != null && io.github.dirnei.mc.servus.storage.site.SiteWakes.status(level.dimension(), worldPosition)
                .map(status -> status.state() == io.github.dirnei.mc.servus.storage.site.WakeMachine.State.WAITING).orElse(false);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal(displayName());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory,
                                                                         net.minecraft.world.entity.player.Player player) {
        return new io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu(containerId, playerInventory, this);
    }

    private void upgradesChanged() {
        setChanged();
        refreshSiteLamp();
        syncRecord();
    }

    /** What the activity lamp shows: red over capacity, green otherwise. */
    protected StorageNodeBlocks.Activity lampActivity() {
        if (siteState() == SiteCapacity.State.OVER) {
            return StorageNodeBlocks.Activity.BLOCKED;
        }
        boolean awake = level != null && io.github.dirnei.mc.servus.storage.site.SiteWakes.isAwake(level.dimension(), worldPosition);
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

    /** The Lib stock provider view: counts from the index, instant extracts. Offline it has nothing. */
    public StockProvider stockProvider() {
        return new StockProvider() {
            @Override
            public long count(ItemStack like) {
                return like.isEmpty() ? 0 : online().map(network -> network.index().count(ItemKey.of(like))).orElse(0L);
            }

            @Override
            public ItemStack extract(ItemStack like, int amount) {
                if (like.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                return online().map(network -> network.extract(ItemKey.of(like), amount)).orElse(ItemStack.EMPTY);
            }
        };
    }

    /** The Lib stock sink view: inserts by the warehouse's zone routing. Offline it takes nothing. */
    public StockSink sink() {
        return stack -> online().map(network -> network.insert(stack)).orElse(stack);
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
        tag.put("ChunkLoaderUpgrades", upgrades.serializeNBT(registries));
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
        team = tag.read("Team", UUIDUtil.CODEC).isPresent() ? tag.read("Team", UUIDUtil.CODEC).orElseThrow() : null;
        span.clear();
        for (long key : tag.getLongArray("Span").orElse(new long[0])) {
            span.add(key);
        }
        span.add(homeChunk());
        if (tag.contains("ChunkLoaderUpgrades")) {
            ChunkLoaderUpgradeSlots saved = new ChunkLoaderUpgradeSlots(0, null);
            saved.deserializeNBT(registries, tag.getCompoundOrEmpty("ChunkLoaderUpgrades"));
            for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                upgrades.setStackInSlot(slot, slot < saved.getSlots() ? saved.getStackInSlot(slot) : ItemStack.EMPTY);
            }
        }
    }

    // Wood: the planks the block was crafted from, shown as its boards

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

    /** Moved from the block's removal hook (26.1): runs on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        WarehouseControllerBlockEntity controller = this;
        controller.upgrades().takeAll().forEach(stack -> Block.popResource(level, pos, stack));
    }
}
