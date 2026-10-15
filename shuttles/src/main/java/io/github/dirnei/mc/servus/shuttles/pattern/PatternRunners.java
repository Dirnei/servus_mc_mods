package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Which producers can execute a pattern, and the recipes patterns add to a
 * producer for the CPU's planner: every Crafting Station runs every crafting
 * pattern of its data line. Server thread only.
 */
public final class PatternRunners {

    private PatternRunners() {
    }

    /** Every Crafting Station is the CPU's hands and can run any crafting pattern. */
    public static boolean canRun(ServerLevel level, BlockPos producer, EncodedPattern pattern) {
        return level.getBlockEntity(producer) instanceof CraftingStationBlockEntity;
    }

    /**
     * The producer's reported recipes plus one recipe per pattern it can run
     * that it does not report already with the same id and inputs.
     */
    public static List<ProductionRecipe> withPatterns(ServerLevel level, BlockPos producer, List<ProductionRecipe> reported,
                                                      List<PatternLibrary.Entry> patterns) {
        if (!(level.getBlockEntity(producer) instanceof CraftingStationBlockEntity)) {
            // Only stations run patterns; the provider's own list stays the same instance for caches.
            return reported;
        }
        List<ProductionRecipe> recipes = new ArrayList<>(reported);
        int ticks = 1;
        for (ProductionRecipe recipe : reported) {
            ticks = Math.max(ticks, recipe.ticks());
        }
        if (level.getBlockEntity(producer) instanceof CraftingStationBlockEntity station) {
            ticks = Math.max(ticks, station.workTicks());
        }
        for (PatternLibrary.Entry entry : patterns) {
            EncodedPattern pattern = entry.pattern();
            if (!canRun(level, producer, pattern) || reports(recipes, pattern)) {
                continue;
            }
            recipes.add(new ProductionRecipe(pattern.recipe(), pattern.inputs(), List.of(pattern.result()), ticks));
        }
        return recipes;
    }

    private static boolean reports(List<ProductionRecipe> recipes, EncodedPattern pattern) {
        Map<ItemKey, Integer> wanted = pattern.required();
        for (ProductionRecipe recipe : recipes) {
            if (!recipe.id().equals(pattern.recipe())) {
                continue;
            }
            if (ItemKey.counts(recipe.inputs()).equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
