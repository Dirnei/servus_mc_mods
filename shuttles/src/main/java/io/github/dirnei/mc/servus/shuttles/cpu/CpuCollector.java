package io.github.dirnei.mc.servus.shuttles.cpu;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.order.Handoff;
import io.github.dirnei.mc.servus.shuttles.order.OrderStep;
import io.github.dirnei.mc.servus.shuttles.order.ProductionLedger;
import io.github.dirnei.mc.servus.shuttles.order.WorkOrder;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalRequests;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Moves the outputs of a CPU's steps away from their producers: what a
 * request still needs straight to its terminal, what later steps need to
 * their producers or into the CPU's buffer, the rest to storage.
 */
final class CpuCollector {

    private final CpuBlockEntity cpu;

    CpuCollector(CpuBlockEntity cpu) {
        this.cpu = cpu;
    }

    /** Moves the step's outputs that are not on their way yet away from its producer. */
    void collect(ServerLevel server, OrderStep step) {
        BlockPos producer = step.producer();
        if (!server.isLoaded(producer)) {
            return;
        }
        Optional<BlockPos> target = Optional.empty();
        WorkOrder order = step.ref() == null ? null : cpu.book().order(step.ref().order());
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
                target = cpu.collectionPoint(server, producer);
                if (target.isEmpty()) {
                    return;
                }
            }
            Optional<Delivery> delivery = DataPackets.move(server, producer, target.get(), key.stack(1), toMove);
            if (delivery.isPresent()) {
                if (order != null) {
                    order.overflowed(key, Math.min(toMove, handed.overflow()));
                }
                ProductionLedger ledger = ProductionLedger.get(server);
                ProductionLedger.Transfer transfer = ledger.open(toMove);
                step.collecting(key, transfer.id(), toMove);
                ledger.track(transfer, delivery.get());
                cpu.setChanged();
            }
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
        ledger.track(transfer, delivery.get(), ended -> {
            if (ended.delivered() < direct && server.getBlockEntity(terminalPos) instanceof StorageTerminalBlockEntity target) {
                TerminalRequests.directShort(target, requestId, direct - ended.delivered());
            }
        });
        cpu.setChanged();
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
        CpuBuffer<ItemKey> buffer = cpu.buffer();
        int room = cpu.memberPos().isPresent() ? buffer.room(order.id(), key, maxStack) : 0;
        int held = buffer.held(order.id(), key) + buffer.incoming(order.id(), key);
        Handoff.Split split = Handoff.split(toMove, facts, room, held, heldFor, ShuttlesStatsManager.INSTANCE.handoffWaitTicks());
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
            ledger.track(transfer, delivery.get());
            cpu.setChanged();
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
        Optional<BlockPos> member = cpu.memberPos();
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
        cpu.buffer().expect(transfer.id(), order.id(), key, count, key.stack(1).getMaxStackSize());
        ledger.track(transfer, delivery.get(), ended -> {
            cpu.buffer().arrived(transfer.id());
            cpu.setChanged();
        });
        cpu.setChanged();
        return count;
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
}
