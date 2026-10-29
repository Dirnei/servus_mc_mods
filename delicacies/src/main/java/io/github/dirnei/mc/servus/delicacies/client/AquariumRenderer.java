package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumClientLook;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumLook;
import io.github.dirnei.mc.servus.delicacies.aquarium.AquariumPartBlockEntity;
import io.github.dirnei.mc.servus.delicacies.aquarium.FishPath;
import io.github.dirnei.mc.servus.delicacies.aquarium.FishSpecies;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SalmonRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a formed aquarium from its master Floor: a gravel bed with swaying seagrass over the tank's floor plate, and
 * the vanilla cod or salmon models swimming on smooth paths in the water. Fish slow down in dirty water, float belly
 * up after deaths and flop on the bed when the water is too low.
 */
public final class AquariumRenderer implements BlockEntityRenderer<AquariumPartBlockEntity, AquariumRenderer.State> {

    private static final Identifier COD_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/cod.png");
    private static final Identifier SALMON_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/salmon.png");
    private static final Identifier GRAVEL = Identifier.withDefaultNamespace("block/gravel");
    private static final Identifier SEAGRASS = Identifier.withDefaultNamespace("block/seagrass");

    private static final float PX = 1 / 16.0F;
    /** The bed sits on the tank's floor plate (1 px) and under the frame bars' top (2 px), off both planes. */
    private static final float BED_BOTTOM = 1.05F * PX;
    private static final float BED_TOP = 1.95F * PX;
    /** Gap between the bed and the glass walls. */
    private static final float BED_INSET = 0.05F * PX;
    /** Depth step between seagrass plants so no two quads share a plane. */
    private static final float PLANT_STEP = 0.005F * PX;
    private static final float SEAGRASS_HEIGHT = 0.6F;
    /** The glass frame is 2 px; fish keep this far from glass, bed and surface. */
    private static final double FISH_MARGIN = 0.22;
    /** Below this much water (6 px) the fish cannot swim and flop on the bed. */
    private static final float FLOP_BELOW = 6 * PX;
    private static final double MAX_PITCH = Math.toRadians(30);
    /** The fish model's centre lies this far above the point a render pose puts it at. */
    private static final float MODEL_CENTRE = 0.125F;

    /** What one frame draws of an aquarium. */
    public static final class State extends BlockEntityRenderState {
        @Nullable TankShape box;
        @Nullable FishSpecies species;
        int drawn;
        int floaters;
        long floatingFor;
        double swimTime;
        float waterHeight;
        float quality;
        long seed;
        int light;
        float gameTime;
    }

    private final CodModel cod;
    private final SalmonModel salmon;

    public AquariumRenderer(BlockEntityRendererProvider.Context context) {
        this.cod = new CodModel(context.bakeLayer(ModelLayers.COD));
        this.salmon = new SalmonModel(context.bakeLayer(ModelLayers.SALMON));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AquariumPartBlockEntity master, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(master, state, partialTick, cameraPosition, breakProgress);
        state.box = null;
        Level level = master.getLevel();
        if (level == null || !master.isMaster() || master.box() == null) {
            return;
        }
        TankShape box = master.box();
        state.box = box;
        state.species = master.state().species();
        double stock = master.state().stock();
        state.drawn = AquariumLook.drawnFish(stock, master.clientCapacity(), master.clientDrawnCap());
        state.quality = master.clientQuality();
        state.waterHeight = master.clientWater() * box.height();
        long gameTime = level.getGameTime();
        state.gameTime = gameTime + partialTick;
        AquariumClientLook look = master.look();
        state.swimTime = look.swim(gameTime + partialTick, 0.25 + 0.75 * state.quality);
        state.floaters = look.floaters(gameTime);
        state.floatingFor = look.floatingFor(gameTime);
        state.seed = master.getBlockPos().asLong();
        state.light = LevelRenderer.getLightCoords(level, master.getBlockPos().above());
    }

    @Override
    public AABB getRenderBoundingBox(AquariumPartBlockEntity master) {
        TankShape box = master.box();
        if (box == null) {
            return new AABB(master.getBlockPos());
        }
        return new AABB(box.minX(), box.minY() - 1, box.minZ(), box.maxX() + 1, box.maxY() + 2, box.maxZ() + 1);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TankShape box = state.box;
        if (box == null) {
            return;
        }
        poseStack.pushPose();
        // From the master Floor to the tank's lowest north west corner.
        poseStack.translate(0, 1, 0);
        submitBed(state, box, poseStack, collector);
        if (state.species != null) {
            submitFish(state, box, poseStack, collector);
        }
        poseStack.popPose();
    }

    /** One gravel box per tank column and one or two seagrass plants on it. */
    private static void submitBed(State state, TankShape box, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite gravel = blockSprite(GRAVEL);
        TextureAtlasSprite seagrass = blockSprite(SEAGRASS);
        int width = box.width();
        int depth = box.depth();
        int light = state.light;
        float sway = state.gameTime;
        long seed = state.seed;
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) -> {
            int plant = 0;
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    float x0 = x == 0 ? BED_INSET : 0;
                    float x1 = x == width - 1 ? 1 - BED_INSET : 1;
                    float z0 = z == 0 ? BED_INSET : 0;
                    float z1 = z == depth - 1 ? 1 - BED_INSET : 1;
                    int faces = 1 << Direction.UP.ordinal();
                    if (x == 0) {
                        faces |= 1 << Direction.WEST.ordinal();
                    }
                    if (x == width - 1) {
                        faces |= 1 << Direction.EAST.ordinal();
                    }
                    if (z == 0) {
                        faces |= 1 << Direction.NORTH.ordinal();
                    }
                    if (z == depth - 1) {
                        faces |= 1 << Direction.SOUTH.ordinal();
                    }
                    // FluidBox takes its UVs from the box corners, so each column is drawn in its own 0 to 1 space.
                    PoseStack.Pose column = pose.copy();
                    column.translate(x, 0, z);
                    new FluidBox(x0, BED_BOTTOM, z0, x1, BED_TOP, z1).draw(consumer, column, faces, gravel, 0xFFFFFFFF, light);
                    long cell = seed * 31 + x * 7919L + z * 104729L;
                    int plants = 1 + (int) Math.floorMod(cell, 2);
                    for (int i = 0; i < plants; i++) {
                        float px = x + 0.25F + 0.5F * fraction(cell, i * 2);
                        float pz = z + 0.25F + 0.5F * fraction(cell, i * 2 + 1);
                        float lean = 0.06F * (float) Math.sin(sway * 0.05 + fraction(cell, i + 5) * 6.28);
                        seagrass(consumer, pose, seagrass, px, pz, plant * PLANT_STEP, lean, light);
                        plant++;
                    }
                }
            }
        });
    }

    private static float fraction(long seed, int index) {
        long mixed = (seed + index * 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 31;
        return (Math.floorMod(mixed, 1000)) / 1000.0F;
    }

    /** Two crossed quads, both sides, whose tops lean by {@code lean}. */
    private static void seagrass(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float x, float z,
                                 float step, float lean, int light) {
        float half = 0.3F;
        float y0 = BED_TOP;
        float y1 = BED_TOP + SEAGRASS_HEIGHT;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float d = half * 0.7071F;
        crossQuad(consumer, pose, x - d + step, z - d, x + d + step, z + d, y0, y1, lean, u0, u1, v0, v1, light);
        crossQuad(consumer, pose, x - d, z + d + step, x + d, z - d + step, y0, y1, lean, u0, u1, v0, v1, light);
    }

    private static void crossQuad(VertexConsumer consumer, PoseStack.Pose pose, float xa, float za, float xb, float zb, float y0,
                                  float y1, float lean, float u0, float u1, float v0, float v1, int light) {
        vertex(consumer, pose, xa, y0, za, u0, v1, light);
        vertex(consumer, pose, xb, y0, zb, u1, v1, light);
        vertex(consumer, pose, xb + lean, y1, zb + lean, u1, v0, light);
        vertex(consumer, pose, xa + lean, y1, za + lean, u0, v0, light);
        vertex(consumer, pose, xa + lean, y1, za + lean, u0, v0, light);
        vertex(consumer, pose, xb + lean, y1, zb + lean, u1, v0, light);
        vertex(consumer, pose, xb, y0, zb, u1, v1, light);
        vertex(consumer, pose, xa, y0, za, u0, v1, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    private static TextureAtlasSprite blockSprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    /** The swimming fish, then the floaters at the surface. */
    private void submitFish(State state, TankShape box, PoseStack poseStack, SubmitNodeCollector collector) {
        FishSpecies species = state.species;
        if (species == null) {
            return;
        }
        double frame = 2 * PX;
        FishPath.Bounds tank = new FishPath.Bounds(frame, BED_TOP, frame, box.width() - frame, box.height() - frame, box.depth() - frame);
        boolean flopping = state.waterHeight < FLOP_BELOW;
        FishPath.Bounds water = tank.inside(FISH_MARGIN, state.waterHeight);
        for (int i = 0; i < state.drawn; i++) {
            FishPath path = new FishPath(state.seed * 1_000_003L + i);
            if (flopping) {
                FishPath.Pose spot = path.at(state.swimTime * 0.1, tank.inside(FISH_MARGIN, box.height()));
                float hop = (float) Math.abs(Math.sin(state.gameTime * 0.3 + i * 1.7)) * 0.15F;
                drawFish(species, poseStack, collector, spot.x(), BED_TOP + 0.08 + hop, spot.z(), spot.yaw(), 0, false, false,
                        state.gameTime, state.light);
            } else {
                FishPath.Pose pose = path.at(state.swimTime, water);
                drawFish(species, poseStack, collector, pose.x(), pose.y(), pose.z(), pose.yaw(),
                        Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pose.pitch())), true, false, (float) state.swimTime, state.light);
            }
        }
        double surface = Math.max(BED_TOP + 0.1, state.waterHeight - 0.08);
        for (int i = 0; i < state.floaters; i++) {
            FishPath path = new FishPath(state.seed * 1_000_003L + 1000 + i);
            FishPath.Pose drift = path.at(state.floatingFor * 0.2, tank.inside(FISH_MARGIN, box.height()));
            drawFish(species, poseStack, collector, drift.x(), surface, drift.z(), drift.yaw(), 0, true, true, 0, state.light);
        }
    }

    private void drawFish(FishSpecies species, PoseStack poseStack, SubmitNodeCollector collector, double x, double y, double z,
                          double yaw, double pitch, boolean inWater, boolean bellyUp, float age, int light) {
        LivingEntityRenderState fish = species == FishSpecies.SALMON ? new SalmonRenderState() : new LivingEntityRenderState();
        fish.ageInTicks = age;
        fish.isInWater = inWater;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotation((float) (Math.PI + yaw)));
        poseStack.mulPose(Axis.XP.rotation((float) -pitch));
        if (bellyUp) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        } else if (!inWater) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        }
        poseStack.translate(0, -MODEL_CENTRE, 0);
        poseStack.scale(-1, -1, 1);
        poseStack.translate(0, -1.501F, 0);
        if (species == FishSpecies.SALMON) {
            submit(salmon, (SalmonRenderState) fish, SALMON_TEXTURE, poseStack, collector, light);
        } else {
            submit(cod, fish, COD_TEXTURE, poseStack, collector, light);
        }
        poseStack.popPose();
    }

    private static <S extends LivingEntityRenderState> void submit(EntityModel<S> model, S fish, Identifier texture, PoseStack poseStack,
                                                                  SubmitNodeCollector collector, int light) {
        collector.submitModel(model, fish, poseStack, model.renderType(texture), light, OverlayTexture.NO_OVERLAY, 0, null);
    }
}
