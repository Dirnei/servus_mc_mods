package io.github.dirnei.mc.servus.manufacturing.journal;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The journal's Modonomicon book id and the journal every player gets once. */
public final class JournalBookItem {

    public static final Identifier BOOK = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal");
    private static final String GIVEN = "servus_manufacturing.journal_given";

    private JournalBookItem() {
    }

    /** A fresh journal. */
    public static ItemStack create() {
        return new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get());
    }

    /** Gives the journal once per player, the first time they log in. */
    public static void giveOnce(ServerPlayer player) {
        if (player.getPersistentData().getBooleanOr(GIVEN, false)) {
            return;
        }
        player.getPersistentData().putBoolean(GIVEN, true);
        ItemStack journal = create();
        if (!player.getInventory().add(journal)) {
            player.drop(journal, false);
        }
    }
}
