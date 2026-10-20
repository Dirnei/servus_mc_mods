package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingOvenBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PanBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The block entity types of Servus: Delicacies. */
public final class ServusDelicaciesBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusDelicaciesIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoveTileBlockEntity>> STOVE_TILE =
            register("stove_tile", StoveTileBlockEntity::new, ServusDelicaciesBlocks.STOVE_TILE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PotBlockEntity>> POT =
            register("pot", PotBlockEntity::new, ServusDelicaciesBlocks.POT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PanBlockEntity>> PAN =
            register("pan", PanBlockEntity::new, ServusDelicaciesBlocks.PAN);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BakingOvenBlockEntity>> BAKING_OVEN =
            register("baking_oven", BakingOvenBlockEntity::new, ServusDelicaciesBlocks.BAKING_OVEN);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD =
            register("cutting_board", CuttingBoardBlockEntity::new, ServusDelicaciesBlocks.CUTTING_BOARD);

    private ServusDelicaciesBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name,
            BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, block.get()));
    }
}
