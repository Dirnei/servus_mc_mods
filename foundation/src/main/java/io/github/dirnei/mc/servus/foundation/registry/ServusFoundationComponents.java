package io.github.dirnei.mc.servus.foundation.registry;

import com.mojang.serialization.Codec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components of the Foundation tools. */
public final class ServusFoundationComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusFoundationIds.MOD_ID);

    /** The side layer the Configuration Carrot configures, by name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARROT_LAYER =
            COMPONENTS.registerComponentType("carrot_layer", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ServusFoundationComponents() {
    }
}
