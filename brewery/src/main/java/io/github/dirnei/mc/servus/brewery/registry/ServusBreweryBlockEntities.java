package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseBlockEntity;
import io.github.dirnei.mc.servus.brewery.malting.MaltingFloorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The block entity types of Servus: Brewery. */
public final class ServusBreweryBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusBreweryIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaltingFloorBlockEntity>> MALTING_FLOOR =
            BLOCK_ENTITIES.register("malting_floor",
                    () -> new BlockEntityType<>(MaltingFloorBlockEntity::new, ServusBreweryBlocks.MALTING_FLOOR.get()));

    /** Every part of the Brewhouse, bricks and kettle blocks. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrewhouseBlockEntity>> BREWHOUSE =
            BLOCK_ENTITIES.register("brewhouse", () -> new BlockEntityType<>(BrewhouseBlockEntity::new,
                    ServusBreweryBlocks.BREWHOUSE_BRICKS.get(), ServusBreweryBlocks.BREW_KETTLE.get()));

    private ServusBreweryBlockEntities() {
    }
}
