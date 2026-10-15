package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: open the pattern editor of the Production Terminal whose menu is open, prefilled with this recipe. */
public record OpenRecipeEditorPayload(Identifier recipe) implements CustomPacketPayload {

    public static final Type<OpenRecipeEditorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "open_recipe_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRecipeEditorPayload> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(OpenRecipeEditorPayload::new, OpenRecipeEditorPayload::recipe).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
