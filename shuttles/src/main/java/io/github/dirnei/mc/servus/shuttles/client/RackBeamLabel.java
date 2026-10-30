package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.zone.Barcode;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * The label on a rack shelf's top beam, drawn per bay: a stripe piece in the
 * zone's colour over the beam, running to the bay edge on a side joined in the
 * shelf so the pieces read as one stripe; on the shelf's code bay a white
 * sticker with a barcode and the location code; and, drawn once by the
 * shelf's leftmost bay, the zone name in the longer free run beside the
 * sticker. Drawn in the rack renderer's front space: x to the viewer's right,
 * y up, the front face at z 0.5, the bay's centre at x 0.
 *
 * <p>Overlapping parts and their planes, all in pixels in front of the face:
 * the stripe spans z 0.005 to 0.1 over the beam (y 15 to 16), ending at the
 * posts' inner edge where the posts' faces lie behind z 0, or at the bay edge
 * without an end face where the next bay of the shelf goes on; two pieces
 * only touch along that edge. The zone name lies at z 0.064 below the stripe
 * (y under 15) and 0.2 beside the sticker; the sticker spans z 0.105 to 0.2,
 * 0.005 in front of the stripe it overlaps at y 15 to 15.6; the bars span
 * z 0.205 to 0.215 and never touch each other; the code text lies at z 0.22
 * below the bars. No two faces share a plane where they overlap.
 */
final class RackBeamLabel {

    private static final float PX = 1 / 16F;
    private static final float FRONT = 0.5F;
    private static final float INNER = 6 * PX;
    private static final float EDGE = 8 * PX;
    private static final float BAY = 16 * PX;
    private static final float STRIPE_BOTTOM = 15 * PX;
    private static final float STRIPE_TOP = 16 * PX;
    private static final float STRIPE_BACK = FRONT + 0.005F * PX;
    private static final float STRIPE_FRONT = FRONT + 0.1F * PX;
    private static final float NAME_TOP = 14.9F * PX;
    private static final float NAME_Z = FRONT + 0.064F * PX;
    private static final float NAME_SCALE = 0.008F;
    private static final float NAME_GAP = 0.2F * PX;
    private static final float STICKER_LEFT = 1 * PX;
    private static final float STICKER_BOTTOM = 13.2F * PX;
    private static final float STICKER_TOP = 15.6F * PX;
    private static final float STICKER_BACK = FRONT + 0.105F * PX;
    private static final float STICKER_FRONT = FRONT + 0.2F * PX;
    private static final float BARS_LEFT = STICKER_LEFT + 0.3F * PX;
    private static final float BARS_RIGHT = INNER - 0.3F * PX;
    private static final float BARS_BOTTOM = 14.4F * PX;
    private static final float BARS_TOP = 15.35F * PX;
    private static final float BARS_BACK = STICKER_FRONT + 0.005F * PX;
    private static final float BARS_FRONT = STICKER_FRONT + 0.015F * PX;
    private static final int BAR_MODULES = 30;
    private static final float CODE_TOP = 14.3F * PX;
    private static final float CODE_Z = STICKER_FRONT + 0.02F * PX;
    private static final float CODE_HEIGHT = 0.8F * PX;
    private static final int STICKER_COLOUR = 0xFFF4F1E6;
    private static final int BAR_COLOUR = 0xFF151515;
    private static final int CODE_COLOUR = 0x151515;
    private static final int NAME_COLOUR = 0xFFF3C0;
    private static final int ALL_FACES = 0b111111;
    private static final int WEST_FACE = 1 << Direction.WEST.ordinal();
    private static final int EAST_FACE = 1 << Direction.EAST.ordinal();
    private static final String ELLIPSIS = "...";
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/zone_sticker");

    private RackBeamLabel() {
    }

    private static TextureAtlasSprite white() {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WHITE);
    }

    /**
     * The bay's piece of the zone stripe; on a side joined to the next bay of
     * the shelf it runs to the bay edge and leaves out its end face.
     */
    static void stripe(PoseStack poseStack, SubmitNodeCollector collector, ZoneDefinition zone, boolean joinedLeft, boolean joinedRight,
                       int light) {
        float left = joinedLeft ? -EDGE : -INNER;
        float right = joinedRight ? EDGE : INNER;
        int faces = ALL_FACES & ~(joinedLeft ? WEST_FACE : 0) & ~(joinedRight ? EAST_FACE : 0);
        box(poseStack, collector, white(), new Span(left, STRIPE_BOTTOM, STRIPE_BACK, right, STRIPE_TOP, STRIPE_FRONT), faces,
                ChannelColours.argb(zone.colour()), light);
    }

    /**
     * The free run along a shelf for its zone name, as {@code {left, right}}
     * in the front space of the leftmost bay: from the left inner edge to the
     * right inner edge of the last bay, or, with a sticker on the bay at
     * {@code stickerBay}, the longer side beside it.
     *
     * @param bays       bays in the shelf
     * @param stickerBay index of the bay with the sticker from the left, -1 for none
     */
    static float[] nameRun(int bays, int stickerBay) {
        float start = -INNER;
        float end = (bays - 1) * BAY + INNER;
        if (stickerBay < 0) {
            return new float[] {start, end};
        }
        float stickerLeft = stickerBay * BAY + STICKER_LEFT - NAME_GAP;
        float stickerRight = stickerBay * BAY + INNER + NAME_GAP;
        return stickerLeft - start >= end - stickerRight ? new float[] {start, stickerLeft} : new float[] {stickerRight, end};
    }

    /** The zone name centred in the run {@code {left, right}}, cut with an ellipsis to fit. */
    static void name(PoseStack poseStack, SubmitNodeCollector collector, String zoneName, float[] run, int light) {
        Font font = Minecraft.getInstance().font;
        String name = fit(font, zoneName, (run[1] - run[0]) / NAME_SCALE);
        text(poseStack, collector, font, name, (run[0] + run[1]) / 2, NAME_TOP, NAME_Z, NAME_SCALE, NAME_COLOUR, light);
    }

    /** The white code sticker with its barcode and code, hanging from the beam on the bay's right. */
    static void sticker(PoseStack poseStack, SubmitNodeCollector collector, String code, int light) {
        TextureAtlasSprite white = white();
        Font font = Minecraft.getInstance().font;
        box(poseStack, collector, white, new Span(STICKER_LEFT, STICKER_BOTTOM, STICKER_BACK, INNER, STICKER_TOP, STICKER_FRONT),
                ALL_FACES, STICKER_COLOUR, light);
        float module = (BARS_RIGHT - BARS_LEFT) / BAR_MODULES;
        for (Barcode.Bar bar : Barcode.bars(code, BAR_MODULES)) {
            box(poseStack, collector, white, new Span(BARS_LEFT + bar.start() * module, BARS_BOTTOM, BARS_BACK,
                    BARS_LEFT + bar.end() * module, BARS_TOP, BARS_FRONT), ALL_FACES, BAR_COLOUR, light);
        }
        float width = INNER - STICKER_LEFT - 0.4F * PX;
        float scale = Math.min(CODE_HEIGHT / font.lineHeight, width / Math.max(1, font.width(code)));
        text(poseStack, collector, font, code, (STICKER_LEFT + INNER) / 2, CODE_TOP, CODE_Z, scale, CODE_COLOUR, light);
    }

    /** The text cut with an ellipsis so it is at most {@code maxWidth} font pixels wide. */
    private static String fit(Font font, String text, float maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, (int) maxWidth - font.width(ELLIPSIS)) + ELLIPSIS;
    }

    /** A box from one corner to the other in the front space. */
    private record Span(float x0, float y0, float z0, float x1, float y1, float z1) {
    }

    /** Draws the box's {@code faces} from its own corner, so its texture coordinates stay inside the sprite. */
    private static void box(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, Span span, int faces,
                            int colour, int light) {
        FluidBox shape = new FluidBox(0, 0, 0, span.x1() - span.x0(), span.y1() - span.y0(), span.z1() - span.z0());
        poseStack.pushPose();
        poseStack.translate(span.x0(), span.y0(), span.z0());
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                (pose, consumer) -> shape.draw(consumer, pose, faces, sprite, colour, light));
        poseStack.popPose();
    }

    /** Text centred on {@code x} with its top at {@code top}, lying in the plane {@code z}. */
    private static void text(PoseStack poseStack, SubmitNodeCollector collector, Font font, String text, float x, float top, float z,
                             float scale, int colour, int light) {
        poseStack.pushPose();
        poseStack.translate(x, top, z);
        poseStack.scale(scale, -scale, scale);
        RenderSubmit.text(poseStack, collector, text, -font.width(text) / 2.0F, 0.0F, colour, false, light);
        poseStack.popPose();
    }
}
