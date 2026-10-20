package io.github.dirnei.mc.servus.foundation.machine.anchor;

import com.mojang.serialization.Codec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.upgrade.AnchorUpgradeSlots;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Keeps a square of chunks loaded and ticking while powered, within the team
 * cap: one chunk without upgrades, 3 by 3 with one Anchor Upgrade, 5 by 5
 * with two, shifted by an offset that keeps its own chunk inside. Pays its
 * {@code energy_per_tick} for every chunk of the square.
 */
public class EnderAnchorBlockEntity extends PoweredMachineBlockEntity implements OwnedMachine {

    public static final int DATA_STATUS = 6;
    public static final int STATUS_NO_POWER = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_TEAM_LIMIT = 2;
    public static final int STATUS_FTB_CHUNKS = 3;
    public static final int STATUS_NO_OWNER = 4;
    public static final int UPGRADE_SLOTS = 2;
    /** Menu data after the common machine entries: the area and the team's load. */
    public static final int DATA_RADIUS = DATA_COUNT;
    public static final int DATA_OFFSET_X = DATA_COUNT + 1;
    public static final int DATA_OFFSET_Z = DATA_COUNT + 2;
    public static final int DATA_TEAM_LOAD = DATA_COUNT + 3;
    public static final int DATA_TEAM_CAP = DATA_COUNT + 4;
    /** FE per tick the current area costs: energy per chunk times the area size. */
    public static final int DATA_UPKEEP = DATA_COUNT + 5;
    public static final int DATA_COUNT_ANCHOR = DATA_COUNT + 6;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "ender_anchor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private BooleanSupplier leavesToFtbChunks = ServusFoundationConfig::leavesChunkLoadingToFtbChunks;
    private UUID owner;
    private int status = STATUS_NO_POWER;
    private int offsetX;
    private int offsetZ;
    /** The chunks this anchor holds a ticket for, packed. */
    private final Set<Long> held = new LinkedHashSet<>();
    /** Set while the carrot dismantles the anchor: the upgrades travel on the item instead of dropping. */
    private boolean keepUpgrades;
    /** Client only: the radius from the last update tag, for the range outline. */
    private int syncedRadius;
    private final AnchorUpgradeSlots upgrades = new AnchorUpgradeSlots(UPGRADE_SLOTS, this::upgradesChanged);
    private final EyeView eyeView = new EyeView();

    public EnderAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.ENDER_ANCHOR_ENTITY.get(), pos, state, 0);
    }

    @Override
    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    @Override
    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /** Decides this anchor's FTB Chunks rule as if the option were {@code respects}, leaving the config alone; for GameTests. */
    void respectFtbChunksAs(boolean respects) {
        leavesToFtbChunks = () -> ServusFoundationConfig.leavesChunkLoadingToFtbChunks(respects);
    }

    public int status() {
        return status;
    }

    /** Client only: where the eye on the front stands, for the renderer. */
    public EyeView eyeView() {
        return eyeView;
    }

    /** Whether this anchor currently holds chunk tickets. */
    public boolean ticketHeld() {
        return !held.isEmpty();
    }

    /** The chunks this anchor holds tickets for right now, packed like {@code ChunkPos#pack}. */
    public Set<Long> heldChunks() {
        return Set.copyOf(held);
    }

    public AnchorUpgradeSlots upgrades() {
        return upgrades;
    }

    /** The anchor's square: radius from the upgrades, the saved offset clamped to it. On the client as last synced. */
    public AnchorArea area() {
        int radius = level != null && level.isClientSide() ? syncedRadius : upgrades.count();
        return AnchorArea.of(radius, offsetX, offsetZ);
    }

    /** Every chunk of the square, packed. */
    public List<Long> areaChunks() {
        ChunkPos own = ChunkPos.containing(worldPosition);
        return area().chunks(own.x(), own.z());
    }

    /** Moves the square one chunk; does nothing at the limit. Server side. */
    public boolean shift(AnchorArea.Shift shift) {
        AnchorArea before = area();
        AnchorArea after = before.shift(shift);
        if (after.equals(before)) {
            return false;
        }
        offsetX = after.offsetX();
        offsetZ = after.offsetZ();
        areaChanged();
        return true;
    }

    /** The upgrades went in or out: the offset shrinks to fit the new radius. */
    private void upgradesChanged() {
        AnchorArea area = area();
        offsetX = area.offsetX();
        offsetZ = area.offsetZ();
        areaChanged();
    }

    private void areaChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected int energyPerTickNow() {
        return stats().energyPerTick() * area().size();
    }

    private int allowance() {
        if (owner == null) {
            return STATUS_NO_OWNER;
        }
        if (!(level instanceof ServerLevel server)) {
            return STATUS_NO_POWER;
        }
        EnderAnchorData data = EnderAnchorData.get(server.getServer());
        String key = EnderAnchorData.key(server, worldPosition);
        if (leavesToFtbChunks.getAsBoolean()) {
            data.unregister(owner, key);
            return STATUS_FTB_CHUNKS;
        }
        // Only an anchor that can pay this tick counts against the cap.
        if (energy.getEnergyStored() < energyPerTickNow()) {
            data.unregister(owner, key);
            return STATUS_NO_POWER;
        }
        Set<String> chunks = EnderAnchorData.chunks(server.dimension(), areaChunks());
        if (data.chunksOf(owner, key).equals(chunks)) {
            // Already counted with this area, so a lowered cap does not release it.
            return STATUS_ACTIVE;
        }
        // New or changed area: checked against the load without this anchor's old area.
        if (!data.fits(owner, key, chunks, ServusFoundationConfig.maxLoadedChunksPerTeam())) {
            data.unregister(owner, key);
            return STATUS_TEAM_LIMIT;
        }
        data.register(owner, key, chunks);
        return STATUS_ACTIVE;
    }

    @Override
    protected boolean canWork() {
        int allowance = allowance();
        if (allowance != STATUS_ACTIVE) {
            status = allowance;
            return false;
        }
        return true;
    }

    @Override
    protected void finishWork() {
    }

    @Override
    protected boolean tick() {
        boolean working = super.tick();
        if (working) {
            status = STATUS_ACTIVE;
        } else if (status == STATUS_ACTIVE) {
            status = STATUS_NO_POWER;
        }
        syncTickets(working ? areaChunks() : List.of());
        return working;
    }

    /** Forces the wanted chunks not held yet and releases held chunks no longer wanted. */
    private void syncTickets(List<Long> wanted) {
        if (!(level instanceof ServerLevel server) || (held.size() == wanted.size() && held.containsAll(wanted))) {
            return;
        }
        for (Long chunk : List.copyOf(held)) {
            if (!wanted.contains(chunk)) {
                force(server, chunk, false);
                held.remove(chunk);
            }
        }
        for (Long chunk : wanted) {
            if (held.add(chunk)) {
                force(server, chunk, true);
            }
        }
        setChanged();
    }

    private void force(ServerLevel server, long chunk, boolean add) {
        EnderAnchorTickets.CONTROLLER.forceChunk(server, worldPosition, AnchorArea.unpackX(chunk), AnchorArea.unpackZ(chunk), add, true);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            syncTickets(List.of());
            if (owner != null) {
                EnderAnchorData.get(server.getServer()).unregister(owner, server, worldPosition);
            }
        }
        super.setRemoved();
    }

    /** The next removal is a dismantle: the upgrades stay in the block entity data saved onto the item. */
    public void keepUpgradesOnRemove() {
        keepUpgrades = true;
    }

    @Override
    public void dropContents() {
        super.dropContents();
        if (level == null || keepUpgrades) {
            return;
        }
        List<ItemStack> taken = upgrades.takeAll();
        if (!taken.isEmpty()) {
            NonNullList<ItemStack> stacks = NonNullList.create();
            stacks.addAll(taken);
            Containers.dropContents(level, worldPosition, stacks);
        }
    }

    @Override
    protected Identifier machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT_ANCHOR;
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_STATUS -> status;
            case DATA_RADIUS -> area().radius();
            case DATA_OFFSET_X -> area().offsetX();
            case DATA_OFFSET_Z -> area().offsetZ();
            case DATA_TEAM_LOAD -> owner != null && level instanceof ServerLevel server
                    ? EnderAnchorData.get(server.getServer()).load(owner) : 0;
            case DATA_TEAM_CAP -> ServusFoundationConfig.maxLoadedChunksPerTeam();
            case DATA_UPKEEP -> energyPerTickNow();
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            ValueIo.putUuid(output, "Owner", owner);
        }
        upgrades.save(output, "Upgrades");
        output.putInt("OffsetX", offsetX);
        output.putInt("OffsetZ", offsetZ);
        // The tickets belong to this position; a dismantled anchor placed elsewhere holds none.
        output.putLong("TicketsAt", worldPosition.asLong());
        output.store("Tickets", Codec.LONG.listOf(), List.copyOf(held));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = ValueIo.getUuid(input, "Owner");
        // The update tag carries no upgrades: the client's slots, filled by an open menu, stay as they are.
        if (input.child("Upgrades").isPresent()) {
            upgrades.load(input, "Upgrades");
        }
        offsetX = input.getIntOr("OffsetX", 0);
        offsetZ = input.getIntOr("OffsetZ", 0);
        syncedRadius = input.getIntOr("Radius", 0);
        held.clear();
        if (input.getLongOr("TicketsAt", Long.MIN_VALUE) == worldPosition.asLong()) {
            held.addAll(input.read("Tickets", Codec.LONG.listOf()).orElse(List.of()));
        } else if (input.getBooleanOr("Ticket", false)) {
            // Older saves only flag that the anchor held its own chunk.
            held.add(ChunkPos.pack(worldPosition));
        }
    }

    /** The client draws the range outline from the radius and the offset. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        AnchorArea area = area();
        tag.putInt("Radius", area.radius());
        tag.putInt("OffsetX", area.offsetX());
        tag.putInt("OffsetZ", area.offsetZ());
        return tag;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnderAnchorMenu(containerId, playerInventory, this);
    }
}
