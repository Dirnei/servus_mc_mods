package io.github.dirnei.mc.servus.manufacturing.machine.lab;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.ResearchLabMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchManager;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchState;
import io.github.dirnei.mc.servus.manufacturing.research.TechNode;
import io.github.dirnei.mc.servus.manufacturing.research.Techs;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Consumes research packs for its owner team's current research.
 */
public class ResearchLabBlockEntity extends PoweredMachineBlockEntity implements OwnedMachine {

    public static final int SLOT_COUNT = 2;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "research_lab");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private UUID owner;

    public ResearchLabBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.RESEARCH_LAB.get(), pos, state, SLOT_COUNT);
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

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return level != null && Techs.isPackItem(level.registryAccess(), stack);
    }

    /** The owner's current research as a node, when the lab can work on it. */
    private Optional<Current> current() {
        if (owner == null || level == null || level.getServer() == null) {
            return Optional.empty();
        }
        MinecraftServer server = level.getServer();
        ResearchState state = ResearchManager.state(server, owner);
        return state.current()
                .flatMap(id -> Techs.get(server.registryAccess(), ResourceLocation.parse(id)))
                .map(entry -> new Current(state, entry, entry.node()));
    }

    private record Current(ResearchState state, Techs.Entry entry, TechNode node) {
    }

    /** First slot holding a pack the current research still needs, or -1. */
    private int usableSlot(Current current) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && current.state().remaining(current.node(), Techs.itemId(stack)) > 0) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected boolean canWork() {
        return current().map(c -> usableSlot(c) >= 0).orElse(false);
    }

    @Override
    protected void finishWork() {
        current().ifPresent(current -> {
            int slot = usableSlot(current);
            if (slot < 0) {
                return;
            }
            ItemStack pack = inventory.extractItem(slot, 1, false);
            ResearchManager.labConsumed(level.getServer(), owner, current.node(), Techs.itemId(pack));
        });
    }

    /** Ticks per pack of the current tech; without research the lab never completes a cycle. */
    @Override
    protected int workTicks() {
        return current().map(c -> Math.max(1, c.entry().definition().ticksPerPack())).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_WORK_TICKS) {
            return current().map(c -> c.entry().definition().ticksPerPack()).orElse(0);
        }
        return super.getData(index);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResearchLabMenu(containerId, playerInventory, this);
    }
}
