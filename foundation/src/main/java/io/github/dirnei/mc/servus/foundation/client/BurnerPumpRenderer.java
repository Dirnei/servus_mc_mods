package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamStroke;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Rocks the Burner Pump's beam while it burns and moves the pump rod and the
 * piston rod with the beam's ends. Drawn from the lower half; the beam keeps
 * its pose while the pump is unlit.
 */
public class BurnerPumpRenderer implements BlockEntityRenderer<BurnerPumpBlockEntity> {

    public static final ModelResourceLocation BEAM_MODEL = model("burner_pump_beam");
    public static final ModelResourceLocation ROD_MODEL = model("burner_pump_rod");
    public static final ModelResourceLocation PISTON_MODEL = model("burner_pump_piston");

    /** Look only: twelve degrees either way, one stroke every two seconds. */
    private static final BeamStroke STROKE = new BeamStroke(12, 40);

    private final BlockEntityRendererProvider.Context context;

    public BurnerPumpRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/" + name));
    }

    @Override
    public void render(BurnerPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = pump.getBlockState();
        float angle = STROKE.angle(advance(pump, state, partialTick));
        int light = pump.getLevel() == null ? packedLight : LevelRenderer.getLightColor(pump.getLevel(), pump.getBlockPos().above());
        Direction facing = state.getValue(MachineBlock.FACING);
        float pivotX = BurnerPumpParts.PIVOT_X / 16.0F;
        float pivotY = BurnerPumpParts.PIVOT_Y / 16.0F;

        poseStack.pushPose();
        // The models are built facing north; turn like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float rod = BeamStroke.rodOffset(angle, BurnerPumpParts.ROD_X - BurnerPumpParts.PIVOT_X) / 16.0F;
        draw(ROD_MODEL, state, poseStack, buffers, light, packedOverlay, 0, rod);

        poseStack.translate(0.0F, 1.0F, 0.0F);
        float piston = BeamStroke.rodOffset(angle, BurnerPumpParts.PISTON_X - BurnerPumpParts.PIVOT_X) / 16.0F;
        draw(PISTON_MODEL, state, poseStack, buffers, light, packedOverlay, 0, piston);

        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-pivotX, -pivotY, 0.0F);
        draw(BEAM_MODEL, state, poseStack, buffers, light, packedOverlay, 0, 0);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void draw(ModelResourceLocation location, BlockState state, PoseStack poseStack, MultiBufferSource buffers,
                      int light, int overlay, float dx, float dy) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        poseStack.pushPose();
        poseStack.translate(dx, dy, 0.0F);
        context.getBlockRenderDispatcher().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(RenderType.cutout()), state, model, 1.0F, 1.0F, 1.0F, light, overlay);
        poseStack.popPose();
    }

    /** Both halves, so the beam stays drawn while only the upper block is in view. */
    @Override
    public AABB getRenderBoundingBox(BurnerPumpBlockEntity pump) {
        return new AABB(pump.getBlockPos()).expandTowards(0, 1, 0);
    }

    /** Advances the stroke while lit and keeps the pose while unlit. */
    private static double advance(BurnerPumpBlockEntity pump, BlockState state, float partialTick) {
        if (pump.getLevel() == null) {
            return pump.beamPhase;
        }
        double now = pump.getLevel().getGameTime() + (double) partialTick;
        double elapsed = pump.beamTime < 0 ? 0 : Math.max(0, now - pump.beamTime);
        pump.beamTime = now;
        if (state.getValue(MachineBlock.LIT)) {
            pump.beamPhase += elapsed;
        }
        return pump.beamPhase;
    }
}
