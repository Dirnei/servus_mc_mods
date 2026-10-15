package io.github.dirnei.mc.servus.foundation.integration.jei;

import io.github.dirnei.mc.servus.foundation.logistics.client.UtilityDuctScreen;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Lets an item dragged from JEI onto a filter slot of the endpoint GUI set that slot, on the Item Pipe page while the search popup is closed. */
final class DuctFilterGhostHandler implements IGhostIngredientHandler<UtilityDuctScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(UtilityDuctScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> stack = ingredient.getItemStack();
        if (stack.isEmpty() || stack.get().isEmpty() || !screen.showsFilter() || screen.isPickerOpen()) {
            return List.of();
        }
        Optional<Identifier> id = Optional.of(BuiltInRegistries.ITEM.getKey(stack.get().getItem()));
        List<Target<I>> targets = new ArrayList<>();
        for (int slot = 0; slot < UtilityDuctBlockEntity.FILTER_SLOTS; slot++) {
            int target = slot;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return screen.filterSlotArea(target);
                }

                @Override
                public void accept(I dropped) {
                    screen.sendFilter(target, id);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
