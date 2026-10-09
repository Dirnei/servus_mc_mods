package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlock;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.CoalGeneratorMenu;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

/** The machines Foundation owns: blocks, items, block entities and menus. Registered from the Foundation entrypoint. */
public final class MachineContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusFoundationIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ServusFoundationIds.MOD_ID);

    public static final DeferredBlock<CoalGeneratorBlock> COAL_GENERATOR =
            BLOCKS.registerBlock("coal_generator", CoalGeneratorBlock::new, machine());
    public static final DeferredItem<BlockItem> COAL_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(COAL_GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_ENTITY =
            BLOCK_ENTITIES.register("coal_generator", () -> BlockEntityType.Builder
                    .of(CoalGeneratorBlockEntity::new, COAL_GENERATOR.get())
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            MENUS.register("coal_generator", () -> IMenuTypeExtension.create(CoalGeneratorMenu::fromNetwork));

    private MachineContent() {
    }

    /** Creative tab order of Foundation's machines. */
    public static List<DeferredItem<? extends Item>> creativeTabOrder() {
        return List.of(COAL_GENERATOR_ITEM);
    }

    /** Block properties every Servus machine shares: metal, lit front glows. */
    public static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0);
    }

    public static void register(IEventBus modEventBus) {
        FoundationMachines.registerDefaults();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
