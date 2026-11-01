package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: lay this vanilla crafting recipe out in the open
 * terminal's order grid (JEI transfer).
 *
 * @param shown the stacks the recipe viewer shows per cell, used when the server does not know the recipe
 * @param keepMode whether the order grid was open already, which keeps its mode
 */
public record OrderGridRecipePayload(Identifier recipe, List<List<ItemStack>> shown, boolean keepMode) implements CustomPacketPayload {

    public static final Type<OrderGridRecipePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "order_grid_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderGridRecipePayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, OrderGridRecipePayload::recipe,
            CraftingGrid.SHOWN_CELLS_CODEC, OrderGridRecipePayload::shown,
            ByteBufCodecs.BOOL, OrderGridRecipePayload::keepMode,
            OrderGridRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
