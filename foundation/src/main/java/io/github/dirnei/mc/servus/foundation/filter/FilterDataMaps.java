package io.github.dirnei.mc.servus.foundation.filter;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/** The Foundation data map that makes items filters. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class FilterDataMaps {

    /** Which items are filters, from {@code data/<ns>/data_maps/item/filter.json}. Synced to clients for the bar and tooltip. */
    public static final DataMapType<Item, FilterSpec> FILTER = DataMapType
            .builder(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "filter"), Registries.ITEM, FilterSpec.CODEC)
            .synced(FilterSpec.CODEC, false)
            .build();

    private FilterDataMaps() {
    }

    /** The filter definition of {@code stack}'s item, or null when it is no filter. */
    public static @Nullable FilterSpec spec(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(FILTER);
    }

    @SubscribeEvent
    public static void onRegisterDataMaps(RegisterDataMapTypesEvent event) {
        event.register(FILTER);
    }
}
