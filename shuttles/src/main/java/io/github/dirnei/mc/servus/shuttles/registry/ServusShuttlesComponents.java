package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.pattern.StoredPatterns;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.handheld.BasketEntry;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.ShelfBasket;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
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

    /** The handheld's mode; missing means ZONE. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HandheldMode>> HANDHELD_MODE =
            COMPONENTS.registerComponentType("handheld_mode", builder -> builder
                    .persistent(HandheldMode.CODEC)
                    .networkSynchronized(HandheldMode.STREAM_CODEC));

    /** The handheld's active zone id, 0 for "No zone"; missing means none was chosen. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HANDHELD_ZONE =
            COMPONENTS.registerComponentType("handheld_zone", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The warehouse controller the handheld is bound to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HandheldBinding>> HANDHELD_BINDING =
            COMPONENTS.registerComponentType("handheld_binding", builder -> builder
                    .persistent(HandheldBinding.CODEC)
                    .networkSynchronized(HandheldBinding.STREAM_CODEC));

    /** The bay the handheld keeps showing in INFO. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> HANDHELD_PIN =
            COMPONENTS.registerComponentType("handheld_pin", builder -> builder
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    /** The basket of the scanned shelf the FIND list shows; missing means empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShelfBasket>> HANDHELD_SHELF_BASKET =
            COMPONENTS.registerComponentType("handheld_shelf_basket", builder -> builder
                    .persistent(ShelfBasket.CODEC)
                    .networkSynchronized(ShelfBasket.STREAM_CODEC));

    /** The relocation the handheld carries in pick target mode: its source shelf and entries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShelfBasket>> HANDHELD_RELOCATION =
            COMPONENTS.registerComponentType("handheld_relocation", builder -> builder
                    .persistent(ShelfBasket.CODEC)
                    .networkSynchronized(ShelfBasket.STREAM_CODEC));

    /** The handheld's warehouse basket, shown while FIND lists the whole warehouse; missing means empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BasketEntry>>> HANDHELD_BASKET =
            COMPONENTS.registerComponentType("handheld_basket", builder -> builder
                    .persistent(BasketEntry.LIST_CODEC)
                    .networkSynchronized(BasketEntry.LIST_STREAM_CODEC));

    /** Set while the handheld waits for the target shelf of a relocation; missing means not waiting. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HANDHELD_PICK_TARGET =
            COMPONENTS.registerComponentType("handheld_pick_target", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /** The planks block a Heavy Duty Rack was crafted from; missing means oak. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> RACK_WOOD =
            COMPONENTS.registerComponentType("rack_wood", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    /** The share of its shelf's fluid a Fluid Rack bay held, carried by its dropped item. */
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
