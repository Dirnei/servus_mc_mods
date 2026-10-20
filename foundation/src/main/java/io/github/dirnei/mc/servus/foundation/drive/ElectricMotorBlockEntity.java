package io.github.dirnei.mc.servus.foundation.drive;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineEnergyStorage;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jspecify.annotations.Nullable;

/**
 * Spends FE from its buffer and hands work to the block its shaft faces, but
 * only while that block wants work. Upgrades multiply work and FE alike. Takes
 * energy only through its back and has no items but its upgrades.
 */
public class ElectricMotorBlockEntity extends PoweredMachineBlockEntity {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "electric_motor");
    public static final int UPGRADE_SLOTS = 1;
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    private static final float SPIN_DEGREES_PER_TICK = 36;

    private final UpgradeSlots upgrades = new UpgradeSlots(UPGRADE_SLOTS, this::setChanged);
    private final Flywheel spin = new Flywheel(SPIN_DEGREES_PER_TICK);
    private @Nullable BlockCapabilityCache<Drivable, Direction> driven;

    public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.ELECTRIC_MOTOR_ENTITY.get(), pos, state, 0);
        for (RelativeSide side : RelativeSide.values()) {
            sideConfig().lock(side, SideMode.DISABLED);
        }
    }

    public UpgradeSlots upgrades() {
        return upgrades;
    }

    /** The shaft and fan flywheel on the client. */
    public Flywheel spin() {
        return spin;
    }

    /** The energy capability on {@code side}: only the back takes power. */
    public @Nullable MachineEnergyStorage energyStorage(@Nullable Direction side) {
        return side == null || side == facing().getOpposite() ? energy : null;
    }

    /** The block the shaft drives, or null when it takes no drive. Server only. */
    private @Nullable Drivable driven() {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        if (driven == null) {
            Direction facing = facing();
            driven = BlockCapabilityCache.create(ServusFoundationCapabilities.DRIVE, server, worldPosition.relative(facing), facing.getOpposite());
        }
        return driven.getCapability();
    }

    /** Work and FE per working tick right now, with the drive data and the upgrades. */
    public MotorRate rate() {
        DriveStats drive = DriveStatsManager.INSTANCE.get(ID, DriveStats.ELECTRIC_MOTOR);
        return MotorRate.of(drive.workPerTick(), stats().energyPerTick(), upgrades.multiplier());
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
    protected boolean canWork() {
        Drivable target = driven();
        return target != null && target.wantsWork();
    }

    @Override
    protected int energyPerTickNow() {
        return rate().energyPerTick();
    }

    @Override
    protected void finishWork() {
        Drivable target = driven();
        if (target != null) {
            target.addWork(rate().workPerTick());
        }
    }

    /** Drops the upgrades along with the (empty) inventory. */
    @Override
    public void dropContents() {
        super.dropContents();
        if (level == null) {
            return;
        }
        for (ItemStack stack : upgrades.takeAll()) {
            Block.popResource(level, worldPosition, stack);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        upgrades.save(output, "Upgrades");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // The update tag carries no upgrades: the client's slots, filled by an open menu, stay as they are.
        if (input.child("Upgrades").isPresent()) {
            upgrades.load(input, "Upgrades");
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricMotorMenu(containerId, playerInventory, this);
    }
}
