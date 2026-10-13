package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusManufacturingIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalcinerBlockEntity>> CALCINER =
            register("calciner", CalcinerBlockEntity::new, ServusManufacturingBlocks.CALCINER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoagulatorBlockEntity>> COAGULATOR =
            register("coagulator", CoagulatorBlockEntity::new, ServusManufacturingBlocks.COAGULATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AthanorBlockEntity>> ATHANOR =
            register("athanor", AthanorBlockEntity::new, ServusManufacturingBlocks.ATHANOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GnomeHollowBlockEntity>> GNOME_HOLLOW =
            register("gnome_hollow", GnomeHollowBlockEntity::new, ServusManufacturingBlocks.GNOME_HOLLOW);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurifierBlockEntity>> PURIFIER =
            register("purifier", PurifierBlockEntity::new, ServusManufacturingBlocks.PURIFIER);

    private ServusManufacturingBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name,
            BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, block.get()));
    }
}
