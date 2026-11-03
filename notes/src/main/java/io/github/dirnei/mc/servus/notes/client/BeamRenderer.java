package io.github.dirnei.mc.servus.notes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Spot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Draws a gold beacon beam at every spot in {@link Beams} that lies in the dimension the player is in. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class BeamRenderer {

    private static final int COLOR = 0xFFFFD76A;
    private static final int ANIMATION_TICKS = 40;
    /** Past this distance the beam widens like a beacon's so it stays visible from afar. */
    private static final float WIDEN_FROM = 96F;

    private BeamRenderer() {
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Beams.all().isEmpty() || minecraft.level == null) {
            return;
        }
        String dimension = minecraft.level.dimension().identifier().toString();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float animation = Math.floorMod(event.getLevelRenderState().gameTime, ANIMATION_TICKS)
                + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        for (Spot spot : Beams.all()) {
            if (!spot.dimension().equals(dimension)) {
                continue;
            }
            double dx = spot.x() - camera.x;
            double dz = spot.z() - camera.z;
            float widen = Math.max(1F, (float) Math.sqrt((dx + 0.5) * (dx + 0.5) + (dz + 0.5) * (dz + 0.5)) / WIDEN_FROM);
            int beamStart = 0;
            int height = Math.max(1, BeaconRenderer.MAX_RENDER_Y - spot.y());
            pose.pushPose();
            pose.translate(dx, spot.y() - camera.y, dz);
            BeaconRenderer.submitBeaconBeam(pose, event.getSubmitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1F, animation,
                    beamStart, height, COLOR, BeaconRenderer.SOLID_BEAM_RADIUS * widen, BeaconRenderer.BEAM_GLOW_RADIUS * widen);
            pose.popPose();
        }
    }
}
