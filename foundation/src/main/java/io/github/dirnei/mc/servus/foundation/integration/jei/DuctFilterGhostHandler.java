package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.logistics.client.UtilityDuctScreen;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Lets an ingredient dragged from JEI onto a filter slot of the endpoint GUI set that slot while the search
 * popup is closed: an item on the Item Pipe page, a fluid or a filled container on the Fluid Pipe page.
 */
final class DuctFilterGhostHandler implements IGhostIngredientHandler<UtilityDuctScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(UtilityDuctScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (screen.isPickerOpen()) {
            return List.of();
        }
        if (screen.showsFluidFilter()) {
            Optional<Identifier> fluid = fluidOf(ingredient).map(BuiltInRegistries.FLUID::getKey);
            return fluid.isEmpty() ? List.of() : targets(screen, FluidFilter.SLOTS, slot -> screen.sendFluidFilter(slot, fluid));
        }
        Optional<ItemStack> stack = ingredient.getItemStack();
        if (stack.isEmpty() || stack.get().isEmpty() || !screen.showsFilter()) {
            return List.of();
        }
        Optional<Identifier> id = Optional.of(BuiltInRegistries.ITEM.getKey(stack.get().getItem()));
        return targets(screen, UtilityDuctBlockEntity.FILTER_SLOTS, slot -> screen.sendFilter(slot, id));
    }

    /** The fluid of a JEI fluid ingredient or of an item that holds fluid. */
    private static Optional<Fluid> fluidOf(ITypedIngredient<?> ingredient) {
        Optional<FluidStack> fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK);
        if (fluid.isPresent()) {
            return fluid.get().isEmpty() || fluid.get().getFluid() == Fluids.EMPTY ? Optional.empty() : Optional.of(fluid.get().getFluid());
        }
        return ingredient.getItemStack().flatMap(Transfer::fluidOf);
    }

    private static <I> List<Target<I>> targets(UtilityDuctScreen screen, int slots, IntConsumer set) {
        List<Target<I>> targets = new ArrayList<>();
        for (int slot = 0; slot < slots; slot++) {
            int target = slot;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return screen.filterSlotArea(target);
                }

                @Override
                public void accept(I dropped) {
                    set.accept(target);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
