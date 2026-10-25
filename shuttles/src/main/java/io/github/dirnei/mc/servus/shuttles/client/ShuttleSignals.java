package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.SignalLayout;
import io.github.dirnei.mc.servus.shuttles.shuttle.TailLight;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The lamps and number plates of a shuttle on the rails, drawn in the
 * shuttle item's space after the item: white headlights, tail lights that
 * show why it drives or stands, and its number on both long sides, the
 * number only for a camera close by. Lit lamps glow at full brightness.
 */
final class ShuttleSignals {

    private static final Identifier LENS = Identifier.withDefaultNamespace("block/white_concrete");
    private static final Identifier PLATE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/heavy_rack_iron");
    private static final int HEADLIGHT = 0xFFFFF1C8;
    private static final int DIM_RED = 0xFF8C1A14;
    private static final int RED = 0xFFFF2A1C;
    private static final int AMBER = 0xFFFFA314;
    private static final int UNLIT = 0xFF4A100C;
    private static final int TEXT = 0xE6E6DC;
    /** How far in front of its plate the number is drawn, in model pixels. */
    private static final double TEXT_GAP = 0.05;
    /** Beyond this many blocks from the camera the number is not drawn, like sign text. */
    private static final double TEXT_DISTANCE = 16;
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;
    private static final float PX = 1 / 16F;

    private ShuttleSignals() {
    }

    /**
     * @param elapsed  ticks since the view arrived, to see whether the shuttle brakes now
     * @param unfolded whether a Lumber shuttle stands unfolded, its walls and the plates on them folded away
     * @param centre   the shuttle's centre in the world, for the distance to the camera
     * @param light    the world light at the shuttle
     */
    static void draw(PoseStack poseStack, SubmitNodeCollector collector, ItemStack cart, ShuttleView view, double elapsed, long gameTime,
                     boolean unfolded, Vec3 centre, int light) {
        ShuttleModules modules = ShuttleModules.of(cart);
        String tool = modules.toolSpec().map(ModuleSpec::id).orElse("");
        ShuttleLook.Key key = ShuttleLook.Key.of(ShuttleItem.tier(cart), modules.speed(), modules.handling(), tool);
        TextureAtlasSprite lens = sprite(LENS);
        SignalLayout.Lamps lamps = SignalLayout.lamps(key);
        TailLight tail = TailLight.of(view.signal(), ShuttleGlide.braking(view, elapsed), gameTime);
        int tailColour = switch (tail) {
            case DIM_RED -> DIM_RED;
            case RED -> RED;
            case AMBER -> AMBER;
            case OFF -> UNLIT;
        };
        poseStack.pushPose();
        // The item is drawn around its centre, so its model space starts half a block down and back.
        poseStack.translate(-0.5, -0.5, -0.5);
        for (SignalLayout.Box box : lamps.head()) {
            box(poseStack, collector, box, lens, HEADLIGHT, LightCoordsUtil.FULL_BRIGHT);
        }
        for (SignalLayout.Box box : lamps.tail()) {
            box(poseStack, collector, box, lens, tailColour, tail == TailLight.OFF ? light : LightCoordsUtil.FULL_BRIGHT);
        }
        OptionalInt number = ShuttleItem.number(cart);
        if (number.isPresent() && !unfolded) {
            plates(poseStack, collector, key, "#" + number.getAsInt(), close(centre), light);
        }
        poseStack.popPose();
    }

    private static void plates(PoseStack poseStack, SubmitNodeCollector collector, ShuttleLook.Key key, String text, boolean withText,
                               int light) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        SignalLayout.Plates plates = SignalLayout.plates(key, width);
        TextureAtlasSprite steel = sprite(PLATE);
        box(poseStack, collector, plates.left(), steel, 0xFFFFFFFF, light);
        box(poseStack, collector, plates.right(), steel, 0xFFFFFFFF, light);
        if (!withText) {
            return;
        }
        SignalLayout.Box right = plates.right();
        SignalLayout.Box left = plates.left();
        double middle = (right.z1() + right.z2()) / 2;
        number(poseStack, collector, font, text, width, right.x2() + TEXT_GAP, plates.textTop(), middle, 90, plates.scale(), light);
        number(poseStack, collector, font, text, width, left.x1() - TEXT_GAP, plates.textTop(), middle, -90, plates.scale(), light);
    }

    /** The number centred on a plate face, turned so it reads from that side; positions in model pixels. */
    private static void number(PoseStack poseStack, SubmitNodeCollector collector, Font font, String text, int width, double x, double top,
                               double z, float yaw, double scale, int light) {
        poseStack.pushPose();
        poseStack.translate(x * PX, top * PX, z * PX);
        // Turned so text x runs to the viewer's right and text z points at the viewer.
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        float size = (float) scale * PX;
        poseStack.scale(size, -size, size);
        RenderSubmit.text(poseStack, collector, text, -width / 2.0F, 0, TEXT, false, light);
        poseStack.popPose();
    }

    /** One box in model pixels; drawn from its own corner so its texture never runs past the sprite. */
    private static void box(PoseStack poseStack, SubmitNodeCollector collector, SignalLayout.Box box, TextureAtlasSprite sprite, int colour,
                            int light) {
        FluidBox shape = new FluidBox(0, 0, 0, (float) (box.x2() - box.x1()) * PX, (float) (box.y2() - box.y1()) * PX,
                (float) (box.z2() - box.z1()) * PX);
        poseStack.pushPose();
        poseStack.translate(box.x1() * PX, box.y1() * PX, box.z1() * PX);
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                (pose, consumer) -> shape.draw(consumer, pose, ALL_FACES, sprite, colour, light));
        poseStack.popPose();
    }

    private static boolean close(Vec3 centre) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        return camera.distanceToSqr(centre) <= TEXT_DISTANCE * TEXT_DISTANCE;
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }
}
