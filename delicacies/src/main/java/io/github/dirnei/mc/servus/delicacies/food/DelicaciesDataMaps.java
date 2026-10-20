package io.github.dirnei.mc.servus.delicacies.food;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.teller.Meal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/** The data maps of Servus: Delicacies. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID)
public final class DelicaciesDataMaps {

    /**
     * What a Delicacies food restores, from {@code data/<ns>/data_maps/item/food.json}: {@code nutrition},
     * {@code saturation} and optionally {@code can_always_eat}. Synced to clients.
     */
    public static final DataMapType<Item, FoodProperties> FOOD = DataMapType
            .builder(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "food"), Registries.ITEM, FoodProperties.DIRECT_CODEC)
            .synced(FoodProperties.DIRECT_CODEC, false)
            .build();

    /**
     * What a plated meal gives, from {@code data/<ns>/data_maps/item/meal.json}: the food of one {@code portion} and the
     * {@code effects} after the last portion. Synced to clients.
     */
    public static final DataMapType<Item, Meal> MEAL = DataMapType
            .builder(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "meal"), Registries.ITEM, Meal.CODEC)
            .synced(Meal.CODEC, false)
            .build();

    private DelicaciesDataMaps() {
    }

    /** The food values of {@code stack}, or null when the data gives it none. */
    public static @Nullable FoodProperties food(ItemStack stack) {
        return stack.typeHolder().getData(FOOD);
    }

    /** What the meal {@code item} gives, or null when the data gives it nothing. */
    public static @Nullable Meal meal(Item item) {
        return item.builtInRegistryHolder().getData(MEAL);
    }

    @SubscribeEvent
    public static void onRegisterDataMaps(RegisterDataMapTypesEvent event) {
        event.register(FOOD);
        event.register(MEAL);
    }
}
