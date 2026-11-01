package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: open the pattern editor of the Production Terminal whose menu is open, prefilled with this recipe.
 *
 * @param shown the stacks the recipe viewer shows per cell, used when the server does not know the recipe
 */
public record OpenRecipeEditorPayload(Identifier recipe, List<List<ItemStack>> shown) implements CustomPacketPayload {

    public static final Type<OpenRecipeEditorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "open_recipe_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRecipeEditorPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, OpenRecipeEditorPayload::recipe,
            CraftingGrid.SHOWN_CELLS_CODEC, OpenRecipeEditorPayload::shown,
            OpenRecipeEditorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
