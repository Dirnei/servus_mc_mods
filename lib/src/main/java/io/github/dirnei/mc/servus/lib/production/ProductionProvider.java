package io.github.dirnei.mc.servus.lib.production;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** A block that can produce items: what it can make right now and what it is working on. */
public interface ProductionProvider {

    /** The recipes the block can run right now. */
    List<ProductionRecipe> recipes();

    /** The id of the recipe the block is working on, if any. */
    Optional<ResourceLocation> active();
}
