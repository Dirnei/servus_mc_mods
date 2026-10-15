package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.salamander.Salamander;
import io.github.dirnei.mc.servus.alchemy.salamander.SalamanderPlacement;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Alchemy's entities, their attributes and where they may spawn. Which biomes they spawn in is a biome modifier in data. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class ServusAlchemyEntities {

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ServusAlchemyIds.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Salamander>> SALAMANDER = ENTITIES.registerEntityType("salamander",
            Salamander::new, MobCategory.CREATURE, builder -> builder.sized(0.6F, 0.5F).fireImmune().clientTrackingRange(10));

    private ServusAlchemyEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SALAMANDER.get(), Salamander.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SALAMANDER.get(), SalamanderPlacement.INSTANCE, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Salamander::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
