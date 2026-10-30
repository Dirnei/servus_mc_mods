package io.github.dirnei.mc.servus.alchemy.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a formed Calciner holds: the matter in the crucible and the ash heap in the Ash Pan by how full it is. While
 * the Calciner works, Mercury drips from the Receiver's tap into the basin, and a puddle lies there while the Mercury tank
 * holds fluid. Ignis Perpetuus set into the Firebox glows behind its grate. The tanks show their own level. The rest of the structure is
 * chunk baked from block models.
 */
public final class CalcinerRenderer extends BufferedBlockEntityRenderer<CalcinerBlockEntity> {

    /** A box in a cell's model space (sixteenths, front facing north) and the height its fill rises by. */
    private record Fill(Role role, float x0, float y0, float z0, float x1, float rise, float z1) {
    }

    /** The Salt lies in the Ash Pan as ash: light grey powder, tiled like a block. */
    private static final Identifier ASH = Identifier.withDefaultNamespace("block/light_gray_concrete_powder");

    private static final Fill ASH_HEAP = new Fill(Role.ASH_PAN, 4, 3, 0.5F, 12, 4, 3);

    /** Ticks a drop takes from the Receiver's tap into the basin. */
    private static final int DRIP_TICKS = 16;
    /** Where a drop starts and where the puddle lies in the basin, in sixteenths of the basin's Stone Bricks. */
    private static final float DRIP_TOP = 16;
    private static final float PUDDLE_BOTTOM = 12.1F;
    private static final float PUDDLE_TOP = 12.7F;

    private static final Supplier<ItemStack> IGNIS = Suppliers.memoize(() -> new ItemStack(ServusAlchemyItems.IGNIS_PERPETUUS.get()));
    private static final float IGNIS_SCALE = 6 / 16F;
    /** Height of the Ignis Perpetuus' centre in the Firebox, in sixteenths. */
    private static final float IGNIS_CENTRE = 8.05F;

    public CalcinerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(CalcinerBlockEntity calciner, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        if (!calciner.formed()) {
            return;
        }
        Direction facing = calciner.getBlockState().getValue(MachineBlock.FACING);
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        renderAsh(calciner, facing, poseStack, consumer, packedLight);
        renderBasin(calciner, facing, partialTick, poseStack, consumer, packedLight);
        renderInput(calciner, facing, poseStack, buffers, packedLight);
        renderIgnis(calciner, facing, poseStack, buffers);
    }

    /**
     * The eternal flame standing on the coals behind the Firebox grate, glowing at full brightness. The item is 6 px
     * tall and less than half a pixel thick, centred 2 px behind the front: clear of the grate plane (1 px), the back
     * wall (3 px) and the top of the coals (5 px, the item starts at 5.05 px).
     */
    private static void renderIgnis(CalcinerBlockEntity calciner, Direction facing, PoseStack poseStack, SubmitBuffers buffers) {
        if (!calciner.hasIgnis()) {
            return;
        }
        Offset offset = CalcinerLayout.worldOffset(CalcinerLayout.part(Role.FIREBOX), CalcinerStructure.facing(facing));
        float[] spot = rotate(8 / 16F, 2 / 16F, 8 / 16F, 2 / 16F, facing);
        poseStack.pushPose();
        poseStack.translate(offset.x() + spot[0], offset.y() + IGNIS_CENTRE / 16, offset.z() + spot[1]);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.scale(IGNIS_SCALE, IGNIS_SCALE, IGNIS_SCALE);
        RenderSubmit.item(poseStack, buffers.collector(), IGNIS.get(), ItemDisplayContext.FIXED, calciner.getLevel(),
                (int) calciner.getBlockPos().asLong(), LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
    }

    /** The ash heap in the Ash Pan, rising with the Salt it holds out of its room. */
    private static void renderAsh(CalcinerBlockEntity calciner, Direction facing, PoseStack poseStack, VertexConsumer consumer,
                                  int light) {
        if (calciner.salt() <= 0) {
            return;
        }
        float share = Math.min(1.0F, calciner.salt() / (float) Math.max(1, calciner.saltRoom()));
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ASH);
        Fill fill = ASH_HEAP;
        Offset offset = CalcinerLayout.worldOffset(CalcinerLayout.part(fill.role()), CalcinerStructure.facing(facing));
        float[] box = rotate(fill.x0() / 16, fill.z0() / 16, fill.x1() / 16, fill.z1() / 16, facing);
        float y0 = fill.y0() / 16;
        float y1 = y0 + fill.rise() / 16 * share;
        poseStack.pushPose();
        poseStack.translate(offset.x(), offset.y(), offset.z());
        drawBox(consumer, poseStack.last(), box[0], y0, box[1], box[2], y1, box[3], sprite, 0xFFFFFFFF, light);
        poseStack.popPose();
    }

    /** The falling drop while the Receiver is lit and the puddle while the Mercury tank holds fluid, in the basin's Stone Bricks. */
    private static void renderBasin(CalcinerBlockEntity calciner, Direction facing, float partialTick, PoseStack poseStack,
                                    VertexConsumer consumer, int light) {
        Level level = calciner.getLevel();
        if (level == null) {
            return;
        }
        BlockPos controller = calciner.getBlockPos();
        BlockState receiver = level.getBlockState(CalcinerStructure.pos(controller, CalcinerLayout.part(Role.RECEIVER), CalcinerStructure.facing(facing)));
        boolean dripping = receiver.getBlock() instanceof CalcinerPartBlock && receiver.getValue(CalcinerPartBlock.LIT);
        FluidStack puddle = level.getBlockEntity(CalcinerStructure.pos(controller, CalcinerLayout.cell(CalcinerLayout.MERCURY_TANK),
                CalcinerStructure.facing(facing))) instanceof FluidTankBlockEntity tank ? tank.rawShare() : FluidStack.EMPTY;
        if (!dripping && puddle.isEmpty()) {
            return;
        }
        Offset basin = CalcinerLayout.worldOffset(CalcinerLayout.cells(Role.STONE_BRICKS).getLast(), CalcinerStructure.facing(facing));
        poseStack.pushPose();
        poseStack.translate(basin.x(), basin.y(), basin.z());
        if (dripping) {
            FluidStack mercury = new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 1);
            float phase = ((level.getGameTime() % DRIP_TICKS) + partialTick) / DRIP_TICKS;
            // Falls like a drop: slow at the tap, fast into the basin.
            float top = DRIP_TOP - (DRIP_TOP - PUDDLE_TOP) * phase * phase;
            float[] box = rotate(7.5F / 16, 7.5F / 16, 8.5F / 16, 8.5F / 16, facing);
            drawBox(consumer, poseStack.last(), box[0], (top - 1) / 16, box[1], box[2], top / 16, box[3],
                    FluidSprites.still(mercury), FluidSprites.tint(mercury), light);
        }
        if (!puddle.isEmpty()) {
            float[] box = rotate(3.05F / 16, 3.05F / 16, 12.95F / 16, 12.95F / 16, facing);
            drawBox(consumer, poseStack.last(), box[0], PUDDLE_BOTTOM / 16, box[1], box[2], PUDDLE_TOP / 16, box[3],
                    FluidSprites.still(puddle), FluidSprites.tint(puddle), light);
        }
        poseStack.popPose();
    }

    /** The matter being calcined, standing in the crucible of the mouth. */
    private static void renderInput(CalcinerBlockEntity calciner, Direction facing, PoseStack poseStack, SubmitBuffers buffers, int light) {
        ItemStack input = calciner.shownInput();
        if (input.isEmpty()) {
            return;
        }
        float[] spot = rotate(8 / 16F, 5 / 16F, 8 / 16F, 5 / 16F, facing);
        poseStack.pushPose();
        poseStack.translate(spot[0], 10 / 16F, spot[1]);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        RenderSubmit.item(poseStack, buffers.collector(), input, ItemDisplayContext.FIXED, calciner.getLevel(), (int) calciner.getBlockPos().asLong(), light);
        poseStack.popPose();
    }

    /**
     * Turns a box drawn for a front facing north to {@code facing}, the way the blockstate turns
     * the model: clockwise seen from above, a quarter per step.
     */
    static float[] rotate(float x0, float z0, float x1, float z1, Direction facing) {
        int steps = (((int) facing.toYRot() + 180) % 360) / 90;
        for (int i = 0; i < steps; i++) {
            float nx0 = 1 - z1;
            float nx1 = 1 - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
        }
        return new float[] {x0, z0, x1, z1};
    }

    /**
     * Draws a box inside one block, the sprite tiled to the size of each face as a block model would,
     * so a low heap or a shallow liquid shows a slice of the texture instead of the whole texture squashed.
     */
    static void drawBox(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                        TextureAtlasSprite sprite, int color, int light) {
        drawFaces(consumer, pose, x0, y0, z0, x1, y1, z1, sprite, color, light, EnumSet.allOf(Direction.class));
    }

    /** Like {@link #drawBox}, drawing only the faces in {@code faces}. */
    static void drawFaces(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                          TextureAtlasSprite sprite, int color, int light, Set<Direction> faces) {
        float top = sprite.getV(1 - y1);
        float bottom = sprite.getV(1 - y0);
        if (faces.contains(Direction.UP)) {
            quad(consumer, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                    sprite.getU(x0), sprite.getV(z0), sprite.getU(x1), sprite.getV(z1), color, light, Direction.UP);
        }
        if (faces.contains(Direction.DOWN)) {
            quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
                    sprite.getU(z0), sprite.getV(x0), sprite.getU(z1), sprite.getV(x1), color, light, Direction.DOWN);
        }
        if (faces.contains(Direction.NORTH)) {
            quad(consumer, pose, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0,
                    sprite.getU(1 - x1), top, sprite.getU(1 - x0), bottom, color, light, Direction.NORTH);
        }
        if (faces.contains(Direction.SOUTH)) {
            quad(consumer, pose, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1,
                    sprite.getU(x0), top, sprite.getU(x1), bottom, color, light, Direction.SOUTH);
        }
        if (faces.contains(Direction.WEST)) {
            quad(consumer, pose, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1,
                    sprite.getU(z0), top, sprite.getU(z1), bottom, color, light, Direction.WEST);
        }
        if (faces.contains(Direction.EAST)) {
            quad(consumer, pose, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0,
                    sprite.getU(1 - z1), top, sprite.getU(1 - z0), bottom, color, light, Direction.EAST);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, int color, int light, Direction face) {
        vertex(consumer, pose, ax, ay, az, u0, v0, color, light, face);
        vertex(consumer, pose, bx, by, bz, u0, v1, color, light, face);
        vertex(consumer, pose, cx, cy, cz, u1, v1, color, light, face);
        vertex(consumer, pose, dx, dy, dz, u1, v0, color, light, face);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light, Direction face) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }

    @Override
    public AABB getRenderBoundingBox(CalcinerBlockEntity calciner) {
        BlockPos pos = calciner.getBlockPos();
        return new AABB(pos.offset(-1, -2, -1).getCenter(), pos.offset(1, 2, 1).getCenter()).inflate(0.5);
    }
}
