package io.github.dirnei.mc.servus.logistics.registry;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusLogisticsIds.MOD_ID);

    /** The block a facade looks like. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Block>> FACADE_BLOCK =
            COMPONENTS.registerComponentType("facade_block", builder -> builder
                    .persistent(BuiltInRegistries.BLOCK.byNameCodec())
                    .networkSynchronized(ByteBufCodecs.registry(Registries.BLOCK)));

    private ServusLogisticsComponents() {
    }
}
