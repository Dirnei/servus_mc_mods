package io.github.dirnei.mc.servus.alchemy.client.salamander;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.salamander.Salamander;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Draws the salamander full bright. A wild salamander is drawn only for a player with Elemental Sight; skipping it here
 * also skips its shadow and name.
 */
public class SalamanderRenderer extends MobRenderer<Salamander, SalamanderRenderState, SalamanderModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "textures/entity/salamander.png");
    private static final float SHADOW_RADIUS = 0.4F;

    public SalamanderRenderer(EntityRendererProvider.Context context) {
        super(context, new SalamanderModel(context.bakeLayer(SalamanderModel.LAYER)), SHADOW_RADIUS);
    }

    @Override
    public boolean shouldRender(Salamander entity, Frustum culler, double camX, double camY, double camZ) {
        return entity.shownTo(ElementalSight.active(Minecraft.getInstance().player))
                && super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override
    protected int getBlockLightLevel(Salamander entity, BlockPos pos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(SalamanderRenderState state) {
        return TEXTURE;
    }

    @Override
    public SalamanderRenderState createRenderState() {
        return new SalamanderRenderState();
    }

    @Override
    public void extractRenderState(Salamander entity, SalamanderRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.lying = entity.isInSittingPose();
    }
}
