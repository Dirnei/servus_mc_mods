package io.github.dirnei.mc.servus.shuttles.gantry;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.upgrade.CapacityUpgradeSlots;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The master of a Fluid Gantry, kept by its {@link GantryPart#LEFT_LOW} part:
 * the two tanks (0 left, 1 right) with their locks and keep levels, the mode
 * and the Capacity Upgrades. Both tanks hold {@code tank_capacity} plus {@code
 * capacity_per_upgrade} per upgrade. When the block goes away the gantry
 * drops one item that keeps all of it, unless a creative player broke it.
 */
public class FluidGantryBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    private static final String[] TANK_KEYS = {"Left", "Right"};
    private static final String[] LOCK_KEYS = {"LeftLock", "RightLock"};
    private static final String[] KEEP_KEYS = {"LeftKeep", "RightKeep"};
    private static final String MODE = "Mode";
    private static final String UPGRADES = "Upgrades";
    private static final String OWN_RAIL = "OwnRail";
    private static final String[] SHOWN_FLUID_KEYS = {"ShownLeft", "ShownRight"};
    private static final String[] SHOWN_STEP_KEYS = {"StepLeft", "StepRight"};
    private static final String STEPS = "Steps";
    private static final String PUMP_TANK = "PumpTank";
    private static final String PUMP_INTO = "PumpInto";
    private static final String PUMP_FLUID = "PumpFluid";

    private final Tank[] tanks = {new Tank(0), new Tank(1)};
    /** The locked fluid of each tank, null for Any. */
    private final Fluid[] locks = new Fluid[2];
    /** The keep level of each tank in mB, kept in every mode but applied only to locked Export tanks. */
    private final int[] keeps = new int[2];
    private GantryMode mode = GantryMode.STORAGE;
    private final CapacityUpgradeSlots upgrades =
            new CapacityUpgradeSlots(ShuttlesStatsManager.INSTANCE.gantryUpgradeSlots(), this::upgradesChanged);
    private boolean dropOnRemove = true;
    /** The gantry laid the rail under its beam, so the rail goes when the gantry goes. */
    private boolean ownRail;
    private final GantryFluidHandler manualHandler = new GantryFluidHandler(this, true, true);
    private final GantryFluidHandler importHandler = new GantryFluidHandler(this, true, false);
    private final GantryFluidHandler exportHandler = new GantryFluidHandler(this, false, true);
    /** Server: what watching clients were last sent; the block is sent again only when it changes. */
    private @Nullable Shown sent;
    /** Client: what the columns show, as the server last sent it. */
    private Shown shown = Shown.EMPTY;
    /** Server: the fluid of an Import tank that no Storage tank has room for, null when none waits. */
    private @Nullable Fluid noRoom;
    /** Server: whether an Import tank holds fluid that no Tanker shuttle comes for. */
    private boolean noTanker;
    /** Server: the fluid a kept Export tank lacks that no Storage tank holds, null when none is missing. */
    private @Nullable Fluid missing;

    /** What clients draw: each column's fluid and fill step out of {@code steps}, and the spout. */
    private record Shown(Fluid left, int leftStep, Fluid right, int rightStep, int steps, @Nullable Fluid leftLock,
                         @Nullable Fluid rightLock, @Nullable Pumping pumping) {

        static final Shown EMPTY = new Shown(Fluids.EMPTY, 0, Fluids.EMPTY, 0, 1, null, null, null);
    }

    /**
     * A tanker stops under the gantry and its spout pumps.
     *
     * @param tank       the gantry tank the fluid comes from or goes to
     * @param intoGantry whether it flows up into the gantry rather than down into the tanker
     * @param fluid      the fluid of the stream
     */
    public record Pumping(int tank, boolean intoGantry, Fluid fluid) {
    }

    /** Server: the spout's stop, null while no tanker pumps here. */
    private @Nullable Pumping pumping;
    /** Client: game time the spout last started to lower or rise. */
    private long pumpChangedAt = Long.MIN_VALUE / 2;

    public FluidGantryBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FLUID_GANTRY.get(), pos, state);
        applyCapacity();
    }

    /** One built-in tank; it takes a fluid only as its lock and content allow. */
    private final class Tank extends FluidSlot {

        private final int index;

        Tank(int index) {
            super(ShuttlesStatsManager.INSTANCE.gantryTankCapacity());
            this.index = index;
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return rule(index).accepts(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    }

    /** The tank with that index, 0 left and 1 right. */
    public FluidSlot tank(int tank) {
        return tanks[tank];
    }

    /** The tank as the lock rules see it, keyed by fluid. */
    public GantryTanks.Tank<Fluid> rule(int tank) {
        FluidStack fluid = tanks[tank].getFluid();
        return new GantryTanks.Tank<>(locks[tank], fluid.isEmpty() ? null : fluid.getFluid(), fluid.getAmount(), tanks[tank].getCapacity());
    }

    /** Both tanks as the lock rules see them. */
    public List<GantryTanks.Tank<Fluid>> rules() {
        return List.of(rule(0), rule(1));
    }

    /** The locked fluid of a tank, null for Any. */
    public Fluid lock(int tank) {
        return locks[tank];
    }

    /** Locks a tank to a fluid, or to Any with null; the content stays. */
    public void setLock(int tank, Fluid fluid) {
        Fluid lock = fluid == null || fluid == Fluids.EMPTY ? null : fluid;
        if (locks[tank] == lock) {
            return;
        }
        locks[tank] = lock;
        contentsChanged();
    }

    public GantryMode mode() {
        return mode;
    }

    /** The keep level of a tank in mB as set, whatever the mode and lock. */
    public int keep(int tank) {
        return keeps[tank];
    }

    /** The level tankers restock a tank to: its keep level while it is a locked Export tank, else 0. */
    public int keptLevel(int tank) {
        return mode == GantryMode.EXPORT && locks[tank] != null ? keeps[tank] : 0;
    }

    /** Sets a tank's keep level, limited to 0 and the tank's capacity. */
    public void setKeep(int tank, int amount) {
        int keep = Math.clamp(amount, 0, capacity());
        if (keeps[tank] != keep) {
            keeps[tank] = keep;
            setChanged();
        }
    }

    /**
     * Sets what the gantry's activity lamp shows and why it is red: the fluid
     * that no Storage tank has room for, no Tanker shuttle at all, or the
     * fluid a kept Export tank lacks that no Storage tank holds. Set by the
     * warehouse, never by the player.
     */
    public void setActivity(StorageNodeBlocks.Activity activity, @Nullable Fluid noRoom, boolean noTanker, @Nullable Fluid missing) {
        this.noRoom = noRoom;
        this.noTanker = noTanker;
        this.missing = missing;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity), Block.UPDATE_CLIENTS);
        }
    }

    /** The fluid that waits because no Storage tank has room for it, null when none does. */
    public @Nullable Fluid noRoom() {
        return noRoom;
    }

    /** The fluid a kept Export tank lacks that no Storage tank holds, null when none is missing. */
    public @Nullable Fluid missing() {
        return missing;
    }

    /** Whether fluid waits because no Tanker shuttle comes for it. */
    public boolean noTanker() {
        return noTanker;
    }

    /** Switches the mode; the beam's sign follows and pipes beside the legs look again. */
    public void setMode(GantryMode mode) {
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        contentsChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(FluidGantryBlock.MODE) && state.getValue(FluidGantryBlock.MODE) != mode) {
                level.setBlock(worldPosition, state.setValue(FluidGantryBlock.MODE, mode), Block.UPDATE_CLIENTS);
            }
            FluidGantryBlock.invalidateCapabilities(level, worldPosition, getBlockState());
        }
    }

    /** The handler pipes and other mods see on the legs: none in Storage, fill only in Import, drain only in Export. */
    public @Nullable ResourceHandler<FluidResource> pipeHandler() {
        return switch (mode) {
            case STORAGE -> null;
            case IMPORT -> importHandler;
            case EXPORT -> exportHandler;
        };
    }

    /** The handler buckets use by hand, in every mode: fills by the locks, drains either tank. */
    public ResourceHandler<FluidResource> manualHandler() {
        return manualHandler;
    }

    public CapacityUpgradeSlots upgrades() {
        return upgrades;
    }

    /** The capacity of each tank with the upgrades in the slots. */
    public int capacity() {
        ShuttlesStatsManager stats = ShuttlesStatsManager.INSTANCE;
        return GantryCapacity.capacity(stats.gantryTankCapacity(), stats.gantryCapacityPerUpgrade(), upgrades.count());
    }

    /** Whether one Capacity Upgrade may come out: both tanks still fit without it. */
    public boolean canRemoveUpgrade() {
        ShuttlesStatsManager stats = ShuttlesStatsManager.INSTANCE;
        int largest = Math.max(tanks[0].getFluidAmount(), tanks[1].getFluidAmount());
        return GantryCapacity.canRemoveUpgrade(stats.gantryTankCapacity(), stats.gantryCapacityPerUpgrade(), upgrades.count(), largest);
    }

    private void applyCapacity() {
        int capacity = capacity();
        for (int tank = 0; tank < 2; tank++) {
            tanks[tank].setCapacity(capacity);
            keeps[tank] = Math.min(keeps[tank], capacity);
        }
    }

    private void upgradesChanged() {
        applyCapacity();
        contentsChanged();
    }

    /** Called after a tank, a lock, the mode or the upgrades changed. */
    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            network().ifPresent(network -> network.post(new WarehouseEvent.FluidChanged(worldPosition)));
            sendShown();
        }
    }

    /** Sends the block to watching clients when what they draw changed. */
    private void sendShown() {
        Shown now = shownNow();
        if (level != null && !now.equals(sent)) {
            sent = now;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private Shown shownNow() {
        int steps = ShuttlesStatsManager.INSTANCE.gantryLevelSteps();
        return new Shown(tanks[0].getFluid().getFluid(), step(0, steps), tanks[1].getFluid().getFluid(), step(1, steps), steps, locks[0], locks[1],
                pumping);
    }

    /**
     * Server: a tanker's stop under the gantry starts or ends. Clients learn
     * only these two moments and lower or raise the spout on their own.
     */
    public void setPumping(int tank, boolean intoGantry, FluidStack fluid, boolean on) {
        pumping = on && !fluid.isEmpty() ? new Pumping(tank, intoGantry, fluid.getFluid()) : null;
        if (level != null && !level.isClientSide()) {
            sendShown();
        }
    }

    /** Client: the spout's stop as last sent, null while none runs. */
    public @Nullable Pumping shownPumping() {
        return shown.pumping();
    }

    /** Client: the game time the spout last started to lower or rise. */
    public long pumpChangedAt() {
        return pumpChangedAt;
    }

    private int step(int tank, int steps) {
        return GantryLook.level(tanks[tank].getFluidAmount(), tanks[tank].getCapacity(), steps);
    }

    /** Client: the fluid the column of a tank shows, empty when it shows none. */
    public FluidStack shownFluid(int tank) {
        Fluid fluid = tank == 0 ? shown.left() : shown.right();
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
    }

    /** Client: how full the column of a tank shows, from 0 to 1 in the synced steps. */
    public float shownFill(int tank) {
        return (float) (tank == 0 ? shown.leftStep() : shown.rightStep()) / Math.max(1, shown.steps());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        Shown now = shownNow();
        sent = now;
        for (int tank = 0; tank < 2; tank++) {
            tag.putString(SHOWN_FLUID_KEYS[tank], BuiltInRegistries.FLUID.getKey(tank == 0 ? now.left() : now.right()).toString());
            tag.putInt(SHOWN_STEP_KEYS[tank], tank == 0 ? now.leftStep() : now.rightStep());
            Optional<Identifier> lock = lockId(tank);
            if (lock.isPresent()) {
                tag.putString(LOCK_KEYS[tank], lock.get().toString());
            }
        }
        tag.putInt(STEPS, now.steps());
        tag.putString(MODE, mode.getSerializedName());
        if (now.pumping() != null) {
            tag.putInt(PUMP_TANK, now.pumping().tank());
            tag.putBoolean(PUMP_INTO, now.pumping().intoGantry());
            tag.putString(PUMP_FLUID, BuiltInRegistries.FLUID.getKey(now.pumping().fluid()).toString());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.GANTRY;
    }

    /** Only the inner face of the left leg, toward the rail between the legs, takes the rail. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(railSide());
    }

    /** The side of the master that faces the rail between the legs. */
    public Direction railSide() {
        BlockState state = getBlockState();
        Direction.Axis axis = state.hasProperty(FluidGantryBlock.AXIS) ? state.getValue(FluidGantryBlock.AXIS) : Direction.Axis.Z;
        return axis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
    }

    /** The rail keeps its straight track through the gantry. */
    @Override
    public boolean showsDock() {
        return false;
    }

    @Override
    public boolean bendsRail() {
        return false;
    }

    /** What the dropped item keeps. */
    public GantryContents contents() {
        return new GantryContents(tanks[0].getFluid().copy(), tanks[1].getFluid().copy(), lockId(0), lockId(1), mode, upgrades.count(),
                keeps[0], keeps[1]);
    }

    private Optional<Identifier> lockId(int tank) {
        return Optional.ofNullable(locks[tank]).map(BuiltInRegistries.FLUID::getKey);
    }

    /** Takes over what a placed item kept. */
    public void applyContents(GantryContents contents) {
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            upgrades.setStackInSlot(slot, slot < contents.upgrades()
                    ? new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()) : ItemStack.EMPTY);
        }
        applyCapacity();
        for (int tank = 0; tank < 2; tank++) {
            locks[tank] = contents.lock(tank).flatMap(BuiltInRegistries.FLUID::getOptional).filter(fluid -> fluid != Fluids.EMPTY).orElse(null);
            tanks[tank].setFluid(contents.tank(tank).copy());
            keeps[tank] = Math.clamp(contents.keep(tank), 0, capacity());
        }
        mode = contents.mode();
        contentsChanged();
    }

    /** The gantry item with what this gantry holds. */
    public ItemStack dropItem() {
        ItemStack stack = new ItemStack(ServusShuttlesItems.FLUID_GANTRY.get());
        GantryContents contents = contents();
        if (!contents.isEmpty()) {
            stack.set(ServusShuttlesComponents.GANTRY_CONTENTS.get(), contents);
        }
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidGantryMenu(containerId, playerInventory, this);
    }

    /** The rail under the beam is the gantry's own: it laid it on the floor. */
    public void markOwnRail() {
        ownRail = true;
        setChanged();
    }

    public boolean ownsRail() {
        return ownRail;
    }

    /** Removing the gantry drops no item: a creative player broke it. */
    public void dropNothing() {
        dropOnRemove = false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int tank = 0; tank < 2; tank++) {
            tanks[tank].save(output, TANK_KEYS[tank]);
            String key = LOCK_KEYS[tank];
            lockId(tank).ifPresent(id -> output.putString(key, id.toString()));
            output.putInt(KEEP_KEYS[tank], keeps[tank]);
        }
        output.store(MODE, GantryMode.CODEC, mode);
        upgrades.save(output, UPGRADES);
        output.putBoolean(OWN_RAIL, ownRail);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        upgrades.load(input, UPGRADES);
        applyCapacity();
        for (int tank = 0; tank < 2; tank++) {
            tanks[tank].load(input, TANK_KEYS[tank]);
            locks[tank] = input.getString(LOCK_KEYS[tank]).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                    .filter(fluid -> fluid != Fluids.EMPTY).orElse(null);
            keeps[tank] = Math.clamp(input.getIntOr(KEEP_KEYS[tank], 0), 0, capacity());
        }
        mode = input.read(MODE, GantryMode.CODEC).orElse(GantryMode.STORAGE);
        ownRail = input.getBooleanOr(OWN_RAIL, false);
        if (input.getString(SHOWN_FLUID_KEYS[0]).isPresent()) {
            Pumping shownPump = input.getString(PUMP_FLUID).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                    .filter(fluid -> fluid != Fluids.EMPTY)
                    .map(fluid -> new Pumping(input.getIntOr(PUMP_TANK, 0), input.getBooleanOr(PUMP_INTO, false), fluid)).orElse(null);
            if ((shownPump == null) != (shown.pumping() == null) && level != null) {
                pumpChangedAt = level.getGameTime();
            }
            shown = new Shown(shownFluid(input, 0), input.getIntOr(SHOWN_STEP_KEYS[0], 0), shownFluid(input, 1),
                    input.getIntOr(SHOWN_STEP_KEYS[1], 0), input.getIntOr(STEPS, 1), locks[0], locks[1], shownPump);
        }
    }

    private static Fluid shownFluid(ValueInput input, int tank) {
        return input.getString(SHOWN_FLUID_KEYS[tank]).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                .orElse(Fluids.EMPTY);
    }

    /**
     * Drops the gantry item when the master goes away, unless a creative player broke it,
     * and takes away the rail the gantry laid, which drops nothing.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        if (dropOnRemove) {
            dropOnRemove = false;
            Block.popResource(level, pos, dropItem());
        }
        if (ownRail) {
            ownRail = false;
            BlockPos rail = FluidGantryBlock.railPos(pos, state);
            if (level.getBlockState(rail).is(ServusShuttlesBlocks.RAIL.get())) {
                level.setBlock(rail, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
