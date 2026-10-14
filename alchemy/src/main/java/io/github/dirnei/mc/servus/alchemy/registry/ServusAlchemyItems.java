package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.journal.ParacelsusJournalItem;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusAlchemyItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusAlchemyIds.MOD_ID);

    public static final DeferredItem<BlockItem> CALCINER = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.CALCINER);
    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.FIREBOX);
    public static final DeferredItem<BlockItem> ASH_PAN = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.ASH_PAN);
    public static final DeferredItem<BlockItem> ALEMBIC = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.ALEMBIC);
    public static final DeferredItem<BlockItem> RECEIVER = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.RECEIVER);
    public static final DeferredItem<BlockItem> SULFUR_HOOD = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.SULFUR_HOOD);
    public static final DeferredItem<BlockItem> COPPER_DUCT = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.COPPER_DUCT);
    public static final DeferredItem<BlockItem> PURIFIER = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.PURIFIER);
    public static final DeferredItem<BlockItem> CATCH_BASIN = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.CATCH_BASIN);
    public static final DeferredItem<BlockItem> RESIDUE_CROCK = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.RESIDUE_CROCK);
    public static final DeferredItem<BlockItem> COAGULATOR = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.COAGULATOR);
    public static final DeferredItem<BlockItem> VAT = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.VAT);
    public static final DeferredItem<BlockItem> ATHANOR = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.ATHANOR);
    public static final DeferredItem<BlockItem> GNOME_HOLLOW = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.GNOME_HOLLOW);

    public static final DeferredItem<Item> ASH = ITEMS.registerSimpleItem("ash");
    /** What stays behind when the Purifier has washed the Salt out of the Ash. */
    public static final DeferredItem<Item> CAPUT_MORTUUM = ITEMS.registerSimpleItem("caput_mortuum");
    public static final DeferredItem<Item> SALT_CRYSTAL = ITEMS.registerSimpleItem("salt_crystal");
    public static final DeferredItem<BlockItem> SALT_CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK);
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> QUICKSILVER = ITEMS.registerSimpleItem("quicksilver");

    public static final DeferredItem<ParacelsusJournalItem> PARACELSUS_JOURNAL = ITEMS.registerItem("paracelsus_journal",
            ParacelsusJournalItem::new, () -> new Item.Properties().stacksTo(1));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(PARACELSUS_JOURNAL, CALCINER, FIREBOX, ASH_PAN,
            ALEMBIC, RECEIVER, SULFUR_HOOD, COPPER_DUCT, PURIFIER, CATCH_BASIN, RESIDUE_CROCK, COAGULATOR, VAT, ATHANOR, GNOME_HOLLOW,
            ASH, CAPUT_MORTUUM, SALT_CRYSTAL, SALT_CRYSTAL_BLOCK, SULFUR, QUICKSILVER);

    private ServusAlchemyItems() {
    }
}
