package io.github.dirnei.mc.servus.storage.pattern;

import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Which producers can execute a pattern, and the recipes patterns add to a
 * producer for the CPU's planner. Until Crafting Stations are the CPU's hands,
 * a station runs a pattern only when its own list holds the same recipe with
 * the same items. Server thread only.
 */
public final class PatternRunners {

    private PatternRunners() {
    }

    public static boolean canRun(ServerLevel level, BlockPos producer, EncodedPattern pattern) {
        if (!(level.getBlockEntity(producer) instanceof CraftingStationBlockEntity station)) {
            return false;
        }
        for (CraftingStationBlockEntity.StationRecipe recipe : station.recipes()) {
            List<ItemKey> keys = new ArrayList<>();
            for (ItemStack cell : recipe.template()) {
                keys.add(cell.isEmpty() ? null : ItemKey.of(cell));
            }
            if (pattern.sameWork(recipe.id(), keys)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The producer's reported recipes plus one recipe per pattern it can run
     * that it does not report already with the same id and inputs.
     */
    public static List<ProductionRecipe> withPatterns(ServerLevel level, BlockPos producer, List<ProductionRecipe> reported,
                                                      List<PatternLibrary.Entry> patterns) {
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
            Map<ItemKey, Integer> inputs = new HashMap<>();
            for (ItemStack input : recipe.inputs()) {
                inputs.merge(ItemKey.of(input), input.getCount(), Integer::sum);
            }
            if (inputs.equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
