package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Standalone block models drawn by block entity renderers, such as the Sink
 * fan and the pump beams. Each is registered with a key and baked into one
 * model part.
 */
public final class ModelKeys {

    private ModelKeys() {
    }

    /** A key for the model at {@code servus_foundation:<path>}. */
    public static StandaloneModelKey<BlockStateModelPart> key(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, path);
        return new StandaloneModelKey<>(id::toString);
    }

    /** Registers a key made by {@link #key(String)} for loading; its name is the model id. */
    public static void register(ModelEvent.RegisterStandalone event, StandaloneModelKey<BlockStateModelPart> key) {
        event.register(key, SimpleUnbakedStandaloneModel.simpleModelWrapper(Identifier.parse(key.getName())));
    }

    /** Submits the model in the cutout block layer, untinted. */
    public static void submit(StandaloneModelKey<BlockStateModelPart> key, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        BlockStateModelPart part = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        if (part == null) {
            return;
        }
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(part), BlockModelRenderState.EMPTY_TINTS, light,
                OverlayTexture.NO_OVERLAY, 0);
    }
}
