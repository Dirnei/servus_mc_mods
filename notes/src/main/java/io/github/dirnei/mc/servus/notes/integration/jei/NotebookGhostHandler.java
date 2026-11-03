package io.github.dirnei.mc.servus.notes.integration.jei;

import io.github.dirnei.mc.servus.notes.client.GoalItems;
import io.github.dirnei.mc.servus.notes.client.ItemDrop;
import io.github.dirnei.mc.servus.notes.client.NotebookScreen;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * Drops an item from JEI onto the open item picker, which picks it. The places come from what the
 * notebook drew last frame and are empty while the picker is closed.
 */
final class NotebookGhostHandler implements IGhostIngredientHandler<NotebookScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(NotebookScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> stack = ingredient.getItemStack();
        if (stack.isEmpty() || stack.get().isEmpty()) {
            return List.of();
        }
        String itemId = GoalItems.idOf(stack.get().getItem());
        return screen.itemDropsOnScreen().stream().map(drop -> NotebookGhostHandler.<I>target(drop, itemId)).toList();
    }

    private static <I> Target<I> target(ItemDrop drop, String itemId) {
        return new Target<>() {
            @Override
            public Rect2i getArea() {
                return new Rect2i(drop.x(), drop.y(), drop.width(), drop.height());
            }

            @Override
            public void accept(I dropped) {
                drop.accept().accept(itemId);
            }
        };
    }

    @Override
    public void onComplete() {
    }
}
