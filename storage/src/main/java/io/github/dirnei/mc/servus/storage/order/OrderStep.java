package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One recipe run a number of times on one producer of the data line. */
public final class OrderStep {

    public enum State { WAITING_STATION, DELIVERING, PRODUCING, DONE }

    /** A shipment of an input on its way to the producer, by ledger id. */
    private record Feeding(ItemKey key, long transfer, int asked) {
    }

    /** Outputs moving from the producer to an Access Point, by ledger id. */
    private record Collecting(ItemKey key, long transfer, int sent) {
    }

    private final BlockPos producer;
    private final ProductionRecipe recipe;
    private final int batches;
    private final Map<ItemKey, Integer> inputs = new LinkedHashMap<>();
    private final Map<ItemKey, Integer> fed = new HashMap<>();
    private final Map<ItemKey, Integer> collected = new HashMap<>();
    private final Map<ItemKey, Integer> inFlight = new HashMap<>();
    private final List<Feeding> feedings = new ArrayList<>();
    private final List<Collecting> collecting = new ArrayList<>();
    private State state = State.WAITING_STATION;
    private String reason = "";
    private StepRef ref;

    public OrderStep(BlockPos producer, ProductionRecipe recipe, int batches) {
        this.producer = producer;
        this.recipe = recipe;
        this.batches = batches;
        for (ItemStack input : recipe.inputs()) {
            inputs.merge(ItemKey.of(input), input.getCount(), Integer::sum);
        }
    }

    /** Which CPU, order and place this step is, for the producer's job. */
    public StepRef ref() {
        return ref;
    }

    void setRef(StepRef ref) {
        this.ref = ref;
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

    void addFed(ItemKey key, int count, List<OrderBook.Shipped> shipped) {
        fed.merge(key, count, Integer::sum);
        for (OrderBook.Shipped one : shipped) {
            feedings.add(new Feeding(key, one.transfer(), one.asked()));
        }
    }

    /**
     * Books ended shipments: what they did not bring is no longer counted as
     * fed. A shipment the ledger forgot counts as bringing nothing, so it is
     * shipped again.
     *
     * @return whether some input fell short
     */
    boolean settleFeedings(LongFunction<ProductionLedger.Transfer> transfers) {
        boolean shortfall = false;
        for (Iterator<Feeding> it = feedings.iterator(); it.hasNext(); ) {
            Feeding feeding = it.next();
            ProductionLedger.Transfer transfer = transfers.apply(feeding.transfer());
            if (transfer != null && !transfer.ended()) {
                continue;
            }
            int missing = feeding.asked() - (transfer == null ? 0 : transfer.delivered());
            if (missing > 0) {
                fed.merge(feeding.key(), -missing, Integer::sum);
                shortfall = true;
            }
            it.remove();
        }
        return shortfall;
    }

    /**
     * Books ended collections: what reached the Access Point is collected. A
     * collection the ledger forgot counts as landed in full.
     */
    void settleCollections(LongFunction<ProductionLedger.Transfer> transfers) {
        for (Iterator<Collecting> it = collecting.iterator(); it.hasNext(); ) {
            Collecting one = it.next();
            ProductionLedger.Transfer transfer = transfers.apply(one.transfer());
            if (transfer != null && !transfer.ended()) {
                continue;
            }
            landed(one.key(), one.sent(), transfer == null ? one.sent() : transfer.delivered());
            it.remove();
        }
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

    /** Outputs left the producer as packets, tracked by the ledger transfer. */
    public void collecting(ItemKey key, long transfer, int count) {
        inFlight.merge(key, count, Integer::sum);
        collecting.add(new Collecting(key, transfer, count));
    }

    /** A collection delivery ended: {@code delivered} of the {@code sent} items reached the Access Point. */
    private void landed(ItemKey key, int sent, int delivered) {
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

    // Persistence

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Producer", NbtUtils.writeBlockPos(producer));
        tag.putString("Recipe", recipe.id().toString());
        tag.put("Inputs", Saving.stacks(recipe.inputs(), registries));
        tag.put("Outputs", Saving.stacks(recipe.outputs(), registries));
        tag.putInt("Ticks", recipe.ticks());
        tag.putInt("Batches", batches);
        tag.putString("State", state.name());
        tag.putString("Reason", reason);
        tag.put("Fed", Saving.counts(fed, registries));
        tag.put("Collected", Saving.counts(collected, registries));
        ListTag feedingList = new ListTag();
        for (Feeding feeding : feedings) {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", Saving.key(feeding.key(), registries));
            entry.putLong("Transfer", feeding.transfer());
            entry.putInt("Asked", feeding.asked());
            feedingList.add(entry);
        }
        tag.put("Feedings", feedingList);
        ListTag collectingList = new ListTag();
        for (Collecting one : collecting) {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", Saving.key(one.key(), registries));
            entry.putLong("Transfer", one.transfer());
            entry.putInt("Sent", one.sent());
            collectingList.add(entry);
        }
        tag.put("Collecting", collectingList);
        return tag;
    }

    static OrderStep load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockPos producer = NbtUtils.readBlockPos(tag, "Producer").orElse(BlockPos.ZERO);
        ProductionRecipe recipe = new ProductionRecipe(ResourceLocation.parse(tag.getString("Recipe")),
                Saving.stacks(tag.getList("Inputs", Tag.TAG_COMPOUND), registries),
                Saving.stacks(tag.getList("Outputs", Tag.TAG_COMPOUND), registries), tag.getInt("Ticks"));
        OrderStep step = new OrderStep(producer, recipe, tag.getInt("Batches"));
        step.state = Saving.state(State.class, tag.getString("State"), State.WAITING_STATION);
        step.reason = tag.getString("Reason");
        step.fed.putAll(Saving.counts(tag.getList("Fed", Tag.TAG_COMPOUND), registries));
        step.collected.putAll(Saving.counts(tag.getList("Collected", Tag.TAG_COMPOUND), registries));
        for (Tag raw : tag.getList("Feedings", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            Saving.key(entry.getCompound("Item"), registries).ifPresent(key ->
                    step.feedings.add(new Feeding(key, entry.getLong("Transfer"), entry.getInt("Asked"))));
        }
        for (Tag raw : tag.getList("Collecting", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            Saving.key(entry.getCompound("Item"), registries).ifPresent(key ->
                    step.collecting(key, entry.getLong("Transfer"), entry.getInt("Sent")));
        }
        return step;
    }
}
