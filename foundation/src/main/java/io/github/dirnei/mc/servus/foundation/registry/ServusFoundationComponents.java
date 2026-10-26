package io.github.dirnei.mc.servus.foundation.registry;

import com.mojang.serialization.Codec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components of the Foundation tools and of named machines. */
public final class ServusFoundationComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusFoundationIds.MOD_ID);

    /** The side layer the Configuration Carrot configures, by name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARROT_LAYER =
            COMPONENTS.registerComponentType("carrot_layer", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The number a dismantled named machine keeps; missing means not numbered. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MACHINE_NUMBER =
            COMPONENTS.registerComponentType("machine_number", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The name a dismantled named machine keeps; missing means unnamed. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MACHINE_NAME =
            COMPONENTS.registerComponentType("machine_name", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The warehouse that numbered a dismantled named machine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> MACHINE_WAREHOUSE =
            COMPONENTS.registerComponentType("machine_warehouse", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    private ServusFoundationComponents() {
    }
}
