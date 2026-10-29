package io.github.dirnei.mc.servus.foundation.filter;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Wears filters for any mod. A filter is an item with an entry in {@link FilterDataMaps#FILTER}; its used points sit in
 * the {@code servus_foundation:filter_used} component. Only single filters wear: holders keep one filter per slot.
 * Call on the server thread.
 */
public final class Filters {

    /** The points actually spent and the stack to store back in the holder's slot (the spent item once worn out). */
    public record WearResult(int spent, ItemStack stack) {
    }

    private Filters() {
    }

    /** Whether {@code stack} holds a filter item, whatever its count or wear. */
    public static boolean isFilter(ItemStack stack) {
        return FilterDataMaps.spec(stack) != null;
    }

    /**
     * Wears a single filter by up to {@code points}. The input stack is never changed; store {@link WearResult#stack()}
     * back. An empty stack, a non filter or a stack of more than one spends 0 and returns the stack itself.
     */
    public static WearResult wear(ItemStack stack, int points) {
        FilterSpec spec = FilterDataMaps.spec(stack);
        if (spec == null || stack.getCount() != 1) {
            return new WearResult(0, stack);
        }
        FilterWear.Spend spend = wearOf(stack, spec).spend(points);
        if (spend.wornOut()) {
            return new WearResult(spend.spent(), new ItemStack(spec.spent()));
        }
        if (spend.spent() == 0) {
            return new WearResult(0, stack);
        }
        ItemStack worn = stack.copy();
        worn.set(ServusFoundationComponents.FILTER_USED.get(), spend.used());
        return new WearResult(spend.spent(), worn);
    }

    /** The points left in a filter stack, 0 for a non filter. */
    public static int pointsLeft(ItemStack stack) {
        FilterSpec spec = FilterDataMaps.spec(stack);
        return spec == null ? 0 : wearOf(stack, spec).left();
    }

    static FilterWear wearOf(ItemStack stack, FilterSpec spec) {
        return new FilterWear(spec.points(), stack.getOrDefault(ServusFoundationComponents.FILTER_USED.get(), 0));
    }
}
