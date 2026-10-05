package io.github.dirnei.mc.servus.logistics.registry;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.ChannelItem;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusLogisticsIds.MOD_ID);

    public static final DeferredItem<ChannelItem> ITEM_CHANNEL = ITEMS.registerItem("item_channel",
            properties -> new ChannelItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Channel.ITEM, properties));
    public static final DeferredItem<ChannelItem> ENERGY_CHANNEL = ITEMS.registerItem("energy_channel",
            properties -> new ChannelItem(ServusLogisticsBlocks.UTILITY_DUCT.get(), Channel.ENERGY, properties));

    /** Items shown in the Servus creative tab, in order. */
    public static final List<DeferredItem<? extends Item>> CREATIVE_TAB_ORDER = List.of(ITEM_CHANNEL, ENERGY_CHANNEL);

    private ServusLogisticsItems() {
    }

    public static Item channelItem(Channel channel) {
        return channel == Channel.ITEM ? ITEM_CHANNEL.get() : ENERGY_CHANNEL.get();
    }
}
