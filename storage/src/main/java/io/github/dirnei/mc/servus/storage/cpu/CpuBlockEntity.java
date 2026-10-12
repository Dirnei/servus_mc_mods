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
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import io.github.dirnei.mc.servus.storage.order.Saving;
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
 * One block of a Crafting CPU, a CPU block or a Buffer block. Every formed
 * block is a link member of the data line it touches and saves the offset to
 * its master; the master (the lowest corner) saves the box and owns the CPU's
 * work orders. They are saved with the master, pause while it is unloaded and
 * are cancelled when the box unforms.
 */
public class CpuBlockEntity extends BlockEntity implements WoodHolder, DataLink {

    /** Carries the data network to touching data nodes, like a piece of cable. */
    private final DataCarrier carrier = new DataCarrier(this);

    private final WoodSlot woodSlot = new WoodSlot(this);

    private OrderBook book;

    /** From this block to its master while formed, else null. */
    private BlockPos masterOffset;
    /** On the master: the size of the box and its counts of CPU and Buffer blocks. */
    private CpuShape.Vec boxSize;
    private int cpuBlocks;
    private int bufferBlocks;
    /** On the master: a block of the box that was a member of a data line when last asked. */
    private BlockPos memberCache;
    /** On the master: intermediates that wait for later steps of their order. */
    private CpuBuffer<ItemKey> buffer;
    /** Items of a saved buffer that a master loading unformed drops in {@link #onLoad()}. */
    private List<ItemStack> lostBuffer = List.of();
    private final CpuInlet inlet = new CpuInlet(this);
    /** Ticks between buffer upkeep runs, like the order book's. */
    private static final int BUFFER_INTERVAL = 20;
    private int bufferTimer;
    /** Leftover moves on their way to a Packing Station, by order and key, with their ledger ids. */
    private final Map<CpuBuffer.Entry<ItemKey>, Long> leaving = new HashMap<>();

    public CpuBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_CPU.get(), pos, state);
    }

    /** This block as a data link while formed, else nothing. */
    public DataLink dataLink() {
        return CpuBlock.isFormed(getBlockState()) ? this : null;
    }

    /** Orders one CPU runs at a time: its CPU blocks times {@code orders_per_cpu_block}. */
    public int slots() {
        return cpuBlocks * StorageStatsManager.INSTANCE.cpuOrdersPerCpuBlock();
    }

    /** Buffer stacks of the CPU: its Buffer blocks times {@code stacks_per_buffer_block}. */
    public int room() {
        return bufferBlocks * StorageStatsManager.INSTANCE.cpuStacksPerBufferBlock();
    }

    /** Buffer stacks in use. */
    public int bufferUsed() {
        return buffer == null ? 0 : buffer().used();
    }

    /** The buffer of this CPU, on the master; its room follows {@code cpu.json}. */
    public CpuBuffer<ItemKey> buffer() {
        if (buffer == null) {
            buffer = new CpuBuffer<>(room());
        }
        buffer.setCapacity(room());
        return buffer;
    }

    /** Items landed through the inlet: credited to the open arrivals of the key. */
    int landInBuffer(ItemKey key, int count) {
        int landed = buffer().land(key, count);
        if (landed > 0) {
            setChanged();
        }
        return landed;
    }

    /** The master of the box this block belongs to, or null while unformed. */
    CpuBlockEntity master() {
        return level == null ? null : CpuBlock.master(level, worldPosition);
    }

    /** The CPU's packet inlet: only on a formed block, never an item capability. */
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> packetInlet() {
        return CpuBlock.isFormed(getBlockState()) && masterOffset != null ? inlet : null;
    }

    public int cpuBlocks() {
        return cpuBlocks;
    }

    public int bufferBlocks() {
        return bufferBlocks;
    }

    // Box

    /** The master's position while formed, else null. */
    public BlockPos masterPos() {
        return masterOffset == null ? null : worldPosition.offset(masterOffset);
    }

    /** Whether this block is the master of a formed box. */
    public boolean isMaster() {
        return masterOffset != null && masterOffset.equals(BlockPos.ZERO) && boxSize != null;
    }

    /** The size of the box, on the master only. */
    public CpuShape.Vec boxSize() {
        return boxSize;
    }

    /** Whether a position lies in this master's box. */
    public boolean boxContains(BlockPos pos) {
        if (boxSize == null) {
            return false;
        }
        int dx = pos.getX() - worldPosition.getX();
        int dy = pos.getY() - worldPosition.getY();
        int dz = pos.getZ() - worldPosition.getZ();
        return dx >= 0 && dy >= 0 && dz >= 0 && dx < boxSize.x() && dy < boxSize.y() && dz < boxSize.z();
    }

    /** Joins a formed box with its lowest corner at {@code corner}; the master gets the box. */
    void joinBox(BlockPos corner, CpuBox.Box box) {
        masterOffset = corner.subtract(worldPosition);
        if (box != null) {
            boxSize = box.size();
            cpuBlocks = box.cpuBlocks();
            bufferBlocks = box.bufferBlocks();
            memberCache = null;
        }
        setChanged();
    }

    /** Forgets the box. */
    void leaveBox() {
        masterOffset = null;
        boxSize = null;
        cpuBlocks = 0;
        bufferBlocks = 0;
        memberCache = null;
        setChanged();
    }

    /**
     * On the master: a block of the box went away. Cancels the orders, sends
     * what the buffer holds as packets to a Packing Station of the line while
     * the CPU is still on it (dropped at the removed block without one) and
     * unforms the rest of the box.
     */
    void unform(BlockPos removed) {
        if (!isMaster() || level == null) {
            return;
        }
        cancelAll();
        if (level instanceof ServerLevel server && buffer != null && !buffer.isEmpty()) {
            emptyBuffer(server, removed);
        }
        CpuBlock.unformBlocks(level, this, removed);
    }

    /**
     * Hands every buffered stack to the line toward the Packing Station that
     * takes collected outputs; the shipment settles by the line's own rule
     * once the CPU has left it. Without a Packing Station, or when a send
     * fails at once, the stack drops at the removed block.
     */
    private void emptyBuffer(ServerLevel server, BlockPos removed) {
        List<ItemStack> stacks = stacks(buffer.clear());
        leaving.clear();
        Optional<BlockPos> from = memberPos(removed);
        Optional<BlockPos> point = from.flatMap(member -> collectionPoint(server, member));
        for (ItemStack stack : stacks) {
            if (from.isPresent() && point.isPresent()) {
                Optional<Delivery> sent = DataPackets.send(server, from.get(), point.get(), stack);
                if (sent.isPresent() && sent.get().state() != Delivery.State.FAILED) {
                    continue;
                }
            }
            Containers.dropItemStack(server, removed.getX() + 0.5, removed.getY() + 0.5, removed.getZ() + 0.5, stack);
        }
        setChanged();
    }

    /**
     * Entries of orders that are no longer open become leftovers and leave as
     * packets for the Packing Station that takes collected outputs; one move
     * per entry at a time.
     */
    private void sendLeftovers(ServerLevel server) {
        if (buffer == null || buffer.isEmpty()) {
            leaving.clear();
            return;
        }
        java.util.Set<Long> open = new java.util.HashSet<>();
        if (book != null) {
            for (WorkOrder order : book.orders()) {
                if (order.open()) {
                    open.add((long) order.id());
                }
            }
        }
        for (CpuBuffer.Entry<ItemKey> entry : buffer.entries()) {
            if (!entry.leaving() && !open.contains(entry.order())) {
                buffer.leave(entry.order());
                setChanged();
            }
        }
        ProductionLedger ledger = ProductionLedger.get(server);
        leaving.entrySet().removeIf(move -> {
            ProductionLedger.Transfer transfer = ledger.transfer(move.getValue());
            return transfer == null || transfer.ended();
        });
        List<CpuBuffer.Entry<ItemKey>> leftovers = buffer.leftovers();
        if (leftovers.isEmpty()) {
            return;
        }
        Optional<BlockPos> point = memberPos().flatMap(member -> collectionPoint(server, member));
        if (point.isEmpty()) {
            return;
        }
        for (CpuBuffer.Entry<ItemKey> entry : leftovers) {
            CpuBuffer.Entry<ItemKey> slot = new CpuBuffer.Entry<>(entry.order(), entry.key(), 0, 0, true);
            if (leaving.containsKey(slot)) {
                continue;
            }
            fromBuffer((int) entry.order(), entry.key(), point.get(), entry.count(), true)
                    .ifPresent(transfer -> leaving.put(slot, transfer));
        }
    }

    /** Positions of the box, on the master only. */
    private List<BlockPos> boxPositions() {
        return boxSize == null ? List.of() : CpuBlock.positions(worldPosition, boxSize);
    }

    /** The block is about to go: a formed block unforms its box through the master first. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel server) || !CpuBlock.isFormed(state) || masterOffset == null) {
            return;
        }
        BlockPos master = masterPos();
        if (isMaster()) {
            unform(pos);
        } else if (server.isLoaded(master) && server.getBlockEntity(master) instanceof CpuBlockEntity cpu && cpu.isMaster()
                && cpu.boxContains(pos)) {
            cpu.unform(pos);
        }
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

    /** A block of the box other than {@code except} that is a member of a data line, else any member. */
    private Optional<BlockPos> memberPos(BlockPos except) {
        if (level instanceof ServerLevel server) {
            for (BlockPos pos : boxPositions()) {
                if (!pos.equals(except) && server.isLoaded(pos) && DataNetworks.networkAt(server, pos).isPresent()) {
                    return Optional.of(pos);
                }
            }
        }
        return memberPos();
    }

    /** A block of the box that is a member of a data line; cached and looked for again once it is not. */
    private Optional<BlockPos> memberPos() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        if (memberCache != null && boxContains(memberCache) && DataNetworks.networkAt(server, memberCache).isPresent()) {
            return Optional.of(memberCache);
        }
        memberCache = null;
        for (BlockPos pos : boxPositions()) {
            if (server.isLoaded(pos) && DataNetworks.networkAt(server, pos).isPresent()) {
                memberCache = pos;
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
        if (++bufferTimer >= BUFFER_INTERVAL && level instanceof ServerLevel server) {
            bufferTimer = 0;
            settleArrivals(server);
            sendLeftovers(server);
        }
    }

    /**
     * Sets the lights of every block of a formed box to running while the
     * master has open orders and to the slow glow otherwise. Called when the
     * order book changes, never per tick; only changed states are written.
     */
    void updateActive() {
        BlockState state = getBlockState();
        if (!(level instanceof ServerLevel server) || !CpuBlock.isFormed(state) || !isMaster()) {
            return;
        }
        boolean active = book != null && book.openCount() > 0;
        if (state.getValue(CpuBlock.ACTIVE) == active) {
            return;
        }
        List<BlockPos> box = boxPositions();
        for (int i = box.size() - 1; i >= 0; i--) {
            BlockPos at = box.get(i);
            if (!server.isLoaded(at)) {
                continue;
            }
            BlockState partState = server.getBlockState(at);
            if (CpuBlock.isFormed(partState) && partState.getValue(CpuBlock.ACTIVE) != active) {
                server.setBlock(at, partState.setValue(CpuBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Cancels every open order; called when the box unforms. */
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
        if (masterOffset != null) {
            CompoundTag box = new CompoundTag();
            box.putInt("MasterX", masterOffset.getX());
            box.putInt("MasterY", masterOffset.getY());
            box.putInt("MasterZ", masterOffset.getZ());
            if (boxSize != null) {
                box.putInt("SizeX", boxSize.x());
                box.putInt("SizeY", boxSize.y());
                box.putInt("SizeZ", boxSize.z());
                box.putInt("CpuBlocks", cpuBlocks);
                box.putInt("BufferBlocks", bufferBlocks);
            }
            tag.put("Box", box);
        }
        if (book != null) {
            tag.put("Orders", book.save(registries));
        }
        if (buffer != null && !buffer.isEmpty()) {
            tag.put("Buffer", saveBuffer(buffer, registries));
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
        masterOffset = null;
        boxSize = null;
        cpuBlocks = 0;
        bufferBlocks = 0;
        if (tag.contains("Box") && CpuBlock.isFormed(state)) {
            CompoundTag box = tag.getCompoundOrEmpty("Box");
            masterOffset = new BlockPos(box.getIntOr("MasterX", 0), box.getIntOr("MasterY", 0), box.getIntOr("MasterZ", 0));
            if (box.contains("SizeX")) {
                boxSize = new CpuShape.Vec(box.getIntOr("SizeX", 2), box.getIntOr("SizeY", 2), box.getIntOr("SizeZ", 2));
                cpuBlocks = box.getIntOr("CpuBlocks", 0);
                bufferBlocks = box.getIntOr("BufferBlocks", 0);
            }
        }
        if (tag.contains("Orders") && CpuBlock.isFormed(state) && isMaster()) {
            book().load(tag.getCompoundOrEmpty("Orders"), registries);
        }
        buffer = null;
        lostBuffer = List.of();
        if (tag.contains("Buffer")) {
            CpuBuffer<ItemKey> loaded = loadBuffer(tag.getCompoundOrEmpty("Buffer"), registries);
            if (CpuBlock.isFormed(state) && isMaster()) {
                buffer = loaded;
            } else {
                lostBuffer = stacks(loaded.clear());
            }
        }
    }

    public Component summary() {
        int open = book == null ? 0 : book.openCount();
        int slots = slots();
        return Component.translatable("message.servus_storage.cpu.summary", Math.min(open, slots), slots, Math.max(0, open - slots),
                bufferUsed(), room());
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
        public OrderBook.Feed feedFromBuffer(WorkOrder order, BlockPos producer, ItemKey key, int amount) {
            int held = buffer == null ? 0 : buffer.held(order.id(), key);
            int count = Math.min(held, amount);
            if (count <= 0) {
                return new OrderBook.Feed(0, "", List.of());
            }
            Optional<Long> transfer = fromBuffer(order.id(), key, producer, count, false);
            return transfer.map(id -> new OrderBook.Feed(count, "", List.of(new OrderBook.Shipped(id, count))))
                    .orElseGet(() -> new OrderBook.Feed(0, "", List.of()));
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
                HandOff handed = handOff(server, step, order, key, toMove);
                toMove = handed.toStorage();
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
                    if (order != null) {
                        // Outputs later steps still needed went to storage because the buffer was full.
                        order.overflowed(key, Math.min(toMove, handed.overflow()));
                    }
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
     * Where the outputs of a step went in {@link #handOff}.
     *
     * @param toStorage how many still go to storage
     * @param overflow  the part of it that later steps still owed
     */
    private record HandOff(int toStorage, int overflow) {
    }

    /**
     * Sends outputs a later step of the same order still needs straight to
     * that step's producer, booked by one ledger id on both steps; puts
     * outputs for later steps that are not delivering yet into the CPU's
     * buffer while it has room, and keeps outputs back for a while when the
     * buffer could not take them and the later step's producer is busy.
     */
    private HandOff handOff(ServerLevel server, OrderStep step, WorkOrder order, ItemKey key, int toMove) {
        if (order == null || step.ref() == null) {
            return new HandOff(toMove, 0);
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
            return new HandOff(toMove, 0);
        }
        long heldFor = waiting ? step.heldFor(server.getGameTime()) : 0;
        int maxStack = key.stack(1).getMaxStackSize();
        CpuBuffer<ItemKey> buffer = buffer();
        int room = memberPos().isPresent() ? buffer.room(order.id(), key, maxStack) : 0;
        int held = buffer.held(order.id(), key) + buffer.incoming(order.id(), key);
        Handoff.Split split = Handoff.split(toMove, facts, room, held, heldFor, StorageStatsManager.INSTANCE.handoffWaitTicks());
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
        int buffered = split.buffered() > 0 ? toBuffer(server, step, order, key, split.buffered()) : 0;
        int unbuffered = split.buffered() - buffered;
        return new HandOff(split.toStorage() + unsent + unbuffered, split.overflow() + unbuffered);
    }

    /**
     * Moves outputs from the step's producer into the CPU's buffer through
     * the CPU block on the line, booked as a collection of the step and as
     * an arrival of the buffer that holds its room until it ends.
     *
     * @return how many are on their way
     */
    private int toBuffer(ServerLevel server, OrderStep step, WorkOrder order, ItemKey key, int count) {
        Optional<BlockPos> member = memberPos();
        if (member.isEmpty()) {
            return 0;
        }
        Optional<Delivery> delivery = DataPackets.move(server, step.producer(), member.get(), key.stack(1), count);
        if (delivery.isEmpty() || delivery.get().state() == Delivery.State.FAILED) {
            return 0;
        }
        ProductionLedger ledger = ProductionLedger.get(server);
        ProductionLedger.Transfer transfer = ledger.open(count);
        step.collecting(key, transfer.id(), count);
        buffer().expect(transfer.id(), order.id(), key, count, key.stack(1).getMaxStackSize());
        delivery.get().onEnd(ended -> {
            ledger.delivered(transfer, ended.delivered());
            ledger.end(transfer);
            buffer().arrived(transfer.id());
            setChanged();
        });
        setChanged();
        return count;
    }

    /**
     * Moves up to {@code count} of an order's buffered items from the CPU
     * block on the line to {@code target}, taken from the buffer as they
     * leave, booked in the ledger.
     *
     * @return the ledger id, empty when the move could not start
     */
    private Optional<Long> fromBuffer(int orderId, ItemKey key, BlockPos target, int count, boolean leaving) {
        Optional<BlockPos> member = memberPos();
        if (member.isEmpty() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        ItemView source = new ItemView(new BufferSource(this, orderId, key, leaving));
        Optional<Delivery> delivery = DataPackets.move(server, member.get(), target, key.stack(1), count, source);
        if (delivery.isEmpty() || delivery.get().state() == Delivery.State.FAILED) {
            return Optional.empty();
        }
        ProductionLedger ledger = ProductionLedger.get(server);
        ProductionLedger.Transfer transfer = ledger.open(count);
        delivery.get().onEnd(ended -> {
            ledger.delivered(transfer, ended.delivered());
            ledger.end(transfer);
        });
        setChanged();
        return Optional.of(transfer.id());
    }

    /** Takes items of one order out of the buffer; what a move cannot place comes back in. */
    int takeFromBuffer(int orderId, ItemKey key, int count, boolean simulate) {
        if (buffer == null) {
            return 0;
        }
        if (simulate) {
            return Math.min(count, buffer.held(orderId, key));
        }
        int taken = buffer.take(orderId, key, count);
        if (taken > 0) {
            setChanged();
        }
        return taken;
    }

    /** Items a move from the buffer could not place come back to their order. */
    void returnToBuffer(int orderId, ItemKey key, int count, boolean leaving) {
        if (count > 0) {
            buffer().restore(new CpuBuffer.Entry<>(orderId, key, count, key.stack(1).getMaxStackSize(), leaving));
            setChanged();
        }
    }

    /** Arrivals whose transfer ended while the CPU was not listening release the rest of their room. */
    private void settleArrivals(ServerLevel server) {
        if (buffer == null) {
            return;
        }
        ProductionLedger ledger = ProductionLedger.get(server);
        for (CpuBuffer.Arrival<ItemKey> arrival : buffer.arrivals()) {
            ProductionLedger.Transfer transfer = ledger.transfer(arrival.transfer());
            if (transfer == null || transfer.ended()) {
                buffer.arrived(arrival.transfer());
                setChanged();
            }
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

    // Buffer saving

    private static CompoundTag saveBuffer(CpuBuffer<ItemKey> buffer, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (CpuBuffer.Entry<ItemKey> entry : buffer.entries()) {
            CompoundTag item = new CompoundTag();
            item.putLong("Order", entry.order());
            item.put("Item", Saving.key(entry.key(), registries));
            item.putInt("Count", entry.count());
            item.putInt("MaxStack", entry.maxStack());
            item.putBoolean("Leaving", entry.leaving());
            entries.add(item);
        }
        ListTag arrivals = new ListTag();
        for (CpuBuffer.Arrival<ItemKey> arrival : buffer.arrivals()) {
            CompoundTag item = new CompoundTag();
            item.putLong("Transfer", arrival.transfer());
            item.putLong("Order", arrival.order());
            item.put("Item", Saving.key(arrival.key(), registries));
            item.putInt("Expected", arrival.expected());
            item.putInt("Landed", arrival.landed());
            item.putInt("MaxStack", arrival.maxStack());
            arrivals.add(item);
        }
        tag.put("Entries", entries);
        tag.put("Arrivals", arrivals);
        return tag;
    }

    private CpuBuffer<ItemKey> loadBuffer(CompoundTag tag, HolderLookup.Provider registries) {
        CpuBuffer<ItemKey> loaded = new CpuBuffer<>(room());
        for (Tag raw : tag.getListOrEmpty("Entries")) {
            CompoundTag item = (CompoundTag) raw;
            Saving.key(item.getCompoundOrEmpty("Item"), registries).ifPresent(key -> loaded.restore(new CpuBuffer.Entry<>(
                    item.getLongOr("Order", 0), key, item.getIntOr("Count", 0), item.getIntOr("MaxStack", 64),
                    item.getBooleanOr("Leaving", false))));
        }
        for (Tag raw : tag.getListOrEmpty("Arrivals")) {
            CompoundTag item = (CompoundTag) raw;
            Saving.key(item.getCompoundOrEmpty("Item"), registries).ifPresent(key -> loaded.restore(new CpuBuffer.Arrival<>(
                    item.getLongOr("Transfer", 0), item.getLongOr("Order", 0), key, item.getIntOr("Expected", 0),
                    item.getIntOr("Landed", 0), item.getIntOr("MaxStack", 64))));
        }
        return loaded;
    }

    /** The items of buffer entries as stacks of at most their max stack size. */
    static List<ItemStack> stacks(List<CpuBuffer.Entry<ItemKey>> entries) {
        List<ItemStack> list = new ArrayList<>();
        for (CpuBuffer.Entry<ItemKey> entry : entries) {
            int left = entry.count();
            while (left > 0) {
                int count = Math.min(left, Math.max(1, entry.maxStack()));
                list.add(entry.key().stack(count));
                left -= count;
            }
        }
        return list;
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
        if (level instanceof ServerLevel server && !lostBuffer.isEmpty()) {
            // A master that loads unformed has lost its CPU; what its buffer held lands at its position.
            for (ItemStack stack : lostBuffer) {
                Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
            }
            lostBuffer = List.of();
            setChanged();
        }
    }

    /** A box carries the data network while formed; forming and unforming change the block state. */
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
