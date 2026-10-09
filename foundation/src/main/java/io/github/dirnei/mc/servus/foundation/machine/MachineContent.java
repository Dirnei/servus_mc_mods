package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlock;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.BurnerPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.CoalGeneratorMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.ElectricPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpBlockEntity;
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

    public static final DeferredBlock<BurnerPumpBlock> BURNER_PUMP =
            BLOCKS.registerBlock("burner_pump", BurnerPumpBlock::new, machine().noOcclusion());
    public static final DeferredItem<BlockItem> BURNER_PUMP_ITEM = ITEMS.registerSimpleBlockItem(BURNER_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerPumpBlockEntity>> BURNER_PUMP_ENTITY =
            BLOCK_ENTITIES.register("burner_pump", () -> BlockEntityType.Builder
                    .of(BurnerPumpBlockEntity::new, BURNER_PUMP.get())
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BurnerPumpMenu>> BURNER_PUMP_MENU =
            MENUS.register("burner_pump", () -> IMenuTypeExtension.create(BurnerPumpMenu::fromNetwork));

    public static final DeferredBlock<ElectricPumpBlock> ELECTRIC_PUMP =
            BLOCKS.registerBlock("electric_pump", ElectricPumpBlock::new, machine().noOcclusion());
    public static final DeferredItem<BlockItem> ELECTRIC_PUMP_ITEM = ITEMS.registerSimpleBlockItem(ELECTRIC_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP_ENTITY =
            BLOCK_ENTITIES.register("electric_pump", () -> BlockEntityType.Builder
                    .of(ElectricPumpBlockEntity::new, ELECTRIC_PUMP.get())
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricPumpMenu>> ELECTRIC_PUMP_MENU =
            MENUS.register("electric_pump", () -> IMenuTypeExtension.create(ElectricPumpMenu::fromNetwork));

    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink", SinkBlock::new, brick().noOcclusion());
    public static final DeferredItem<BlockItem> SINK_ITEM = ITEMS.registerSimpleBlockItem(SINK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SinkBlockEntity>> SINK_ENTITY =
            BLOCK_ENTITIES.register("sink", () -> BlockEntityType.Builder
                    .of(SinkBlockEntity::new, SINK.get())
                    .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SinkMenu>> SINK_MENU =
            MENUS.register("sink", () -> IMenuTypeExtension.create(SinkMenu::fromNetwork));
    public static final DeferredBlock<ChimneyBlock> CHIMNEY = BLOCKS.registerBlock("chimney", ChimneyBlock::new, brick().noOcclusion());
    public static final DeferredItem<BlockItem> CHIMNEY_ITEM = ITEMS.registerSimpleBlockItem(CHIMNEY);

    private MachineContent() {
    }

    /** Creative tab order of Foundation's machines. */
    public static List<DeferredItem<? extends Item>> creativeTabOrder() {
        return List.of(BURNER_PUMP_ITEM, ELECTRIC_PUMP_ITEM, COAL_GENERATOR_ITEM, SINK_ITEM, CHIMNEY_ITEM);
    }

    /** Brick blocks of the Sink family: like vanilla bricks. */
    private static BlockBehaviour.Properties brick() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(2.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
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
