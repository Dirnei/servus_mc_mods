package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.board.MachineRow;
import io.github.dirnei.mc.servus.storage.board.OrderBoardBlockEntity;
import io.github.dirnei.mc.servus.storage.board.OrderBoardPayload;
import io.github.dirnei.mc.servus.storage.board.OrderRow;
import io.github.dirnei.mc.servus.storage.board.StepRow;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** No slots: the board only syncs the warehouse's machines and orders to the open screen. */
public class OrderBoardMenu extends AbstractContainerMenu {

    private static final int SYNC_INTERVAL = 10;

    private final OrderBoardBlockEntity board;
    private final ContainerLevelAccess access;
    private final Player player;
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;

    public OrderBoardMenu(int containerId, Inventory playerInventory, OrderBoardBlockEntity board) {
        super(ServusStorageMenus.ORDER_BOARD.get(), containerId);
        this.board = board;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(board.getLevel(), board.getBlockPos());
    }

    public static OrderBoardMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof OrderBoardBlockEntity board) {
            return new OrderBoardMenu(containerId, playerInventory, board);
        }
        throw new IllegalStateException("No order board at " + pos);
    }

    public OrderBoardBlockEntity board() {
        return board;
    }

    /** Cancels an open work order. */
    public boolean cancel(int orderId) {
        boolean cancelled = board.network().map(network -> network.cancelOrder(orderId)).orElse(false);
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
        return cancelled;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.ORDER_BOARD.get());
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
        OrderBoardPayload payload = view();
        long signature = signature(payload);
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The current machines and orders as the client should see them. */
    public OrderBoardPayload view() {
        Optional<WarehouseNetwork> network = board.network();
        if (network.isEmpty()) {
            return new OrderBoardPayload(false, "not_connected", List.of(), List.of());
        }
        WarehouseNetwork warehouse = network.get();
        return switch (warehouse.status()) {
            case NO_CONTROLLER -> new OrderBoardPayload(false, "no_controller", List.of(), List.of());
            case MULTIPLE_CONTROLLERS -> new OrderBoardPayload(false, "multiple_controllers", List.of(), List.of());
            case ONLINE -> new OrderBoardPayload(true, "", warehouse.machineRows(), warehouse.orderRows());
        };
    }

    private static long signature(OrderBoardPayload payload) {
        long hash = payload.online() ? 1 : 0;
        hash = hash * 31 + payload.reason().hashCode();
        for (MachineRow machine : payload.machines()) {
            hash = hash * 31 + machine.dock().hashCode();
            hash = hash * 31 + machine.machine().hashCode();
            hash = hash * 31 + machine.working().getItem().hashCode();
            hash = hash * 31 + machine.state().hashCode();
        }
        for (OrderRow order : payload.orders()) {
            hash = hash * 31 + order.id();
            hash = hash * 31 + order.state().hashCode();
            hash = hash * 31 + order.reason().hashCode();
            for (StepRow step : order.steps()) {
                hash = hash * 31 + step.state().hashCode();
                hash = hash * 31 + step.reason().hashCode();
            }
        }
        return hash;
    }
}
