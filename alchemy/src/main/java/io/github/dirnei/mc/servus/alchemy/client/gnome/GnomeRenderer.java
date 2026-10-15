package io.github.dirnei.mc.servus.alchemy.client.gnome;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.gnome.Gnome;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeKind;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Draws a gnome full bright with the texture of his kind, at the sheet's scale. Gnomes are drawn only for a player with
 * Elemental Sight; skipping them here also skips their shadow and name.
 */
public class GnomeRenderer extends MobRenderer<Gnome, GnomeRenderState, GnomeModel> {

    private static final Map<GnomeKind, Identifier> TEXTURES = new EnumMap<>(GnomeKind.class);
    private static final float SHADOW_RADIUS = 0.3F;
    private static final float SCALE = 0.8F;

    static {
        for (GnomeKind kind : GnomeKind.values()) {
            TEXTURES.put(kind, Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID,
                    "textures/entity/gnome/" + kind.serializedName() + ".png"));
        }
    }

    public GnomeRenderer(EntityRendererProvider.Context context) {
        super(context, new GnomeModel(context.bakeLayer(GnomeModel.LAYER)), SHADOW_RADIUS);
    }

    @Override
    public boolean shouldRender(Gnome entity, Frustum culler, double camX, double camY, double camZ) {
        return ElementalSight.active(Minecraft.getInstance().player) && super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override
    protected int getBlockLightLevel(Gnome entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected void scale(GnomeRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public Identifier getTextureLocation(GnomeRenderState state) {
        return TEXTURES.get(state.kind);
    }

    @Override
    public GnomeRenderState createRenderState() {
        return new GnomeRenderState();
    }

    @Override
    public void extractRenderState(Gnome entity, GnomeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.kind();
        state.action = entity.action();
    }
}
