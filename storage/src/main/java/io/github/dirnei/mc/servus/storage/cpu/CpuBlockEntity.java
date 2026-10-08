package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.storage.access.AccessPointBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.storage.pattern.PatternRunners;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One block of a Crafting CPU. Every formed block is a link member of the
 * data line it touches; the master (part 0) owns the CPU's work orders, which
 * live in memory and are cancelled when the cube unforms or unloads.
 */
public class CpuBlockEntity extends BlockEntity implements DataLink {

    private OrderBook book;

    public CpuBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_CPU.get(), pos, state);
    }

    /** This block as a data link while formed, else nothing. */
    public DataLink dataLink() {
        return CpuBlock.isFormed(getBlockState()) ? this : null;
    }

    /** Orders one CPU runs at a time. */
    public int slots() {
        return Math.max(1, StorageStatsManager.INSTANCE.cpuOrderSlots());
    }

    /** The work orders of this CPU; only meaningful on the master. */
    public OrderBook book() {
        if (book == null) {
            book = new OrderBook(new Host());
        }
        return book;
    }

    /** The data line any block of this CPU touches. */
    public Optional<DataNetwork> dataNetwork() {
        return memberPos().flatMap(pos -> DataNetworks.networkAt(serverLevel(), pos));
    }

    /** A block of the cube that is a member of a data line. */
    private Optional<BlockPos> memberPos() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            BlockPos pos = worldPosition.offset(offset.x(), offset.y(), offset.z());
            if (DataNetworks.networkAt(server, pos).isPresent()) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private ServerLevel serverLevel() {
        return (ServerLevel) level;
    }

    void serverTick() {
        if (book != null) {
            book.tick();
        }
    }

    /** Cancels every open order; called when the cube unforms. */
    void cancelAll() {
        if (book != null) {
            book.cancelAll();
        }
    }

    @Override
    public void setRemoved() {
        cancelAll();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        cancelAll();
    }

    public Component summary() {
        int open = book == null ? 0 : book.openCount();
        int slots = slots();
        return Component.translatable("message.servus_storage.cpu.summary", Math.min(open, slots), slots, Math.max(0, open - slots));
    }

    private List<WarehouseNetwork> warehouses() {
        return memberPos().map(pos -> TerminalWarehouses.reachable(serverLevel(), pos)).orElse(List.of());
    }

    /** The CPU's view of its data line for the order book. */
    private final class Host implements OrderBook.Host {

        @Override
        public List<OrderBook.Producer> producers() {
            return dataNetwork().map(network -> CpuOrders.producers(serverLevel(), network)).orElse(List.of());
        }

        @Override
        public Map<ItemKey, Long> stock() {
            Map<ItemKey, Long> stock = new HashMap<>();
            for (WarehouseNetwork warehouse : warehouses()) {
                for (Map.Entry<ItemKey, Long> entry : warehouse.index().entries()) {
                    stock.merge(entry.getKey(), entry.getValue(), Long::sum);
                }
            }
            return stock;
        }

        @Override
        public Map<ItemKey, Integer> reservedElsewhere() {
            return dataNetwork().map(network -> CpuOrders.reservedOnLine(serverLevel(), network, book)).orElse(Map.of());
        }

        @Override
        public OrderBook.Feed feed(BlockPos producer, ItemKey key, int amount) {
            List<WarehouseNetwork> byStock = new ArrayList<>(warehouses());
            byStock.sort(Comparator.comparingLong((WarehouseNetwork warehouse) -> warehouse.available(key)).reversed());
            int remaining = amount;
            int planned = 0;
            String refusal = "";
            List<OrderBook.Pending> pending = new ArrayList<>();
            for (WarehouseNetwork warehouse : byStock) {
                if (remaining <= 0) {
                    break;
                }
                long available = warehouse.available(key);
                if (available <= 0) {
                    continue;
                }
                WarehouseNetwork.Shipping shipping = warehouse.ship(producer, key, (int) Math.min(available, remaining));
                if (shipping.shipped()) {
                    AccessPointBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
                    int asked = shipment.planned();
                    pending.add(new OrderBook.Pending() {
                        @Override
                        public int asked() {
                            return asked;
                        }

                        @Override
                        public int delivered() {
                            return shipment.delivered();
                        }

                        @Override
                        public boolean ended() {
                            return shipment.ended();
                        }
                    });
                    planned += asked;
                    remaining -= asked;
                } else if (refusal.isEmpty()) {
                    refusal = shipping.refusal();
                }
            }
            if (planned < amount && refusal.isEmpty()) {
                refusal = "out_of_stock";
            }
            return new OrderBook.Feed(planned, planned >= amount ? "" : refusal, pending);
        }

        @Override
        public void collect(OrderStep step) {
            BlockPos producer = step.producer();
            ServerLevel server = serverLevel();
            if (!server.isLoaded(producer)) {
                return;
            }
            Optional<BlockPos> target = Optional.empty();
            for (ItemKey key : outputs(step)) {
                int toMove = extractable(server, producer, key) - step.inFlight(key);
                if (toMove <= 0) {
                    continue;
                }
                if (target.isEmpty()) {
                    target = collectionPoint(server, producer);
                    if (target.isEmpty()) {
                        return;
                    }
                }
                Optional<Delivery> delivery = DataPackets.move(server, producer, target.get(), key.stack(1), toMove);
                if (delivery.isPresent()) {
                    int sent = toMove;
                    step.sent(key, sent);
                    delivery.get().onEnd(ended -> step.landed(key, sent, ended.delivered()));
                }
            }
        }

        @Override
        public boolean start(OrderStep step) {
            ServerLevel server = serverLevel();
            if (!server.isLoaded(step.producer())
                    || !(server.getBlockEntity(step.producer()) instanceof CraftingStationBlockEntity station)) {
                return true;
            }
            Optional<EncodedPattern> pattern = patternFor(server, step);
            return pattern.isPresent() && station.assign(pattern.get(), step.batches(), step);
        }

        @Override
        public void release(OrderStep step) {
            ServerLevel server = serverLevel();
            if (server.isLoaded(step.producer()) && server.getBlockEntity(step.producer()) instanceof CraftingStationBlockEntity station) {
                station.release(step);
            }
        }

        @Override
        public boolean busyElsewhere(BlockPos producer) {
            return dataNetwork().map(network -> CpuOrders.busyElsewhere(serverLevel(), network, producer, book)).orElse(false);
        }

        @Override
        public int slots() {
            return CpuBlockEntity.this.slots();
        }

        @Override
        public long gameTime() {
            return level == null ? 0 : level.getGameTime();
        }
    }

    /** The pattern of the CPU's data line that the step was planned from: same recipe id, same inputs per batch. */
    private Optional<EncodedPattern> patternFor(ServerLevel level, OrderStep step) {
        Optional<DataNetwork> network = dataNetwork();
        if (network.isEmpty()) {
            return Optional.empty();
        }
        for (PatternLibrary.Entry entry : PatternLibrary.entries(level, network.get())) {
            EncodedPattern pattern = entry.pattern();
            if (pattern.recipe().equals(step.recipe().id()) && pattern.required().equals(step.inputs())) {
                return Optional.of(pattern);
            }
        }
        return Optional.empty();
    }

    /** The distinct outputs of the step's recipe. */
    private static List<ItemKey> outputs(OrderStep step) {
        List<ItemKey> list = new ArrayList<>();
        for (ItemStack output : step.recipe().outputs()) {
            ItemKey key = ItemKey.of(output);
            if (!list.contains(key)) {
                list.add(key);
            }
        }
        return list;
    }

    /** How many of the key the producer gives out on its most generous side. */
    private static int extractable(ServerLevel level, BlockPos producer, ItemKey key) {
        int best = 0;
        for (Direction side : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, producer, side);
            if (handler == null) {
                continue;
            }
            int count = 0;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && ItemKey.of(stack).equals(key)) {
                    count += handler.extractItem(slot, stack.getCount(), true).getCount();
                }
            }
            best = Math.max(best, count);
        }
        return best;
    }

    /** The Access Point of the line that takes collected outputs: the lowest position whose line reaches the producer. */
    private Optional<BlockPos> collectionPoint(ServerLevel level, BlockPos producer) {
        List<BlockPos> points = new ArrayList<>();
        for (WarehouseNetwork warehouse : warehouses()) {
            for (BlockPos point : warehouse.accessPoints()) {
                if (level.getBlockEntity(point) instanceof AccessPointBlockEntity
                        && DataNetworks.networkAt(level, point).filter(network -> network.reaches(producer)).isPresent()) {
                    points.add(point);
                }
            }
        }
        return points.stream().min(Comparator.comparingLong(BlockPos::asLong));
    }
}
