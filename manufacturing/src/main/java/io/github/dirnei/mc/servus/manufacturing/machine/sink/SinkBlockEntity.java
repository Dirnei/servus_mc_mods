package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import io.github.dirnei.mc.servus.manufacturing.menu.SinkMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A Sink block entity. Every block of the cube has one, but only the
 * controller's (part 0) is used: it voids items, fluid and energy at a rate set
 * by the tallest Chimney column on top. The other parts route their
 * capabilities to it. Has no ticker; budgets refill lazily from the game time
 * and the active state is cleared by a scheduled block tick.
 */
public class SinkBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DATA_HEIGHT = 0;
    public static final int DATA_ITEMS = 1;
    public static final int DATA_FLUID = 2;
    public static final int DATA_ENERGY = 3;
    public static final int DATA_COUNT = 4;

    /** How far up the column is walked, and down from a Chimney to its Sink. */
    public static final int MAX_COLUMN = 32;

    private final VoidBudget itemBudget = new VoidBudget();
    private final VoidBudget fluidBudget = new VoidBudget();
    private final VoidBudget energyBudget = new VoidBudget();
    private final SinkHandlers.Items itemHandler = new SinkHandlers.Items(this, false);
    private final SinkHandlers.Fluid fluidHandler = new SinkHandlers.Fluid(this, false);
    private final SinkHandlers.Energy energyHandler = new SinkHandlers.Energy(this);
    private final SinkHandlers.Fluid manualFluidHandler = new SinkHandlers.Fluid(this, true);
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            SinkRates rates = rates();
            return switch (index) {
                case DATA_HEIGHT -> chimneyHeight;
                case DATA_ITEMS -> rates.itemsDisplay(chimneyHeight);
                case DATA_FLUID -> rates.fluidDisplay(chimneyHeight);
                case DATA_ENERGY -> rates.energyDisplay(chimneyHeight);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private int chimneyHeight;
    /** The top part carrying the tallest column, or -1 without a chimney; synced for the smoke. */
    private int chimneyColumn = -1;
    private long lastVoided = Long.MIN_VALUE / 2;
    /** Client only: the fan's current angle in degrees and the frame time it was advanced to. */
    public float fanAngle;
    public double fanTime = -1;

    public SinkBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.SINK.get(), pos, state);
    }

    // Rates and budgets

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

    /** How many of {@code requested} items the Sink voids now; spends the budget unless simulating. */
    long grantItems(long requested, boolean simulate) {
        return grant(itemBudget, requested, rates().items(chimneyHeight), simulate);
    }

    long grantFluid(long requested, boolean simulate) {
        return grant(fluidBudget, requested, rates().fluid(chimneyHeight), simulate);
    }

    long grantEnergy(long requested, boolean simulate) {
        return grant(energyBudget, requested, rates().energy(chimneyHeight), simulate);
    }

    private long grant(VoidBudget budget, long requested, double rate, boolean simulate) {
        if (level == null || level.isClientSide()) {
            return 0;
        }
        long granted = simulate ? budget.peek(requested, gameTime(), rate) : budget.take(requested, gameTime(), rate);
        if (granted > 0 && !simulate) {
            markVoided();
        }
        return granted;
    }

    /** Something was voided: turn active and schedule the tick that turns it idle again. */
    public void markVoided() {
        if (level == null || level.isClientSide()) {
            return;
        }
        lastVoided = level.getGameTime();
        BlockState state = getBlockState();
        if (state.hasProperty(SinkBlock.ACTIVE) && !state.getValue(SinkBlock.ACTIVE)) {
            level.setBlock(worldPosition, state.setValue(SinkBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(worldPosition, state.getBlock(), stats().activeTicks());
        }
    }

    /** Ticks until the active time ends, or 0 when it has ended. */
    public int activeTicksLeft() {
        long left = lastVoided + stats().activeTicks() - gameTime();
        return (int) Math.max(0, Math.min(left, Integer.MAX_VALUE));
    }

    // Chimney

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
            // lastVoided is not saved: an active Sink found on load turns idle with one scheduled tick.
            if (state.hasProperty(SinkBlock.ACTIVE) && state.getValue(SinkBlock.ACTIVE)
                    && !level.getBlockTicks().hasScheduledTick(worldPosition, state.getBlock())) {
                level.scheduleTick(worldPosition, state.getBlock(), 1);
            }
        }
    }

    // Capabilities

    /** The item handler automation sees on this part from {@code side}, or null on a casing or a front face. */
    public IItemHandler itemHandler(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.itemHandler;
    }

    public IFluidHandler fluidHandler(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.fluidHandler;
    }

    public IEnergyStorage energyStorage(Direction side) {
        SinkBlockEntity controller = acceptingController(side);
        return controller == null ? null : controller.energyHandler;
    }

    /** A fluid handler without a rate limit, for buckets used by hand. */
    public IFluidHandler manualFluidHandler() {
        return manualFluidHandler;
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

    public ContainerData containerData() {
        return data;
    }

    // Persistence: nothing is saved; the height is recounted on load and only the client update tag carries it.

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Height")) {
            chimneyHeight = tag.getInt("Height");
            chimneyColumn = tag.getInt("Column");
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

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SinkMenu(containerId, playerInventory, this);
    }
}
