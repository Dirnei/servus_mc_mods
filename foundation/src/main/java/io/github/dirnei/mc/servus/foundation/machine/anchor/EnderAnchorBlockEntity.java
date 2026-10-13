package io.github.dirnei.mc.servus.foundation.machine.anchor;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/** Keeps its chunk loaded and ticking while powered, within the team cap. */
public class EnderAnchorBlockEntity extends PoweredMachineBlockEntity implements OwnedMachine {

    public static final int DATA_STATUS = 6;
    public static final int STATUS_NO_POWER = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_TEAM_LIMIT = 2;
    public static final int STATUS_FTB_CHUNKS = 3;
    public static final int STATUS_NO_OWNER = 4;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "ender_anchor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final String FTB_CHUNKS = "ftbchunks";

    private UUID owner;
    private boolean ticketHeld;
    private int status = STATUS_NO_POWER;
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

    public int status() {
        return status;
    }

    /** Client only: where the eye on the front stands, for the renderer. */
    public EyeView eyeView() {
        return eyeView;
    }

    /** Whether this loader currently holds its chunk ticket. */
    public boolean ticketHeld() {
        return ticketHeld;
    }

    private int allowance() {
        if (owner == null) {
            return STATUS_NO_OWNER;
        }
        if (ServusFoundationConfig.enderAnchorRespectsFtbChunks() && ModList.get().isLoaded(FTB_CHUNKS)) {
            return STATUS_FTB_CHUNKS;
        }
        if (!(level instanceof ServerLevel server)) {
            return STATUS_NO_POWER;
        }
        EnderAnchorData data = EnderAnchorData.get(server.getServer());
        if (!data.isRegistered(owner, server, worldPosition)) {
            if (data.countFor(owner) >= ServusFoundationConfig.maxEnderAnchorsPerTeam()) {
                return STATUS_TEAM_LIMIT;
            }
            data.register(owner, server, worldPosition);
        }
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
        syncTicket(working);
        return working;
    }

    private void syncTicket(boolean wanted) {
        if (wanted == ticketHeld || !(level instanceof ServerLevel server)) {
            return;
        }
        ChunkPos chunk = ChunkPos.containing(worldPosition);
        EnderAnchorTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x(), chunk.z(), wanted, true);
        ticketHeld = wanted;
        setChanged();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            syncTicket(false);
            if (owner != null) {
                EnderAnchorData.get(server.getServer()).unregister(owner, server, worldPosition);
            }
        }
        super.setRemoved();
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
    protected boolean isInputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_STATUS) {
            return status;
        }
        return super.getData(index);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            ValueIo.putUuid(output, "Owner", owner);
        }
        output.putBoolean("Ticket", ticketHeld);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = ValueIo.getUuid(input, "Owner");
        ticketHeld = input.getBooleanOr("Ticket", false);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnderAnchorMenu(containerId, playerInventory, this);
    }
}
