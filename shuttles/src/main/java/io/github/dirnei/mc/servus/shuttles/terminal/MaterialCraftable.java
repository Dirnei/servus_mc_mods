package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * A craftable entry of a material pattern: its item without the material and
 * the ingredient the material follows, for the entry's tooltip.
 */
public record MaterialCraftable(ItemStack generic, AnyGroup follows) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialCraftable> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, MaterialCraftable::generic,
            AnyGroup.STREAM_CODEC, MaterialCraftable::follows,
            MaterialCraftable::new);
}
