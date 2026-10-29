package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.foundation.filter.Filters;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankShape;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * An Aquarium Floor or Lid. In a formed aquarium every part knows its master, the Floor block under the tank's north
 * west corner; the master keeps the tank box and the {@link AquariumMasterState}, ticks the cycle and the power, and is
 * the item, fluid and energy handler every part offers.
 */
public class AquariumPartBlockEntity extends BlockEntity {

    /** Ticks between two checks that the tank still has the box the aquarium formed on. */
    public static final int CHECK_INTERVAL = 20;

    private @Nullable BlockPos masterOffset;
    private @Nullable TankShape box;
    private final AquariumMasterState state = new AquariumMasterState(this::setChanged);
    private final AquariumFluidHandler fluidHandler = new AquariumFluidHandler(this);
    private int syncedWaterPermille = -1;
    private @Nullable Boolean shownLit;

    private int clientCapacity;
    private int clientDrawnCap = AquariumStats.DEFAULTS.drawnCap();
    private float clientWater;
    private float clientQuality = 1;
    private int clientDeathMark;
    private boolean deathMarkKnown;
    private int deathMark;
    private int lastDied;
    private final AquariumClientLook look = new AquariumClientLook();

    public AquariumPartBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.AQUARIUM_PART.get(), pos, state);
    }

    public boolean formed() {
        return masterOffset != null;
    }

    public boolean isMaster() {
        return BlockPos.ZERO.equals(masterOffset);
    }

    /** The master of the aquarium this part belongs to, while it still counts this part; else null. */
    public @Nullable AquariumPartBlockEntity master() {
        if (masterOffset == null || level == null) {
            return null;
        }
        BlockPos master = worldPosition.offset(masterOffset);
        if (!level.isLoaded(master) || !(level.getBlockEntity(master) instanceof AquariumPartBlockEntity part)
                || !part.isMaster() || !part.hasPart(worldPosition)) {
            return null;
        }
        return part;
    }

    /** Whether {@code pos} is a Floor or Lid position of this master's aquarium. */
    private boolean hasPart(BlockPos pos) {
        TankShape box = this.box;
        return box != null && pos.getX() >= box.minX() && pos.getX() <= box.maxX() && pos.getZ() >= box.minZ()
                && pos.getZ() <= box.maxZ() && (pos.getY() == box.minY() - 1 || pos.getY() == box.maxY() + 1);
    }

    /** The master's state; only meaningful on the master. */
    public AquariumMasterState state() {
        return state;
    }

    /** The tank box of this master, or null. */
    public @Nullable TankShape box() {
        return box;
    }

    /** The Floor positions of this master's aquarium. */
    public List<BlockPos> floorPositions() {
        return layer(box == null ? 0 : box.minY() - 1);
    }

    /** The Lid positions of this master's aquarium. */
    public List<BlockPos> lidPositions() {
        return layer(box == null ? 0 : box.maxY() + 1);
    }

    private List<BlockPos> layer(int y) {
        List<BlockPos> positions = new ArrayList<>();
        if (box != null) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    /** The tank block at the box's lowest north west corner, or null when it is gone. */
    public @Nullable FluidTankBlockEntity tank() {
        if (box == null || level == null) {
            return null;
        }
        BlockPos pos = new BlockPos(box.minX(), box.minY(), box.minZ());
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? tank : null;
    }

    Optional<TankNetwork> network() {
        FluidTankBlockEntity tank = tank();
        return tank == null ? Optional.empty() : tank.network();
    }

    BlockPos tankPos() {
        return box == null ? worldPosition.above() : new BlockPos(box.minX(), box.minY(), box.minZ());
    }

    /** mB of water in the tank; any other fluid counts as none. */
    public long water() {
        FluidTankBlockEntity tank = tank();
        if (tank == null) {
            return 0;
        }
        FluidStack content = tank.tankContent();
        return content.is(FluidTags.WATER) ? content.getAmount() : 0;
    }

    public int tankCapacity() {
        FluidTankBlockEntity tank = tank();
        return tank == null ? 0 : tank.tankCapacity();
    }

    public int capacity() {
        return AquariumCycle.capacity(water(), stats());
    }

    public double quality() {
        return AquariumCycle.quality(state.dirt(), water(), stats());
    }

    /** FE per tick this master's aquarium draws: its tank blocks and lid slabs. */
    public int powerCost() {
        return box == null ? 0 : AquariumPower.cost((int) box.volume(), box.blocksPerLayer(), stats());
    }

    private static AquariumStats stats() {
        return AquariumStatsManager.INSTANCE.stats();
    }

    /** Makes this Floor the master of an aquarium on {@code box}. */
    void becomeMaster(TankShape box) {
        state.reset();
        this.masterOffset = BlockPos.ZERO;
        this.box = box;
        syncedWaterPermille = -1;
        shownLit = null;
        setChanged();
        sendUpdate();
    }

    void joinMaster(BlockPos master) {
        this.masterOffset = master.subtract(worldPosition);
        this.box = null;
        setChanged();
        sendUpdate();
    }

    private void leave() {
        masterOffset = null;
        box = null;
        state.reset();
        setChanged();
        sendUpdate();
    }

    /** Unforms this master's aquarium: drops its slots and fish and turns every part but {@code removed} loose. */
    public void unform(@Nullable BlockPos removed) {
        if (level == null || !isMaster()) {
            return;
        }
        List<BlockPos> parts = new ArrayList<>(floorPositions());
        parts.addAll(lidPositions());
        state.dropAll(level, worldPosition);
        for (BlockPos at : parts) {
            if (at.equals(worldPosition) || at.equals(removed) || !level.isLoaded(at)) {
                continue;
            }
            if (level.getBlockEntity(at) instanceof AquariumPartBlockEntity part && worldPosition.equals(part.masterPosition())) {
                part.leave();
                AquariumLidBlock.setLit(level, at, false);
            }
        }
        leave();
        for (BlockPos at : parts) {
            if (level.isLoaded(at)) {
                level.invalidateCapabilities(at);
            }
        }
    }

    /** Turns a part whose master is gone back into a loose part. */
    void unformStray() {
        if (level == null) {
            return;
        }
        leave();
        AquariumLidBlock.setLit(level, worldPosition, false);
        level.invalidateCapabilities(worldPosition);
    }

    @Nullable BlockPos masterPosition() {
        return masterOffset == null ? null : worldPosition.offset(masterOffset);
    }

    /** The master's tick: the tank box check, the power and, once due, the cycle. */
    void serverTick() {
        if (level == null || !isMaster() || box == null) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            if (!tankStillFits()) {
                unform(null);
                return;
            }
            syncWaterWhenChanged();
        }
        boolean due = state.tickPower(powerCost(), stats());
        if (shownLit == null || shownLit != state.lastTickPowered()) {
            shownLit = state.lastTickPowered();
            showLit(shownLit);
        }
        if (due) {
            runCycle(state.takePoweredShare());
        }
    }

    /** False when the tank block is gone or its box is no longer the one the aquarium formed on. */
    private boolean tankStillFits() {
        if (box == null || level == null) {
            return false;
        }
        BlockPos corner = tankPos();
        if (!level.isLoaded(corner)) {
            return true;
        }
        if (!(level.getBlockEntity(corner) instanceof FluidTankBlockEntity tank)) {
            return false;
        }
        Optional<TankNetwork> network = tank.network();
        if (network.isEmpty()) {
            return true;
        }
        TankShape now = network.get().boxAt(Conversions.toNodePos(corner));
        return sameBox(now, box);
    }

    static boolean sameBox(TankShape a, TankShape b) {
        return a.minX() == b.minX() && a.minY() == b.minY() && a.minZ() == b.minZ()
                && a.maxX() == b.maxX() && a.maxY() == b.maxY() && a.maxZ() == b.maxZ();
    }

    /** Runs one cycle now with {@code poweredShare} of power; the ticker calls it when due, GameTests directly. */
    public AquariumCycle.Output runCycle(double poweredShare) {
        state.moveSpentFilter();
        AquariumStats stats = stats();
        long water = water();
        AquariumCycle.Input input = new AquariumCycle.Input(state.stock(), water, state.dirt(),
                state.slots().getStackInSlot(AquariumMasterState.FEED_SLOT).getCount(), state.unfedCycles(),
                Filters.pointsLeft(state.slots().getStackInSlot(AquariumMasterState.FILTER_SLOT)),
                poweredShare, state.keep(stats), state.autoCatch(), state.catchRoom());
        AquariumCycle.Output out = AquariumCycle.run(input, stats);
        state.apply(out);
        if (out.died() >= 0.5) {
            deathMark++;
            lastDied = (int) Math.round(out.died());
        }
        sendUpdate();
        return out;
    }

    private void showLit(boolean lit) {
        if (level == null) {
            return;
        }
        for (BlockPos at : lidPositions()) {
            AquariumLidBlock.setLit(level, at, lit);
        }
    }

    private void syncWaterWhenChanged() {
        int capacity = tankCapacity();
        int permille = capacity <= 0 ? 0 : (int) (water() * 1000 / capacity);
        if (permille != syncedWaterPermille) {
            sendUpdate();
        }
    }

    /** What pouring a fish bucket into the aquarium does. */
    public enum PourResult { POURED, OTHER_SPECIES, FULL }

    /** Pours one fish of {@code species} in; only meaningful on the master. */
    public PourResult pour(FishSpecies species) {
        FishSpecies kept = state.species();
        boolean empty = state.wholeFish() < 1;
        if (!empty && kept != species) {
            return PourResult.OTHER_SPECIES;
        }
        if (state.wholeFish() + 1 > capacity()) {
            return PourResult.FULL;
        }
        state.setStock(species, empty && kept != species ? 1 : state.stock() + 1);
        sendUpdate();
        return PourResult.POURED;
    }

    /** Takes one fish out; returns its species, or null when there is no whole fish. */
    public @Nullable FishSpecies catchOne() {
        FishSpecies species = state.species();
        if (species == null || state.wholeFish() < 1) {
            return null;
        }
        state.setStock(species, state.stock() - 1);
        sendUpdate();
        return species;
    }

    /** Sets auto catch and the keep level, then tells clients. */
    public void setCatch(boolean autoCatch, int keep) {
        state.setCatch(autoCatch, keep);
        sendUpdate();
    }

    /** The items automation sees from any part, or null on a loose part. */
    public @Nullable ResourceHandler<ItemResource> itemCapability() {
        AquariumPartBlockEntity master = master();
        return master == null ? null : master.state.automationSlots();
    }

    /** Water in, Murky Water out, from any part; null on a loose part. */
    public @Nullable ResourceHandler<FluidResource> fluidCapability() {
        AquariumPartBlockEntity master = master();
        return master == null ? null : master.fluidHandler;
    }

    /** The FE buffer, offered by Floor parts only; null elsewhere. */
    public @Nullable EnergyHandler energyCapability(@Nullable Direction side) {
        if (!(getBlockState().getBlock() instanceof AquariumFloorBlock)) {
            return null;
        }
        AquariumPartBlockEntity master = master();
        return master == null ? null : master.state.energy();
    }

    /** What a carrot click on a formed part says: size, stock and water. */
    public Component status() {
        AquariumPartBlockEntity master = master();
        if (master == null || master.box == null) {
            return Component.translatable("message.servus_delicacies.aquarium.loose");
        }
        TankShape box = master.box;
        return Component.translatable("message.servus_delicacies.aquarium.status", box.width(), box.depth(), box.height(),
                master.state.wholeFish(), master.capacity(), Math.round(master.quality() * 100));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level == null || level.isClientSide() || masterOffset == null) {
            return;
        }
        AquariumPartBlockEntity master = isMaster() ? this : master();
        if (master != null) {
            master.unform(pos);
        }
    }

    void sendUpdate() {
        if (level != null && !level.isClientSide()) {
            int capacity = tankCapacity();
            syncedWaterPermille = capacity <= 0 ? 0 : (int) (water() * 1000 / capacity);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (masterOffset == null) {
            return;
        }
        output.store("Master", BlockPos.CODEC, masterOffset);
        if (isMaster() && box != null) {
            saveBox(output);
            state.save(output);
            output.putInt("DeathMark", deathMark);
            output.putInt("LastDied", lastDied);
        }
    }

    private void saveBox(ValueOutput output) {
        if (box != null) {
            output.putIntArray("Box", new int[] {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterOffset = input.read("Master", BlockPos.CODEC).orElse(null);
        int[] saved = input.getIntArray("Box").orElse(new int[0]);
        box = saved.length == 6 ? TankShape.box(saved[0], saved[1], saved[2], saved[3], saved[4], saved[5]) : null;
        state.load(input);
        deathMark = input.getIntOr("DeathMark", 0);
        lastDied = input.getIntOr("LastDied", 0);
        clientCapacity = input.getIntOr("ClientCapacity", clientCapacity);
        clientDrawnCap = input.getIntOr("DrawnCap", clientDrawnCap);
        clientWater = input.getFloatOr("ClientWater", clientWater);
        clientQuality = input.getFloatOr("ClientQuality", clientQuality);
        if (level != null && level.isClientSide()) {
            if (deathMarkKnown && deathMark != clientDeathMark) {
                look.fishDied(level.getGameTime(), lastDied);
            }
            clientDeathMark = deathMark;
            deathMarkKnown = true;
        }
    }

    /** Clients get the master's look: box, species, stock, water, quality and deaths; parts only their master. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return ValueIo.write(this, registries, output -> {
            if (masterOffset == null) {
                return;
            }
            output.store("Master", BlockPos.CODEC, masterOffset);
            if (isMaster() && box != null) {
                saveBox(output);
                output.putInt("Species", state.species() == null ? -1 : state.species().ordinal());
                output.putDouble("Stock", state.stock());
                output.putInt("ClientCapacity", capacity());
                output.putInt("DrawnCap", stats().drawnCap());
                int tankCapacity = tankCapacity();
                output.putFloat("ClientWater", tankCapacity <= 0 ? 0 : (float) water() / tankCapacity);
                output.putFloat("ClientQuality", (float) quality());
                output.putInt("DeathMark", deathMark);
                output.putInt("LastDied", lastDied);
            }
        });
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** On the client: the capacity the master last synced. */
    public int clientCapacity() {
        return clientCapacity;
    }

    /** On the client: the most fish the data lets one aquarium draw. */
    public int clientDrawnCap() {
        return clientDrawnCap;
    }

    /** On the client: how full of water the tank is, 0 to 1. */
    public float clientWater() {
        return clientWater;
    }

    /** On the client: the water quality, 0 to 1. */
    public float clientQuality() {
        return clientQuality;
    }

    /** On the client: the swimming clock, floaters and the like of this master. */
    public AquariumClientLook look() {
        return look;
    }

    /** For the lid's light and the GUI: whether the last tick had power. */
    public boolean powered() {
        return state.lastTickPowered();
    }
}
