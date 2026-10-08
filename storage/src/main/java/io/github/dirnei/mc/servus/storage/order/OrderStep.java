package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** One recipe run a number of times on one producer of the data line. */
public final class OrderStep {

    public enum State { WAITING_STATION, DELIVERING, PRODUCING, DONE }

    /** A shipment of an input on its way to the producer. */
    private record Feeding(ItemKey key, OrderBook.Pending pending) {
    }

    private final BlockPos producer;
    private final ProductionRecipe recipe;
    private final int batches;
    private final Map<ItemKey, Integer> inputs = new LinkedHashMap<>();
    private final Map<ItemKey, Integer> fed = new HashMap<>();
    private final Map<ItemKey, Integer> collected = new HashMap<>();
    private final Map<ItemKey, Integer> inFlight = new HashMap<>();
    private final List<Feeding> feedings = new ArrayList<>();
    private State state = State.WAITING_STATION;
    private String reason = "";

    public OrderStep(BlockPos producer, ProductionRecipe recipe, int batches) {
        this.producer = producer;
        this.recipe = recipe;
        this.batches = batches;
        for (ItemStack input : recipe.inputs()) {
            inputs.merge(ItemKey.of(input), input.getCount(), Integer::sum);
        }
    }

    /** The producer on the data line that runs the step. */
    public BlockPos producer() {
        return producer;
    }

    public ProductionRecipe recipe() {
        return recipe;
    }

    public int batches() {
        return batches;
    }

    public State state() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    public String reason() {
        return reason;
    }

    void setReason(String reason) {
        this.reason = reason == null ? "" : reason;
    }

    /** Inputs per batch, merged by item. */
    public Map<ItemKey, Integer> inputs() {
        return inputs;
    }

    /** How many of an input the whole step needs. */
    public int needed(ItemKey key) {
        return inputs.getOrDefault(key, 0) * batches;
    }

    public int fed(ItemKey key) {
        return fed.getOrDefault(key, 0);
    }

    void addFed(ItemKey key, int count, List<OrderBook.Pending> pending) {
        fed.merge(key, count, Integer::sum);
        for (OrderBook.Pending one : pending) {
            feedings.add(new Feeding(key, one));
        }
    }

    /**
     * Books ended shipments: what they did not bring is no longer counted as fed.
     *
     * @return whether some input fell short
     */
    boolean settleFeedings() {
        boolean shortfall = false;
        for (Iterator<Feeding> it = feedings.iterator(); it.hasNext(); ) {
            Feeding feeding = it.next();
            if (!feeding.pending().ended()) {
                continue;
            }
            int missing = feeding.pending().asked() - feeding.pending().delivered();
            if (missing > 0) {
                fed.merge(feeding.key(), -missing, Integer::sum);
                shortfall = true;
            }
            it.remove();
        }
        return shortfall;
    }

    public boolean inputsFed() {
        for (Map.Entry<ItemKey, Integer> input : inputs.entrySet()) {
            if (fed(input.getKey()) < input.getValue() * batches) {
                return false;
            }
        }
        return true;
    }

    /** Whether the recipe produces the key. */
    public boolean produces(ItemKey key) {
        for (ItemStack output : recipe.outputs()) {
            if (ItemKey.of(output).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the step is being delivered to or producing, so the producer's outputs may be its own. */
    public boolean active() {
        return state == State.DELIVERING || state == State.PRODUCING;
    }

    /** Outputs of the key travelling from the producer to an Access Point. */
    public int inFlight(ItemKey key) {
        return inFlight.getOrDefault(key, 0);
    }

    /** Outputs left the producer as packets. */
    public void sent(ItemKey key, int count) {
        inFlight.merge(key, count, Integer::sum);
    }

    /** A collection delivery ended: {@code delivered} of the {@code sent} items reached the Access Point. */
    public void landed(ItemKey key, int sent, int delivered) {
        inFlight.merge(key, -sent, Integer::sum);
        if (delivered > 0) {
            collected.merge(key, delivered, Integer::sum);
        }
    }

    /**
     * The producer pushed outputs of the key into an Access Point on its own.
     * Counts up to what the step still needs of the key.
     *
     * @return how many were counted
     */
    public int creditPushed(ItemKey key, int count) {
        int wanted = 0;
        for (ItemStack output : recipe.outputs()) {
            if (ItemKey.of(output).equals(key)) {
                wanted += output.getCount() * batches;
            }
        }
        int credit = Math.min(count, wanted - collected(key));
        if (credit <= 0) {
            return 0;
        }
        collected.merge(key, credit, Integer::sum);
        return credit;
    }

    public int collected(ItemKey key) {
        return collected.getOrDefault(key, 0);
    }

    /** The step is done once its first output was collected in full. */
    public boolean outputsCollected() {
        if (recipe.outputs().isEmpty()) {
            return true;
        }
        ItemStack first = recipe.outputs().get(0);
        return collected(ItemKey.of(first)) >= first.getCount() * batches;
    }

    /** The first output, for listings. */
    public ItemStack output() {
        return recipe.outputs().isEmpty() ? ItemStack.EMPTY : recipe.outputs().get(0);
    }
}
