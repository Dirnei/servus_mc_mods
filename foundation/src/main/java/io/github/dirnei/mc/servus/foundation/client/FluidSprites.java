package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/** The still sprite and tint of a fluid for GUIs and renderers, read from its baked fluid model. */
public final class FluidSprites {

    private FluidSprites() {
    }

    private static FluidModel model(FluidStack fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    }

    public static TextureAtlasSprite still(FluidStack fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    /** ARGB tint of the fluid as it shows in the world at that position (biome water colour), opaque. */
    public static int tint(BlockAndTintGetter level, BlockPos pos, FluidStack fluid) {
        FluidTintSource source = model(fluid).fluidTintSource();
        if (source == null) {
            return 0xFFFFFFFF;
        }
        FluidState state = fluid.getFluid().defaultFluidState();
        return Argb.opaque(source.colorInWorld(state, state.createLegacyBlock(), level, pos));
    }

    /** ARGB tint of the fluid as a stack, opaque white when it has none. */
    public static int tint(FluidStack fluid) {
        FluidTintSource source = model(fluid).fluidTintSource();
        return source == null ? 0xFFFFFFFF : Argb.opaque(source.colorAsStack(fluid));
    }
}
