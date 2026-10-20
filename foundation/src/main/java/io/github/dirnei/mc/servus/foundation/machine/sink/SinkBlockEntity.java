package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.CapacityUpgradeSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A Sink block entity. Every block of the cube has one, but only the
 * controller's (part 0) is used: it holds the buffer of item slots and fluid
 * tanks, voids it in batches every {@link SinkRates#BATCH_TICKS} ticks at the
 * rate of the tallest Chimney column on top, and destroys energy at once. The
 * other parts route their capabilities to it. Has no ticker: one scheduled
 * block tick on the controller, the heartbeat, runs while the buffer holds
 * something or the Sink is active.
 */
public class SinkBlockEntity extends BlockEntity implements MenuProvider {

    /** How far up the column is walked, and down from a Chimney to its Sink. */
    public static final int MAX_COLUMN = 32;
    /** The value of {@link #ticksToBatch()} while no batch waits. */
    public static final int NO_BATCH = -1;

    private static final String ITEMS = "Items";
    private static final String TANKS = "Tanks";
    private static final String UPGRADES = "Upgrades";
    private static final String LAST_BATCH = "LastBatch";

    private final SinkCapacity capacity;
    private final ItemBuffer items;
    private final FluidBuffer tanks;
    private final CapacityUpgradeSlots upgrades;
    private final ResourceHandler<ItemResource> itemView;
    private final ResourceHandler<FluidResource> fluidView;
    private final VoidBudget energyBudget = new VoidBudget();
    private final SinkHandlers.Energy energyHandler = new SinkHandlers.Energy(this);

    private int chimneyHeight;
    /** The top part carrying the tallest column, or -1 without a chimney; synced for the smoke. */
    private int chimneyColumn = -1;
    private long lastVoided = Long.MIN_VALUE / 2;
    private long lastBatch = Long.MIN_VALUE / 2;
    /** The game time the heartbeat is scheduled for. */
    private long heartbeatAt;
    /** Set while a batch runs or the buffer is cleared, so these changes do not reschedule the heartbeat. */
    private boolean batching;
    /** Client only: the fan's current angle in degrees and the frame time it was advanced to. */
    public float fanAngle;
    public double fanTime = -1;

    public SinkBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.SINK_ENTITY.get(), pos, state);
        SinkStats stats = stats();
        capacity = stats.capacity();
        items = new ItemBuffer(capacity.maxSlots());
        tanks = new FluidBuffer(capacity.maxRows(), stats.tankCapacity());
        upgrades = new CapacityUpgradeSlots(stats.upgradeSlots(), this::upgradesChanged);
        itemView = new SinkHandlers.InsertOnly<>(items);
        fluidView = new SinkHandlers.InsertOnly<>(tanks);
    }

    private static SinkStats stats() {
        return SinkStatsManager.INSTANCE.stats();
    }

    private static SinkRates rates() {
        return stats().rates();
    }

    public int chimneyHeight() {
        return chimneyHeight;
    }

    /** The top part whose column is the tallest, or -1 without a chimney. */
    public int chimneyColumn() {
        return chimneyColumn;
    }

    private long gameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    /** The buffer layout, fixed when this block entity was created. */
    public SinkCapacity capacity() {
        return capacity;
    }

    /** The item slots, open and closed; the menu works on these, automation on an insert-only view. */
    public ItemSlots items() {
        return items;
    }

    public CapacityUpgradeSlots upgrades() {
        return upgrades;
    }

    /** Open rows, and open tanks, with the upgrades held now. */
    public int openRows() {
        return capacity.openRows(upgrades.count());
    }

    public boolean isSlotOpen(int slot) {
        return capacity.rowOf(slot) < openRows();
    }

    public boolean isTankOpen(int tank) {
        return tank < openRows();
    }

    public int tankCount() {
        return tanks.size();
    }

    /** What a tank holds; do not change the stack. */
    public FluidStack tank(int tank) {
        return tanks.stack(tank);
    }

    public int tankCapacity() {
        return tanks.capacity();
    }

    /** Whether one Capacity Upgrade may come out: there is one, and the last open row and tank are empty. */
    public boolean canRemoveUpgrade() {
        int lastRow = openRows() - 1;
        int rowStart = lastRow * capacity.slotsPerRow();
        boolean rowEmpty = true;
        for (int slot = rowStart; slot < rowStart + capacity.slotsPerRow(); slot++) {
            rowEmpty &= items.getStackInSlot(slot).isEmpty();
        }
        return SinkCapacity.canRemoveUpgrade(upgrades.count(), rowEmpty, tanks.getAmountAsLong(lastRow) == 0);
    }

    /** Whether any slot or tank holds something. */
    public boolean holdsAnything() {
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        for (int tank = 0; tank < tanks.size(); tank++) {
            if (tanks.getAmountAsLong(tank) > 0) {
                return true;
            }
        }
        return false;
    }

    long grantEnergy(long requested, boolean simulate) {
        if (level == null || level.isClientSide()) {
            return 0;
        }
        double rate = rates().energy(chimneyHeight);
        long granted = simulate ? energyBudget.peek(requested, gameTime(), rate) : energyBudget.take(requested, gameTime(), rate);
        if (granted > 0 && !simulate) {
            markVoided();
        }
        return granted;
    }

    /** Something was voided: turn active and make sure the heartbeat runs to turn it idle again. */
    public void markVoided() {
        if (level == null || level.isClientSide()) {
            return;
        }
        lastVoided = level.getGameTime();
        BlockState state = getBlockState();
        if (state.hasProperty(SinkBlock.ACTIVE) && !state.getValue(SinkBlock.ACTIVE)) {
            level.setBlock(worldPosition, state.setValue(SinkBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
        }
        wake();
    }

    /** Ticks until the active time ends, or 0 when it has ended. */
    public int activeTicksLeft() {
        long left = lastVoided + stats().activeTicks() - gameTime();
        return (int) Math.max(0, Math.min(left, Integer.MAX_VALUE));
    }

    /** Schedules the heartbeat one batch ahead unless it is already scheduled. Server side. */
    public void wake() {
        if (level == null || level.isClientSide() || batching) {
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(worldPosition, getBlockState().getBlock())) {
            scheduleHeartbeat(SinkRates.BATCH_TICKS);
        }
    }

    private void scheduleHeartbeat(int delay) {
        heartbeatAt = gameTime() + delay;
        level.scheduleTick(worldPosition, getBlockState().getBlock(), delay);
    }

    /**
     * The scheduled tick: voids one batch when one is due, then schedules the
     * next heartbeat while the buffer holds something or the Sink is active,
     * or turns the Sink idle.
     */
    void heartbeat() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        boolean controller = SinkBlock.isFormed(state) && state.getValue(SinkBlock.PART) == SinkLayout.CONTROLLER;
        long now = gameTime();
        if (controller && holdsAnything() && now - lastBatch >= SinkRates.BATCH_TICKS) {
            lastBatch = now;
            voidBatch();
            setChanged();
        }
        int activeLeft = activeTicksLeft();
        if (controller && holdsAnything()) {
            scheduleHeartbeat((int) Math.max(1, lastBatch + SinkRates.BATCH_TICKS - now));
        } else if (controller && activeLeft > 0) {
            // Capped at one batch, so items that arrive while active wait no longer than a batch.
            scheduleHeartbeat(Math.min(activeLeft, SinkRates.BATCH_TICKS));
        } else if (state.getValue(SinkBlock.ACTIVE)) {
            level.setBlock(worldPosition, state.setValue(SinkBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
        }
    }

    /** Voids up to the item rate from the first slot on and up to the fluid rate from the first tank on. */
    private void voidBatch() {
        SinkRates rates = rates();
        int[] itemCounts = new int[items.size()];
        for (int slot = 0; slot < itemCounts.length; slot++) {
            itemCounts[slot] = items.getStackInSlot(slot).getCount();
        }
        int[] fluidAmounts = new int[tanks.size()];
        for (int tank = 0; tank < fluidAmounts.length; tank++) {
            fluidAmounts[tank] = (int) tanks.getAmountAsLong(tank);
        }
        int[] itemTaken = SinkBatch.plan(itemCounts, rates.itemsPerBatch(chimneyHeight));
        int[] fluidTaken = SinkBatch.plan(fluidAmounts, rates.fluidPerBatch(chimneyHeight));
        boolean voided = false;
        batching = true;
        try {
            for (int slot = 0; slot < itemTaken.length; slot++) {
                if (itemTaken[slot] > 0) {
                    ItemStack stack = items.getStackInSlot(slot);
                    items.setStackInSlot(slot, stack.copyWithCount(stack.getCount() - itemTaken[slot]));
                    voided = true;
                }
            }
            for (int tank = 0; tank < fluidTaken.length; tank++) {
                if (fluidTaken[tank] > 0) {
                    int left = fluidAmounts[tank] - fluidTaken[tank];
                    tanks.set(tank, left > 0 ? tanks.getResource(tank) : FluidResource.EMPTY, left);
                    voided = true;
                }
            }
        } finally {
            batching = false;
        }
        if (voided) {
            markVoided();
        }
    }

    /** Ticks until the next batch, or {@link #NO_BATCH} while the buffer is empty. */
    public int ticksToBatch() {
        if (!holdsAnything()) {
            return NO_BATCH;
        }
        long due = Math.max(heartbeatAt, lastBatch + SinkRates.BATCH_TICKS);
        return (int) Math.clamp(due - gameTime(), 0, SinkRates.BATCH_TICKS);
    }

    /**
     * The Sink is unformed or its controller goes away: drops the Capacity
     * Upgrades at the controller and voids the buffer. Server side.
     */
    public void dissolve() {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (ItemStack upgrade : upgrades.takeAll()) {
            Block.popResource(level, worldPosition, upgrade);
        }
        batching = true;
        try {
            for (int slot = 0; slot < items.size(); slot++) {
                if (!items.getStackInSlot(slot).isEmpty()) {
                    items.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            for (int tank = 0; tank < tanks.size(); tank++) {
                if (tanks.getAmountAsLong(tank) > 0) {
                    tanks.set(tank, FluidResource.EMPTY, 0);
                }
            }
        } finally {
            batching = false;
        }
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        dissolve();
    }

    private void upgradesChanged() {
        setChanged();
        wake();
    }

    private void bufferChanged() {
        setChanged();
        wake();
    }

    /** Counts the columns on the four top parts, keeps the tallest and syncs it when it changed. */
    public void recountChimney() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        int height = 0;
        int column = -1;
        if (SinkBlock.isFormed(state) && state.getValue(SinkBlock.PART) == SinkLayout.CONTROLLER) {
            for (int part : SinkLayout.topParts()) {
                int count = countColumn(level, SinkBlock.partPos(worldPosition, state.getValue(SinkBlock.FACING), part));
                if (count > height) {
                    height = count;
                    column = part;
                }
            }
        }
        if (height != chimneyHeight || column != chimneyColumn) {
            chimneyHeight = height;
            chimneyColumn = column;
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** The number of Chimney blocks straight above {@code pos}, at most {@link #MAX_COLUMN}. */
    public static int countColumn(Level level, BlockPos pos) {
        int height = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (height < MAX_COLUMN) {
            cursor.move(Direction.UP);
            if (!(level.getBlockState(cursor).getBlock() instanceof ChimneyBlock)) {
                break;
            }
            height++;
        }
        return height;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getValue(SinkBlock.PART) == SinkLayout.CONTROLLER) {
                recountChimney();
            }
            if (holdsAnything() || state.getValue(SinkBlock.ACTIVE)) {
                wake();
            }
        }
    }

    /** The item handler automation sees on this part from {@code side}, or null on a casing or a front face. */
    public ResourceHandler<ItemResource> itemHandler(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.itemView;
    }

    public ResourceHandler<FluidResource> fluidHandler(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.fluidView;
    }

    public EnergyHandler energyStorage(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.energyHandler;
    }

    /** The insert-only fluid handler senders see, for buckets used by hand. */
    public ResourceHandler<FluidResource> fluidInput() {
        return fluidView;
    }

    /** The controller this part hands {@code side} to: none on a casing, an orphaned part or the fan's front faces. */
    private SinkBlockEntity acceptingController(Direction side) {
        if (level == null) {
            return null;
        }
        BlockState state = getBlockState();
        if (!SinkBlock.isFormed(state)
                || side != null && side == state.getValue(SinkBlock.FACING) && SinkLayout.isFront(state.getValue(SinkBlock.PART))) {
            return null;
        }
        return SinkBlock.controller(level, worldPosition, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.save(output, ITEMS);
        tanks.save(output, TANKS);
        upgrades.save(output, UPGRADES);
        output.putLong(LAST_BATCH, lastBatch);
    }

    /** The chimney is recounted on load, so only the client update tag carries it. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.load(input, ITEMS);
        tanks.load(input, TANKS);
        upgrades.load(input, UPGRADES);
        lastBatch = input.getLongOr(LAST_BATCH, Long.MIN_VALUE / 2);
        if (input.getInt("Height").isPresent()) {
            chimneyHeight = input.getIntOr("Height", 0);
            chimneyColumn = input.getIntOr("Column", 0);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Height", chimneyHeight);
        tag.putInt("Column", chimneyColumn);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SinkMenu(containerId, playerInventory, this);
    }

    /** The item slots: closed rows take nothing, inserts stack onto matching slots first. */
    private final class ItemBuffer extends ItemSlots {

        ItemBuffer(int size) {
            super(size);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.isEmpty() || isSlotOpen(index);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return isSlotOpen(index) ? super.getCapacity(index, resource) : 0;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return ResourceHandlerUtil.insertStacking(this, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int slot) {
            bufferChanged();
        }
    }

    /** The fluid tanks: closed tanks take nothing, inserts go into tanks of the same fluid first. */
    private final class FluidBuffer extends FluidStacksResourceHandler {

        FluidBuffer(int size, int capacity) {
            super(size, capacity);
        }

        int capacity() {
            return capacity;
        }

        FluidStack stack(int tank) {
            return stacks.get(tank);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty() || isTankOpen(index);
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return isTankOpen(index) ? capacity : 0;
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            return ResourceHandlerUtil.insertStacking(this, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            bufferChanged();
        }

        void save(ValueOutput output, String key) {
            serialize(output.child(key));
        }

        /** Loads into the fixed number of tanks: extra saved tanks are dropped, missing ones emptied. */
        void load(ValueInput input, String key) {
            Optional<NonNullList<FluidStack>> saved = input.child(key).flatMap(child -> child.read(VALUE_IO_KEY, codec));
            NonNullList<FluidStack> list = saved.orElse(NonNullList.create());
            for (int tank = 0; tank < size(); tank++) {
                stacks.set(tank, tank < list.size() ? list.get(tank) : FluidStack.EMPTY);
            }
        }
    }
}
