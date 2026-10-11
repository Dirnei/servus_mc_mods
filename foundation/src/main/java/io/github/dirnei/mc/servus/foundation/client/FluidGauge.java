package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A vertical fluid gauge for GUIs: a dark frame filled from the bottom with the
 * fluid's still texture, tiled and tinted, plus an amount and capacity tooltip.
 * Positions are absolute screen coordinates.
 */
public final class FluidGauge {

    private static final int TILE = 16;

    private final int width;
    private final int height;
    private int x;
    private int y;

    public FluidGauge(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** Places the gauge at absolute screen coordinates. */
    public void place(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void render(GuiGraphics graphics, FluidStack fluid, int capacity) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF373737);
        graphics.fill(x, y, x + width, y + height, 0xFF1E1E1E);
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int filled = (int) Math.min(height, Math.max(1, (long) fluid.getAmount() * height / capacity));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        Identifier still = extensions.getStillTexture(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint = extensions.getTintColor(fluid);
        float alpha = ((tint >>> 24) & 0xFF) / 255F;
        float red = ((tint >> 16) & 0xFF) / 255F;
        float green = ((tint >> 8) & 0xFF) / 255F;
        float blue = (tint & 0xFF) / 255F;
        int top = y + height - filled;
        // Whole tiles from the bottom up, clipped to the filled part.
        graphics.enableScissor(x, top, x + width, y + height);
        for (int tileY = y + height - TILE; tileY > top - TILE; tileY -= TILE) {
            for (int tileX = x; tileX < x + width; tileX += TILE) {
                graphics.blit(tileX, tileY, 0, TILE, TILE, sprite, red, green, blue, alpha == 0 ? 1F : alpha);
            }
        }
        graphics.disableScissor();
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Shows the fluid name with amount and capacity while the mouse is over the gauge. */
    public void renderTooltip(GuiGraphics graphics, Font font, FluidStack fluid, int capacity, int mouseX, int mouseY) {
        if (!isHovering(mouseX, mouseY)) {
            return;
        }
        Component amount = Component.literal(fluid.getAmount() + " / " + capacity + " mB");
        if (fluid.isEmpty()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.servus_foundation.fluid_gauge.empty"), amount), mouseX, mouseY);
        } else {
            graphics.renderComponentTooltip(font, java.util.List.of(fluid.getHoverName(), amount), mouseX, mouseY);
        }
    }
}
