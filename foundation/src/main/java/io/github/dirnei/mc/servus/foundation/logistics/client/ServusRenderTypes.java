package io.github.dirnei.mc.servus.foundation.logistics.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Render types of the Foundation logistics. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class ServusRenderTypes {

    /** Translucent coloured quads drawn through everything, without culling and without writing depth. */
    public static final RenderPipeline OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "pipeline/duct_overlay"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    /** For the hidden duct overlay and the placement ghost arrow. */
    public static final RenderType OVERLAY = RenderType.create("servus_duct_overlay",
            RenderSetup.builder(OVERLAY_PIPELINE).sortOnUpload().createRenderSetup());

    private ServusRenderTypes() {
    }

    @SubscribeEvent
    public static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(OVERLAY_PIPELINE);
    }
}
