package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.foundation.production.OrderPlanner;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
        /** The producers on the data line; Any patterns craft from the accepted items {@code stock} holds most of. */
        List<Producer> producers(Map<ItemKey, Long> stock);

        /** The producers with one more pattern every Crafting Station of the line can run, for a one-off order. */
        default List<Producer> producersWith(EncodedPattern extra, Map<ItemKey, Long> stock) {
            return producers(stock);
        }

        /** Every item the warehouses of the line hold that no pick has locked, with its count. */
        Map<ItemKey, Long> stock();

        /** Stock reserved by the other CPUs of the line, per key. */
        Map<ItemKey, Integer> reservedElsewhere();

        /** Ships up to amount of the key to the producer. */
        Feed feed(BlockPos producer, ItemKey key, int amount);

        /** Sends up to amount of the key that waits for the order in the CPU's buffer to the producer. */
        default Feed feedFromBuffer(WorkOrder order, BlockPos producer, ItemKey key, int amount) {
            return new Feed(0, "", List.of());
        }

        /** A shipment or collection by its ledger id, null when the ledger forgot it. */
        ProductionLedger.Transfer transfer(long id);

        /** The CPU's master position, for step references. */
        BlockPos self();

        /** Orders changed and should be saved. */
        default void changed() {
        }

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
    public record Producer(BlockPos pos, Component name, List<ProductionRecipe> recipes, Optional<Identifier> active) {
    }

    /** A shipment of inputs on its way: its ledger id and how many it was asked to bring. */
    public record Shipped(long transfer, int asked) {
    }

    /**
     * @param planned how many are on their way
     * @param refusal why fewer than asked were planned, empty when all were
     * @param shipped the shipments, so short endings can be shipped again
     */
    public record Feed(int planned, String refusal, List<Shipped> shipped) {
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

    /** Which producer runs which recipe with which inputs; the planner's recipe id. */
    public record StepKey(BlockPos producer, Identifier recipe, Map<ItemKey, Integer> inputs) {
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

    /**
     * Plans and creates an order.
     *
     * @return the order, or the refusal: {@code no_producer} when nothing makes the item,
     *         {@code in_stock} when the stock already covers it
     */
    public Created create(ItemKey key, int amount, int requestId) {
        return create(key, amount, requestId, null);
    }

    /**
     * Plans and creates an order whose item is made only by {@code oneOff}
     * when given: a pattern the order keeps, saved on the line or not.
     */
    public Created create(ItemKey key, int amount, int requestId, EncodedPattern oneOff) {
        Map<StepKey, ProductionRecipe> byKey = new HashMap<>();
        OrderPlanner.Plan<ItemKey> plan = plan(key, amount, byKey, oneOff);
        if (plan.steps().isEmpty()) {
            return new Created(null, plan.missing().containsKey(key) ? "no_producer" : "in_stock");
        }
        List<OrderStep> steps = new ArrayList<>();
        for (OrderPlanner.Step<ItemKey> step : plan.steps()) {
            StepKey stepKey = (StepKey) step.recipe().id();
            steps.add(new OrderStep(stepKey.producer(), byKey.get(stepKey), step.batches()));
        }
        WorkOrder order = new WorkOrder(nextId++, key, amount, requestId, steps, plan.reserved(), plan.missing());
        order.setOneOff(oneOff);
        assignRefs(order);
        if (openCount() >= host.slots()) {
            order.setState(WorkOrder.State.WAITING, WAITING_REASON);
        } else if (!plan.missing().isEmpty()) {
            order.setState(WorkOrder.State.BLOCKED, missingReason(plan.missing()));
        }
        orders.add(order);
        host.changed();
        return new Created(order, "");
    }

    /**
     * What an order for the amount would do, without making it: the stock it
     * reserves, what its steps make and the raw material it misses.
     *
     * @return the preview; its refusal is {@code no_producer} when nothing makes the item
     */
    public Preview preview(ItemKey key, int amount) {
        return preview(key, amount, null);
    }

    /** What an order made only by {@code oneOff}, when given, would do. */
    public Preview preview(ItemKey key, int amount, EncodedPattern oneOff) {
        OrderPlanner.Plan<ItemKey> plan = plan(key, amount, new HashMap<>(), oneOff);
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
    private OrderPlanner.Plan<ItemKey> plan(ItemKey key, int amount, Map<StepKey, ProductionRecipe> byKey, EncodedPattern oneOff) {
        Map<ItemKey, Long> available = new HashMap<>(host.stock());
        reservedTotal().forEach((k, reserved) -> available.merge(k, (long) -reserved, Long::sum));
        host.reservedElsewhere().forEach((k, reserved) -> available.merge(k, (long) -reserved, Long::sum));
        available.remove(key);
        List<OrderPlanner.Recipe<ItemKey>> recipes = recipes(byKey, oneOff, key, available);
        return OrderPlanner.plan(key, amount, available, recipes, MAX_DEPTH);
    }

    /**
     * The producers' recipes as the planner sees them: producers without
     * planned or running steps first, then idle ones, then by time. With a
     * one-off pattern only that pattern makes {@code target}. Any patterns
     * craft from the accepted items {@code available} holds most of.
     */
    private List<OrderPlanner.Recipe<ItemKey>> recipes(Map<StepKey, ProductionRecipe> byKey, EncodedPattern oneOff, ItemKey target,
                                                       Map<ItemKey, Long> available) {
        List<Producer> producers = new ArrayList<>(producersFor(oneOff, available));
        producers.sort(Comparator.comparing((Producer producer) -> plannedOn(producer.pos()) || host.busyElsewhere(producer.pos()))
                .thenComparing(producer -> producer.active().isPresent()));
        List<OrderPlanner.Recipe<ItemKey>> list = new ArrayList<>();
        for (Producer producer : producers) {
            for (ProductionRecipe recipe : producer.recipes()) {
                if (recipe.outputs().isEmpty() || oneOff != null && makes(recipe, target) && !runs(recipe, oneOff)) {
                    continue;
                }
                Map<ItemKey, Integer> inputs = ItemKey.counts(recipe.inputs());
                StepKey stepKey = new StepKey(producer.pos(), recipe.id(), inputs);
                byKey.put(stepKey, recipe);
                list.add(new OrderPlanner.Recipe<>(stepKey, inputs, ItemKey.counts(recipe.outputs()), recipe.ticks()));
            }
        }
        // Stable sort keeps idle producers first among equal times.
        list.sort(Comparator.comparingInt(OrderPlanner.Recipe::ticks));
        return list;
    }

    private List<Producer> producersFor(EncodedPattern oneOff, Map<ItemKey, Long> stock) {
        return oneOff == null ? host.producers(stock) : host.producersWith(oneOff, stock);
    }

    private static boolean makes(ProductionRecipe recipe, ItemKey key) {
        for (ItemStack output : recipe.outputs()) {
            if (key.matches(output)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the recipe is the pattern's work: the same recipe with the same
     * inputs, or for an Any pattern the same recipe with the same result.
     */
    private static boolean runs(ProductionRecipe recipe, EncodedPattern pattern) {
        if (!recipe.id().equals(pattern.recipe())) {
            return false;
        }
        if (pattern.any()) {
            return recipe.outputs().size() == 1 && pattern.sameResult(recipe.outputs().get(0));
        }
        return ItemKey.counts(recipe.inputs()).equals(pattern.required());
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
        host.changed();
        return true;
    }

    public void cancelForRequest(int requestId) {
        for (WorkOrder order : orders) {
            if (order.requestId() == requestId) {
                cancel(order.id());
            }
        }
    }

    /** Cancels every open order, when the CPU goes away. */
    public void cancelAll() {
        for (WorkOrder order : orders) {
            cancel(order.id());
        }
    }

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
        boolean removed = false;
        for (Iterator<WorkOrder> it = orders.iterator(); it.hasNext(); ) {
            WorkOrder order = it.next();
            if (!order.open() && order.doneAt() >= 0 && now - order.doneAt() > KEEP_DONE_TICKS) {
                it.remove();
                removed = true;
            }
        }
        if (removed || !orders.isEmpty()) {
            host.changed();
        }
    }

    private void advance(WorkOrder order, long now) {
        String blocked = null;
        boolean allDone = true;
        for (OrderStep step : order.steps()) {
            step.settleCollections(host::transfer);
            if (step.active() && step.settleFeedings(host::transfer) && step.state() == OrderStep.State.PRODUCING) {
                step.setState(OrderStep.State.DELIVERING);
            }
            if (step.active()) {
                host.collect(step);
            }
            switch (step.state()) {
                case WAITING_STATION -> {
                    blocked = start(order, step, blocked);
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

    /** Moves the step to a free producer when needed and starts feeding it once its producer takes the job. */
    private String start(WorkOrder order, OrderStep step, String blocked) {
        if (producerBusy(step.producer(), step)) {
            relocate(order, step);
        }
        if (producerBusy(step.producer(), step)) {
            step.setReason("producer busy");
            return blocked;
        }
        if (!host.start(step)) {
            step.setReason("station busy");
            return blocked;
        }
        step.setState(OrderStep.State.DELIVERING);
        step.setReason("");
        return feed(order, step, blocked);
    }

    /** Ships every input still owed, from the CPU's buffer first; returns the blocked reason to report, if any. */
    private String feed(WorkOrder order, OrderStep step, String blocked) {
        String reason = null;
        for (Map.Entry<ItemKey, Integer> input : step.inputs().entrySet()) {
            ItemKey key = input.getKey();
            int owed = step.needed(key) - step.fed(key);
            if (owed <= 0) {
                continue;
            }
            Feed buffered = host.feedFromBuffer(order, step.producer(), key, owed);
            if (buffered.planned() > 0) {
                step.addFed(key, buffered.planned(), buffered.shipped());
                owed -= buffered.planned();
                if (owed <= 0) {
                    continue;
                }
            }
            Feed feed = host.feed(step.producer(), key, owed);
            if (feed.planned() > 0) {
                step.addFed(key, feed.planned(), feed.shipped());
                order.useReserved(key, feed.planned());
            }
            if (feed.planned() < owed && reason == null && !madeEarlier(order, step, key)) {
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

    /** Whether an earlier step of the order that is not done makes the key, so the step waits for it rather than misses it. */
    private static boolean madeEarlier(WorkOrder order, OrderStep step, ItemKey key) {
        for (OrderStep earlier : order.steps()) {
            if (earlier == step) {
                return false;
            }
            if (earlier.state() != OrderStep.State.DONE && earlier.produces(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves a waiting step to the first free producer of the line that offers
     * the same recipe with the same inputs, so chains spread over producers.
     */
    private void relocate(WorkOrder order, OrderStep step) {
        for (Producer producer : producersFor(order.oneOff(), step.inputStock())) {
            if (producer.pos().equals(step.producer()) || producerBusy(producer.pos(), step)) {
                continue;
            }
            for (ProductionRecipe recipe : producer.recipes()) {
                if (recipe.id().equals(step.recipe().id()) && ItemKey.counts(recipe.inputs()).equals(step.inputs())) {
                    step.setProducer(producer.pos());
                    host.changed();
                    return;
                }
            }
        }
    }

    private boolean producerBusy(BlockPos producer, OrderStep except) {
        for (WorkOrder order : orders) {
            if (order.occupies(producer, except)) {
                return true;
            }
        }
        return host.busyElsewhere(producer);
    }

    private void assignRefs(WorkOrder order) {
        List<OrderStep> steps = order.steps();
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setRef(new StepRef(host.self(), order.id(), i));
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (WorkOrder order : orders) {
            list.add(order.save(registries));
        }
        tag.put("Orders", list);
        return tag;
    }

    /** Takes over saved orders; their ids stay and new ids continue above them. */
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        orders.clear();
        nextId = Math.max(1, tag.getIntOr("NextId", 0));
        for (Tag raw : tag.getListOrEmpty("Orders")) {
            WorkOrder order = WorkOrder.load((CompoundTag) raw, registries);
            if (order != null) {
                assignRefs(order);
                orders.add(order);
                nextId = Math.max(nextId, order.id() + 1);
            }
        }
    }
}
