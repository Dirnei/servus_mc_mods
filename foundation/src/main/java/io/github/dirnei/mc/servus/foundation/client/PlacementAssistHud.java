package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A small indicator beside the crosshair while a sneak click would place:
 * an arrow towards the ghost on screen, or a ring when the ghost lies straight
 * ahead, and the distance in blocks.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class PlacementAssistHud {

    private static final int COLOR = PlacementAssistRenderer.A << 24 | PlacementAssistRenderer.R << 16
            | PlacementAssistRenderer.G << 8 | PlacementAssistRenderer.B;
    /** Below this share of the full length the on screen direction is too short to point anywhere. */
    private static final double STRAIGHT_AHEAD = 0.25;

    private PlacementAssistHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "placement_assist"), PlacementAssistHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        Optional<PlacementAssistPreview.Preview> found = PlacementAssistPreview.current();
        if (found.isEmpty()) {
            return;
        }
        PlacementAssistPreview.Preview preview = found.get();
        BlockPos target = preview.target().pos();
        Vec3 toTarget = Vec3.atCenterOf(target).subtract(preview.hit().getLocation());
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3fc look = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3f right = new Vector3f(look).cross(up);
        double screenX = toTarget.x * right.x() + toTarget.y * right.y() + toTarget.z * right.z();
        double screenY = toTarget.x * up.x() + toTarget.y * up.y() + toTarget.z * up.z();

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        if (Math.hypot(screenX, screenY) < STRAIGHT_AHEAD * toTarget.length()) {
            ring(graphics);
        } else {
            graphics.pose().rotate((float) Math.atan2(-screenY, screenX));
            arrow(graphics);
        }
        graphics.pose().popMatrix();
        graphics.text(minecraft.font, Integer.toString(preview.distance()), centerX + 9, centerY + 7, COLOR, true);
    }

    /** An arrow pointing along +x, starting a little way out from the crosshair. */
    private static void arrow(GuiGraphicsExtractor graphics) {
        graphics.fill(9, -1, 15, 1, COLOR);
        for (int i = 0; i < 4; i++) {
            graphics.fill(15 + i, -4 + i, 16 + i, 4 - i, COLOR);
        }
    }

    /** A square ring around the crosshair. */
    private static void ring(GuiGraphicsExtractor graphics) {
        graphics.fill(-7, -7, 7, -6, COLOR);
        graphics.fill(-7, 6, 7, 7, COLOR);
        graphics.fill(-7, -6, -6, 6, COLOR);
        graphics.fill(6, -6, 7, 6, COLOR);
    }
}
