package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ClientChannels;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** How a channel shows in the GUI: its name, icon and colour swatch. */
final class ChannelDisplay {

    private static final int SWATCH_BORDER = 0xFF202020;

    private ChannelDisplay() {
    }

    /** The channel with that id from the synced list; unknown ids show Default. */
    static Channel channel(int id) {
        Channel channel = ClientChannels.list().get(id);
        return channel == null ? Channel.DEFAULT : channel;
    }

    static Component name(Channel channel) {
        return channel.isDefault() ? Component.translatable("gui.servus_foundation.channel.default") : Component.literal(channel.name());
    }

    /** The icon stack, empty without icon or for an unknown item id. */
    static ItemStack icon(Channel channel) {
        return iconOf(channel.icon());
    }

    static ItemStack iconOf(String itemId) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(itemId);
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** A square of the colour with a dark frame; {@code x, y, size} is the inside, absolute. */
    static void swatch(GuiGraphicsExtractor graphics, int x, int y, int size, int argb) {
        PopupPanel.bordered(graphics, x, y, size, size, SWATCH_BORDER, argb);
    }
}
