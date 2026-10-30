package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixerBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.mixer.MixerParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Turns the Mixer's paddle about the vertical axis from the flywheel its block entity runs on the client: smooth while
 * driven, coasting to a stop after. Draws the contents inside the tub as one surface: the tank's fluid, else the dry mix
 * while the inputs hold items, else nothing. Also draws the side markers.
 */
public class MixerRenderer implements BlockEntityRenderer<MixerBlockEntity, MixerRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> PADDLE_MODEL = ModelKeys.key("block/mixer_paddle");
    private static final Identifier MIX = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/mixer_mix");
    private static final FluidBox CONTENTS = new FluidBox(MixerParts.CONTENTS[0] / 16, MixerParts.CONTENTS[1] / 16,
            MixerParts.CONTENTS[2] / 16, MixerParts.CONTENTS[3] / 16, MixerParts.CONTENTS[4] / 16, MixerParts.CONTENTS[5] / 16);

    private final SideMarkerRenderer<MixerBlockEntity> markers;

    /** The paddle's angle of one frame, the contents surface and the side markers. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State markers = new SideMarkerRenderer.State();
        float angle;
        FluidStack fluid = FluidStack.EMPTY;
        int fluidTint;
        boolean mix;
    }

    public MixerRenderer(BlockEntityRendererProvider.Context context) {
        markers = new SideMarkerRenderer<>(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MixerBlockEntity mixer, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mixer, state, partialTick, cameraPosition, breakProgress);
        markers.extractRenderState(mixer, state.markers, partialTick, cameraPosition, breakProgress);
        state.angle = mixer.paddle().angle(partialTick);
        state.fluid = mixer.shownFluid();
        state.mix = mixer.showsMix();
        if (!state.fluid.isEmpty()) {
            state.fluidTint = mixer.getLevel() instanceof ClientLevel level
                    ? FluidSprites.tint(level, mixer.getBlockPos(), state.fluid) : FluidSprites.tint(state.fluid);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        markers.submit(state.markers, poseStack, collector, camera);
        int light = state.lightCoords;
        ModelPoses.rotateAbout(poseStack, MixerParts.AXIS_X, 0, MixerParts.AXIS_Z, Axis.YP, state.angle,
                () -> ModelKeys.submit(PADDLE_MODEL, poseStack, collector, light));
        if (!state.fluid.isEmpty()) {
            surface(poseStack, collector, Sheets.translucentBlockSheet(), FluidSprites.still(state.fluid), state.fluidTint, light);
        } else if (state.mix) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MIX);
            surface(poseStack, collector, Sheets.cutoutBlockSheet(), sprite, 0xFFFFFFFF, light);
        }
    }

    private static void surface(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, TextureAtlasSprite sprite,
                                int color, int light) {
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, consumer) -> CONTENTS.quad(consumer, pose, Direction.UP, sprite, color, light));
    }
}
