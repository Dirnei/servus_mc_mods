package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlockEntity;
import io.github.dirnei.mc.servus.alchemy.coagulator.GrownVatBlockEntity;
import io.github.dirnei.mc.servus.alchemy.forge.SalamanderForgeBlockEntity;
import io.github.dirnei.mc.servus.alchemy.prelum.PrelumBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.ReceptaculumBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.PrincipleDropperBlockEntity;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.alchemy.venter.VenterEquinusBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusAlchemyBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusAlchemyIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalcinerBlockEntity>> CALCINER =
            register("calciner", CalcinerBlockEntity::new, ServusAlchemyBlocks.CALCINER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoagulatorBlockEntity>> COAGULATOR =
            register("coagulator", CoagulatorBlockEntity::new, ServusAlchemyBlocks.COAGULATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AthanorBlockEntity>> ATHANOR =
            register("athanor", AthanorBlockEntity::new, ServusAlchemyBlocks.ATHANOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GnomeHollowBlockEntity>> GNOME_HOLLOW =
            register("gnome_hollow", GnomeHollowBlockEntity::new, ServusAlchemyBlocks.GNOME_HOLLOW);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrincipleDropperBlockEntity>> PRINCIPLE_DROPPER =
            register("principle_dropper", PrincipleDropperBlockEntity::new, ServusAlchemyBlocks.PRINCIPLE_DROPPER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurifierBlockEntity>> PURIFIER =
            register("purifier", PurifierBlockEntity::new, ServusAlchemyBlocks.PURIFIER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VenterEquinusBlockEntity>> VENTER_EQUINUS =
            register("venter_equinus", VenterEquinusBlockEntity::new, ServusAlchemyBlocks.CUCURBIT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SalamanderForgeBlockEntity>> SALAMANDER_FORGE =
            register("salamander_forge", SalamanderForgeBlockEntity::new, ServusAlchemyBlocks.SALAMANDER_FORGE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReceptaculumBlockEntity>> RECEPTACULUM =
            register("receptaculum", ReceptaculumBlockEntity::new, ServusAlchemyBlocks.RECEPTACULUM);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrelumBlockEntity>> PRELUM =
            register("prelum", PrelumBlockEntity::new, ServusAlchemyBlocks.PRELUM);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VasBlockEntity>> VAS_HERMETICUM =
            register("vas_hermeticum", VasBlockEntity::new, ServusAlchemyBlocks.VAS_HERMETICUM);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientHollowBlockEntity>> ANCIENT_HOLLOW =
            register("ancient_hollow", AncientHollowBlockEntity::new, ServusAlchemyBlocks.ANCIENT_HOLLOW);

    /** Only grown vats have one; plain Principle Vats get none. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrownVatBlockEntity>> GROWN_VAT =
            register("grown_vat", GrownVatBlockEntity::new, ServusAlchemyBlocks.VAT);

    /** The lower half of a Rubedo Obelisk, which draws the whole obelisk. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubedoObeliskBlockEntity>> RUBEDO_OBELISK =
            register("rubedo_obelisk", RubedoObeliskBlockEntity::new, ServusAlchemyBlocks.RUBEDO_OBELISK);

    private ServusAlchemyBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name,
            BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, block.get()));
    }
}
