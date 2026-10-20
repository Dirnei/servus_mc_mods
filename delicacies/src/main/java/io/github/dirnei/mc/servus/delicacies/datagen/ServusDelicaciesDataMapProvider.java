package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.food.DelicaciesDataMaps;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesEffects;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.teller.Meal;
import io.github.dirnei.mc.servus.delicacies.teller.MealKind;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** Compost chances (seeds like Wheat Seeds, vegetables like a Carrot) and what each Delicacies food restores. */
final class ServusDelicaciesDataMapProvider extends DataMapProvider {

    private static final float SEEDS = 0.3F;
    private static final float VEGETABLE = 0.65F;

    ServusDelicaciesDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        Builder<Compostable, Item> compostables = builder(NeoForgeDataMaps.COMPOSTABLES);
        for (CropKind crop : CropKind.values()) {
            compostables.add(ServusDelicaciesItems.PLANTING.get(crop), new Compostable(crop.seedId().isPresent() ? SEEDS : VEGETABLE), false);
            if (!crop.selfPlanting()) {
                compostables.add(ServusDelicaciesItems.HARVEST.get(crop), new Compostable(VEGETABLE), false);
            }
        }
        Builder<FoodProperties, Item> food = builder(DelicaciesDataMaps.FOOD);
        food.add(ServusDelicaciesItems.PRETZEL, new FoodProperties(6, 7.2F, false), false);
        food.add(ServusDelicaciesItems.BREAD_ROLL, new FoodProperties(4, 4.8F, false), false);
        food.add(ServusDelicaciesItems.BOILED_POTATOES, new FoodProperties(5, 6.0F, false), false);
        food.add(ServusDelicaciesItems.FRIED_EGG, new FoodProperties(4, 4.8F, false), false);
        food.add(ServusDelicaciesItems.FRIED_ONIONS, new FoodProperties(3, 3.6F, false), false);
        food.add(ServusDelicaciesItems.FRIED_POTATOES, new FoodProperties(8, 12.8F, false), false);
        food.add(ServusDelicaciesItems.BOWL_OF_SAUERKRAUT, new FoodProperties(4, 4.8F, false), false);
        food.add(ServusDelicaciesItems.BOWL_OF_RED_CABBAGE, new FoodProperties(5, 6.0F, false), false);
        food.add(ServusDelicaciesItems.POTATO_SALAD, new FoodProperties(6, 7.2F, false), false);
        food.add(ServusDelicaciesItems.BREAD_DUMPLING, new FoodProperties(6, 7.2F, false), false);
        food.add(ServusDelicaciesItems.POTATO_DUMPLING, new FoodProperties(5, 6.0F, false), false);
        food.add(ServusDelicaciesItems.CHEESE, new FoodProperties(3, 3.6F, false), false);
        meatDishes(food);
        meals(builder(DelicaciesDataMaps.MEAL));
    }

    /**
     * The plated meals: four portions give a little more than their parts eaten alone, and the last one Properly Full
     * (saturation is the nutrition times 1.6, as the meat dishes).
     */
    private static void meals(Builder<Meal, Item> meals) {
        meals.add(ServusDelicaciesItems.MEALS.get(MealKind.ROAST_PORK_PLATE), meal(8, 1200), false);
        meals.add(ServusDelicaciesItems.MEALS.get(MealKind.SCHNITZEL_PLATE), meal(5, 900), false);
        meals.add(ServusDelicaciesItems.MEALS.get(MealKind.WEISSWURST_BREAKFAST), meal(6, 900), false);
        meals.add(ServusDelicaciesItems.MEALS.get(MealKind.WEISSWURST_BREAKFAST_WITH_WEISSBIER), meal(6, 900), false);
    }

    private static Meal meal(int nutrition, int properlyFullTicks) {
        return new Meal(new FoodProperties(nutrition, nutrition * 1.6F, false),
                List.of(new MobEffectInstance(ServusDelicaciesEffects.PROPERLY_FULL, properlyFullTicks)));
    }

    /** The meat dishes; the finished ones restore more than a vanilla Cooked Porkchop (8 and 12.8). */
    private static void meatDishes(Builder<FoodProperties, Item> food) {
        food.add(ServusDelicaciesItems.SCHNITZEL, new FoodProperties(10, 16.0F, false), false);
        food.add(ServusDelicaciesItems.CORDON_BLEU, new FoodProperties(12, 19.2F, false), false);
        food.add(ServusDelicaciesItems.ROAST_PORK, new FoodProperties(10, 16.0F, false), false);
        food.add(ServusDelicaciesItems.LEBERKAESE_ROLL, new FoodProperties(11, 17.6F, false), false);
        food.add(ServusDelicaciesItems.WEISSWURST, new FoodProperties(7, 11.2F, false), false);
        food.add(ServusDelicaciesItems.LEBERKAESE_SLICE, new FoodProperties(6, 9.6F, false), false);
        food.add(ServusDelicaciesItems.HAM, new FoodProperties(4, 4.8F, false), false);
        food.add(ServusDelicaciesItems.GRAVY, new FoodProperties(3, 3.6F, false), false);
    }
}
