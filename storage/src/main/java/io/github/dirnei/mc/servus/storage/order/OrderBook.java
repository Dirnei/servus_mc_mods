package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.foundation.production.OrderPlanner;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The work orders of one Crafting CPU: plans them with the Foundation planner
 * over the recipes of the producers on the CPU's data line, keeps
 * reservations, and drives the steps: ship the inputs to the producer, collect
 * its outputs, count what arrives. At most {@link Host#slots()} orders run at
 * a time, oldest first; the rest wait.
 */
public final class OrderBook {

    /** What the book needs from its CPU. */
    public interface Host {
        /** The producers on the data line. */
        List<Producer> producers();

        /** Every item the warehouses of the line hold, with its count. */
        Map<ItemKey, Long> stock();

        /** Stock reserved by the other CPUs of the line, per key. */
        Map<ItemKey, Integer> reservedElsewhere();

        /** Ships up to amount of the key to the producer. */
        Feed feed(BlockPos producer, ItemKey key, int amount);

        /** Starts moving the step's outputs out of its producer. */
        void collect(OrderStep step);

        /**
         * The step is about to be fed: hands its producer the work, a pattern
         * job on a Crafting Station.
         *
         * @return false when the producer refuses, so the step keeps waiting
         */
        boolean start(OrderStep step);

        /** The step is done or its order is cancelled: the producer's job for it ends. */
        void release(OrderStep step);

        /** Whether a step of another CPU of the line occupies the producer. */
        boolean busyElsewhere(BlockPos producer);

        /** Orders that run at a time. */
        int slots();

        long gameTime();
    }

    /** A producer of the data line and what it reports. */
    public record Producer(BlockPos pos, Component name, List<ProductionRecipe> recipes, Optional<ResourceLocation> active) {
    }

    /** A shipment of inputs on its way. */
    public interface Pending {
        int asked();

        int delivered();

        boolean ended();
    }

    /**
     * @param planned how many are on their way
     * @param refusal why fewer than asked were planned, empty when all were
     * @param pending the shipments, so short endings can be shipped again
     */
    public record Feed(int planned, String refusal, List<Pending> pending) {
    }

    /** The outcome of creating an order: the order, or why none was made. */
    public record Created(WorkOrder order, String refusal) {
        public boolean made() {
            return order != null;
        }
    }

    /**
     * What an order would do.
     *
     * @param fromStock stock it reserves, per key
     * @param toCraft   what its steps make, per key
     * @param missing   raw material it cannot get, per key
     * @param refusal   {@code no_cpu} or {@code no_producer} when no order can be planned, else empty
     */
    public record Preview(Map<ItemKey, Integer> fromStock, Map<ItemKey, Integer> toCraft, Map<ItemKey, Integer> missing,
                          String refusal) {
        public static Preview refused(String refusal) {
            return new Preview(Map.of(), Map.of(), Map.of(), refusal);
        }

        /** Whether an order can start right away: planned, with nothing missing. */
        public boolean startable() {
            return refusal.isEmpty() && missing.isEmpty() && !toCraft.isEmpty();
        }
    }

    /** Which producer runs which recipe; the planner's recipe id. */
    public record StepKey(BlockPos producer, ResourceLocation recipe) {
    }

    /** The request id of orders a stock rule created. */
    public static final int RULE_ORIGIN = -1;
    public static final String WAITING_REASON = "waiting for CPU";
    private static final int TICK_INTERVAL = 20;
    private static final long KEEP_DONE_TICKS = 200;
    private static final int MAX_DEPTH = 8;

    private final Host host;
    private final List<WorkOrder> orders = new ArrayList<>();
    private int nextId = 1;
    private int timer;

    public OrderBook(Host host) {
        this.host = host;
    }

    public List<WorkOrder> orders() {
        return Collections.unmodifiableList(orders);
    }

    public WorkOrder order(int id) {
        for (WorkOrder order : orders) {
            if (order.id() == id) {
                return order;
            }
        }
        return null;
    }

    /** How many orders are open, waiting ones included. */
    public int openCount() {
        int count = 0;
        for (WorkOrder order : orders) {
            if (order.open()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasOpenOrderFor(int requestId) {
        for (WorkOrder order : orders) {
            if (order.requestId() == requestId && order.open()) {
                return true;
            }
        }
        return false;
    }

    /** Amount of the key on open orders. */
    public int openAmountFor(ItemKey key) {
        int sum = 0;
        for (WorkOrder order : orders) {
            if (order.open() && order.key().equals(key)) {
                sum += order.amount();
            }
        }
        return sum;
    }

    /** Stock reserved by every open order, per key. */
    public Map<ItemKey, Integer> reservedTotal() {
        Map<ItemKey, Integer> total = new HashMap<>();
        for (WorkOrder order : orders) {
            if (order.open()) {
                order.reserved().forEach((key, count) -> total.merge(key, count, Integer::sum));
            }
        }
        return total;
    }

    /** Whether a step of an open order of this book feeds or produces on the producer. */
    public boolean occupies(BlockPos producer) {
        for (WorkOrder order : orders) {
            if (order.occupies(producer, null)) {
                return true;
            }
        }
        return false;
    }

    // Creation

    /**
     * Plans and creates an order.
     *
     * @return the order, or the refusal: {@code no_producer} when nothing makes the item,
     *         {@code in_stock} when the stock already covers it
     */
    public Created create(ItemKey key, int amount, int requestId) {
        Map<StepKey, ProductionRecipe> byKey = new HashMap<>();
        OrderPlanner.Plan<ItemKey> plan = plan(key, amount, byKey);
        if (plan.steps().isEmpty()) {
            return new Created(null, plan.missing().containsKey(key) ? "no_producer" : "in_stock");
        }
        List<OrderStep> steps = new ArrayList<>();
        for (OrderPlanner.Step<ItemKey> step : plan.steps()) {
            StepKey stepKey = (StepKey) step.recipe().id();
            steps.add(new OrderStep(stepKey.producer(), byKey.get(stepKey), step.batches()));
        }
        WorkOrder order = new WorkOrder(nextId++, key, amount, requestId, steps, plan.reserved(), plan.missing());
        if (openCount() >= host.slots()) {
            order.setState(WorkOrder.State.WAITING, WAITING_REASON);
        } else if (!plan.missing().isEmpty()) {
            order.setState(WorkOrder.State.BLOCKED, missingReason(plan.missing()));
        }
        orders.add(order);
        return new Created(order, "");
    }

    /**
     * What an order for the amount would do, without making it: the stock it
     * reserves, what its steps make and the raw material it misses.
     *
     * @return the preview; its refusal is {@code no_producer} when nothing makes the item
     */
    public Preview preview(ItemKey key, int amount) {
        OrderPlanner.Plan<ItemKey> plan = plan(key, amount, new HashMap<>());
        if (plan.steps().isEmpty() && plan.missing().containsKey(key)) {
            return Preview.refused("no_producer");
        }
        Map<ItemKey, Integer> toCraft = new LinkedHashMap<>();
        for (OrderPlanner.Step<ItemKey> step : plan.steps()) {
            step.recipe().outputs().forEach((output, count) -> toCraft.merge(output, count * step.batches(), Integer::sum));
        }
        return new Preview(new LinkedHashMap<>(plan.reserved()), toCraft, new LinkedHashMap<>(plan.missing()), "");
    }

    /** Plans the amount from the line's stock minus every reservation; stock of the item itself does not count. */
    private OrderPlanner.Plan<ItemKey> plan(ItemKey key, int amount, Map<StepKey, ProductionRecipe> byKey) {
        List<OrderPlanner.Recipe<ItemKey>> recipes = recipes(byKey);
        Map<ItemKey, Long> available = new HashMap<>(host.stock());
        reservedTotal().forEach((k, reserved) -> available.merge(k, (long) -reserved, Long::sum));
        host.reservedElsewhere().forEach((k, reserved) -> available.merge(k, (long) -reserved, Long::sum));
        // The amount is what to produce; stock of the item itself is already accounted for by the caller.
        available.remove(key);
        return OrderPlanner.plan(key, amount, available, recipes, MAX_DEPTH);
    }

    /** The producers' recipes as the planner sees them: producers without planned or running steps first, then idle ones, then by time. */
    private List<OrderPlanner.Recipe<ItemKey>> recipes(Map<StepKey, ProductionRecipe> byKey) {
        List<Producer> producers = new ArrayList<>(host.producers());
        producers.sort(Comparator.comparing((Producer producer) -> plannedOn(producer.pos()) || host.busyElsewhere(producer.pos()))
                .thenComparing(producer -> producer.active().isPresent()));
        List<OrderPlanner.Recipe<ItemKey>> list = new ArrayList<>();
        for (Producer producer : producers) {
            for (ProductionRecipe recipe : producer.recipes()) {
                if (recipe.outputs().isEmpty()) {
                    continue;
                }
                StepKey stepKey = new StepKey(producer.pos(), recipe.id());
                byKey.put(stepKey, recipe);
                Map<ItemKey, Integer> inputs = new LinkedHashMap<>();
                for (ItemStack input : recipe.inputs()) {
                    inputs.merge(ItemKey.of(input), input.getCount(), Integer::sum);
                }
                Map<ItemKey, Integer> outputs = new LinkedHashMap<>();
                for (ItemStack output : recipe.outputs()) {
                    outputs.merge(ItemKey.of(output), output.getCount(), Integer::sum);
                }
                list.add(new OrderPlanner.Recipe<>(stepKey, inputs, outputs, recipe.ticks()));
            }
        }
        // Stable sort keeps idle producers first among equal times.
        list.sort(Comparator.comparingInt(OrderPlanner.Recipe::ticks));
        return list;
    }

    /** Whether an open order of this book has a step on the producer that is not done yet. */
    private boolean plannedOn(BlockPos producer) {
        for (WorkOrder order : orders) {
            if (!order.open()) {
                continue;
            }
            for (OrderStep step : order.steps()) {
                if (step.producer().equals(producer) && step.state() != OrderStep.State.DONE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String missingReason(Map<ItemKey, Integer> missing) {
        ItemKey first = missing.keySet().iterator().next();
        return "missing " + first.stack(1).getHoverName().getString();
    }

    // Cancellation

    public boolean cancel(int orderId) {
        WorkOrder order = order(orderId);
        if (order == null || !order.open()) {
            return false;
        }
        order.clearReserved();
        order.setState(WorkOrder.State.CANCELLED, "");
        order.setDoneAt(host.gameTime());
        for (OrderStep step : order.steps()) {
            host.release(step);
        }
        return true;
    }

    public void cancelForRequest(int requestId) {
        for (WorkOrder order : orders) {
            if (order.requestId() == requestId && order.open()) {
                cancel(order.id());
            }
        }
    }

    /** Cancels every open order, when the CPU goes away. */
    public void cancelAll() {
        for (WorkOrder order : orders) {
            if (order.open()) {
                cancel(order.id());
            }
        }
    }

    // Execution

    public void tick() {
        if (++timer < TICK_INTERVAL) {
            return;
        }
        timer = 0;
        long now = host.gameTime();
        int running = 0;
        for (WorkOrder order : orders) {
            if (!order.open()) {
                continue;
            }
            if (running < host.slots()) {
                running++;
                if (order.state() == WorkOrder.State.WAITING) {
                    order.setState(WorkOrder.State.RUNNING, "");
                }
                advance(order, now);
            } else {
                order.setState(WorkOrder.State.WAITING, WAITING_REASON);
            }
        }
        for (Iterator<WorkOrder> it = orders.iterator(); it.hasNext(); ) {
            WorkOrder order = it.next();
            if (!order.open() && order.doneAt() >= 0 && now - order.doneAt() > KEEP_DONE_TICKS) {
                it.remove();
            }
        }
    }

    private void advance(WorkOrder order, long now) {
        String blocked = null;
        boolean allDone = true;
        for (OrderStep step : order.steps()) {
            if (step.active() && step.settleFeedings() && step.state() == OrderStep.State.PRODUCING) {
                step.setState(OrderStep.State.DELIVERING);
            }
            if (step.active()) {
                host.collect(step);
            }
            switch (step.state()) {
                case WAITING_STATION -> {
                    if (producerBusy(step.producer(), step)) {
                        step.setReason("producer busy");
                    } else if (!host.start(step)) {
                        step.setReason("station busy");
                    } else {
                        step.setState(OrderStep.State.DELIVERING);
                        step.setReason("");
                        blocked = feed(order, step, blocked);
                    }
                    allDone = false;
                }
                case DELIVERING -> {
                    blocked = feed(order, step, blocked);
                    allDone = false;
                }
                case PRODUCING -> {
                    if (step.outputsCollected()) {
                        step.setState(OrderStep.State.DONE);
                        step.setReason("");
                        host.release(step);
                    } else {
                        allDone = false;
                    }
                }
                case DONE -> { }
            }
        }
        if (allDone) {
            order.clearReserved();
            order.setState(WorkOrder.State.DONE, "");
            order.setDoneAt(now);
        } else if (blocked != null) {
            order.setState(WorkOrder.State.BLOCKED, blocked);
        } else {
            order.setState(WorkOrder.State.RUNNING, "");
        }
    }

    /** Ships every input still owed; returns the blocked reason to report, if any. */
    private String feed(WorkOrder order, OrderStep step, String blocked) {
        String reason = null;
        for (Map.Entry<ItemKey, Integer> input : step.inputs().entrySet()) {
            ItemKey key = input.getKey();
            int owed = step.needed(key) - step.fed(key);
            if (owed <= 0) {
                continue;
            }
            Feed feed = host.feed(step.producer(), key, owed);
            if (feed.planned() > 0) {
                step.addFed(key, feed.planned(), feed.pending());
                order.useReserved(key, feed.planned());
            }
            if (feed.planned() < owed && reason == null) {
                String why = feed.refusal();
                reason = why.isEmpty() || why.equals("out_of_stock") ? "missing " + key.stack(1).getHoverName().getString() : why;
            }
        }
        step.setReason(reason == null ? "" : reason);
        if (step.inputsFed()) {
            step.setState(OrderStep.State.PRODUCING);
            step.setReason("");
            return blocked;
        }
        return blocked == null ? reason : blocked;
    }

    private boolean producerBusy(BlockPos producer, OrderStep except) {
        for (WorkOrder order : orders) {
            if (order.occupies(producer, except)) {
                return true;
            }
        }
        return host.busyElsewhere(producer);
    }
}
