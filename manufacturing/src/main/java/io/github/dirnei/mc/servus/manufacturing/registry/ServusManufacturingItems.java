package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.journal.ParacelsusJournalItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusManufacturingIds.MOD_ID);

    public static final DeferredItem<BlockItem> CALCINER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.CALCINER);
    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.FIREBOX);
    public static final DeferredItem<BlockItem> ASH_PAN = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ASH_PAN);
    public static final DeferredItem<BlockItem> ALEMBIC = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ALEMBIC);
    public static final DeferredItem<BlockItem> RECEIVER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.RECEIVER);
    public static final DeferredItem<BlockItem> SULFUR_HOOD = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.SULFUR_HOOD);
    public static final DeferredItem<BlockItem> SULFUR_DUCT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.SULFUR_DUCT);
    public static final DeferredItem<BlockItem> PURIFIER = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.PURIFIER);
    public static final DeferredItem<BlockItem> COAGULATOR = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.COAGULATOR);
    public static final DeferredItem<BlockItem> VAT = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.VAT);
    public static final DeferredItem<BlockItem> ATHANOR = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.ATHANOR);
    public static final DeferredItem<BlockItem> GNOME_HOLLOW = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.GNOME_HOLLOW);

    public static final DeferredItem<Item> ASH = ITEMS.registerSimpleItem("ash");
    public static final DeferredItem<Item> SALT_CRYSTAL = ITEMS.registerSimpleItem("salt_crystal");
    public static final DeferredItem<BlockItem> SALT_CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK);
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> QUICKSILVER = ITEMS.registerSimpleItem("quicksilver");

    public static final DeferredItem<ParacelsusJournalItem> PARACELSUS_JOURNAL = ITEMS.registerItem("paracelsus_journal",
            ParacelsusJournalItem::new, new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(PARACELSUS_JOURNAL, CALCINER, FIREBOX, ASH_PAN,
            ALEMBIC, RECEIVER, SULFUR_HOOD, SULFUR_DUCT, PURIFIER, COAGULATOR, VAT, ATHANOR, GNOME_HOLLOW,
            ASH, SALT_CRYSTAL, SALT_CRYSTAL_BLOCK, SULFUR, QUICKSILVER);

    private ServusManufacturingItems() {
    }
}
