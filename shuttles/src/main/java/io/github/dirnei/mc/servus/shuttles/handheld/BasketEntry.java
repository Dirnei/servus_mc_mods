package io.github.dirnei.mc.servus.shuttles.handheld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * One entry of the basket as the handheld stores it: the item, kept as a
 * stack of one, and how many of it are wanted.
 */
public record BasketEntry(ItemStack item, int amount) {

    public static final Codec<BasketEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("item").forGetter(BasketEntry::item),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(BasketEntry::amount))
            .apply(i, BasketEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BasketEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, BasketEntry::item,
            ByteBufCodecs.VAR_INT, BasketEntry::amount,
            BasketEntry::new);

    public static final Codec<List<BasketEntry>> LIST_CODEC = CODEC.listOf(0, Basket.MAX_ENTRIES);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<BasketEntry>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.list(Basket.MAX_ENTRIES));

    public BasketEntry {
        item = item.copyWithCount(1);
    }

    /** The stored entries as a basket keyed by item. */
    public static Basket<ItemKey> toBasket(List<BasketEntry> entries) {
        List<Basket.Entry<ItemKey>> list = new ArrayList<>(entries.size());
        for (BasketEntry entry : entries) {
            list.add(new Basket.Entry<>(ItemKey.of(entry.item()), entry.amount()));
        }
        return new Basket<>(list);
    }

    /** A basket as the handheld stores it. */
    public static List<BasketEntry> of(Basket<ItemKey> basket) {
        List<BasketEntry> list = new ArrayList<>(basket.size());
        for (Basket.Entry<ItemKey> entry : basket.entries()) {
            list.add(new BasketEntry(entry.key().stack(1), entry.amount()));
        }
        return List.copyOf(list);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BasketEntry that && amount == that.amount && ItemStack.isSameItemSameComponents(item, that.item);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStack.hashItemAndComponents(item) + amount;
    }
}
