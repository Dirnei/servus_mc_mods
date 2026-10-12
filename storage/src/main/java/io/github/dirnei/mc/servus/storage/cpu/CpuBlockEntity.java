package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.data.DataCarrier;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.Handoff;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.StepRef;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.TerminalRequests;
import io.github.dirnei.mc.servus.storage.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.storage.pattern.PatternLibrary;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * One block of a Crafting CPU. Every formed block is a link member of the
 * data line it touches; the master (part 0) owns the CPU's work orders. They
 * are saved with the master, pause while it is unloaded and are cancelled
 * when the cube unforms.
 */
public class CpuBlockEntity extends BlockEntity implements WoodHolder, DataLink {

    /** Carries the data network to touching data nodes, like a piece of cable. */
    private final DataCarrier carrier = new DataCarrier(this);

    private final WoodSlot woodSlot = new WoodSlot(this);

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

    /**
     * Sets the lights of every block of a formed cube to running while the
     * master has open orders and to the slow glow otherwise. Called when the
     * order book changes, never per tick; only changed states are written.
     */
    void updateActive() {
        BlockState state = getBlockState();
        if (!(level instanceof ServerLevel server) || !CpuBlock.isFormed(state) || state.getValue(CpuBlock.PART) != 0) {
            return;
        }
        boolean active = book != null && book.openCount() > 0;
        if (state.getValue(CpuBlock.ACTIVE) == active) {
            return;
        }
        for (int part = CpuShape.PARTS - 1; part >= 0; part--) {
            CpuShape.Vec offset = CpuShape.offset(part);
            BlockPos at = worldPosition.offset(offset.x(), offset.y(), offset.z());
            BlockState partState = server.getBlockState(at);
            if (CpuBlock.isFormed(partState) && partState.getValue(CpuBlock.ACTIVE) != active) {
                server.setBlock(at, partState.setValue(CpuBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Cancels every open order; called when the cube unforms. */
    void cancelAll() {
        if (book != null) {
            book.cancelAll();
        }
    }

    // Persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        woodSlot.save(output);
        if (book != null) {
            tag.put("Orders", book.save(registries));
        }
        output.store(tag);
    }

    /** Only a formed master takes its saved orders back; one that loads unformed has lost them. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        woodSlot.load(input);
        BlockState state = getBlockState();
        if (tag.contains("Orders") && CpuBlock.isFormed(state) && state.getValue(CpuBlock.PART) == 0) {
            book().load(tag.getCompoundOrEmpty("Orders"), registries);
        }
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
            List<OrderBook.Shipped> shipped = new ArrayList<>();
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
                    PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
                    int asked = shipment.planned();
                    shipped.add(new OrderBook.Shipped(shipment.id(), asked));
                    planned += asked;
                    remaining -= asked;
                } else if (refusal.isEmpty()) {
                    refusal = shipping.refusal();
                }
            }
            if (planned < amount && refusal.isEmpty()) {
                refusal = "out_of_stock";
            }
            return new OrderBook.Feed(planned, planned >= amount ? "" : refusal, shipped);
        }

        @Override
        public ProductionLedger.Transfer transfer(long id) {
            return ProductionLedger.get(serverLevel()).transfer(id);
        }

        @Override
        public BlockPos self() {
            return worldPosition;
        }

        @Override
        public void changed() {
            setChanged();
            updateActive();
        }

        @Override
        public void collect(OrderStep step) {
            BlockPos producer = step.producer();
            ServerLevel server = serverLevel();
            if (!server.isLoaded(producer)) {
                return;
            }
            Optional<BlockPos> target = Optional.empty();
            WorkOrder order = step.ref() == null ? null : book.order(step.ref().order());
            for (ItemKey key : outputs(step)) {
                int toMove = extractable(server, producer, key) - step.inFlight(key);
                if (toMove <= 0) {
                    continue;
                }
                toMove -= sendDirect(server, step, order, key, toMove);
                if (toMove <= 0) {
                    continue;
                }
                toMove = handOff(server, step, order, key, toMove);
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
                    // The ledger outlives this block entity, so a landing while the CPU is unloaded still counts.
                    ProductionLedger ledger = ProductionLedger.get(server);
                    ProductionLedger.Transfer transfer = ledger.open(toMove);
                    step.collecting(key, transfer.id(), toMove);
                    delivery.get().onEnd(ended -> {
                        ledger.delivered(transfer, ended.delivered());
                        ledger.end(transfer);
                    });
                    setChanged();
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
            return pattern.isPresent() && station.assign(pattern.get(), step.batches(), step.ref());
        }

        @Override
        public void release(OrderStep step) {
            ServerLevel server = serverLevel();
            if (server.isLoaded(step.producer()) && server.getBlockEntity(step.producer()) instanceof CraftingStationBlockEntity station) {
                station.release(step.ref());
            }
        }

        @Override
        public boolean busyElsewhere(BlockPos producer) {
            ServerLevel server = serverLevel();
            // A station whose outputs wait for someone else's collection, or hold leftovers, takes no new step.
            if (server.isLoaded(producer) && server.getBlockEntity(producer) instanceof CraftingStationBlockEntity station
                    && station.hasOutputs()
                    && !(station.jobOwner() instanceof StepRef ref && ref.cpu().equals(getBlockPos()))) {
                return true;
            }
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

    /**
     * Sends the part of a request's item that the request still needs straight
     * from the producer to its terminal, booked like any collection.
     *
     * @return how many were sent
     */
    private int sendDirect(ServerLevel server, OrderStep step, WorkOrder order, ItemKey key, int toMove) {
        if (order == null || order.deliverTo() == null || !order.key().equals(key) || !server.isLoaded(order.deliverTo())
                || !(server.getBlockEntity(order.deliverTo()) instanceof StorageTerminalBlockEntity terminal)) {
            return 0;
        }
        int budget = Math.min(order.amount() - order.directSent(), TerminalRequests.need(terminal, order.requestId()));
        int direct = Math.min(toMove, budget);
        if (direct <= 0) {
            return 0;
        }
        Optional<Delivery> delivery = DataPackets.move(server, step.producer(), order.deliverTo(), key.stack(1), direct);
        if (delivery.isEmpty()) {
            return 0;
        }
        ProductionLedger ledger = ProductionLedger.get(server);
        ProductionLedger.Transfer transfer = ledger.open(direct);
        step.collecting(key, transfer.id(), direct);
        order.sentDirect(direct);
        TerminalRequests.expectDirect(terminal, order.requestId(), direct);
        BlockPos terminalPos = order.deliverTo();
        int requestId = order.requestId();
        delivery.get().onEnd(ended -> {
            ledger.delivered(transfer, ended.delivered());
            ledger.end(transfer);
            if (ended.delivered() < direct && server.getBlockEntity(terminalPos) instanceof StorageTerminalBlockEntity target) {
                TerminalRequests.directShort(target, requestId, direct - ended.delivered());
            }
        });
        setChanged();
        return direct;
    }

    /**
     * Sends outputs a later step of the same order still needs straight to
     * that step's producer, booked by one ledger id on both steps; keeps
     * outputs back for a while when the later step's producer is still busy.
     *
     * @return how many of the key still go to storage
     */
    private int handOff(ServerLevel server, OrderStep step, WorkOrder order, ItemKey key, int toMove) {
        if (order == null || step.ref() == null) {
            return toMove;
        }
        List<OrderStep> dependants = new ArrayList<>();
        List<Handoff.Dependant> facts = new ArrayList<>();
        boolean waiting = false;
        List<OrderStep> steps = order.steps();
        for (int i = step.ref().step() + 1; i < steps.size(); i++) {
            OrderStep later = steps.get(i);
            if (later.state() == OrderStep.State.DONE || later.needed(key) <= 0) {
                continue;
            }
            boolean same = later.producer().equals(step.producer());
            boolean delivering = later.state() == OrderStep.State.DELIVERING && server.isLoaded(later.producer());
            dependants.add(later);
            facts.add(new Handoff.Dependant(later.owed(key), delivering, same));
            waiting |= !delivering && !same && later.owed(key) > 0;
        }
        if (dependants.isEmpty()) {
            step.clearHold();
            return toMove;
        }
        long heldFor = waiting ? step.heldFor(server.getGameTime()) : 0;
        Handoff.Split split = Handoff.split(toMove, facts, heldFor, StorageStatsManager.INSTANCE.handoffWaitTicks());
        if (split.hold() == 0) {
            step.clearHold();
        }
        int unsent = 0;
        for (int i = 0; i < dependants.size(); i++) {
            int count = split.handed().get(i);
            if (count <= 0) {
                continue;
            }
            OrderStep later = dependants.get(i);
            Optional<Delivery> delivery = DataPackets.move(server, step.producer(), later.producer(), key.stack(1), count);
            if (delivery.isEmpty()) {
                unsent += count;
                continue;
            }
            ProductionLedger ledger = ProductionLedger.get(server);
            ProductionLedger.Transfer transfer = ledger.open(count);
            step.collecting(key, transfer.id(), count, later.producer());
            later.handedIn(key, transfer.id(), count);
            delivery.get().onEnd(ended -> {
                ledger.delivered(transfer, ended.delivered());
                ledger.end(transfer);
            });
            setChanged();
        }
        return split.toStorage() + unsent;
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
            ItemView handler = PacketInlets.handler(level, producer, side);
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

    /** The Packing Station of the line that takes collected outputs: the lowest position whose line reaches the producer. */
    private Optional<BlockPos> collectionPoint(ServerLevel level, BlockPos producer) {
        List<BlockPos> points = new ArrayList<>();
        for (WarehouseNetwork warehouse : warehouses()) {
            for (BlockPos point : warehouse.packingStations()) {
                if (level.getBlockEntity(point) instanceof PackingStationBlockEntity
                        && DataNetworks.networkAt(level, point).filter(network -> network.reaches(producer)).isPresent()) {
                    points.add(point);
                }
            }
        }
        return points.stream().min(Comparator.comparingLong(BlockPos::asLong));
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    // Data carrier

    @Override
    public void onLoad() {
        super.onLoad();
        carrier.carry(CpuBlock.isFormed(getBlockState()));
    }

    /** A cube carries the data network while formed; forming and unforming change the block state. */
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (level != null && !level.isClientSide() && !isRemoved()) {
            carrier.carry(CpuBlock.isFormed(state));
        }
    }

    @Override
    public void setRemoved() {
        carrier.leave();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        carrier.leave();
    }
}
