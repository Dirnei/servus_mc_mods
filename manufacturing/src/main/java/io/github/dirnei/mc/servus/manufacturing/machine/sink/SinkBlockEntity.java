package io.github.dirnei.mc.servus.manufacturing.machine.sink;

import io.github.dirnei.mc.servus.lib.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideConfig;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.manufacturing.menu.SinkMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import java.util.List;
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
 * The Sink: voids items, fluid and energy at a rate set by the Chimney column
 * on top. Has no ticker; budgets refill lazily from the game time and the
 * active state is cleared by a scheduled block tick.
 */
public class SinkBlockEntity extends BlockEntity implements MenuProvider, SidedBlockEntity {

    public static final int DATA_HEIGHT = 0;
    public static final int DATA_ITEMS = 1;
    public static final int DATA_FLUID = 2;
    public static final int DATA_ENERGY = 3;
    public static final int DATA_SIDE_FIRST = 4;
    public static final int DATA_COUNT = DATA_SIDE_FIRST + 6;

    /** How far up the column is walked, and down from a Chimney to its Sink. */
    public static final int MAX_COLUMN = 32;

    private final SideConfig sideConfig = new SideConfig(List.of(SideMode.INPUT, SideMode.DISABLED), SideMode.INPUT);
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
            if (index >= DATA_SIDE_FIRST && index < DATA_COUNT) {
                return sideConfig.mode(RelativeSide.byOrdinal(index - DATA_SIDE_FIRST)).ordinal();
            }
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
    private long lastVoided = Long.MIN_VALUE / 2;

    public SinkBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.SINK.get(), pos, state);
        // The fan sits in the front: nothing goes in there.
        sideConfig.lock(RelativeSide.FRONT, SideMode.DISABLED);
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

    /** Counts the Chimney blocks straight above and syncs the height when it changed. */
    public void recountChimney() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int height = countColumn(level, worldPosition);
        if (height != chimneyHeight) {
            chimneyHeight = height;
            BlockState state = getBlockState();
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
            recountChimney();
            BlockState state = getBlockState();
            // lastVoided is not saved: an active Sink found on load turns idle with one scheduled tick.
            if (state.hasProperty(SinkBlock.ACTIVE) && state.getValue(SinkBlock.ACTIVE)
                    && !level.getBlockTicks().hasScheduledTick(worldPosition, state.getBlock())) {
                level.scheduleTick(worldPosition, state.getBlock(), 1);
            }
        }
    }

    // Capabilities

    /** The item handler automation sees from {@code side}, or null on an Off side. */
    public IItemHandler itemHandler(Direction side) {
        return accepts(side) ? itemHandler : null;
    }

    public IFluidHandler fluidHandler(Direction side) {
        return accepts(side) ? fluidHandler : null;
    }

    public IEnergyStorage energyStorage(Direction side) {
        return accepts(side) ? energyHandler : null;
    }

    /** A fluid handler without a rate limit, for buckets used by hand. */
    public IFluidHandler manualFluidHandler() {
        return manualFluidHandler;
    }

    private boolean accepts(Direction side) {
        return side == null || sideMode(side).allowsInsert();
    }

    // Sides

    @Override
    public SideConfig sideConfig() {
        return sideConfig;
    }

    @Override
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(SinkBlock.FACING) ? state.getValue(SinkBlock.FACING) : Direction.NORTH;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sideConfig.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
    }

    public ContainerData containerData() {
        return data;
    }

    // Persistence: the side configuration only; the height is recounted on load.

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!sideConfig.isDefault()) {
            tag.putByteArray("Sides", sideConfig.toBytes());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sideConfig.fromBytes(tag.contains("Sides") ? tag.getByteArray("Sides") : new byte[0]);
        if (tag.contains("Height")) {
            // Only the client update tag carries the height.
            chimneyHeight = tag.getInt("Height");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray("Sides", sideConfig.toBytes());
        tag.putInt("Height", chimneyHeight);
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
