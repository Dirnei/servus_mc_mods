package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.OrderRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.StepRow;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** No slots: syncs the CPUs, orders and producers of the terminal's line to the open screen and cancels orders. */
public class ProductionTerminalMenu extends AbstractContainerMenu {

    private static final int SYNC_INTERVAL = 10;

    private final ProductionTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;

    public ProductionTerminalMenu(int containerId, Inventory playerInventory, ProductionTerminalBlockEntity terminal) {
        super(ServusStorageMenus.PRODUCTION_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
    }

    public static ProductionTerminalMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ProductionTerminalBlockEntity terminal) {
            return new ProductionTerminalMenu(containerId, playerInventory, terminal);
        }
        throw new IllegalStateException("No production terminal at " + pos);
    }

    public ProductionTerminalBlockEntity terminal() {
        return terminal;
    }

    /** Cancels an open order of a CPU on the terminal's line. */
    public boolean cancel(BlockPos cpuPos, int orderId) {
        if (!(terminal.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        boolean cancelled = false;
        var line = DataNetworks.networkAt(level, terminal.getBlockPos());
        if (line.isPresent()) {
            for (CpuBlockEntity cpu : CpuOrders.cpus(level, line.get())) {
                if (cpu.getBlockPos().equals(cpuPos)) {
                    cancelled = cpu.book().cancel(orderId);
                }
            }
        }
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
        return cancelled;
    }

    /** What the screen should show now. */
    public ProductionPayload view() {
        if (terminal.getLevel() instanceof ServerLevel level) {
            return ProductionView.of(level, terminal.getBlockPos());
        }
        return new ProductionPayload(false, "no_line", List.of(), List.of());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.PRODUCTION_TERMINAL.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;
        ProductionPayload payload = view();
        long signature = signature(payload);
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    private static long signature(ProductionPayload payload) {
        long hash = payload.online() ? 1 : 0;
        hash = hash * 31 + payload.reason().hashCode();
        for (MachineRow machine : payload.machines()) {
            hash = hash * 31 + machine.pos().hashCode();
            hash = hash * 31 + machine.state().hashCode();
            hash = hash * 31 + machine.working().getItem().hashCode();
            hash = hash * 31 + machine.recipes().size();
            for (var recipe : machine.recipes()) {
                for (ItemStack output : recipe.outputs()) {
                    hash = hash * 31 + output.getItem().hashCode() * 7 + output.getCount();
                }
            }
        }
        for (CpuRow cpu : payload.cpus()) {
            hash = hash * 31 + cpu.cpu().hashCode();
            hash = hash * 31 + cpu.slots() * 7 + cpu.inUse() * 3 + cpu.waiting();
            for (OrderRow order : cpu.orders()) {
                hash = hash * 31 + order.id();
                hash = hash * 31 + order.state().hashCode();
                hash = hash * 31 + order.reason().hashCode();
                for (StepRow step : order.steps()) {
                    hash = hash * 31 + step.state().hashCode();
                    hash = hash * 31 + step.reason().hashCode();
                }
            }
        }
        return hash;
    }
}
