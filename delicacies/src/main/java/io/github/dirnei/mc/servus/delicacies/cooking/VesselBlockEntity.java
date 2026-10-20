package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.delicacies.firebox.DelicaciesCapabilities;
import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenBlockEntity;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A cooking vessel's contents and cooking: four input stacks, one output stack, the recipe it runs and its progress.
 * It asks the firebox below for heat while it has a recipe and counts up only while that firebox is hot. One item
 * kind sits in one input stack only, so a recipe sees each ingredient once.
 *
 * @param <R> the vessel's recipe kind
 */
public abstract class VesselBlockEntity<R extends VesselRecipe> extends KitchenBlockEntity {

    public static final int INPUTS = 4;
    public static final int OUTPUT = INPUTS;

    private final KitchenSlots automation = new KitchenSlots(slots, this::canHoldInput, (slot, stack) -> slot == OUTPUT);
    private int progress;
    private boolean recipeDirty = true;
    private @Nullable RecipeHolder<R> recipe;
    private @Nullable BlockCapabilityCache<Firebox, Void> firebox;

    protected VesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, INPUTS + 1);
    }

    @Override
    protected boolean slotTakes(int slot, ItemStack stack) {
        return slot == OUTPUT || canHoldInput(slot, stack);
    }

    protected abstract RecipeType<R> recipeType();

    /** The fluid the vessel holds; none for the Pan. */
    public FluidStack fluid() {
        return FluidStack.EMPTY;
    }

    /** Drains what one cooking takes from the tank. */
    protected void drainFluid(int amount) {
    }

    /** Fills or empties a fluid container in the player's hand; false when the vessel has no tank or it is no container. */
    public boolean useFluidContainer(Player player, InteractionHand hand) {
        return false;
    }

    /** The item handler automation sees on every side: ingredients in, the result out. */
    public ResourceHandler<ItemResource> itemCapability() {
        return automation;
    }

    /** Whether some recipe of this vessel takes {@code stack}, regardless of count. */
    public boolean accepts(ItemStack stack) {
        return level != null && !stack.isEmpty()
                && ServusRecipes.all(level, recipeType()).stream().anyMatch(holder -> holder.value().accepts(stack));
    }

    /** An ingredient goes into the input stack that holds its kind, or into an empty one when none does. */
    protected boolean canHoldInput(int slot, ItemStack stack) {
        if (slot >= INPUTS || !accepts(stack)) {
            return false;
        }
        ItemStack here = slots.getStackInSlot(slot);
        if (!here.isEmpty()) {
            return ItemStack.isSameItemSameComponents(here, stack);
        }
        for (int other = 0; other < INPUTS; other++) {
            if (ItemStack.isSameItemSameComponents(slots.getStackInSlot(other), stack)) {
                return false;
            }
        }
        return true;
    }

    /** Puts as much of {@code stack} into the inputs as fits; returns the rest. */
    public ItemStack insertInputs(ItemStack stack) {
        ItemStack left = stack;
        for (int slot = 0; slot < INPUTS && !left.isEmpty(); slot++) {
            left = slots.insertItem(slot, left, false);
        }
        return left;
    }

    /** Takes the whole result, or the last filled input stack when there is no result. */
    public ItemStack takeOut() {
        if (!slots.getStackInSlot(OUTPUT).isEmpty()) {
            return slots.extractItem(OUTPUT, Integer.MAX_VALUE, false);
        }
        for (int slot = INPUTS - 1; slot >= 0; slot--) {
            if (!slots.getStackInSlot(slot).isEmpty()) {
                return slots.extractItem(slot, Integer.MAX_VALUE, false);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The input stacks, for the renderer. */
    public List<ItemStack> inputs() {
        List<ItemStack> inputs = new ArrayList<>(INPUTS);
        for (int slot = 0; slot < INPUTS; slot++) {
            inputs.add(slots.getStackInSlot(slot));
        }
        return inputs;
    }

    public ItemStack output() {
        return slots.getStackInSlot(OUTPUT);
    }

    public int progress() {
        return progress;
    }

    private VesselInput input() {
        return new VesselInput(inputs(), fluid());
    }

    private Optional<RecipeHolder<R>> currentRecipe() {
        if (recipeDirty && level != null) {
            recipe = ServusRecipes.first(level, recipeType(), input()).orElse(null);
            recipeDirty = false;
        }
        return Optional.ofNullable(recipe);
    }

    /** The current recipe while its result fits into the output. */
    private Optional<R> runnableRecipe() {
        return currentRecipe().map(RecipeHolder::value)
                .filter(found -> slots.insertItem(OUTPUT, found.result(), true).isEmpty());
    }

    private @Nullable Firebox firebox() {
        if (firebox == null && level instanceof ServerLevel server) {
            firebox = BlockCapabilityCache.create(DelicaciesCapabilities.FIREBOX, server, worldPosition.below(), null);
        }
        return firebox == null ? null : firebox.getCapability();
    }

    void serverTick() {
        Optional<R> found = runnableRecipe();
        boolean cooking = false;
        if (found.isEmpty()) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
        } else {
            Firebox below = firebox();
            if (below != null) {
                below.demandHeat();
                if (below.isHot()) {
                    cooking = true;
                    progress++;
                    if (progress >= Math.max(1, found.get().time())) {
                        progress = 0;
                        cook(found.get());
                    }
                    setChanged();
                }
            }
        }
        showCooking(cooking);
    }

    private void cook(R found) {
        int[] assignment = found.slotsFor(input()).orElse(null);
        if (assignment == null) {
            return;
        }
        List<SizedIngredient> ingredients = found.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            slots.extractItem(assignment[i], ingredients.get(i).count(), false);
        }
        found.fluid().ifPresent(fluid -> drainFluid(fluid.amount()));
        slots.insertItem(OUTPUT, found.result(), false);
    }

    private void showCooking(boolean cooking) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(VesselBlock.COOKING) && state.getValue(VesselBlock.COOKING) != cooking) {
            level.setBlock(worldPosition, state.setValue(VesselBlock.COOKING, cooking), Block.UPDATE_CLIENTS);
        }
    }

    /** After any change of items or fluid: look for the recipe again and show the change to clients. */
    @Override
    protected void contentsChanged() {
        recipeDirty = true;
        super.contentsChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
        recipeDirty = true;
    }

}
