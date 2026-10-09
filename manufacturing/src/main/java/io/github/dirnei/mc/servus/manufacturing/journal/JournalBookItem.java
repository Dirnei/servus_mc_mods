package io.github.dirnei.mc.servus.manufacturing.journal;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The journal as an item: Modonomicon's book item with its {@code modonomicon:book_id} component set to
 * the journal. Looked up through the registries, so only Modonomicon's ids are relied on.
 */
public final class JournalBookItem {

    public static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal");
    private static final ResourceLocation ITEM = ResourceLocation.fromNamespaceAndPath("modonomicon", "modonomicon");
    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("modonomicon", "book_id");
    private static final String GIVEN = "servus_manufacturing.journal_given";

    private JournalBookItem() {
    }

    @SuppressWarnings("unchecked")
    public static ItemStack create() {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ITEM));
        DataComponentType<ResourceLocation> bookId = (DataComponentType<ResourceLocation>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(BOOK_ID);
        if (bookId != null) {
            stack.set(bookId, BOOK);
        }
        return stack;
    }

    /** Gives the journal once per player, the first time they log in. */
    public static void giveOnce(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(GIVEN)) {
            return;
        }
        player.getPersistentData().putBoolean(GIVEN, true);
        ItemStack journal = create();
        if (!player.getInventory().add(journal)) {
            player.drop(journal, false);
        }
    }
}
