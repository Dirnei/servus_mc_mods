package io.github.dirnei.mc.servus.notes.client;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Turns the item id a goal saves into the item, its icon and its name on the client; a missing item reads as unknown. */
public final class GoalItems {

    private GoalItems() {
    }

    /** The item named by {@code itemId}, empty when no such item exists (a removed mod, a broken id, air). */
    public static Optional<Item> item(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR);
    }

    /** The icon to draw: the item, or a barrier for an unknown item. */
    public static ItemStack icon(String itemId) {
        return item(itemId).map(Item::getDefaultInstance).orElseGet(() -> new ItemStack(Items.BARRIER));
    }

    /** The item's display name, or the raw id in grey marked as unknown. */
    public static Component name(String itemId) {
        return item(itemId).map(item -> item.getDefaultInstance().getHoverName())
                .orElseGet(() -> Component.translatable("gui.servus_notes.notebook.goal.unknown", itemId).withStyle(ChatFormatting.GRAY));
    }

    /** The id {@link io.github.dirnei.mc.servus.notes.model.Goal} saves for {@code item}. */
    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
