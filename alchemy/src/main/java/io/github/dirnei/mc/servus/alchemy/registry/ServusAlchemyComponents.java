package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components of Servus: Alchemy. */
public final class ServusAlchemyComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ServusAlchemyIds.MOD_ID);

    /** The vein a Seed plants, by its id in the vein data. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> VEIN =
            COMPONENTS.registerComponentType("vein", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    private ServusAlchemyComponents() {
    }
}
