package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.pattern.StoredPatterns;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusShuttlesIds.MOD_ID);

    /** The filter a Zone Label stamps onto blocks; its name is the vanilla custom name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ZoneFilter>> ZONE_FILTER =
            COMPONENTS.registerComponentType("zone_filter", builder -> builder
                    .persistent(ZoneFilter.CODEC)
                    .networkSynchronized(ZoneFilter.STREAM_CODEC));

    /** The planks block a Heavy Duty Rack was crafted from; missing means oak. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> RACK_WOOD =
            COMPONENTS.registerComponentType("rack_wood", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    /** The patterns a Pattern Storage keeps, carried by its dropped item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredPatterns>> PATTERNS =
            COMPONENTS.registerComponentType("patterns", builder -> builder
                    .persistent(StoredPatterns.CODEC)
                    .networkSynchronized(StoredPatterns.STREAM_CODEC));

    /** The modules fitted into a Shuttle item; missing means a plain shuttle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShuttleModules>> SHUTTLE_MODULES =
            COMPONENTS.registerComponentType("shuttle_modules", builder -> builder
                    .persistent(ShuttleModules.CODEC)
                    .networkSynchronized(ShuttleModules.STREAM_CODEC));

    /** The name plate a build by hand engraved on a shuttle; only tier builds write it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SHUTTLE_NAME =
            COMPONENTS.registerComponentType("shuttle_name", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ServusShuttlesComponents() {
    }
}
