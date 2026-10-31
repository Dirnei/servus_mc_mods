package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLamp;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerPart;
import io.github.dirnei.mc.servus.shuttles.locker.LockerScreen;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws what moves on a Parcel Locker, from the dock: every box's door as a
 * slab hinged on its left edge, swung as {@link io.github.dirnei.mc.servus.shuttles.locker.LockerDoors}
 * says, with its lamp full bright in the colour the local player sees; the
 * first items of an open box with their counts; and on the screen the flash
 * of a scan and the count of opened boxes under HELLO.
 *
 * <p>Drawn in the locker's front space in pixels: x from the locker's left
 * edge to the right (0 to 80), y up from its bottom (0 to 32), z towards the
 * viewer with the block faces at z 0. Planes, model z counted into the
 * block: a closed door spans z -0.95 to 0 in front of the frames' faces at
 * z -1, x 0.05 to 15 and from a pixel above its box's bottom to 0.05 below
 * its top, so it touches neither the neighbour column's frame or door nor
 * the roof's underside at y 32; its lamp stands on its front from z 0 to
 * 0.3 without a back face. Items lie in the box's recess at z -5, in front
 * of the back wall at z -8, their counts at z -3. The scan flash lies at
 * z -0.47 and the count digits at z -0.45, in front of the LCD at z -0.5 and
 * behind the bezel's front at z 0. No two faces share a plane where they
 * overlap.
 */
public final class ParcelLockerRenderer extends LiveBlockEntityRenderer<ParcelLockerBlockEntity> {

    private static final float PX = 1 / 16F;
    private static final float DOOR_INSET = 0.05F;
    private static final float DOOR_WIDTH = 15;
    private static final float DOOR_DEPTH = 0.95F;
    private static final float LAMP_X0 = 10.5F;
    private static final float LAMP_X1 = 12;
    private static final float LAMP_DEPTH = 0.3F;
    private static final float ITEM_Z = -5;
    private static final float COUNT_Z = -3;
    private static final float COUNT_HEIGHT = 2.2F;
    private static final float FLASH_Z = -0.47F;
    private static final float DIGIT_Z = -0.45F;
    private static final float TEXEL = 0.5F;
    private static final int FLASH_TICKS = 4;
    private static final int LCD_TEXT = 0xFF8DFF9A;
    private static final int FLASH = 0xFF3C8A4A;
    private static final int COUNT_COLOUR = 0xFFFFFF;
    private static final int SCREEN_COLUMN_X = LockerLayout.HEAD_COLUMN * LockerLayout.BLOCK;
    /** The second line of the HELLO screen in texels from the screen's top. */
    private static final int HELLO_LINE = 15;
    private static final Map<LockerLamp, Integer> LAMP_COLOURS = Map.of(
            LockerLamp.OFF, 0xFF7A5C00, LockerLamp.TAKEN, 0xFF9B7600, LockerLamp.RESERVED, 0xFFFFB000, LockerLamp.READY, 0xFF3CFF5A);
    private static final Map<Character, String[]> GLYPHS = Map.ofEntries(
            Map.entry('0', new String[] {"###", "#.#", "#.#", "#.#", "###"}), Map.entry('1', new String[] {".#.", "##.", ".#.", ".#.", "###"}),
            Map.entry('2', new String[] {"###", "..#", "###", "#..", "###"}), Map.entry('3', new String[] {"###", "..#", ".##", "..#", "###"}),
            Map.entry('4', new String[] {"#.#", "#.#", "###", "..#", "..#"}), Map.entry('5', new String[] {"###", "#..", "###", "..#", "###"}),
            Map.entry('6', new String[] {"###", "#..", "###", "#.#", "###"}), Map.entry('7', new String[] {"###", "..#", ".#.", ".#.", ".#."}),
            Map.entry('8', new String[] {"###", "#.#", "###", "#.#", "###"}), Map.entry('9', new String[] {"###", "#.#", "###", "..#", "###"}),
            Map.entry('B', new String[] {"##.", "#.#", "##.", "#.#", "##."}), Map.entry('O', new String[] {"###", "#.#", "#.#", "#.#", "###"}),
            Map.entry('X', new String[] {"#.#", "#.#", ".#.", "#.#", "#.#"}), Map.entry(' ', new String[] {"...", "...", "...", "...", "..."}));
    private static final Identifier LAMP = id("lamp");
    private static final Identifier EDGE = id("yellow");
    private static final Identifier INSIDE = id("door_inside");

    public ParcelLockerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/parcel_locker_" + name);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /** All ten parts and the doors swung out in front. */
    @Override
    public AABB getRenderBoundingBox(ParcelLockerBlockEntity locker) {
        Direction facing = locker.facing();
        BlockPos dock = locker.getBlockPos();
        return new AABB(LockerPart.S_COLUMN_LOW.at(dock, facing)).minmax(new AABB(LockerPart.L_HIGH.at(dock, facing))).inflate(1);
    }

    @Override
    protected void render(ParcelLockerBlockEntity locker, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Level level = locker.getLevel();
        if (level == null) {
            return;
        }
        double time = level.getGameTime() + partialTick;
        Direction facing = locker.facing();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        LocalPlayer player = Minecraft.getInstance().player;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(PX, PX, PX);
        poseStack.translate(-SCREEN_COLUMN_X - LockerLayout.BLOCK / 2.0F, 0, LockerLayout.BLOCK / 2.0F);
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            ParcelLockerBlockEntity.Shown shown = locker.shown(box.index());
            int boxLight = LevelRenderer.getLightCoords(level, locker.boxPos(box.index()).relative(facing));
            boolean mine = player != null && player.getUUID().equals(shown.owner());
            float angle = locker.doors().angle(box.index(), time);
            door(poseStack, collector, atlas, box, angle, LAMP_COLOURS.get(LockerLamp.of(shown.state(), mine)), boxLight);
            if (angle > 0) {
                items(poseStack, collector, level, box, shown.stacks(), boxLight);
            }
        }
        screen(locker, level, time, poseStack, collector, atlas);
        poseStack.popPose();
    }

    /** The door of a box turned {@code angle} degrees out about its left edge, with its lamp. */
    private static void door(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlas atlas, LockerLayout.Box box, float angle,
                             int lampColour, int light) {
        float left = box.column() * LockerLayout.BLOCK + DOOR_INSET;
        float bottom = box.bottom() + 1;
        float top = box.top() - DOOR_INSET;
        float width = DOOR_WIDTH - DOOR_INSET;
        float height = top - bottom;
        TextureAtlasSprite front = atlas.getSprite(id("door_" + box.id().replace("-", "").toLowerCase(Locale.ROOT)));
        TextureAtlasSprite inside = atlas.getSprite(INSIDE);
        TextureAtlasSprite edge = atlas.getSprite(EDGE);
        TextureAtlasSprite lamp = atlas.getSprite(LAMP);
        int frontHeight = (box.top() - box.bottom()) * 2 - 2;
        float frontV = frontHeight / (float) front.contents().height();
        float frontU = 30F / front.contents().width();
        float insideV = (box.top() - box.bottom() - 1) / (float) inside.contents().height();
        poseStack.pushPose();
        poseStack.translate(left, 0, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) -> {
            Quads quads = new Quads(consumer, pose, 0, bottom, -DOOR_DEPTH, width, top, 0, 0xFFFFFFFF, light);
            quads.face(Direction.SOUTH, front, 0, 0, frontU, frontV);
            quads.face(Direction.NORTH, inside, 0, 0, 15F / 16, insideV);
            for (Direction side : List.of(Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN)) {
                quads.face(side, edge, 0, 0, 1 / 16F, Math.min(1, height / 16));
            }
            Quads lit = new Quads(consumer, pose, LAMP_X0, top - 2.5F, 0, LAMP_X1, top - 1, LAMP_DEPTH, lampColour, LightCoordsUtil.FULL_BRIGHT);
            for (Direction side : List.of(Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN)) {
                lit.face(side, lamp, 0.2F, 0.2F, 0.8F, 0.8F);
            }
        });
        poseStack.popPose();
    }

    /** The first items of an open box lying on its floor, an S box one, the bigger boxes up to three, each with its count. */
    private static void items(PoseStack poseStack, SubmitNodeCollector collector, Level level, LockerLayout.Box box, List<ItemStack> stacks,
                             int light) {
        int shown = Math.min(stacks.size(), box.size() == LockerLayout.Size.S ? 1 : 3);
        if (shown == 0) {
            return;
        }
        float left = box.column() * LockerLayout.BLOCK + 1;
        float floor = box.bottom() + 2;
        float inner = box.top() - 1 - floor;
        float size = Math.min(inner, 13F / shown) * 0.8F;
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < shown; i++) {
            ItemStack stack = stacks.get(i);
            float centre = left + 13F * (i + 0.5F) / shown;
            poseStack.pushPose();
            poseStack.translate(centre, floor + size / 2, ITEM_Z);
            poseStack.scale(size, size, size);
            RenderSubmit.item(poseStack, collector, stack, ItemDisplayContext.FIXED, level, i, light);
            poseStack.popPose();
            String count = CompactCount.of(stack.getCount());
            float scale = COUNT_HEIGHT / font.lineHeight;
            poseStack.pushPose();
            poseStack.translate(centre + size / 2, floor + COUNT_HEIGHT, COUNT_Z);
            poseStack.scale(scale, -scale, scale);
            RenderSubmit.text(poseStack, collector, count, -font.width(count), 0, COUNT_COLOUR, true, light);
            poseStack.popPose();
        }
    }

    /** The scan's flash on the LCD, then under HELLO the count of opened boxes. */
    private static void screen(ParcelLockerBlockEntity locker, Level level, double time, PoseStack poseStack, SubmitNodeCollector collector,
                               TextureAtlas atlas) {
        BlockState state = level.getBlockState(LockerPart.HEAD_SCREEN.at(locker.getBlockPos(), locker.facing()));
        if (!(state.getBlock() instanceof ParcelLockerBlock) || state.getValue(ParcelLockerBlock.SCREEN) != LockerScreen.HELLO) {
            return;
        }
        TextureAtlasSprite white = atlas.getSprite(LAMP);
        float x0 = SCREEN_COLUMN_X;
        float top = 2 * LockerLayout.BLOCK;
        boolean flashing = time - locker.scanTime() < FLASH_TICKS;
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) -> {
            if (flashing) {
                new Quads(consumer, pose, x0 + 1.5F, LockerLayout.BLOCK + 1.5F, FLASH_Z, x0 + 14.5F, LockerLayout.BLOCK + 13, FLASH_Z, FLASH,
                        LightCoordsUtil.FULL_BRIGHT).face(Direction.SOUTH, white, 0.4F, 0.4F, 0.6F, 0.6F);
                return;
            }
            String line = locker.greeted() + " BOX";
            int width = line.length() * 4 - 1;
            int x = 16 - width / 2;
            for (char glyph : line.toCharArray()) {
                String[] rows = GLYPHS.get(glyph);
                for (int row = 0; row < rows.length; row++) {
                    for (int column = 0; column < rows[row].length(); column++) {
                        if (rows[row].charAt(column) != '#') {
                            continue;
                        }
                        float px = x0 + (x + column) * TEXEL;
                        float py = top - (HELLO_LINE + row) * TEXEL;
                        new Quads(consumer, pose, px, py - TEXEL, DIGIT_Z, px + TEXEL, py, DIGIT_Z, LCD_TEXT, LightCoordsUtil.FULL_BRIGHT)
                                .face(Direction.SOUTH, white, 0.4F, 0.4F, 0.6F, 0.6F);
                    }
                }
                x += 4;
            }
        });
    }

    /**
     * The faces of one box in the front space with explicit texture
     * coordinates as fractions of the sprite, upright for a viewer facing
     * each face.
     */
    private record Quads(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int colour,
                         int light) {

        void face(Direction face, TextureAtlasSprite sprite, float fu0, float fv0, float fu1, float fv1) {
            float u0 = sprite.getU(fu0);
            float u1 = sprite.getU(fu1);
            float v0 = sprite.getV(fv0);
            float v1 = sprite.getV(fv1);
            switch (face) {
                case UP -> {
                    vertex(x0, y1, z0, u0, v0, face);
                    vertex(x0, y1, z1, u0, v1, face);
                    vertex(x1, y1, z1, u1, v1, face);
                    vertex(x1, y1, z0, u1, v0, face);
                }
                case DOWN -> {
                    vertex(x0, y0, z0, u0, v0, face);
                    vertex(x1, y0, z0, u1, v0, face);
                    vertex(x1, y0, z1, u1, v1, face);
                    vertex(x0, y0, z1, u0, v1, face);
                }
                case NORTH -> {
                    vertex(x1, y1, z0, u0, v0, face);
                    vertex(x1, y0, z0, u0, v1, face);
                    vertex(x0, y0, z0, u1, v1, face);
                    vertex(x0, y1, z0, u1, v0, face);
                }
                case SOUTH -> {
                    vertex(x0, y1, z1, u0, v0, face);
                    vertex(x0, y0, z1, u0, v1, face);
                    vertex(x1, y0, z1, u1, v1, face);
                    vertex(x1, y1, z1, u1, v0, face);
                }
                case WEST -> {
                    vertex(x0, y1, z0, u0, v0, face);
                    vertex(x0, y0, z0, u0, v1, face);
                    vertex(x0, y0, z1, u1, v1, face);
                    vertex(x0, y1, z1, u1, v0, face);
                }
                case EAST -> {
                    vertex(x1, y1, z1, u0, v0, face);
                    vertex(x1, y0, z1, u0, v1, face);
                    vertex(x1, y0, z0, u1, v1, face);
                    vertex(x1, y1, z0, u1, v0, face);
                }
            }
        }

        private void vertex(float x, float y, float z, float u, float v, Direction face) {
            consumer.addVertex(pose, x, y, z)
                    .setColor(colour)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }
}
