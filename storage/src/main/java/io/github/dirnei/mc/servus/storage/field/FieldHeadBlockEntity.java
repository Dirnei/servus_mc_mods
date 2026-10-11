package io.github.dirnei.mc.servus.storage.field;

import net.minecraft.world.Containers;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * Serves the field of the farm rail beside it: buffers water from a pipe,
 * keeps the field's farmland wet through NeoForge water tickets while the
 * water lasts, and knows which plots hold a ripe crop. The field, plots,
 * ripeness and tickets are rebuilt after a load or a change, never scanned
 * per tick; events from {@link FieldEvents} keep the ripeness current.
 * The block shows the wood of its recipe, the ripe share bar ({@link FieldHeadBlock#RIPE}),
 * the water level in steps and the crop; see {@link FieldHeadLook}.
 */
public class FieldHeadBlockEntity extends BlockEntity implements MenuProvider, WoodHolder {

    /** Items that hold a field's copper stage for one aging step. */
    public static final TagKey<Item> ANTI_CORROSION = ItemTags.create(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "anti_corrosion"));
    /** Items a Spreader doses fields with, each one bone meal application. */
    public static final TagKey<Item> FERTILIZER = ItemTags.create(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "fertilizer"));

    private final WoodSlot woodSlot = new WoodSlot(this);

    private final FluidSlot tank = new FluidSlot(StorageStatsManager.INSTANCE.fieldCapacity(), stack -> stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncWaterLevel();
        }
    };
    private final LegacyFluidHandler fillOnly = new FillOnly();
    private final ItemSlots crop = new ItemSlots(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            lookDirty = true;
            syncClients();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    /** Range Upgrades: each widens the field by one block, up to {@code max_radius}. */
    private final ItemSlots rangeUpgrades = new ItemSlots(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            requestRefresh();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ServusStorageItems.RANGE_UPGRADE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return Math.max(1, FieldRadius.maxUpgrades(StorageStatsManager.INSTANCE.fieldRadius(), StorageStatsManager.INSTANCE.fieldMaxRadius()));
        }
    };
    /** Anti-corrosion upkeep: one item holds the stage for one aging step. */
    private final ItemSlots antiCorrosion = new ItemSlots(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isAntiCorrosion(stack);
        }
    };
    private final LegacyItemHandler insertOnly = new InsertOnly();
    private final UpkeepHolder upkeep = new AntiCorrosionUpkeep();
    /** The corrosion stage of the served field; meaningful while {@link #stageKnown}. */
    private int stage = FieldCorrosion.COPPER;
    /** Whether the head decides its field's stage: it served the field before this refresh. */
    private boolean stageKnown;
    /** Hydration water counted toward the next aging step. */
    private long agedWater;
    /** Crop items from the registry, filled on first use; registries are frozen by then. */
    private static volatile List<Item> cropItems;
    private boolean harvestOnly;
    /** The ripe percent that calls a harvest; 0 follows {@code harvest_share} from {@code field.json}. */
    private int harvestPercent;
    /** The item a Spreader doses this field with, an item of {@link #FERTILIZER}; null for none. */
    private Item fertilizer;
    private FertilizeMode fertilizeMode = FertilizeMode.OFF;
    /** The ripe percent below which Until ripe share doses; 0 follows {@code fertilize_share}. */
    private int fertilizePercent;
    /** The Keep in stock reserve; -1 follows {@code fertilizer_reserve}. */
    private int reserve = -1;
    /** Plots planted since their last dose, learned from events; saved. */
    private final Set<BlockPos> unfed = new HashSet<>();
    /** Game time of the last call for a Spreader, -1 for none. */
    private long lastFertilizeCall = -1;

    // Rebuilt after a load or change, never saved.
    private FieldStatus status = FieldStatus.NO_FIELD;
    private Set<BlockPos> rails = Set.of();
    /** Every position within reach of a rail; events on these update {@link #ground}. */
    private Set<BlockPos> candidates = Set.of();
    /** The candidates the rails can see, see {@link FieldReach}; the field's plots. */
    private Set<BlockPos> plots = Set.of();
    /** What every loaded candidate holds, blocked ones included. */
    private final Map<BlockPos, PlotState> ground = new HashMap<>();
    private List<FieldStrip.Box> boxes = List.of();
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private final FieldRipeness<BlockPos> ripeness = new FieldRipeness<>();
    private final Set<BlockPos> rechecks = new HashSet<>();
    private final List<AABBTicket> tickets = new ArrayList<>();
    private boolean refreshPending = true;
    /** The ripe bar may have changed; the next server tick sets {@link FieldHeadBlock#RIPE}. */
    private boolean lookDirty = true;
    /** The water level last sent to clients, -1 before the first. */
    private int sentWaterLevel = -1;
    /** Client side: the water level from the last update. */
    private int clientWaterLevel;
    private boolean hydrated;
    private int countdown;

    public FieldHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.FIELD_HEAD.get(), pos, state);
    }

    // Lifecycle

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            FieldHeads.add(this);
            refreshPending = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            dropTickets();
            hydrated = false;
            FieldHeads.remove(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide()) {
            dropTickets();
            hydrated = false;
            FieldHeads.remove(this);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldHeadBlockEntity head) {
        head.tick();
    }

    private void tick() {
        int capacity = StorageStatsManager.INSTANCE.fieldCapacity();
        if (tank.getCapacity() != capacity) {
            tank.setCapacity(capacity);
            if (tank.getFluidAmount() > capacity) {
                tank.setFluid(tank.getFluid().copyWithAmount(capacity));
            }
            syncWaterLevel();
        }
        if (refreshPending) {
            refreshPending = false;
            refreshField();
        }
        if (lookDirty) {
            lookDirty = false;
            applyRipeStep();
        }
        if (status != FieldStatus.SERVING) {
            return;
        }
        if (!rechecks.isEmpty()) {
            for (BlockPos plot : rechecks) {
                recheckNow(plot);
            }
            rechecks.clear();
        }
        useWater();
    }

    // Field

    /** Looks at the field again on the next tick. */
    public void requestRefresh() {
        refreshPending = true;
    }

    /** The first farm rail beside the head, north, east, south, west. */
    private BlockPos startRail() {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = worldPosition.relative(direction);
            if (isFarmRail(neighbour)) {
                return neighbour;
            }
        }
        return null;
    }

    private boolean isFarmRail(BlockPos pos) {
        return level != null && level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof FarmRailBlock;
    }

    private Collection<BlockPos> farmNeighbours(BlockPos rail) {
        List<BlockPos> neighbours = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = rail.relative(direction);
            if (isFarmRail(neighbour)) {
                neighbours.add(neighbour);
            }
        }
        return neighbours;
    }

    private void refreshField() {
        if (level == null || level.isClientSide()) {
            return;
        }
        FieldStatus oldStatus = status;
        Set<BlockPos> oldRails = rails;
        BlockPos start = startRail();
        if (start == null) {
            unserve(FieldStatus.NO_FIELD, Set.of());
        } else {
            FieldGraph.Result<BlockPos> result = FieldGraph.collect(start, this::farmNeighbours, StorageStatsManager.INSTANCE.fieldMaxRails());
            if (result.tooLarge()) {
                unserve(FieldStatus.TOO_LARGE, Set.of());
            } else {
                Set<BlockPos> members = Collections.unmodifiableSet(result.members());
                List<FieldHeadBlockEntity> rivals = rivals(members);
                boolean serves = rivals.stream().allMatch(rival -> worldPosition.asLong() < rival.worldPosition.asLong());
                if (serves) {
                    serve(members);
                } else {
                    unserve(FieldStatus.OTHER_HEAD, members);
                }
                if (status != oldStatus || !members.equals(oldRails)) {
                    rivals.forEach(FieldHeadBlockEntity::requestRefresh);
                }
            }
        }
        setChanged();
    }

    /** Other loaded heads whose first farm rail lies in this field. */
    private List<FieldHeadBlockEntity> rivals(Set<BlockPos> members) {
        Set<FieldHeadBlockEntity> rivals = new LinkedHashSet<>();
        for (BlockPos rail : members) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbour = rail.relative(direction);
                if (!neighbour.equals(worldPosition) && level.isLoaded(neighbour)
                        && level.getBlockEntity(neighbour) instanceof FieldHeadBlockEntity other) {
                    BlockPos otherStart = other.startRail();
                    if (otherStart != null && members.contains(otherStart)) {
                        rivals.add(other);
                    }
                }
            }
        }
        return new ArrayList<>(rivals);
    }

    private void serve(Set<BlockPos> members) {
        status = FieldStatus.SERVING;
        rails = members;
        List<FieldPos> railPositions = new ArrayList<>(members.size());
        for (BlockPos rail : members) {
            railPositions.add(new FieldPos(rail.getX(), rail.getY(), rail.getZ()));
        }
        Set<BlockPos> candidateSet = new HashSet<>();
        for (FieldPos plot : FieldStrip.plots(railPositions, radius())) {
            candidateSet.add(new BlockPos(plot.x(), plot.y(), plot.z()));
        }
        candidates = Collections.unmodifiableSet(candidateSet);
        plots = Set.of();
        boxes = FieldStrip.boxes(railPositions, radius());
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;
        for (FieldStrip.Box box : boxes) {
            minX = Math.min(minX, box.minX());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX() - 1);
            maxZ = Math.max(maxZ, box.maxZ() - 1);
        }
        rechecks.clear();
        recheckAll();
        joinStage(members);
        if (hydrated) {
            dropTickets();
            addTickets();
        }
    }

    /**
     * The field takes the head's stage when the head served it before, else the
     * youngest stage its rails already have (copper when none); every rail
     * that differs is set.
     */
    private void joinStage(Set<BlockPos> members) {
        List<Integer> stages = new ArrayList<>(members.size());
        for (BlockPos rail : members) {
            if (level.getBlockEntity(rail) instanceof RailBlockEntity railEntity) {
                stages.add(railEntity.corrosion());
            }
        }
        int joined = FieldCorrosion.join(stageKnown ? stage : FieldCorrosion.LOOSE, stages);
        stageKnown = true;
        setStage(joined == FieldCorrosion.LOOSE ? FieldCorrosion.COPPER : joined);
    }

    private void unserve(FieldStatus why, Set<BlockPos> members) {
        status = why;
        rails = members;
        candidates = Set.of();
        plots = Set.of();
        ground.clear();
        boxes = List.of();
        ripeness.clear();
        rechecks.clear();
        dropTickets();
        hydrated = false;
        lookDirty = true;
        stageKnown = false;
        agedWater = 0;
    }

    // Water and hydration

    private void useWater() {
        if (stage == FieldCorrosion.CORRODED) {
            // Corroded rails no longer water the field: no water used, so no further aging.
            if (hydrated) {
                hydrated = false;
                dropTickets();
            }
            return;
        }
        int need = FieldWater.use(rails.size(), StorageStatsManager.INSTANCE.fieldWaterPerRail());
        if (!hydrated) {
            if (tank.getFluidAmount() >= need) {
                tank.drain(need, false);
                age(need);
                hydrated = true;
                countdown = StorageStatsManager.INSTANCE.fieldWaterInterval();
                addTickets();
            }
            return;
        }
        if (--countdown > 0) {
            return;
        }
        if (tank.getFluidAmount() >= need) {
            tank.drain(need, false);
            countdown = StorageStatsManager.INSTANCE.fieldWaterInterval();
            renewTickets();
            age(need);
        } else {
            hydrated = false;
            dropTickets();
        }
    }

    private void addTickets() {
        for (FieldStrip.Box box : boxes) {
            tickets.add(ticket(box));
        }
    }

    private AABBTicket ticket(FieldStrip.Box box) {
        return FarmlandWaterManager.addAABBTicket(level,
                new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
    }

    /** NeoForge drops a ticket with its chunk; the ones it dropped while the head stayed loaded come back. */
    private void renewTickets() {
        for (int i = 0; i < tickets.size() && i < boxes.size(); i++) {
            if (!tickets.get(i).isValid()) {
                tickets.set(i, ticket(boxes.get(i)));
            }
        }
    }

    private void dropTickets() {
        tickets.forEach(AABBTicket::invalidate);
        tickets.clear();
    }

    // Corrosion

    /**
     * Counts water as used by hydration: every {@code corrosion_water} mB one
     * aging step is due, held by one anti-corrosion item or else moving the
     * field's copper one stage older. Server side; does nothing while the head
     * serves no field.
     */
    public void age(int water) {
        if (status != FieldStatus.SERVING || water <= 0) {
            return;
        }
        ItemStack held = antiCorrosion.getStackInSlot(0);
        FieldCorrosion.Aged aged = FieldCorrosion.age(stage, agedWater, water, StorageStatsManager.INSTANCE.fieldCorrosionWater(),
                held.getCount());
        agedWater = aged.counted();
        if (aged.itemsUsed() > 0) {
            antiCorrosion.extractItem(0, aged.itemsUsed(), false);
        }
        if (aged.stage() != stage) {
            setStage(aged.stage());
        }
        setChanged();
    }

    /** Sets the stage of the served field on the head and on every rail of the field. */
    public void setStage(int newStage) {
        stage = FieldCorrosion.clamp(newStage);
        if (status == FieldStatus.SERVING && level != null && !level.isClientSide()) {
            for (BlockPos rail : rails) {
                if (level.isLoaded(rail) && level.getBlockEntity(rail) instanceof RailBlockEntity railEntity) {
                    railEntity.setCorrosion(stage);
                }
            }
        }
        applyCorrodedLook();
        setChanged();
    }

    /** The corrosion stage of the served field, {@link FieldCorrosion#LOOSE} while the head serves none. */
    public int corrosion() {
        return status == FieldStatus.SERVING ? stage : FieldCorrosion.LOOSE;
    }

    /** Hydration water counted toward the next aging step. */
    public long agedWater() {
        return agedWater;
    }

    /** Percent of the way to the next aging step. */
    public int corrosionProgress() {
        return FieldCorrosion.progress(agedWater, StorageStatsManager.INSTANCE.fieldCorrosionWater());
    }

    /** Whether the item may go into the anti-corrosion slot. */
    public static boolean isAntiCorrosion(ItemStack stack) {
        return stack.is(ANTI_CORROSION);
    }

    /** The anti-corrosion slot. */
    public ItemSlots antiCorrosion() {
        return antiCorrosion;
    }

    /** What hoppers and ducts see: anti-corrosion items go in, nothing comes out. */
    public ResourceHandler<ItemResource> itemHandler() {
        return insertOnly;
    }

    /** The anti-corrosion slot as upkeep of the data line. */
    public UpkeepHolder upkeep() {
        return upkeep;
    }

    /** Takes the anti-corrosion items out, for dropping when the head breaks. */
    public ItemStack takeAntiCorrosion() {
        ItemStack held = antiCorrosion.getStackInSlot(0).copy();
        antiCorrosion.setStackInSlot(0, ItemStack.EMPTY);
        return held;
    }

    /** Shows the rust rim on top while the served field is corroded. */
    private void applyCorrodedLook() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean corroded = status == FieldStatus.SERVING && stage == FieldCorrosion.CORRODED;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(FieldHeadBlock.CORRODED) && state.getValue(FieldHeadBlock.CORRODED) != corroded) {
            level.setBlock(worldPosition, state.setValue(FieldHeadBlock.CORRODED, corroded), Block.UPDATE_CLIENTS);
        }
    }

    // Ripeness

    /** Reads every loaded plot again and works out what the rails can see. */
    public void recheckAll() {
        ground.clear();
        if (level == null) {
            return;
        }
        for (BlockPos plot : candidates) {
            if (level.isLoaded(plot)) {
                ground.put(plot, HarvesterWork.classify(level, plot, level.getBlockState(plot)));
            }
        }
        refreshReach();
    }

    /** The plots the rails see now, and their ripeness rebuilt from {@link #ground}. */
    private void refreshReach() {
        List<FieldPos> railPositions = new ArrayList<>(rails.size());
        for (BlockPos rail : rails) {
            railPositions.add(new FieldPos(rail.getX(), rail.getY(), rail.getZ()));
        }
        Set<FieldPos> candidatePositions = new HashSet<>();
        for (BlockPos plot : candidates) {
            candidatePositions.add(new FieldPos(plot.getX(), plot.getY(), plot.getZ()));
        }
        Set<BlockPos> seen = new HashSet<>();
        for (FieldPos plot : FieldReach.connected(railPositions, candidatePositions,
                pos -> ground.getOrDefault(new BlockPos(pos.x(), pos.y(), pos.z()), PlotState.BLOCKED) != PlotState.BLOCKED, radius())) {
            seen.add(new BlockPos(plot.x(), plot.y(), plot.z()));
        }
        plots = Collections.unmodifiableSet(seen);
        ripeness.clear();
        for (BlockPos plot : plots) {
            ripeness.update(plot, ground.get(plot));
        }
        lookDirty = true;
    }

    /** Reads the plot on the next tick, after the block has changed. */
    public void recheck(BlockPos pos) {
        if (covers(pos)) {
            rechecks.add(pos.immutable());
        }
    }

    /** Reads the plot now, after a block on it changed. */
    public void recheckNow(BlockPos plot) {
        if (level != null && level.isLoaded(plot)) {
            update(plot, level.getBlockState(plot));
        }
    }

    /** Records what a plot holds now, the block on it being that state. */
    void update(BlockPos plot, BlockState state) {
        BlockPos key = plot.immutable();
        PlotState now = HarvesterWork.classify(level, key, state);
        PlotState before = ground.put(key, now);
        // A crop that appears where none grew (a ripe one harvested and replanted counts too) is newly planted.
        if (now == PlotState.GROWING) {
            if (before != null && before != PlotState.GROWING && unfed.add(key)) {
                setChanged();
            }
        } else if (unfed.remove(key)) {
            setChanged();
        }
        if ((before == null || before == PlotState.BLOCKED) != (now == PlotState.BLOCKED)) {
            refreshReach();
        } else if (plots.contains(key)) {
            ripeness.update(key, now);
            lookDirty = true;
        }
    }

    /** Whether the head serves a field that reaches that position, seen by the rails or not. */
    public boolean covers(BlockPos pos) {
        return status == FieldStatus.SERVING && candidates.contains(pos);
    }

    /** Whether the field holds that rail or one of its neighbours. */
    boolean touchesRail(BlockPos pos) {
        if (rails.contains(pos)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (rails.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    /** Whether the served field's ground reaches into the chunk. */
    boolean reaches(ChunkPos chunk) {
        return status == FieldStatus.SERVING
                && chunk.getMaxBlockX() >= minX && chunk.getMinBlockX() <= maxX
                && chunk.getMaxBlockZ() >= minZ && chunk.getMinBlockZ() <= maxZ;
    }

    // Crop and mode

    /** Whether the item plants an age crop and can be the crop of a field. */
    public static boolean isCropItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock;
    }

    /** The crop item of the field, empty when none is set. */
    public ItemStack crop() {
        return crop.getStackInSlot(0).copy();
    }

    /** The one slot handler that stores the crop item. */
    public ItemSlots cropHandler() {
        return crop;
    }

    /** Sets the crop of the field (empty clears it); a crop turns harvest only off. Items that plant no age crop are ignored. */
    public void setCrop(ItemStack stack) {
        if (stack.isEmpty()) {
            crop.setStackInSlot(0, ItemStack.EMPTY);
        } else if (isCropItem(stack)) {
            crop.setStackInSlot(0, stack.copyWithCount(1));
            harvestOnly = false;
        }
        lookDirty = true;
        setChanged();
        syncClients();
    }

    /** Every item that plants an age crop, from the item registry, so mods' crops count too. */
    public static List<Item> cropItems() {
        List<Item> items = cropItems;
        if (items == null) {
            List<Item> found = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem block && block.getBlock() instanceof CropBlock) {
                    found.add(item);
                }
            }
            items = List.copyOf(found);
            cropItems = items;
        }
        return items;
    }

    /**
     * The warehouse stock of every crop item with a non zero count, keyed by
     * item id; empty while the head serves no field on a warehouse with its
     * one controller. Server side.
     */
    public Optional<Map<Identifier, Long>> cropStock() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return Optional.empty();
        }
        return network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).map(warehouse -> {
            Map<Identifier, Long> counts = new HashMap<>();
            for (Item item : cropItems()) {
                long count = warehouse.stockCount(ItemKey.of(new ItemStack(item)));
                if (count > 0) {
                    counts.put(BuiltInRegistries.ITEM.getKey(item), count);
                }
            }
            return counts;
        });
    }

    public boolean harvestOnly() {
        return harvestOnly;
    }

    /** The ripe percent of the planted plots that calls a harvest. */
    public int harvestPercent() {
        return harvestPercent > 0 ? harvestPercent : FieldThreshold.fromShare(StorageStatsManager.INSTANCE.fieldHarvestShare());
    }

    /** Moves the harvest threshold by whole steps of {@link FieldThreshold#STEP} percent. */
    public void stepHarvestPercent(int steps) {
        harvestPercent = FieldThreshold.step(harvestPercent(), steps);
        lookDirty = true;
        setChanged();
    }

    /** Harvest without planting; turning it on clears the crop. */
    public void setHarvestOnly(boolean on) {
        harvestOnly = on;
        if (on) {
            crop.setStackInSlot(0, ItemStack.EMPTY);
        }
        lookDirty = true;
        setChanged();
        syncClients();
    }

    // Fertilizing

    /** Whether the item may be a field's fertilizer. */
    public static boolean isFertilizer(ItemStack stack) {
        return !stack.isEmpty() && stack.is(FERTILIZER);
    }

    /** The fertilizer of the field, empty when none is chosen. */
    public ItemStack fertilizer() {
        return fertilizer == null ? ItemStack.EMPTY : new ItemStack(fertilizer);
    }

    /** Chooses the fertilizer (empty for none); items outside the tag are ignored. */
    public void setFertilizer(ItemStack stack) {
        if (stack.isEmpty()) {
            fertilizer = null;
        } else if (isFertilizer(stack)) {
            fertilizer = stack.getItem();
        } else {
            return;
        }
        setChanged();
        syncClients();
    }

    public FertilizeMode fertilizeMode() {
        return fertilizeMode;
    }

    public void setFertilizeMode(FertilizeMode mode) {
        fertilizeMode = mode;
        setChanged();
    }

    /** Whether a fertilizer is chosen and the mode is not off. */
    public boolean fertilizing() {
        return fertilizer != null && fertilizeMode != FertilizeMode.OFF;
    }

    /** The ripe percent below which Until ripe share doses. */
    public int fertilizePercent() {
        return fertilizePercent > 0 ? fertilizePercent : FieldThreshold.fromShare(StorageStatsManager.INSTANCE.fertilizeShare());
    }

    /** Moves the fertilize threshold by whole steps of {@link FieldThreshold#STEP} percent. */
    public void stepFertilizePercent(int steps) {
        fertilizePercent = FieldThreshold.step(fertilizePercent(), steps);
        setChanged();
    }

    /** The fertilizer items this field keeps in stock: a Spreader never takes them. */
    public int reserve() {
        return reserve >= 0 ? reserve : FertilizePlan.clampReserve(StorageStatsManager.INSTANCE.fertilizerReserve());
    }

    /** Moves the reserve, kept between 0 and {@link FertilizePlan#MAX_RESERVE}. */
    public void stepReserve(int delta) {
        reserve = FertilizePlan.stepReserve(reserve(), delta);
        setChanged();
    }

    /** Planted plots that are not ripe and were planted since their last dose. */
    private Set<BlockPos> unfedGrowing() {
        Set<BlockPos> growing = ripeness.growing();
        growing.retainAll(unfed);
        return growing;
    }

    /** The plots that need a dose now by the mode; empty while fertilizing is off or the head serves no field. */
    public Set<BlockPos> fertilizeTargets() {
        if (status != FieldStatus.SERVING || !fertilizing()) {
            return Set.of();
        }
        return switch (fertilizeMode) {
            case OFF -> Set.of();
            case AFTER_PLANTING -> unfedGrowing();
            case UNTIL_RIPE -> FertilizePlan.belowShare(ripeCount(), plantedCount(), fertilizePercent()) ? ripeness.growing() : Set.of();
        };
    }

    /** How many plots need a dose now. */
    public int fertilizeTargetCount() {
        if (status != FieldStatus.SERVING || !fertilizing()) {
            return 0;
        }
        return FertilizePlan.targets(fertilizeMode, unfedGrowing().size(), ripeness.growing().size(), ripeCount(), plantedCount(), fertilizePercent());
    }

    /** A Spreader gave the plot its round of doses: it is no longer newly planted. */
    public void dosed(BlockPos plot) {
        if (unfed.remove(plot)) {
            setChanged();
        }
    }

    /** Plots planted since their last dose, for tests and the command. */
    public int unfedCount() {
        return unfed.size();
    }

    /** Whether the field calls a Spreader now, the warehouse holding that much of the fertilizer. */
    public boolean fertilizeDue(long now, long stock) {
        return status == FieldStatus.SERVING && fertilizing()
                && FertilizePlan.due(fertilizeTargetCount(), stock, reserve(), now, lastFertilizeCall, StorageStatsManager.INSTANCE.fertilizeInterval());
    }

    /**
     * What the fertilizing of the field is doing, or the first thing that
     * keeps a Spreader away. Server side; null when the head serves no field.
     */
    public FertilizeStatus fertilizeStatus() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return null;
        }
        if (!fertilizing()) {
            return FertilizeStatus.OFF;
        }
        Optional<WarehouseNetwork> network = network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE);
        boolean spreader = network.map(warehouse -> warehouse.hasFieldShuttles(JobNeeds.SPREADER)).orElse(false);
        boolean reachable = network.map(warehouse -> warehouse.fieldReachable(worldPosition, JobNeeds.SPREADER)).orElse(false);
        boolean working = network.map(warehouse -> warehouse.hasFertilizeJob(worldPosition)).orElse(false);
        long stock = network.map(warehouse -> warehouse.available(ItemKey.of(fertilizer()))).orElse(0L);
        return FertilizeStatus.of(true, network.isPresent(), spreader, reachable, working, stock, reserve());
    }

    /**
     * The warehouse stock of every fertilizer item with a non zero count,
     * keyed by item id; empty while the head serves no field on a warehouse
     * with its one controller. Server side.
     */
    public Optional<Map<Identifier, Long>> fertilizerStock() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return Optional.empty();
        }
        return network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).map(warehouse -> {
            Map<Identifier, Long> counts = new HashMap<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(FERTILIZER).forEach(holder -> {
                long count = warehouse.stockCount(ItemKey.of(new ItemStack(holder.value())));
                if (count > 0) {
                    counts.put(BuiltInRegistries.ITEM.getKey(holder.value()), count);
                }
            });
            return counts;
        });
    }

    /** The name plate of the Spreader dosing this field right now, or empty. Server side. */
    public Optional<String> spreaderName() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return Optional.empty();
        }
        return network().flatMap(warehouse -> warehouse.spreadWorker(worldPosition)).flatMap(ShuttleItem::plate);
    }

    /** The warehouse queued a fertilize run for this field at that game time. */
    public void fertilizeCalled(long now) {
        lastFertilizeCall = now;
        setChanged();
    }

    // Read side for the GUI, the command and m10-harvester

    public FieldStatus status() {
        return status;
    }

    /** How far the field reaches to each side of its rails: {@code radius} plus one per Range Upgrade. */
    public int radius() {
        return FieldRadius.of(StorageStatsManager.INSTANCE.fieldRadius(), StorageStatsManager.INSTANCE.fieldMaxRadius(),
                rangeUpgrades.getStackInSlot(0).getCount());
    }

    /** The one slot handler of the Range Upgrades. */
    public ItemSlots rangeUpgrades() {
        return rangeUpgrades;
    }

    /** Takes the Range Upgrades out, for dropping when the head breaks. */
    public ItemStack takeRangeUpgrades() {
        ItemStack held = rangeUpgrades.getStackInSlot(0).copy();
        rangeUpgrades.setStackInSlot(0, ItemStack.EMPTY);
        return held;
    }

    /** The farm rails of the field; for a head that sees another head, the field it would serve. */
    public Set<BlockPos> rails() {
        return rails;
    }

    /** The plots the rails of the served field can see, empty when not serving. */
    public Set<BlockPos> plots() {
        return plots;
    }

    public Set<BlockPos> ripePlots() {
        return ripeness.ripePlots();
    }

    public int plantedCount() {
        return ripeness.planted();
    }

    public int ripeCount() {
        return ripeness.ripe();
    }

    /** What the head last saw on the plot; blocked for positions it does not track. */
    public PlotState plotState(BlockPos plot) {
        return ripeness.state(plot);
    }

    /** Empty plots on dirt or grass. */
    public int tillableCount() {
        return ripeness.tillable();
    }

    /** Empty plots on farmland. */
    public int plantableCount() {
        return ripeness.plantable();
    }

    /** Whether a crop is set and harvest only is off, so the field gets tilled and planted. */
    public boolean plantMode() {
        return !harvestOnly && !crop.getStackInSlot(0).isEmpty();
    }

    public boolean hydrated() {
        return hydrated;
    }

    /** Whether the field has a crop or harvest only set and its ripe share reached the threshold from {@code field.json}. */
    public boolean harvestDue() {
        return status == FieldStatus.SERVING && (harvestOnly || !crop.getStackInSlot(0).isEmpty())
                && ripeness.due(harvestPercent() / 100.0);
    }

    /**
     * What the field is doing, or the first thing that keeps a harvester away,
     * read from the warehouse its rails belong to. Server side; null when the
     * head serves no field.
     */
    public FieldWork work() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return null;
        }
        Optional<WarehouseNetwork> network = network();
        boolean online = network.map(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).orElse(false);
        boolean harvester = network.map(WarehouseNetwork::hasFieldShuttles).orElse(false);
        boolean reachable = network.map(warehouse -> warehouse.fieldReachable(worldPosition)).orElse(false);
        boolean working = network.map(warehouse -> warehouse.hasFieldJob(worldPosition)).orElse(false);
        boolean seeds = plantMode() && network.map(warehouse -> warehouse.available(ItemKey.of(crop())) > 0).orElse(false);
        return FieldWork.of(network.isPresent(), online, harvestOnly || plantMode(), harvester, reachable, working, harvestDue(),
                plantMode(), hydrated, tillableCount(), plantableCount(), plantedCount(), seeds);
    }

    /** The warehouse the field's rails belong to; call only while the head serves a field. */
    private Optional<WarehouseNetwork> network() {
        return level.getBlockEntity(rails.iterator().next()) instanceof StorageNodeBlockEntity node ? node.network() : Optional.empty();
    }

    /** The name plate of the shuttle working this field right now, or empty. Server side. */
    public Optional<String> workerName() {
        if (status != FieldStatus.SERVING || rails.isEmpty()) {
            return Optional.empty();
        }
        return network().flatMap(warehouse -> warehouse.fieldWorker(worldPosition)).flatMap(ShuttleItem::plate);
    }

    public int water() {
        return tank.getFluidAmount();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    /** The fluid handler other blocks and pipes see: takes water, gives nothing back. */
    public ResourceHandler<FluidResource> fluidHandler() {
        return fillOnly;
    }

    // Look

    /** Sets the ripe bar step on the block when it changed. Server side, from the tick. */
    private void applyRipeStep() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int step = status == FieldStatus.SERVING
                ? FieldHeadLook.ripeStep(plantedCount(), ripeCount(), harvestPercent(), harvestDue())
                : FieldHeadLook.EMPTY;
        BlockState state = getBlockState();
        if (state.hasProperty(FieldHeadBlock.RIPE) && state.getValue(FieldHeadBlock.RIPE) != step) {
            level.setBlock(worldPosition, state.setValue(FieldHeadBlock.RIPE, step), Block.UPDATE_CLIENTS);
        }
        applyCorrodedLook();
    }

    /** The water level of the windows: synced steps on the client, the tank on the server. */
    public int waterLevel() {
        if (level != null && level.isClientSide()) {
            return clientWaterLevel;
        }
        return FieldHeadLook.waterLevel(tank.getFluidAmount(), tank.getCapacity());
    }

    /** Sends the block to watching clients when the water level step changed. */
    private void syncWaterLevel() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int water = FieldHeadLook.waterLevel(tank.getFluidAmount(), tank.getCapacity());
        if (water != sentWaterLevel) {
            sentWaterLevel = water;
            syncClients();
        }
    }

    private void syncClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FieldHeadMenu(containerId, playerInventory, this);
    }

    // Saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        tag.put("Water", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Crop", crop.serializeNBT(registries));
        output.putBoolean("HarvestOnly", harvestOnly);
        tag.put("RangeUpgrades", rangeUpgrades.serializeNBT(registries));
        if (harvestPercent > 0) {
            output.putInt("HarvestPercent", harvestPercent);
        }
        tag.put("AntiCorrosion", antiCorrosion.serializeNBT(registries));
        if (stageKnown) {
            output.putInt("Corrosion", stage);
        }
        output.putLong("CorrosionWater", agedWater);
        if (fertilizer != null) {
            output.putString("Fertilizer", BuiltInRegistries.ITEM.getKey(fertilizer).toString());
        }
        output.putInt("FertilizeMode", fertilizeMode.ordinal());
        if (fertilizePercent > 0) {
            output.putInt("FertilizePercent", fertilizePercent);
        }
        if (reserve >= 0) {
            output.putInt("Reserve", reserve);
        }
        tag.putLongArray("Unfed", unfed.stream().mapToLong(BlockPos::asLong).toArray());
        output.putLong("FertilizeCalled", lastFertilizeCall);
        output.store(tag);
    }

    /** The fertilizer saved in the tag, null for none or an item no longer in the fertilizer tag. */
    private static Item readFertilizer(CompoundTag tag) {
        if (!tag.contains("Fertilizer")) {
            return null;
        }
        Identifier id = Identifier.tryParse(tag.getStringOr("Fertilizer", ""));
        Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
        return item.filter(found -> isFertilizer(new ItemStack(found))).orElse(null);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        tank.readFromNBT(registries, tag.getCompoundOrEmpty("Water"));
        if (tag.contains("Crop")) {
            crop.deserializeNBT(registries, tag.getCompoundOrEmpty("Crop"));
        }
        harvestOnly = input.getBooleanOr("HarvestOnly", false);
        if (tag.contains("RangeUpgrades")) {
            rangeUpgrades.deserializeNBT(registries, tag.getCompoundOrEmpty("RangeUpgrades"));
        }
        harvestPercent = tag.contains("HarvestPercent") ? FieldThreshold.clamp(input.getIntOr("HarvestPercent", 0)) : 0;
        if (tag.contains("AntiCorrosion")) {
            antiCorrosion.deserializeNBT(registries, tag.getCompoundOrEmpty("AntiCorrosion"));
        }
        stageKnown = tag.contains("Corrosion");
        stage = stageKnown ? FieldCorrosion.clamp(input.getIntOr("Corrosion", 0)) : FieldCorrosion.COPPER;
        agedWater = Math.max(0, input.getLongOr("CorrosionWater", 0L));
        fertilizer = readFertilizer(tag);
        fertilizeMode = FertilizeMode.byId(input.getIntOr("FertilizeMode", 0));
        fertilizePercent = tag.contains("FertilizePercent") ? FieldThreshold.clamp(input.getIntOr("FertilizePercent", 0)) : 0;
        reserve = tag.contains("Reserve") ? FertilizePlan.clampReserve(input.getIntOr("Reserve", 0)) : -1;
        unfed.clear();
        for (long packed : tag.getLongArray("Unfed").orElse(new long[0])) {
            unfed.add(BlockPos.of(packed));
        }
        lastFertilizeCall = tag.contains("FertilizeCalled") ? input.getLongOr("FertilizeCalled", 0L) : -1;
        if (tag.contains("WaterLevel")) {
            clientWaterLevel = input.getByteOr("WaterLevel", (byte) 0);
        }
    }

    // Wood and client sync: the wood, the crop, the mode and the water level in steps

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        tag.put("Crop", crop.serializeNBT(registries));
        tag.putBoolean("HarvestOnly", harvestOnly);
        if (fertilizer != null) {
            tag.putString("Fertilizer", BuiltInRegistries.ITEM.getKey(fertilizer).toString());
        }
        tag.putByte("WaterLevel", (byte) FieldHeadLook.waterLevel(tank.getFluidAmount(), tank.getCapacity()));
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

    /** Takes anti-corrosion items into the upkeep slot; gives nothing out. */
    private final class InsertOnly extends LegacyItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return antiCorrosion.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return antiCorrosion.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return antiCorrosion.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return antiCorrosion.isItemValid(slot, stack);
        }
    }

    /** The anti-corrosion slot kept at {@code anti_corrosion_level} by the data line. */
    private final class AntiCorrosionUpkeep implements UpkeepHolder {

        @Override
        public ItemStack upkeepItem() {
            ItemStack held = antiCorrosion.getStackInSlot(0);
            if (!held.isEmpty()) {
                return held.copyWithCount(1);
            }
            return BuiltInRegistries.ITEM.get(ANTI_CORROSION)
                    .flatMap(set -> set.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
        }

        @Override
        public int upkeepLevel() {
            return StorageStatsManager.INSTANCE.fieldAntiCorrosionLevel();
        }

        @Override
        public int upkeepCount() {
            return antiCorrosion.getStackInSlot(0).getCount();
        }
    }

    /** Fills the tank with water; never drains. */
    private final class FillOnly extends LegacyFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            return tank.fill(resource, simulate);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            return FluidStack.EMPTY;
        }
    }

    /** Moved from the block's removal hook (26.1): runs on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        FieldHeadBlockEntity head = this;
        FieldHeads.headRemoved(level, head);
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), head.takeRangeUpgrades());
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), head.takeAntiCorrosion());
    }
}
