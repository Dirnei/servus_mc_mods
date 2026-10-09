package io.github.dirnei.mc.servus.manufacturing.integration.jei;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

/** Shows Manufacturing's recipes in JEI. Only loaded when JEI is present. */
@JeiPlugin
public final class ServusManufacturingJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
}
