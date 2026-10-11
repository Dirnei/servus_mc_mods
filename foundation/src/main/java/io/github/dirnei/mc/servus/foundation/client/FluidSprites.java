package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The still sprite and tint of a fluid for GUIs and renderers. Since 26.1 they
 * come from the baked fluid model instead of the client fluid type extensions.
 */
public final class FluidSprites {

    private FluidSprites() {
    }

    private static FluidModel model(FluidStack fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    }

    public static TextureAtlasSprite still(FluidStack fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    public static TextureAtlasSprite flowing(FluidStack fluid) {
        return model(fluid).flowingMaterial().sprite();
    }

    /** ARGB tint of the fluid as a stack, opaque white when it has none. */
    public static int tint(FluidStack fluid) {
        FluidTintSource source = model(fluid).fluidTintSource();
        int tint = source == null ? 0xFFFFFFFF : source.colorAsStack(fluid);
        return (tint >>> 24) == 0 ? tint | 0xFF000000 : tint;
    }
}
