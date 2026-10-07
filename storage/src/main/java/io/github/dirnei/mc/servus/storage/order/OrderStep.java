package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.lib.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** One recipe run a number of times on one dock. */
public final class OrderStep {

    public enum State { WAITING_DOCK, FEEDING, PRODUCING, DONE }

    private final BlockPos dock;
    private final ProductionRecipe recipe;
    private final int batches;
    private final Map<ItemKey, Integer> inputs = new LinkedHashMap<>();
    private final Map<ItemKey, Integer> fed = new HashMap<>();
    private final Map<ItemKey, Integer> collected = new HashMap<>();
    private State state = State.WAITING_DOCK;
    private String reason = "";

    public OrderStep(BlockPos dock, ProductionRecipe recipe, int batches) {
        this.dock = dock;
        this.recipe = recipe;
        this.batches = batches;
        for (ItemStack input : recipe.inputs()) {
            inputs.merge(ItemKey.of(input), input.getCount(), Integer::sum);
        }
    }

    public BlockPos dock() {
        return dock;
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

    void addFed(ItemKey key, int count) {
        fed.merge(key, count, Integer::sum);
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

    void addCollected(ItemKey key, int count) {
        collected.merge(key, count, Integer::sum);
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
