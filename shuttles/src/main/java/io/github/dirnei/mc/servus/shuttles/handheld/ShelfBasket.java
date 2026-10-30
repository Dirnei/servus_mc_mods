package io.github.dirnei.mc.servus.shuttles.handheld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Basket entries that belong to one rack shelf: the shelf basket of the INFO
 * screen, or a stashed relocation that takes its items from that shelf.
 *
 * @param shelf   the shelf's key, its leftmost bay
 * @param entries the entries as the handheld stores them
 */
public record ShelfBasket(BlockPos shelf, List<BasketEntry> entries) {

    public static final Codec<ShelfBasket> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("shelf").forGetter(ShelfBasket::shelf),
            BasketEntry.LIST_CODEC.fieldOf("entries").forGetter(ShelfBasket::entries))
            .apply(i, ShelfBasket::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShelfBasket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShelfBasket::shelf,
            BasketEntry.LIST_STREAM_CODEC, ShelfBasket::entries,
            ShelfBasket::new);

    public ShelfBasket {
        shelf = shelf.immutable();
        entries = List.copyOf(entries);
    }

    public static ShelfBasket of(BlockPos shelf, Basket<ItemKey> basket) {
        return new ShelfBasket(shelf, BasketEntry.of(basket));
    }

    public Basket<ItemKey> basket() {
        return BasketEntry.toBasket(entries);
    }
}
