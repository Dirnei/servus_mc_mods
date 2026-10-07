package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.lib.production.OrderPlanner;
import io.github.dirnei.mc.servus.lib.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The warehouse's work orders: plans them with the Lib planner over the docks'
 * recipes, keeps reservations, and drives the steps: feed the dock, wait for
 * the machine, count what the dock collects. First come first served.
 */
public final class OrderBook {

    /** What the book needs from its warehouse. */
    public interface Host {
        List<WarehouseNetwork.Producer> producers();

        /** Every indexed item with its count. */
        Map<ItemKey, Long> stock();

        /** Delivers up to amount of the key into a dock's feed buffer. */
        Feed feed(BlockPos dock, ItemKey key, int amount);

        long gameTime();
    }

    /**
     * @param planned    how many were planned for delivery
     * @param unservable how many the stock could not cover
     * @param refusal    why nothing was planned, empty when something was
     */
    public record Feed(int planned, int unservable, String refusal) {
    }

    /** The outcome of creating an order: the order, or why none was made. */
    public record Created(WorkOrder order, String refusal) {
        public boolean made() {
            return order != null;
        }
    }

    /** Which station and machine run which recipe; the planner's recipe id. */
    public record StepKey(BlockPos station, BlockPos machine, ResourceLocation recipe) {
    }

    /** The request id of orders a stock rule created. */
    public static final int RULE_ORIGIN = -1;
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

    // Creation

    /**
     * Plans and creates an order.
     *
     * @return the order, or the refusal: {@code no_producer} when nothing makes the item,
     *         {@code in_stock} when the stock already covers it
     */
    public Created create(ItemKey key, int amount, int requestId) {
        Map<StepKey, ProductionRecipe> byKey = new HashMap<>();
        List<OrderPlanner.Recipe<ItemKey>> recipes = recipes(byKey);
        Map<ItemKey, Long> available = new HashMap<>(host.stock());
        reservedTotal().forEach((k, reserved) -> available.merge(k, (long) -reserved, Long::sum));
        // The amount is what to produce; stock of the item itself is already accounted for by the caller.
        available.remove(key);
        OrderPlanner.Plan<ItemKey> plan = OrderPlanner.plan(key, amount, available, recipes, MAX_DEPTH);
        if (plan.steps().isEmpty()) {
            return new Created(null, plan.missing().containsKey(key) ? "no_producer" : "in_stock");
        }
        List<OrderStep> steps = new ArrayList<>();
        for (OrderPlanner.Step<ItemKey> step : plan.steps()) {
            StepKey stepKey = (StepKey) step.recipe().id();
            steps.add(new OrderStep(stepKey.station(), stepKey.machine(), byKey.get(stepKey), step.batches()));
        }
        WorkOrder order = new WorkOrder(nextId++, key, amount, requestId, steps, plan.reserved(), plan.missing());
        if (!plan.missing().isEmpty()) {
            order.setState(WorkOrder.State.BLOCKED, missingReason(plan.missing()));
        }
        orders.add(order);
        return new Created(order, "");
    }

    /** The stations' recipes as the planner sees them, idle stations first, then by time. */
    private List<OrderPlanner.Recipe<ItemKey>> recipes(Map<StepKey, ProductionRecipe> byKey) {
        List<WarehouseNetwork.Producer> producers = new ArrayList<>(host.producers());
        producers.sort(Comparator.comparing((WarehouseNetwork.Producer producer) -> producer.active().isPresent()));
        List<OrderPlanner.Recipe<ItemKey>> list = new ArrayList<>();
        for (WarehouseNetwork.Producer producer : producers) {
            for (ProductionRecipe recipe : producer.recipes()) {
                if (recipe.outputs().isEmpty()) {
                    continue;
                }
                StepKey stepKey = new StepKey(producer.station(), producer.machine(), recipe.id());
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
        list.sort(Comparator.comparingInt(OrderPlanner.Recipe::ticks));
        // Stable sort keeps idle stations first among equal times.
        return list;
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
        return true;
    }

    public void cancelForRequest(int requestId) {
        for (WorkOrder order : orders) {
            if (order.requestId() == requestId && order.open()) {
                cancel(order.id());
            }
        }
    }

    // Execution

    /**
     * A stack of the key left an inbound station's buffer: the oldest active step
     * producing it whose machine touches that station gets the credit, else the
     * oldest active step producing it anywhere.
     */
    public void collected(BlockPos inbound, ItemKey key, int count) {
        OrderStep fallback = null;
        for (WorkOrder order : orders) {
            if (!order.open()) {
                continue;
            }
            for (OrderStep step : order.steps()) {
                if (!step.active() || !step.produces(key)) {
                    continue;
                }
                if (step.collectsAt(inbound)) {
                    step.addCollected(key, count);
                    return;
                }
                if (fallback == null) {
                    fallback = step;
                }
            }
        }
        if (fallback != null) {
            fallback.addCollected(key, count);
        }
    }

    public void tick() {
        if (++timer < TICK_INTERVAL) {
            return;
        }
        timer = 0;
        long now = host.gameTime();
        for (WorkOrder order : orders) {
            if (order.open()) {
                advance(order, now);
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
            switch (step.state()) {
                case WAITING_STATION -> {
                    if (!stationBusy(step.station(), step)) {
                        step.setState(OrderStep.State.DELIVERING);
                        step.setReason("");
                        blocked = feed(order, step, blocked);
                    } else {
                        step.setReason("station busy");
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

    /** Asks the host for every input still owed; returns the blocked reason to report, if any. */
    private String feed(WorkOrder order, OrderStep step, String blocked) {
        String reason = null;
        for (Map.Entry<ItemKey, Integer> input : step.inputs().entrySet()) {
            ItemKey key = input.getKey();
            int owed = step.needed(key) - step.fed(key);
            if (owed <= 0) {
                continue;
            }
            Feed feed = host.feed(step.station(), key, owed);
            if (feed.planned() > 0) {
                step.addFed(key, feed.planned());
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

    private boolean stationBusy(BlockPos station, OrderStep except) {
        for (WorkOrder order : orders) {
            if (order.occupies(station, except)) {
                return true;
            }
        }
        return false;
    }
}
