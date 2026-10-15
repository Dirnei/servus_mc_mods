package io.github.dirnei.mc.servus.alchemy.client.sight;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.client.sight.HollowLook.Cell;
import io.github.dirnei.mc.servus.alchemy.client.sight.HollowLook.Crack;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.HollowTracker;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

/**
 * Draws what Elemental Sight shows of a Gnome Hollow: a darkened, cracked shell over the stone. A planted Hollow
 * fills its cracks with the vein's material up to its ripeness, flowing and glowing in the vein's colour, with
 * nodules near ripe. A bound Hollow shows a cave entry on its facing side, which crumbles open when the client sees
 * the gnome being bound, and crystals around its mouth as the vein ripens. Called only under the sight, by
 * {@link RevealRenderers}.
 *
 * <p>Layers lie {@link HollowShell#ROCK_OUT}, {@link HollowShell#MATERIAL_OUT} and {@link HollowShell#CAVE_OUT}
 * out from the block, so no two share a plane with each other or with the stone. Nodules and crystals stand on
 * pixels that never touch on a side, and pixels of one layer only meet at their edges.
 */
public final class HollowRevealRenderer implements RevealRenderers.RevealRenderer {

    private static final double RANGE_SQ = 32 * 32;
    private static final Identifier ROCK = texture("hollow_rock");
    private static final Identifier MASK = texture("hollow_cracks");
    private static final Identifier MATERIAL = texture("hollow_material");
    private static final Identifier CAVE = texture("hollow_cave");
    private static final Identifier CRYSTALS = texture("hollow_crystals");
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MATERIAL_ALPHA = 0xE0;
    /** Ticks for one bright band to run along a crack, and how much faster it runs for a gifted gnome; whole, so time wraps without a jump. */
    private static final int FLOW_TICKS = 40;
    private static final int GIFTED_PACE = 2;
    /** Ticks the cave takes to crumble open. */
    private static final float OPENING_TICKS = 20;
    private static final float CRUMBLE_CHANCE = 0.5F;
    private static final BlockParticleOption CRUMBLE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());

    /** Read on first draw, once resources are loaded. */
    private static HollowLook cachedLook;

    /** Per Hollow being opened: when it was bound and how many cave rings have crumbled since. */
    private final Map<GnomeHollowBlockEntity, Crumbled> crumbled = new WeakHashMap<>();

    private record Crumbled(long boundAt, int rings) {
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "textures/sight/" + name + ".png");
    }

    @Override
    public void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitBuffers buffers = new SubmitBuffers(event.getSubmitNodeCollector());
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (GnomeHollowBlockEntity hollow : HollowTracker.hollows()) {
            BlockPos pos = hollow.getBlockPos();
            if (hollow.isRemoved() || pos.getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            draw(level, hollow, new HollowShell(poseStack.last()), buffers, partialTick);
            poseStack.popPose();
        }
        buffers.submit();
    }

    /** What the client knows of one Hollow, as the sight shows it this frame. */
    private record Shown(Direction facing, boolean planted, boolean bound, float ripeness, float opening, float phase, int color) {
    }

    /** The buffers of the shell's layers. */
    private record Layers(VertexConsumer rock, VertexConsumer material, VertexConsumer nodules, VertexConsumer cave,
                          VertexConsumer crystals) {

        static Layers of(SubmitBuffers buffers) {
            return new Layers(buffers.getBuffer(RenderTypes.entityCutoutCull(ROCK)),
                    buffers.getBuffer(RenderTypes.entityTranslucent(MATERIAL)),
                    buffers.getBuffer(RenderTypes.entityCutout(MATERIAL)),
                    buffers.getBuffer(RenderTypes.entityCutoutCull(CAVE)),
                    buffers.getBuffer(RenderTypes.entityCutout(CRYSTALS)));
        }
    }

    private void draw(ClientLevel level, GnomeHollowBlockEntity hollow, HollowShell shell, SubmitBuffers buffers, float partialTick) {
        HollowLook look = look();
        BlockPos pos = hollow.getBlockPos();
        Shown shown = shown(hollow, level.getGameTime(), partialTick);
        Layers layers = Layers.of(buffers);
        for (Direction face : Direction.values()) {
            BlockPos neighbour = pos.relative(face);
            if (!level.getBlockState(neighbour).isSolidRender()) {
                drawFace(look, shell, layers, face, LevelRenderer.getLightCoords(level, neighbour), shown);
            }
        }
        crumble(level, hollow, look, shown.facing(), shown.opening());
    }

    private static Shown shown(GnomeHollowBlockEntity hollow, long gameTime, float partialTick) {
        BlockState state = hollow.getBlockState();
        Direction facing = state.hasProperty(GnomeHollowBlock.FACING) ? state.getValue(GnomeHollowBlock.FACING) : Direction.NORTH;
        int pace = hollow.shownBoosted() ? GIFTED_PACE : 1;
        float phase = (float) (Math.TAU * pace * (gameTime % FLOW_TICKS + partialTick) / FLOW_TICKS);
        return new Shown(facing, !hollow.shownYield().isEmpty(), hollow.shownBound(), hollow.shownRipeness(),
                opening(hollow, gameTime, partialTick), phase, hollow.shownColor());
    }

    private static void drawFace(HollowLook look, HollowShell shell, Layers layers, Direction face, int light, Shown shown) {
        shell.face(layers.rock(), face, HollowShell.ROCK_OUT, WHITE, light);
        if (shown.planted()) {
            for (Crack crack : look.filled(shown.ripeness())) {
                int glow = scaled(shown.color(), HollowLook.flow(shown.phase(), crack.position()));
                shell.pixel(layers.material(), face, crack.x(), crack.y(), HollowShell.MATERIAL_OUT, MATERIAL_ALPHA << 24 | glow,
                        LightCoordsUtil.FULL_BRIGHT);
            }
            for (Crack nodule : look.nodules(shown.ripeness())) {
                shell.box(layers.nodules(), face, nodule.x(), nodule.y(), HollowShell.MATERIAL_OUT, HollowShell.PIXEL,
                        opaque(shown.color()), LightCoordsUtil.FULL_BRIGHT);
            }
        }
        if (!shown.bound() || face != shown.facing()) {
            return;
        }
        for (Cell cell : look.cave(shown.opening())) {
            shell.pixel(layers.cave(), face, cell.x(), cell.y(), HollowShell.CAVE_OUT, WHITE, light);
        }
        float ripeness = shown.planted() ? shown.ripeness() : 0;
        float height = HollowShell.PIXEL * (1 + (ripeness - HollowLook.CRYSTALS_FROM) / (1 - HollowLook.CRYSTALS_FROM));
        for (Cell crystal : look.crystals(ripeness, true)) {
            shell.box(layers.crystals(), face, crystal.x(), crystal.y(), HollowShell.CAVE_OUT, height, opaque(shown.color()),
                    LightCoordsUtil.FULL_BRIGHT);
        }
    }

    /** How far the cave has opened, 0 to 1: fully open unless the client saw the binding less than a second ago. */
    private static float opening(GnomeHollowBlockEntity hollow, long gameTime, float partialTick) {
        if (!hollow.shownBound()) {
            return 0;
        }
        long boundAt = hollow.shownBoundAt();
        if (boundAt == GnomeHollowBlockEntity.NOT_SEEN_BINDING) {
            return 1;
        }
        return Math.clamp((gameTime - boundAt + partialTick) / OPENING_TICKS, 0F, 1F);
    }

    /** Throws stone crumbs off every cave ring that opened since the last frame. */
    private void crumble(ClientLevel level, GnomeHollowBlockEntity hollow, HollowLook look, Direction facing, float opening) {
        long boundAt = hollow.shownBoundAt();
        if (boundAt == GnomeHollowBlockEntity.NOT_SEEN_BINDING) {
            crumbled.remove(hollow);
            return;
        }
        Crumbled before = crumbled.get(hollow);
        int done = before != null && before.boundAt() == boundAt ? before.rings() : 0;
        int open = look.ringsOpen(opening);
        RandomSource random = level.getRandom();
        BlockPos pos = hollow.getBlockPos();
        for (int ring = done; ring < open; ring++) {
            for (Cell cell : look.caveRing(ring)) {
                if (random.nextFloat() >= CRUMBLE_CHANCE) {
                    continue;
                }
                Vector3f at = HollowShell.at(facing, (cell.x() + 0.5F) * HollowShell.PIXEL, (cell.y() + 0.5F) * HollowShell.PIXEL, 0.05F);
                double speed = 0.05 + random.nextDouble() * 0.05;
                level.addParticle(CRUMBLE, pos.getX() + at.x, pos.getY() + at.y, pos.getZ() + at.z,
                        facing.getStepX() * speed, random.nextDouble() * 0.05, facing.getStepZ() * speed);
            }
        }
        crumbled.put(hollow, new Crumbled(boundAt, open));
    }

    private static HollowLook look() {
        if (cachedLook == null) {
            cachedLook = readLook();
        }
        return cachedLook;
    }

    private static HollowLook readLook() {
        try (NativeImage mask = read(MASK); NativeImage cave = read(CAVE); NativeImage crystals = read(CRYSTALS)) {
            return HollowLook.read(mask::getPixel, cave::getPixel, crystals::getPixel);
        } catch (IOException e) {
            ServusAlchemy.LOGGER.error("Could not read the Gnome Hollow sight textures", e);
            return HollowLook.read((x, y) -> 0, (x, y) -> 0, (x, y) -> 0);
        }
    }

    private static NativeImage read(Identifier texture) throws IOException {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(texture)) {
            return NativeImage.read(stream);
        }
    }

    /** An RGB colour with each channel times {@code factor}. */
    private static int scaled(int rgb, float factor) {
        int r = Math.min(255, Math.round((rgb >> 16 & 0xFF) * factor));
        int g = Math.min(255, Math.round((rgb >> 8 & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
