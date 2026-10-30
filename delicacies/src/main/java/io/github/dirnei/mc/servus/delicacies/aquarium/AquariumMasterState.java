package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.foundation.filter.Filters;
import io.github.dirnei.mc.servus.foundation.machine.MachineEnergyStorage;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Everything an aquarium keeps, held by its master Floor block: species, stock, dirt, feeding, the catch settings, the
 * slots (feed, filter, three catch) and the FE buffer with the cycle's power counters. Server side logic; the block
 * entity saves and syncs it.
 */
public final class AquariumMasterState {

    public static final int FEED_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int FIRST_CATCH_SLOT = 2;
    public static final int SLOT_COUNT = 5;

    private final Runnable onChanged;
    private final ItemSlots slots;
    private final KitchenSlots automationSlots;
    private final MachineEnergyStorage energy;

    private @Nullable FishSpecies species;
    private double stock;
    private double dirt;
    private int unfedCycles;
    private int murky;
    private boolean autoCatch = true;
    private int keep = -1;
    private int ticks;
    private int poweredTicks;
    private boolean lastTickPowered;

    AquariumMasterState(Runnable onChanged) {
        this.onChanged = onChanged;
        this.slots = new ItemSlots(SLOT_COUNT) {
            @Override
            public int getSlotLimit(int slot) {
                return slot == FILTER_SLOT ? 1 : super.getSlotLimit(slot);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }
        };
        this.automationSlots = new KitchenSlots(slots, AquariumMasterState::acceptsFromOutside,
                (slot, stack) -> slot >= FIRST_CATCH_SLOT);
        this.energy = new MachineEnergyStorage(1, Integer.MAX_VALUE, 0, onChanged);
    }

    /** Whether automation or a player may put {@code stack} into {@code slot}: feed and filters only, never the catch. */
    public static boolean acceptsFromOutside(int slot, ItemStack stack) {
        return switch (slot) {
            case FEED_SLOT -> stack.is(KitchenTags.FISH_FEED);
            case FILTER_SLOT -> Filters.isFilter(stack);
            default -> false;
        };
    }

    public ItemSlots slots() {
        return slots;
    }

    public KitchenSlots automationSlots() {
        return automationSlots;
    }

    public MachineEnergyStorage energy() {
        return energy;
    }

    public @Nullable FishSpecies species() {
        return species;
    }

    public double stock() {
        return stock;
    }

    public int wholeFish() {
        return (int) Math.floor(stock);
    }

    public double dirt() {
        return dirt;
    }

    /** mB of Murky Water the filter made that wait to be drained. */
    public int murky() {
        return murky;
    }

    /** Sets the Murky Water held, for GameTests and the cycle. */
    public void setMurky(int murky) {
        this.murky = Math.max(0, murky);
        onChanged.run();
    }

    public int unfedCycles() {
        return unfedCycles;
    }

    public boolean autoCatch() {
        return autoCatch;
    }

    public int keep(AquariumStats stats) {
        return keep < 0 ? stats.keepDefault() : keep;
    }

    /** Sets the catch settings; a negative keep is taken as 0. */
    public void setCatch(boolean autoCatch, int keep) {
        this.autoCatch = autoCatch;
        this.keep = Math.max(0, keep);
        onChanged.run();
    }

    /** Sets the stock and species directly, for buckets and GameTests. */
    public void setStock(@Nullable FishSpecies species, double stock) {
        this.species = species;
        this.stock = Math.max(0, stock);
        onChanged.run();
    }

    public void setDirt(double dirt) {
        this.dirt = Math.max(0, dirt);
        onChanged.run();
    }

    public void setUnfedCycles(int unfedCycles) {
        this.unfedCycles = Math.max(0, unfedCycles);
        onChanged.run();
    }

    /** Drains up to {@code amount} mB of Murky Water; returns what was drained. */
    int takeMurky(int amount, boolean simulate) {
        int taken = Math.max(0, Math.min(amount, murky));
        if (!simulate && taken > 0) {
            setMurky(murky - taken);
        }
        return taken;
    }

    /**
     * Spends this tick's power: {@code cost} FE when stored, else nothing. Returns whether the tick was powered and
     * counts it toward the cycle; returns true once the cycle is due.
     */
    boolean tickPower(int cost, AquariumStats stats) {
        energy.setCapacity(AquariumPower.buffer(cost, stats));
        ticks++;
        lastTickPowered = energy.consume(cost);
        if (lastTickPowered) {
            poweredTicks++;
        }
        return ticks >= stats.cycleTicks();
    }

    public boolean lastTickPowered() {
        return lastTickPowered;
    }

    /** The powered share of the ticks since the last cycle, and resets the counters. */
    double takePoweredShare() {
        double share = AquariumPower.share(poweredTicks, ticks);
        ticks = 0;
        poweredTicks = 0;
        return share;
    }

    /** The raw fish the catch slots still take for this species. */
    int catchRoom() {
        if (species == null) {
            return 0;
        }
        ItemStack fish = new ItemStack(species.rawFish());
        int room = 0;
        for (int slot = FIRST_CATCH_SLOT; slot < SLOT_COUNT; slot++) {
            ItemStack in = slots.getStackInSlot(slot);
            if (in.isEmpty()) {
                room += fish.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(in, fish)) {
                room += in.getMaxStackSize() - in.getCount();
            }
        }
        return room;
    }

    /** Puts {@code stack} into the catch slots; returns what did not fit. */
    ItemStack putIntoCatch(ItemStack stack) {
        ItemStack left = stack;
        for (int slot = FIRST_CATCH_SLOT; slot < SLOT_COUNT && !left.isEmpty(); slot++) {
            left = slots.insertItem(slot, left, false);
        }
        return left;
    }

    /** Moves a spent filter out of the filter slot into the catch slots, as far as there is room. */
    void moveSpentFilter() {
        ItemStack filter = slots.getStackInSlot(FILTER_SLOT);
        if (!filter.isEmpty() && !Filters.isFilter(filter)) {
            slots.setStackInSlot(FILTER_SLOT, putIntoCatch(filter.copy()));
        }
    }

    /** Applies a cycle's result: stock, dirt, feed eaten, filter wear and the catch. */
    void apply(AquariumCycle.Output out) {
        stock = out.stock();
        dirt = out.dirt();
        unfedCycles = out.unfedCycles();
        if (out.feedUsed() > 0) {
            slots.extractItem(FEED_SLOT, out.feedUsed(), false);
        }
        if (out.filterUsed() > 0) {
            slots.setStackInSlot(FILTER_SLOT, Filters.wear(slots.getStackInSlot(FILTER_SLOT), out.filterUsed()).stack());
        }
        if (out.caught() > 0 && species != null) {
            ItemStack left = putIntoCatch(new ItemStack(species.rawFish(), out.caught()));
            stock += left.getCount();
        }
        moveSpentFilter();
        onChanged.run();
    }

    /** Drops the slots and one raw fish per whole fish at {@code pos}, and empties the aquarium. */
    void dropAll(Level level, BlockPos pos) {
        slots.dropAll(level, pos);
        if (species != null) {
            int fish = wholeFish();
            ItemStack one = new ItemStack(species.rawFish());
            while (fish > 0) {
                int count = Math.min(fish, one.getMaxStackSize());
                Block.popResource(level, pos, one.copyWithCount(count));
                fish -= count;
            }
        }
        reset();
    }

    /** Forgets everything, as a part that is no master. */
    void reset() {
        species = null;
        stock = 0;
        dirt = 0;
        unfedCycles = 0;
        murky = 0;
        autoCatch = true;
        keep = -1;
        ticks = 0;
        poweredTicks = 0;
        lastTickPowered = false;
        energy.setEnergy(0);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            slots.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    void save(ValueOutput output) {
        output.putInt("Species", species == null ? -1 : species.ordinal());
        output.putDouble("Stock", stock);
        output.putDouble("Dirt", dirt);
        output.putInt("Unfed", unfedCycles);
        output.putInt("Murky", murky);
        output.putBoolean("AutoCatch", autoCatch);
        output.putInt("Keep", keep);
        output.putInt("Ticks", ticks);
        output.putInt("PoweredTicks", poweredTicks);
        output.putInt("Energy", energy.getEnergyStored());
        output.putInt("EnergyCapacity", energy.getMaxEnergyStored());
        slots.save(output, "Slots");
    }

    void load(ValueInput input) {
        species = FishSpecies.byOrdinal(input.getIntOr("Species", -1));
        stock = input.getDoubleOr("Stock", 0);
        dirt = input.getDoubleOr("Dirt", 0);
        unfedCycles = input.getIntOr("Unfed", 0);
        murky = input.getIntOr("Murky", 0);
        autoCatch = input.getBooleanOr("AutoCatch", true);
        keep = input.getIntOr("Keep", -1);
        ticks = input.getIntOr("Ticks", 0);
        poweredTicks = input.getIntOr("PoweredTicks", 0);
        energy.setCapacity(input.getIntOr("EnergyCapacity", 1));
        energy.setEnergy(input.getIntOr("Energy", 0));
        // Client update tags carry no slots; the open menu keeps the client's copy in sync, so it must not be cleared.
        if (input.child("Slots").isPresent()) {
            slots.load(input, "Slots");
        }
    }
}
