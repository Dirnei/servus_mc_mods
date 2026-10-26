package io.github.dirnei.mc.servus.shuttles.integration.jei;

import io.github.dirnei.mc.servus.shuttles.client.StorageTerminalScreen;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/** JEI ghost drops on the Storage Terminal's Rules tab: an item dropped on the add slot opens its Rule tab. */
final class RuleAddSlotHandler implements IGhostIngredientHandler<StorageTerminalScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(StorageTerminalScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> stack = ingredient.getItemStack();
        Optional<Rect2i> area = screen.ruleAddTarget();
        if (stack.isEmpty() || stack.get().isEmpty() || area.isEmpty()) {
            return List.of();
        }
        ItemStack item = stack.get().copyWithCount(1);
        return List.of(new Target<>() {
            @Override
            public Rect2i getArea() {
                return area.get();
            }

            @Override
            public void accept(I dropped) {
                screen.openRuleFor(item);
            }
        });
    }

    @Override
    public void onComplete() {
    }
}
