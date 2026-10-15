package io.github.dirnei.mc.servus.shuttles.field;

import net.minecraft.world.Containers;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import java.util.ArrayList;
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
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
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
import net.minecraft.world.level.block.state.properties.Property;
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
    public static final TagKey<Item> ANTI_CORROSION = ItemTags.create(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "anti_corrosion"));
    /** Items a Spreader doses fields with, each one bone meal application. */
    public static final TagKey<Item> FERTILIZER = ItemTags.create(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "fertilizer"));

    private final WoodSlot woodSlot = new WoodSlot(this);

    private final FluidSlot tank = new FluidSlot(ShuttlesStatsManager.INSTANCE.fieldCapacity(), stack -> stack.is(FluidTags.WATER)) {
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
            return stack.is(ServusShuttlesItems.RANGE_UPGRADE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return Math.max(1, FieldRadius.maxUpgrades(ShuttlesStatsManager.INSTANCE.fieldRadius(), ShuttlesStatsManager.INSTANCE.fieldMaxRadius()));
        }
    };
    /** Seeds, fertilizer and anti-corrosion items, filled by supply runs and taken by field shuttles only. */
    private final SupplyBuffer buffer = new SupplyBuffer(this::bufferChanged);
    /** Whether the player chose the anti-corrosion item; until then it is the first item of the tag. */
    private boolean antiCorrosionChosen;
    /** The chosen anti-corrosion item, null for Off; meaningful while {@link #antiCorrosionChosen}. */
    private Item antiCorrosion;
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
    /** The anti-corrosion Keep in stock reserve; -1 follows {@code anti_corrosion_reserve}. */
    private int antiCorrosionReserve = -1;
    /** Plots planted since their last dose, learned from events; saved. */
    private final Set<BlockPos> unfed = new HashSet<>();
    /** Game time of the last call for a Spreader, -1 for none. */
    private long lastFertilizeCall = -1;
    /** The item the field's harvests bring home the most of, see {@link Produce}; null until the first harvest. */
    private Item produce;
    /** The harvest limit in items of the produce; -1 follows {@code harvest_limit}. */
    private int harvestLimit = -1;
    /** The fertilize level in items of the produce; -1 follows {@code fertilize_below}. */
    private int fertilizeBelow = -1;
    /** The warehouse stock of the produce, read at each field check; not saved. */
    private long produceStock;

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
    /** How often the head looks whether it has a problem, for the red bar, besides every change of its own. */
    private static final int PROBLEM_CHECK_TICKS = 20;
    /** The water level last sent to clients, -1 before the first. */
    private int sentWaterLevel = -1;
    /** Client side: the water level from the last update. */
    private int clientWaterLevel;
    private boolean hydrated;
    private int countdown;

    public FieldHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FIELD_HEAD.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (onServer()) {
            FieldHeads.add(this);
            refreshPending = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        leave();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        leave();
    }

    private boolean onServer() {
        return level != null && !level.isClientSide();
    }

    /** The head goes away with its block or chunk: its tickets end and it is no longer known. */
    private void leave() {
        if (onServer()) {
            dropTickets();
            hydrated = false;
            FieldHeads.remove(this);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldHeadBlockEntity head) {
        head.tick();
    }

    private void tick() {
        int capacity = ShuttlesStatsManager.INSTANCE.fieldCapacity();
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
        } else if (level != null && (level.getGameTime() + worldPosition.asLong()) % PROBLEM_CHECK_TICKS == 0) {
            // Warehouse problems (controller, harvester, way) raise no event here.
            applyProblemLook();
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

    /** Looks at the field again on the next tick. */
    public void requestRefresh() {
        refreshPending = true;
    }

    /** The first farm rail beside the head, north, east, south, west. */
    private BlockPos startRail() {
        if (level == null) {
            return null;
        }
        List<BlockPos> neighbours = FieldHeads.farmNeighbours(level, worldPosition);
        return neighbours.isEmpty() ? null : neighbours.get(0);
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
            FieldGraph.Result<BlockPos> result = FieldHeads.field(level, start);
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
        List<FieldPos> railPositions = FieldPositions.of(members);
        Set<BlockPos> candidateSet = new HashSet<>();
        for (FieldPos plot : FieldStrip.plots(railPositions, radius())) {
            candidateSet.add(FieldPositions.toBlockPos(plot));
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

    private void useWater() {
        if (stage == FieldCorrosion.CORRODED) {
            // Corroded rails water nothing, so they use no water and age no further.
            if (hydrated) {
                hydrated = false;
                dropTickets();
            }
            return;
        }
        int need = FieldWater.use(rails.size(), ShuttlesStatsManager.INSTANCE.fieldWaterPerRail());
        if (!hydrated) {
            if (tank.getFluidAmount() >= need) {
                tank.drain(need, false);
                age(need);
                hydrated = true;
                countdown = ShuttlesStatsManager.INSTANCE.fieldWaterInterval();
                addTickets();
            }
            return;
        }
        if (--countdown > 0) {
            return;
        }
        if (tank.getFluidAmount() >= need) {
            tank.drain(need, false);
            countdown = ShuttlesStatsManager.INSTANCE.fieldWaterInterval();
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
        FieldCorrosion.Aged aged = FieldCorrosion.age(stage, agedWater, water, ShuttlesStatsManager.INSTANCE.fieldCorrosionWater(),
                supplyCount(SupplyBuffer.Part.ANTI_CORROSION));
        agedWater = aged.counted();
        if (aged.itemsUsed() > 0) {
            buffer.take(SupplyBuffer.Part.ANTI_CORROSION, aged.itemsUsed());
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

    /** The mB per minute the served field uses while watered; 0 while it serves none or its rails are corroded. */
    public int waterPerMinute() {
        if (status != FieldStatus.SERVING || stage == FieldCorrosion.CORRODED) {
            return 0;
        }
        return FieldWater.perMinute(rails.size(), ShuttlesStatsManager.INSTANCE.fieldWaterPerRail(),
                ShuttlesStatsManager.INSTANCE.fieldWaterInterval());
    }

    /**
     * Seconds of watering until the next aging step is due, -1 while the head
     * uses no water. Server side.
     */
    public long secondsToAging() {
        if (waterPerMinute() <= 0) {
            return -1;
        }
        long ticks = FieldWater.ticksToUse(ShuttlesStatsManager.INSTANCE.fieldCorrosionWater() - agedWater, rails.size(),
                ShuttlesStatsManager.INSTANCE.fieldWaterPerRail(), ShuttlesStatsManager.INSTANCE.fieldWaterInterval());
        return ticks < 0 ? -1 : ticks / 20;
    }

    /** Percent of the way to the next aging step. */
    public int corrosionProgress() {
        return FieldCorrosion.progress(agedWater, ShuttlesStatsManager.INSTANCE.fieldCorrosionWater());
    }

    /** Whether the item is an anti-corrosion item. */
    public static boolean isAntiCorrosion(ItemStack stack) {
        return stack.is(ANTI_CORROSION);
    }

    /** The supply buffer as it is, stale items included; change it through the head. */
    public SupplyBuffer buffer() {
        return buffer;
    }

    private void bufferChanged() {
        setChanged();
    }

    /** The anti-corrosion item of the field: the chosen one, else the first of the tag; empty for Off. */
    public ItemStack antiCorrosion() {
        Item item = antiCorrosionItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Item antiCorrosionItem() {
        return antiCorrosionChosen ? antiCorrosion : firstAntiCorrosion();
    }

    /** Chooses the anti-corrosion item (empty for Off); items outside the tag are ignored. */
    public void setAntiCorrosion(ItemStack stack) {
        if (!stack.isEmpty() && !isAntiCorrosion(stack)) {
            return;
        }
        antiCorrosionChosen = true;
        antiCorrosion = stack.isEmpty() ? null : stack.getItem();
        setChanged();
        syncClients();
    }

    /**
     * The level supply runs fill the part to: seeds while the field plants a
     * crop, fertilizer while fertilizing is on, anti-corrosion items unless
     * they are Off. 0 while the part is off.
     */
    public int supplyLevel(SupplyBuffer.Part part) {
        return switch (part) {
            case SEEDS -> plantMode() ? ShuttlesStatsManager.INSTANCE.seedBuffer() : 0;
            case FERTILIZER -> fertilizing() ? ShuttlesStatsManager.INSTANCE.fertilizerBuffer() : 0;
            case ANTI_CORROSION -> antiCorrosionItem() != null ? ShuttlesStatsManager.INSTANCE.fieldAntiCorrosionLevel() : 0;
        };
    }

    /**
     * The item the part is filled with: the crop, the fertilizer or the
     * anti-corrosion item. Null while the part is off or the tag is empty.
     */
    public Item supplyItem(SupplyBuffer.Part part) {
        return switch (part) {
            case SEEDS -> plantMode() ? crop.getStackInSlot(0).getItem() : null;
            case FERTILIZER -> fertilizing() ? fertilizer : null;
            case ANTI_CORROSION -> antiCorrosionItem();
        };
    }

    private static Item firstAntiCorrosion() {
        return BuiltInRegistries.ITEM.get(ANTI_CORROSION)
                .flatMap(set -> set.stream().findFirst())
                .map(holder -> holder.value())
                .orElse(null);
    }

    /** How many of the part's item the buffer holds; stale items count as none. */
    public int supplyCount(SupplyBuffer.Part part) {
        Item item = supplyItem(part);
        return item != null && buffer.item(part) == item ? buffer.count(part) : 0;
    }

    /** How many more of the part's item the part takes; none while it holds a stale item. */
    public int supplyRoom(SupplyBuffer.Part part) {
        Item item = supplyItem(part);
        return item != null && buffer.accepts(part, item) ? Math.max(0, supplyLevel(part) - buffer.count(part)) : 0;
    }

    /** Puts up to count of the item into the part, up to its level and only the part's item; returns how many went in. */
    public int fillSupply(SupplyBuffer.Part part, Item item, int count) {
        if (item == null || item != supplyItem(part)) {
            return 0;
        }
        return buffer.insert(part, item, count, supplyLevel(part));
    }

    /** Takes up to amount of the part's item out of the buffer, for a field shuttle; returns how many came out. */
    public int takeSupply(SupplyBuffer.Part part, int amount) {
        return supplyCount(part) > 0 ? buffer.take(part, amount) : 0;
    }

    /** Whether a part holds an item that is no longer the part's item. */
    public boolean holdsStale() {
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            if (stale(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean stale(SupplyBuffer.Part part) {
        return buffer.item(part) != null && buffer.item(part) != supplyItem(part);
    }

    /** Takes out every item that is no longer its part's item, for a supply run to put away. */
    public List<ItemStack> takeStale() {
        List<ItemStack> stale = new ArrayList<>();
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            if (stale(part)) {
                stale.addAll(buffer.takeAll(part));
            }
        }
        return stale;
    }

    /** The first farm rail beside the head, where supply runs unload and field shuttles take from the buffer; null for none. */
    public BlockPos headRail() {
        return startRail();
    }

    private static ItemStack takeAll(ItemSlots slot) {
        ItemStack held = slot.getStackInSlot(0).copy();
        slot.setStackInSlot(0, ItemStack.EMPTY);
        return held;
    }

    /** Shows the rust rim on top while the served field is corroded. */
    private void applyCorrodedLook() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean corroded = status == FieldStatus.SERVING && stage == FieldCorrosion.CORRODED;
        show(level.getBlockState(worldPosition), FieldHeadBlock.CORRODED, corroded);
    }

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
        Set<FieldPos> candidatePositions = new HashSet<>();
        candidatePositions.addAll(FieldPositions.of(candidates));
        Set<BlockPos> seen = new HashSet<>();
        for (FieldPos plot : FieldReach.connected(FieldPositions.of(rails), candidatePositions,
                pos -> ground.getOrDefault(FieldPositions.toBlockPos(pos), PlotState.BLOCKED) != PlotState.BLOCKED, radius())) {
            seen.add(FieldPositions.toBlockPos(plot));
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

    /** Whether the item plants an age crop and can be the crop of a field. */
    public static boolean isCropItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock;
    }

    /** The crop item of the field, empty when none is set. */
    public ItemStack crop() {
        return crop.getStackInSlot(0).copy();
    }

    /** Sets the crop of the field (empty clears it); a crop turns harvest only off. Items that plant no age crop are ignored. */
    public void setCrop(ItemStack stack) {
        Item before = crop.getStackInSlot(0).getItem();
        if (stack.isEmpty()) {
            crop.setStackInSlot(0, ItemStack.EMPTY);
        } else if (isCropItem(stack)) {
            crop.setStackInSlot(0, stack.copyWithCount(1));
            harvestOnly = false;
        }
        if (crop.getStackInSlot(0).getItem() != before) {
            forgetProduce();
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
        return stockOf(cropItems());
    }

    /** The warehouse stock of each of the items with a non zero count, keyed by item id; empty without a served field online. */
    private Optional<Map<Identifier, Long>> stockOf(Iterable<Item> items) {
        if (!servesRails()) {
            return Optional.empty();
        }
        return onlineNetwork().map(warehouse -> {
            Map<Identifier, Long> counts = new HashMap<>();
            for (Item item : items) {
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
        return harvestPercent > 0 ? harvestPercent : FieldThreshold.fromShare(ShuttlesStatsManager.INSTANCE.fieldHarvestShare());
    }

    /** Sets the harvest threshold, kept in range. */
    public void setHarvestPercent(int percent) {
        harvestPercent = FieldThreshold.clamp(percent);
        lookDirty = true;
        setChanged();
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
        if (on && !crop.getStackInSlot(0).isEmpty()) {
            crop.setStackInSlot(0, ItemStack.EMPTY);
            forgetProduce();
        }
        lookDirty = true;
        setChanged();
        syncClients();
    }

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
        return fertilizePercent > 0 ? fertilizePercent : FieldThreshold.fromShare(ShuttlesStatsManager.INSTANCE.fertilizeShare());
    }

    /** Moves the fertilize threshold by whole steps of {@link FieldThreshold#STEP} percent. */
    public void stepFertilizePercent(int steps) {
        fertilizePercent = FieldThreshold.step(fertilizePercent(), steps);
        setChanged();
    }

    /** The fertilizer items this field keeps in stock: a Spreader never takes them. */
    public int reserve() {
        return reserve >= 0 ? reserve : FertilizePlan.clampReserve(ShuttlesStatsManager.INSTANCE.fertilizerReserve());
    }

    /** Sets the reserve, kept between 0 and {@link FertilizePlan#MAX_RESERVE}. */
    public void setReserve(long value) {
        reserve = FertilizePlan.clampReserve(value);
        setChanged();
    }

    /** The anti-corrosion items this field keeps in stock: a supply run never takes them. */
    public int antiCorrosionReserve() {
        return antiCorrosionReserve >= 0 ? antiCorrosionReserve
                : FertilizePlan.clampReserve(ShuttlesStatsManager.INSTANCE.antiCorrosionReserve());
    }

    /** Sets the anti-corrosion reserve, kept between 0 and {@link FertilizePlan#MAX_RESERVE}. */
    public void setAntiCorrosionReserve(long value) {
        antiCorrosionReserve = FertilizePlan.clampReserve(value);
        setChanged();
    }

    /** The items of the part's item the warehouse keeps; supply runs never take them. */
    public int supplyReserve(SupplyBuffer.Part part) {
        return switch (part) {
            case SEEDS -> 0;
            case FERTILIZER -> reserve();
            case ANTI_CORROSION -> antiCorrosionReserve();
        };
    }

    /** Planted plots that are not ripe and were planted since their last dose. */
    private Set<BlockPos> unfedGrowing() {
        Set<BlockPos> growing = ripeness.growing();
        growing.retainAll(unfed);
        return growing;
    }

    /**
     * The plots that need a dose now by the mode; empty while fertilizing is
     * off, waits on the stock levels or the head serves no field.
     */
    public Set<BlockPos> fertilizeTargets() {
        if (status != FieldStatus.SERVING || !fertilizing() || !fertilizeAllowed()) {
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
        if (status != FieldStatus.SERVING || !fertilizing() || !fertilizeAllowed()) {
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

    /**
     * Whether the field calls a Spreader now: the stock levels allow it, plots
     * need a dose, the buffer holds the fertilizer and the interval passed.
     */
    public boolean fertilizeDue(long now) {
        return status == FieldStatus.SERVING && fertilizing() && fertilizeAllowed()
                && FertilizePlan.due(fertilizeTargetCount(), supplyCount(SupplyBuffer.Part.FERTILIZER), 0, now, lastFertilizeCall,
                        ShuttlesStatsManager.INSTANCE.fertilizeInterval());
    }

    /**
     * What the fertilizing of the field is doing, or the first thing that
     * keeps a Spreader away. Server side; null when the head serves no field.
     */
    public FertilizeStatus fertilizeStatus() {
        if (!servesRails()) {
            return null;
        }
        if (!fertilizing()) {
            return FertilizeStatus.OFF;
        }
        Optional<WarehouseNetwork> network = onlineNetwork();
        boolean spreader = network.map(warehouse -> warehouse.hasFieldShuttles(JobNeeds.SPREADER)).orElse(false);
        boolean reachable = network.map(warehouse -> warehouse.fieldReachable(worldPosition, JobNeeds.SPREADER)).orElse(false);
        boolean working = network.map(warehouse -> warehouse.hasFertilizeJob(worldPosition)).orElse(false);
        long stock = network.map(warehouse -> warehouse.available(ItemKey.of(fertilizer()))).orElse(0L);
        int buffered = supplyCount(SupplyBuffer.Part.FERTILIZER);
        boolean supplier = buffered == 0 && stock > reserve()
                && network.map(warehouse -> warehouse.supplyReachable(worldPosition)).orElse(false);
        return FertilizeStatus.of(true, network.isPresent(), spreader, reachable, working, buffered, stock, reserve(), supplier)
                .whileNeeded(fertilizeAllowed());
    }

    /**
     * The warehouse stock of every fertilizer and anti-corrosion item with a non zero count,
     * keyed by item id; empty while the head serves no field on a warehouse
     * with its one controller. Server side.
     */
    public Optional<Map<Identifier, Long>> supplyStock() {
        List<Item> items = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(FERTILIZER).forEach(holder -> items.add(holder.value()));
        BuiltInRegistries.ITEM.getTagOrEmpty(ANTI_CORROSION).forEach(holder -> items.add(holder.value()));
        return stockOf(items);
    }

    /** The name plate of the Spreader dosing this field right now, or empty. Server side. */
    public Optional<String> spreaderName() {
        if (!servesRails()) {
            return Optional.empty();
        }
        return network().flatMap(warehouse -> warehouse.spreadWorker(worldPosition)).flatMap(ShuttleItem::plate);
    }

    /** The field's produce, empty until the first harvest; on the client as last synced. */
    public ItemStack produce() {
        return produce == null ? ItemStack.EMPTY : new ItemStack(produce);
    }

    /** Whether the head learned its produce, so the stock levels apply. */
    public boolean produceKnown() {
        return produce != null;
    }

    /**
     * Learns the produce from what a run that harvested brought home, by item
     * id and count; the crop item counts only when nothing else came.
     */
    public void learnProduce(Map<Identifier, Long> tally) {
        Map<String, Long> byId = new HashMap<>();
        tally.forEach((id, count) -> byId.merge(id.toString(), count, Long::sum));
        String cropId = plantMode() ? BuiltInRegistries.ITEM.getKey(crop.getStackInSlot(0).getItem()).toString() : null;
        Produce.pick(byId, cropId)
                .map(Identifier::tryParse)
                .flatMap(BuiltInRegistries.ITEM::getOptional)
                .ifPresent(this::setProduce);
    }

    private void setProduce(Item item) {
        if (item != produce) {
            produce = item;
            produceStock = 0;
            setChanged();
            syncClients();
        }
    }

    private void forgetProduce() {
        setProduce(null);
    }

    /** The warehouse stock of the produce as last read at a field check. */
    public long produceStock() {
        return produceStock;
    }

    /** Reads the produce stock from the warehouse; called at each field check. Server side. */
    public void readProduceStock(WarehouseNetwork warehouse) {
        boolean paused = harvestPaused();
        produceStock = produce == null ? 0 : warehouse.available(ItemKey.of(new ItemStack(produce)));
        if (harvestPaused() != paused) {
            lookDirty = true;
        }
    }

    /** The harvest limit in items of the produce; 0 for none. */
    public int harvestLimit() {
        return harvestLimit >= 0 ? harvestLimit : StockLevels.clamp(ShuttlesStatsManager.INSTANCE.harvestLimit());
    }

    /** The fertilize level in items of the produce; 0 for none. */
    public int fertilizeBelow() {
        return fertilizeBelow >= 0 ? fertilizeBelow : StockLevels.clamp(ShuttlesStatsManager.INSTANCE.fertilizeBelow());
    }

    /** Sets one level, kept in range, and moves the other along so the fertilize level stays at or below the limit. */
    public void setLevel(StockLevels.Level which, long value) {
        int limit = harvestLimit();
        int below = fertilizeBelow();
        if (which == StockLevels.Level.HARVEST) {
            limit = StockLevels.clamp(value);
        } else {
            below = StockLevels.clamp(value);
        }
        StockLevels.Levels ordered = StockLevels.keepOrder(limit, below, which);
        harvestLimit = ordered.harvestLimit();
        fertilizeBelow = ordered.fertilizeBelow();
        lookDirty = true;
        setChanged();
    }

    /** Whether the field harvests nothing now: the produce stock reached the harvest limit. */
    public boolean harvestPaused() {
        return StockLevels.harvestPaused(produceKnown(), produceStock, harvestLimit());
    }

    /** Whether the stock levels let the field call a Spreader. */
    public boolean fertilizeAllowed() {
        return StockLevels.fertilizeAllowed(produceKnown(), produceStock, fertilizeBelow(), harvestLimit());
    }

    /** The warehouse queued a fertilize run for this field at that game time. */
    public void fertilizeCalled(long now) {
        lastFertilizeCall = now;
        setChanged();
    }

    public FieldStatus status() {
        return status;
    }

    /** How far the field reaches to each side of its rails: {@code radius} plus one per Range Upgrade. */
    public int radius() {
        return FieldRadius.of(ShuttlesStatsManager.INSTANCE.fieldRadius(), ShuttlesStatsManager.INSTANCE.fieldMaxRadius(),
                rangeUpgrades.getStackInSlot(0).getCount());
    }

    /** The one slot handler of the Range Upgrades. */
    public ItemSlots rangeUpgrades() {
        return rangeUpgrades;
    }

    /** Takes the Range Upgrades out, for dropping when the head breaks. */
    public ItemStack takeRangeUpgrades() {
        return takeAll(rangeUpgrades);
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

    /** Whether a harvest is due and not paused at the harvest limit, so a harvester gets called. */
    public boolean harvestCalled() {
        return harvestDue() && !harvestPaused();
    }

    /**
     * What the field is doing, or the first thing that keeps a harvester away,
     * read from the warehouse its rails belong to. Server side; null when the
     * head serves no field.
     */
    public FieldWork work() {
        if (!servesRails()) {
            return null;
        }
        Optional<WarehouseNetwork> network = network();
        boolean online = network.map(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).orElse(false);
        boolean harvester = network.map(WarehouseNetwork::hasFieldShuttles).orElse(false);
        boolean reachable = network.map(warehouse -> warehouse.fieldReachable(worldPosition)).orElse(false);
        boolean working = network.map(warehouse -> warehouse.hasFieldJob(worldPosition)).orElse(false);
        boolean buffered = supplyCount(SupplyBuffer.Part.SEEDS) > 0;
        boolean inStock = !buffered && plantMode() && network.map(warehouse -> warehouse.available(ItemKey.of(crop())) > 0).orElse(false);
        boolean supplier = inStock && network.map(warehouse -> warehouse.supplyReachable(worldPosition)).orElse(false);
        FieldWork.Seeds seeds = FieldWork.Seeds.of(buffered, inStock, supplier);
        return FieldWork.of(network.isPresent(), online, harvestOnly || plantMode(), harvester, reachable, working, harvestCalled(),
                plantMode(), hydrated, tillableCount(), plantableCount(), plantedCount(), seeds).whilePaused(harvestPaused());
    }

    /** The warehouse the field's rails belong to; call only while the head serves a field. */
    private Optional<WarehouseNetwork> network() {
        return level.getBlockEntity(rails.iterator().next()) instanceof StorageNodeBlockEntity node ? node.network() : Optional.empty();
    }

    private Optional<WarehouseNetwork> onlineNetwork() {
        return network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE);
    }

    /** Whether the head serves a field with rails, so it has a warehouse to ask. */
    private boolean servesRails() {
        return status == FieldStatus.SERVING && !rails.isEmpty();
    }

    /** The name plate of the shuttle working this field right now, or empty. Server side. */
    public Optional<String> workerName() {
        if (!servesRails()) {
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

    /** Sets the ripe bar step on the block when it changed. Server side, from the tick. */
    private void applyRipeStep() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int step = status == FieldStatus.SERVING
                ? FieldHeadLook.ripeStep(plantedCount(), ripeCount(), harvestPercent(), harvestCalled())
                : FieldHeadLook.EMPTY;
        show(getBlockState(), FieldHeadBlock.RIPE, step);
        applyCorrodedLook();
        applyProblemLook();
    }

    /**
     * Whether the head has one of the problems its GUI marks red; the bar on the
     * head is red while it does. Server side.
     */
    public boolean problem() {
        boolean serving = servesRails();
        return FieldHeadLook.problem(serving, serving ? work() : null, corrosion() == FieldCorrosion.CORRODED, hydrated,
                serving ? fertilizeStatus() : null);
    }

    /** Sets {@link FieldHeadBlock#PROBLEM} on the block when it changed. Server side. */
    private void applyProblemLook() {
        if (level == null || level.isClientSide()) {
            return;
        }
        show(level.getBlockState(worldPosition), FieldHeadBlock.PROBLEM, problem());
    }

    /** Sets the property on the block when the state has it with another value. */
    private <T extends Comparable<T>> void show(BlockState state, Property<T> property, T value) {
        if (state.hasProperty(property) && !state.getValue(property).equals(value)) {
            level.setBlock(worldPosition, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FieldHeadMenu(containerId, playerInventory, this);
    }

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
        tag.put("Buffer", buffer.save());
        if (stageKnown) {
            output.putInt("Corrosion", stage);
        }
        output.putLong("CorrosionWater", agedWater);
        if (fertilizer != null) {
            output.putString("Fertilizer", BuiltInRegistries.ITEM.getKey(fertilizer).toString());
        }
        output.putInt("FertilizeMode", fertilizeMode.ordinal());
        putAntiCorrosion(tag);
        if (fertilizePercent > 0) {
            output.putInt("FertilizePercent", fertilizePercent);
        }
        if (reserve >= 0) {
            output.putInt("Reserve", reserve);
        }
        if (antiCorrosionReserve >= 0) {
            output.putInt("AntiCorrosionReserve", antiCorrosionReserve);
        }
        tag.putLongArray("Unfed", unfed.stream().mapToLong(BlockPos::asLong).toArray());
        output.putLong("FertilizeCalled", lastFertilizeCall);
        if (produce != null) {
            output.putString("Produce", BuiltInRegistries.ITEM.getKey(produce).toString());
        }
        if (harvestLimit >= 0) {
            output.putInt("HarvestLimit", harvestLimit);
        }
        if (fertilizeBelow >= 0) {
            output.putInt("FertilizeBelow", fertilizeBelow);
        }
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
        if (tag.contains("Buffer")) {
            buffer.load(tag.getCompoundOrEmpty("Buffer"));
        }
        antiCorrosion = readItem(tag, "AntiCorrosion");
        antiCorrosionChosen = tag.contains("AntiCorrosion") && (antiCorrosion == null || isAntiCorrosion(new ItemStack(antiCorrosion)));
        stageKnown = tag.contains("Corrosion");
        stage = stageKnown ? FieldCorrosion.clamp(input.getIntOr("Corrosion", 0)) : FieldCorrosion.COPPER;
        agedWater = Math.max(0, input.getLongOr("CorrosionWater", 0L));
        fertilizer = readFertilizer(tag);
        fertilizeMode = FertilizeMode.byId(input.getIntOr("FertilizeMode", 0));
        fertilizePercent = tag.contains("FertilizePercent") ? FieldThreshold.clamp(input.getIntOr("FertilizePercent", 0)) : 0;
        reserve = tag.contains("Reserve") ? FertilizePlan.clampReserve(input.getIntOr("Reserve", 0)) : -1;
        antiCorrosionReserve = tag.contains("AntiCorrosionReserve")
                ? FertilizePlan.clampReserve(input.getIntOr("AntiCorrosionReserve", 0)) : -1;
        unfed.clear();
        for (long packed : tag.getLongArray("Unfed").orElse(new long[0])) {
            unfed.add(BlockPos.of(packed));
        }
        lastFertilizeCall = tag.contains("FertilizeCalled") ? input.getLongOr("FertilizeCalled", 0L) : -1;
        produce = readItem(tag, "Produce");
        harvestLimit = tag.contains("HarvestLimit") ? StockLevels.clamp(input.getIntOr("HarvestLimit", 0)) : -1;
        fertilizeBelow = tag.contains("FertilizeBelow") ? StockLevels.clamp(input.getIntOr("FertilizeBelow", 0)) : -1;
        if (tag.contains("WaterLevel")) {
            clientWaterLevel = input.getByteOr("WaterLevel", (byte) 0);
        }
    }

    /** Saves the chosen anti-corrosion item, an empty id for Off; nothing while it was never chosen. */
    private void putAntiCorrosion(CompoundTag tag) {
        if (antiCorrosionChosen) {
            tag.putString("AntiCorrosion", antiCorrosion == null ? "" : BuiltInRegistries.ITEM.getKey(antiCorrosion).toString());
        }
    }

    /** The item saved under the key, null for none or an unknown id. */
    private static Item readItem(CompoundTag tag, String key) {
        Identifier id = tag.contains(key) ? Identifier.tryParse(tag.getStringOr(key, "")) : null;
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

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
        putAntiCorrosion(tag);
        tag.putString("Produce", produce == null ? "" : BuiltInRegistries.ITEM.getKey(produce).toString());
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

    /** Tells the field it lost its head and drops the Range Upgrades and the supply buffer when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) {
            return;
        }
        FieldHeads.headRemoved(level, this);
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), takeRangeUpgrades());
        for (ItemStack stack : buffer.takeEverything()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }
}
