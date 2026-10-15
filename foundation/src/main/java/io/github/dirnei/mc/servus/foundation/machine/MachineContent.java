package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlock;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlock;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.BurnerPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.CoalGeneratorMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.ElectricPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpItem;
import io.github.dirnei.mc.servus.foundation.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.upgrade.AnchorUpgradeItem;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
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
            BLOCKS.registerBlock("coal_generator", CoalGeneratorBlock::new,
                    () -> machine().noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> COAL_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(COAL_GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_ENTITY =
            blockEntity("coal_generator", CoalGeneratorBlockEntity::new, COAL_GENERATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            menu("coal_generator", CoalGeneratorMenu::fromNetwork);

    public static final DeferredBlock<BurnerPumpBlock> BURNER_PUMP =
            BLOCKS.registerBlock("burner_pump", BurnerPumpBlock::new,
                    () -> machine().noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<PumpItem> BURNER_PUMP_ITEM = pumpItem("burner_pump", BURNER_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerPumpBlockEntity>> BURNER_PUMP_ENTITY =
            blockEntity("burner_pump", BurnerPumpBlockEntity::new, BURNER_PUMP);
    public static final DeferredHolder<MenuType<?>, MenuType<BurnerPumpMenu>> BURNER_PUMP_MENU =
            menu("burner_pump", BurnerPumpMenu::fromNetwork);

    public static final DeferredBlock<ElectricPumpBlock> ELECTRIC_PUMP =
            BLOCKS.registerBlock("electric_pump", ElectricPumpBlock::new,
                    () -> machine().noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<PumpItem> ELECTRIC_PUMP_ITEM = pumpItem("electric_pump", ELECTRIC_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP_ENTITY =
            blockEntity("electric_pump", ElectricPumpBlockEntity::new, ELECTRIC_PUMP);
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricPumpMenu>> ELECTRIC_PUMP_MENU =
            menu("electric_pump", ElectricPumpMenu::fromNetwork);

    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink", SinkBlock::new, () -> brick().noOcclusion());
    public static final DeferredItem<BlockItem> SINK_ITEM = ITEMS.registerSimpleBlockItem(SINK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SinkBlockEntity>> SINK_ENTITY =
            blockEntity("sink", SinkBlockEntity::new, SINK);
    public static final DeferredHolder<MenuType<?>, MenuType<SinkMenu>> SINK_MENU = menu("sink", SinkMenu::fromNetwork);
    public static final DeferredBlock<ChimneyBlock> CHIMNEY = BLOCKS.registerBlock("chimney", ChimneyBlock::new, () -> brick().noOcclusion());
    public static final DeferredItem<BlockItem> CHIMNEY_ITEM = ITEMS.registerSimpleBlockItem(CHIMNEY);

    public static final DeferredBlock<EnderAnchorBlock> ENDER_ANCHOR =
            BLOCKS.registerBlock("ender_anchor", EnderAnchorBlock::new, () -> machine());
    public static final DeferredItem<BlockItem> ENDER_ANCHOR_ITEM = ITEMS.registerSimpleBlockItem(ENDER_ANCHOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderAnchorBlockEntity>> ENDER_ANCHOR_ENTITY =
            blockEntity("ender_anchor", EnderAnchorBlockEntity::new, ENDER_ANCHOR);
    public static final DeferredHolder<MenuType<?>, MenuType<EnderAnchorMenu>> ENDER_ANCHOR_MENU =
            menu("ender_anchor", EnderAnchorMenu::fromNetwork);
    /** Widens an Ender Anchor's area and lets a Shuttles warehouse site wake. */
    public static final DeferredItem<AnchorUpgradeItem> ANCHOR_UPGRADE =
            ITEMS.registerItem("anchor_upgrade", AnchorUpgradeItem::new, () -> new Item.Properties().stacksTo(16));

    private MachineContent() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> blockEntity(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, DeferredBlock<?> block) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, block.get()));
    }

    private static <M extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<M>> menu(
            String name, IContainerFactory<M> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    private static DeferredItem<PumpItem> pumpItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.registerItem(name, properties -> new PumpItem(block.get(), properties), Item.Properties::useBlockDescriptionPrefix);
    }

    /** Creative tab order of Foundation's machines. */
    public static List<DeferredItem<? extends Item>> creativeTabOrder() {
        return List.of(BURNER_PUMP_ITEM, ELECTRIC_PUMP_ITEM, COAL_GENERATOR_ITEM, ENDER_ANCHOR_ITEM, ANCHOR_UPGRADE, SINK_ITEM, CHIMNEY_ITEM);
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
