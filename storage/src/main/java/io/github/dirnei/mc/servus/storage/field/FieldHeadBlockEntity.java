package io.github.dirnei.mc.servus.storage.field;

import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Serves the field of the farm rail beside it: buffers water from a pipe,
 * keeps the field's farmland wet through NeoForge water tickets while the
 * water lasts, and knows which plots hold a ripe crop. The field, plots,
 * ripeness and tickets are rebuilt after a load or a change, never scanned
 * per tick; events from {@link FieldEvents} keep the ripeness current.
 */
public class FieldHeadBlockEntity extends BlockEntity implements MenuProvider {

    private final FluidTank tank = new FluidTank(StorageStatsManager.INSTANCE.fieldCapacity(), stack -> stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fillOnly = new FillOnly();
    private final ItemStackHandler crop = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private boolean harvestOnly;

    // Rebuilt after a load or change, never saved.
    private FieldStatus status = FieldStatus.NO_FIELD;
    private Set<BlockPos> rails = Set.of();
    private Set<BlockPos> plots = Set.of();
    private List<FieldStrip.Box> boxes = List.of();
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private final FieldRipeness<BlockPos> ripeness = new FieldRipeness<>();
    private final Set<BlockPos> rechecks = new HashSet<>();
    private final List<AABBTicket> tickets = new ArrayList<>();
    private boolean refreshPending = true;
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
        }
        if (refreshPending) {
            refreshPending = false;
            refreshField();
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
        Set<BlockPos> plotSet = new HashSet<>();
        for (FieldPos plot : FieldStrip.plots(railPositions)) {
            plotSet.add(new BlockPos(plot.x(), plot.y(), plot.z()));
        }
        plots = Collections.unmodifiableSet(plotSet);
        boxes = FieldStrip.boxes(railPositions);
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
        if (hydrated) {
            dropTickets();
            addTickets();
        }
    }

    private void unserve(FieldStatus why, Set<BlockPos> members) {
        status = why;
        rails = members;
        plots = Set.of();
        boxes = List.of();
        ripeness.clear();
        rechecks.clear();
        dropTickets();
        hydrated = false;
    }

    // Water and hydration

    private void useWater() {
        int need = FieldWater.use(rails.size(), StorageStatsManager.INSTANCE.fieldWaterPerRail());
        if (!hydrated) {
            if (tank.getFluidAmount() >= need) {
                tank.drain(need, IFluidHandler.FluidAction.EXECUTE);
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
            tank.drain(need, IFluidHandler.FluidAction.EXECUTE);
            countdown = StorageStatsManager.INSTANCE.fieldWaterInterval();
            renewTickets();
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

    // Ripeness

    /** Reads every loaded plot again. */
    public void recheckAll() {
        ripeness.clear();
        if (level == null) {
            return;
        }
        for (BlockPos plot : plots) {
            if (level.isLoaded(plot)) {
                update(plot, level.getBlockState(plot));
            }
        }
    }

    /** Reads the plot on the next tick, after the block has changed. */
    public void recheck(BlockPos pos) {
        if (covers(pos)) {
            rechecks.add(pos.immutable());
        }
    }

    private void recheckNow(BlockPos plot) {
        if (level != null && level.isLoaded(plot)) {
            update(plot, level.getBlockState(plot));
        }
    }

    /** Records what a plot holds now, the block on it being that state. */
    void update(BlockPos plot, BlockState state) {
        ripeness.update(plot.immutable(), HarvesterWork.classify(level, plot, state));
    }

    /** Whether the head serves a field whose plots hold that position. */
    public boolean covers(BlockPos pos) {
        return status == FieldStatus.SERVING && plots.contains(pos);
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

    /** The one slot handler behind the GUI's ghost slot. */
    public ItemStackHandler cropHandler() {
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
        setChanged();
    }

    public boolean harvestOnly() {
        return harvestOnly;
    }

    /** Harvest without planting; turning it on clears the crop. */
    public void setHarvestOnly(boolean on) {
        harvestOnly = on;
        if (on) {
            crop.setStackInSlot(0, ItemStack.EMPTY);
        }
        setChanged();
    }

    // Read side for the GUI, the command and m10-harvester

    public FieldStatus status() {
        return status;
    }

    /** The farm rails of the field; for a head that sees another head, the field it would serve. */
    public Set<BlockPos> rails() {
        return rails;
    }

    /** The plots of the served field, empty when not serving. */
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
                && ripeness.due(StorageStatsManager.INSTANCE.fieldHarvestShare());
    }

    public int water() {
        return tank.getFluidAmount();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    /** The fluid handler other blocks and pipes see: takes water, gives nothing back. */
    public IFluidHandler fluidHandler() {
        return fillOnly;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Crop", crop.serializeNBT(registries));
        tag.putBoolean("HarvestOnly", harvestOnly);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Water"));
        if (tag.contains("Crop", CompoundTag.TAG_COMPOUND)) {
            crop.deserializeNBT(registries, tag.getCompound("Crop"));
        }
        harvestOnly = tag.getBoolean("HarvestOnly");
    }

    /** Fills the tank with water; never drains. */
    private final class FillOnly implements IFluidHandler {

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
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
