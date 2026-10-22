package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import net.minecraft.world.item.Item;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Machine Adapter's block, item, block entity and menu. Registered from the Foundation entrypoint. */
public final class AdapterContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusFoundationIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusFoundationIds.MOD_ID);

    public static final DeferredBlock<MachineAdapterBlock> BLOCK = BLOCKS.registerBlock("machine_adapter", MachineAdapterBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<MaterialBlockItem> ITEM = ITEMS.registerItem("machine_adapter",
            properties -> new MaterialBlockItem(BLOCK.get(), properties),
            Item.Properties::useBlockDescriptionPrefix);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineAdapterBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("machine_adapter", () -> new BlockEntityType<>(MachineAdapterBlockEntity::new, BLOCK.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<AdapterMenu>> MENU =
            MENUS.register("machine_adapter", () -> IMenuTypeExtension.create(AdapterMenu::fromNetwork));

    private AdapterContent() {
    }

    public static void register(IEventBus modEventBus) {
        Material.register(BLOCK.getId(), MaterialKind.WOOD);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
