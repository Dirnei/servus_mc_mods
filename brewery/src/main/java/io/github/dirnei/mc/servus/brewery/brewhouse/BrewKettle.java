package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The Brew Kettle's contents and brewing: four input stacks, a water tank, a wort tank, the recipe it runs and its
 * progress. It asks the firebox for heat while it has a recipe and counts up only while the firebox is hot. One item
 * kind sits in one input stack only.
 */
public final class BrewKettle {

    public static final int INPUTS = 4;

    private final Supplier<@Nullable Level> level;
    private final Runnable changed;
    private final ItemSlots inputs = new ItemSlots(INPUTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canHold(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    private final FluidSlot water;
    private final FluidSlot wort;
    private final KitchenSlots automation = new KitchenSlots(inputs, this::canHold, (slot, stack) -> false);
    private final KettleFluids fluids;
    private int progress;
    private boolean recipeDirty = true;
    private @Nullable BrewingRecipe recipe;

    /**
     * @param level   the level of the Brewhouse, null while it has none
     * @param changed called after any change that must be saved
     */
    public BrewKettle(Supplier<@Nullable Level> level, Runnable changed) {
        this.level = level;
        this.changed = changed;
        BrewhouseStats stats = BrewhouseStatsManager.INSTANCE.stats();
        this.water = new FluidSlot(stats.kettleCapacity(), this::takesWater) {
            @Override
            protected void onContentsChanged() {
                contentsChanged();
            }
        };
        this.wort = new FluidSlot(stats.wortCapacity()) {
            @Override
            protected void onContentsChanged() {
                contentsChanged();
            }
        };
        this.fluids = new KettleFluids(water, wort);
    }

    /** The item handler automation sees: ingredients in, nothing out. */
    public KitchenSlots items() {
        return automation;
    }

    /** The fluid handler automation and buckets see: water in, wort out. */
    public KettleFluids fluids() {
        return fluids;
    }

    public FluidStack water() {
        return water.getFluid();
    }

    public FluidStack wort() {
        return wort.getFluid();
    }

    public int progress() {
        return progress;
    }

    /** Whether some brewing recipe takes {@code stack}, regardless of count. */
    public boolean accepts(ItemStack stack) {
        Level at = level.get();
        return at != null && ServusBreweryRecipes.brews(at, stack);
    }

    private boolean takesWater(FluidStack fluid) {
        Level at = level.get();
        return at == null || ServusRecipes.all(at, ServusBreweryRecipes.BREWING.get()).stream()
                .anyMatch(holder -> holder.value().acceptsFluid(fluid));
    }

    /** An ingredient goes into the input stack that holds its kind, or into an empty one when none does. */
    private boolean canHold(int slot, ItemStack stack) {
        if (slot >= INPUTS || !accepts(stack)) {
            return false;
        }
        ItemStack here = inputs.getStackInSlot(slot);
        if (!here.isEmpty()) {
            return ItemStack.isSameItemSameComponents(here, stack);
        }
        for (int other = 0; other < INPUTS; other++) {
            if (ItemStack.isSameItemSameComponents(inputs.getStackInSlot(other), stack)) {
                return false;
            }
        }
        return true;
    }

    /** Puts as much of {@code stack} into the inputs as fits; returns the rest. */
    public ItemStack insertInputs(ItemStack stack) {
        ItemStack left = stack;
        for (int slot = 0; slot < INPUTS && !left.isEmpty(); slot++) {
            left = inputs.insertItem(slot, left, false);
        }
        return left;
    }

    /** Takes the last filled input stack. */
    public ItemStack takeLast() {
        for (int slot = INPUTS - 1; slot >= 0; slot--) {
            if (!inputs.getStackInSlot(slot).isEmpty()) {
                return inputs.extractItem(slot, Integer.MAX_VALUE, false);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The input stacks. */
    public List<ItemStack> stacks() {
        List<ItemStack> stacks = new ArrayList<>(INPUTS);
        for (int slot = 0; slot < INPUTS; slot++) {
            stacks.add(inputs.getStackInSlot(slot));
        }
        return stacks;
    }

    private BrewingInput input() {
        return new BrewingInput(stacks(), water.getFluid());
    }

    private Optional<BrewingRecipe> currentRecipe() {
        Level at = level.get();
        if (recipeDirty && at != null) {
            recipe = ServusRecipes.first(at, ServusBreweryRecipes.BREWING.get(), input()).map(RecipeHolder::value).orElse(null);
            recipeDirty = false;
        }
        return Optional.ofNullable(recipe);
    }

    /** The current recipe while its wort fits into the wort tank. */
    private Optional<BrewingRecipe> runnableRecipe() {
        return currentRecipe().filter(found -> wort.fill(found.wort(), true) >= found.result().amount());
    }

    /** One server tick; returns whether the kettle brews (has a runnable recipe over a hot fire). */
    public boolean tick(Firebox firebox) {
        Optional<BrewingRecipe> found = runnableRecipe();
        if (found.isEmpty()) {
            if (progress > 0) {
                progress = 0;
                changed.run();
            }
            return false;
        }
        firebox.demandHeat();
        if (!firebox.isHot()) {
            return false;
        }
        progress++;
        if (progress >= Math.max(1, found.get().time())) {
            progress = 0;
            brew(found.get());
        }
        changed.run();
        return true;
    }

    private void brew(BrewingRecipe found) {
        int[] assignment = found.slotsFor(input()).orElse(null);
        if (assignment == null) {
            return;
        }
        List<SizedIngredient> ingredients = found.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            inputs.extractItem(assignment[i], ingredients.get(i).count(), false);
        }
        water.drain(found.fluid().amount(), false);
        wort.fill(found.wort(), false);
    }

    private void contentsChanged() {
        recipeDirty = true;
        changed.run();
    }

    /** Drops the input stacks; the fluids are lost. */
    public void dropAll(Level at, BlockPos pos) {
        inputs.dropAll(at, pos);
    }

    public void save(ValueOutput output) {
        inputs.save(output, "KettleItems");
        water.save(output, "Water");
        wort.save(output, "Wort");
        output.putInt("KettleProgress", progress);
    }

    public void load(ValueInput input) {
        inputs.load(input, "KettleItems");
        water.load(input, "Water");
        wort.load(input, "Wort");
        progress = input.getIntOr("KettleProgress", 0);
        recipeDirty = true;
    }
}
