package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.zone.ZoneFilter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusStorageIds.MOD_ID);

    /** The filter a Zone Label stamps onto blocks; its name is the vanilla custom name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ZoneFilter>> ZONE_FILTER =
            COMPONENTS.registerComponentType("zone_filter", builder -> builder
                    .persistent(ZoneFilter.CODEC)
                    .networkSynchronized(ZoneFilter.STREAM_CODEC));

    private ServusStorageComponents() {
    }
}
