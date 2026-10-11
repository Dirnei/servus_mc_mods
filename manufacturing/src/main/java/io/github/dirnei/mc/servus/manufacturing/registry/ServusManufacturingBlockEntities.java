package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusManufacturingIds.MOD_ID);


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalcinerBlockEntity>> CALCINER =
            BLOCK_ENTITIES.register("calciner", () -> new BlockEntityType<>(CalcinerBlockEntity::new, ServusManufacturingBlocks.CALCINER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoagulatorBlockEntity>> COAGULATOR =
            BLOCK_ENTITIES.register("coagulator", () -> new BlockEntityType<>(CoagulatorBlockEntity::new, ServusManufacturingBlocks.COAGULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AthanorBlockEntity>> ATHANOR =
            BLOCK_ENTITIES.register("athanor", () -> new BlockEntityType<>(AthanorBlockEntity::new, ServusManufacturingBlocks.ATHANOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GnomeHollowBlockEntity>> GNOME_HOLLOW =
            BLOCK_ENTITIES.register("gnome_hollow", () -> new BlockEntityType<>(GnomeHollowBlockEntity::new, ServusManufacturingBlocks.GNOME_HOLLOW.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurifierBlockEntity>> PURIFIER =
            BLOCK_ENTITIES.register("purifier", () -> new BlockEntityType<>(PurifierBlockEntity::new, ServusManufacturingBlocks.PURIFIER.get()));

    private ServusManufacturingBlockEntities() {
    }
}
