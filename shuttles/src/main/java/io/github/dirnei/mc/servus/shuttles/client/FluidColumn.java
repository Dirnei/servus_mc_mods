package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws fluid held in a tank as a translucent box with the fluid's still
 * texture, its tint at a block and at least its own glow; shared by the
 * fluid stations' sight glasses and the Fluid Rack's glass front.
 */
final class FluidColumn {

    private FluidColumn() {
    }

    /**
     * One box of the fluid with the faces in {@code faces} (bits by {@code
     * Direction} ordinal), tinted as at {@code tintAt} and lit as at {@code lightAt}.
     */
    static void box(Level level, BlockPos tintAt, BlockPos lightAt, FluidStack fluid, FluidBox box, int faces, PoseStack poseStack,
                    SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int color = level instanceof BlockAndTintGetter tinted ? FluidSprites.tint(tinted, tintAt, fluid) : FluidSprites.tint(fluid);
        int light = glowing(LevelRenderer.getLightCoords(level, lightAt), fluid);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                (pose, consumer) -> box.draw(consumer, pose, faces, sprite, color, light));
    }

    /** The light at a block, at least the fluid's own glow. */
    static int glowing(int packedLight, FluidStack fluid) {
        int glow = fluid.getFluidType().getLightLevel(fluid);
        return LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(packedLight), glow), LightCoordsUtil.sky(packedLight));
    }
}
