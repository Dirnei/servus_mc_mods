package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.pattern.StoredPatterns;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryContents;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
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

    /** The share of its section's fluid a Fluid Rack bay held, carried by its dropped item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> RACK_FLUID =
            COMPONENTS.registerComponentType("rack_fluid", builder -> builder
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

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

    /** The world's number of a shuttle, given in its first garage; missing means not numbered yet. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHUTTLE_NUMBER =
            COMPONENTS.registerComponentType("shuttle_number", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The tanks, locks, mode and Capacity Upgrades a dropped Fluid Gantry keeps. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GantryContents>> GANTRY_CONTENTS =
            COMPONENTS.registerComponentType("gantry_contents", builder -> builder
                    .persistent(GantryContents.CODEC)
                    .networkSynchronized(GantryContents.STREAM_CODEC));

    /** The site name a dropped Warehouse Controller keeps; missing means unnamed. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SITE_NAME =
            COMPONENTS.registerComponentType("site_name", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The FE a dropped Warehouse Controller keeps; missing means none. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CONTROLLER_ENERGY =
            COMPONENTS.registerComponentType("controller_energy", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The identity of the warehouse a dropped Warehouse Controller keeps; missing means a new warehouse. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> WAREHOUSE_ID =
            COMPONENTS.registerComponentType("warehouse_id", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** The last machine number per kind a dropped Warehouse Controller keeps; missing means counting from 1. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<String, Integer>>> MACHINE_COUNTERS =
            COMPONENTS.registerComponentType("machine_counters", builder -> builder
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .networkSynchronized(ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)));

    private ServusShuttlesComponents() {
    }
}
