package io.github.dirnei.mc.servus.foundation.production;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One thing a block can make: exact input stacks with counts, the output
 * stacks with counts and the time one batch takes.
 *
 * @param id      the recipe id, unique within the provider
 * @param inputs  what one batch consumes, one stack per item
 * @param outputs what one batch produces, one stack per item
 * @param ticks   how long one batch takes
 */
public record ProductionRecipe(ResourceLocation id, List<ItemStack> inputs, List<ItemStack> outputs, int ticks) {

    public ProductionRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
