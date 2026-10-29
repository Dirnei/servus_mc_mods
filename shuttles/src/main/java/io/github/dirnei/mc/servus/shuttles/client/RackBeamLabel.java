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
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * The label on a rack bay's top beam: a stripe in the zone's colour over the
 * beam, the zone name below it, and on the right a white sticker with a
 * barcode and the location code. Drawn in the rack renderer's front space:
 * x to the viewer's right, y up, the front face at z 0.5.
 *
 * <p>Overlapping parts and their planes, all in pixels in front of the face:
 * the stripe spans z 0.005 to 0.1 over the beam (y 15 to 16), ending at the
 * posts' inner edge where the posts' faces lie behind z 0; the zone name lies
 * at z 0.064 below the stripe (y under 15), left of the sticker; the sticker
 * spans z 0.105 to 0.2, 0.005 in front of the stripe it overlaps at y 15 to
 * 15.6; the bars span z 0.205 to 0.215 and never touch each other; the code
 * text lies at z 0.22 below the bars. No two faces share a plane where they
 * overlap.
 */
final class RackBeamLabel {

    private static final float PX = 1 / 16F;
    private static final float FRONT = 0.5F;
    private static final float INNER = 6 * PX;
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
    private static final String ELLIPSIS = "...";
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/zone_sticker");

    private RackBeamLabel() {
    }

    /** Draws what the bay has: stripe and name when zoned, sticker when it has a code. */
    static void draw(PoseStack poseStack, SubmitNodeCollector collector, ZoneDefinition zone, String code, int light) {
        TextureAtlasSprite white = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WHITE);
        Font font = Minecraft.getInstance().font;
        boolean sticker = !code.isEmpty();
        if (zone != null) {
            box(poseStack, collector, white, new Span(-INNER, STRIPE_BOTTOM, STRIPE_BACK, INNER, STRIPE_TOP, STRIPE_FRONT),
                    ChannelColours.argb(zone.colour()), light);
            float right = sticker ? STICKER_LEFT - NAME_GAP : INNER;
            String name = fit(font, zone.name(), (right + INNER) / NAME_SCALE);
            text(poseStack, collector, font, name, (-INNER + right) / 2, NAME_TOP, NAME_Z, NAME_SCALE, NAME_COLOUR, light);
        }
        if (sticker) {
            box(poseStack, collector, white, new Span(STICKER_LEFT, STICKER_BOTTOM, STICKER_BACK, INNER, STICKER_TOP, STICKER_FRONT),
                    STICKER_COLOUR, light);
            float module = (BARS_RIGHT - BARS_LEFT) / BAR_MODULES;
            for (Barcode.Bar bar : Barcode.bars(code, BAR_MODULES)) {
                box(poseStack, collector, white, new Span(BARS_LEFT + bar.start() * module, BARS_BOTTOM, BARS_BACK,
                        BARS_LEFT + bar.end() * module, BARS_TOP, BARS_FRONT), BAR_COLOUR, light);
            }
            float width = INNER - STICKER_LEFT - 0.4F * PX;
            float scale = Math.min(CODE_HEIGHT / font.lineHeight, width / Math.max(1, font.width(code)));
            text(poseStack, collector, font, code, (STICKER_LEFT + INNER) / 2, CODE_TOP, CODE_Z, scale, CODE_COLOUR, light);
        }
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

    /** Draws the box from its own corner, so its texture coordinates stay inside the sprite. */
    private static void box(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, Span span, int colour,
                            int light) {
        FluidBox shape = new FluidBox(0, 0, 0, span.x1() - span.x0(), span.y1() - span.y0(), span.z1() - span.z0());
        poseStack.pushPose();
        poseStack.translate(span.x0(), span.y0(), span.z0());
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                (pose, consumer) -> shape.draw(consumer, pose, ALL_FACES, sprite, colour, light));
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
