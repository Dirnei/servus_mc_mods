package io.github.dirnei.mc.servus.alchemy.client.salamander;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A lizard of glowing coals: body with a glowing belly, head with snout and eyes, three tail segments that sway with a
 * flame at the tip, four legs and a crest of flickering flames. Model space has y down and the head at negative z.
 */
public class SalamanderModel extends EntityModel<SalamanderRenderState> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salamander"), "main");

    private static final String[] LEGS = {"front_right_leg", "front_left_leg", "back_right_leg", "back_left_leg"};
    /** Height of each crest flame from back to front, as a share of the full flame. */
    private static final float[] CREST = {0.5F, 0.7F, 0.9F, 1.0F, 0.85F, 0.65F, 0.5F};
    private static final float TAIL_SWAY = 0.35F;
    /** How far the body sinks while lying, in pixels. */
    private static final float LYING_DROP = 1.5F;
    private static final float LYING_SPREAD = 1.3F;

    private final ModelPart salamander;
    private final ModelPart head;
    private final ModelPart[] tail;
    private final ModelPart tailFlame;
    private final ModelPart[] legs;
    private final ModelPart[] crest;

    public SalamanderModel(ModelPart root) {
        super(root);
        salamander = root.getChild("salamander");
        head = salamander.getChild("head");
        ModelPart tail1 = salamander.getChild("tail1");
        ModelPart tail2 = tail1.getChild("tail2");
        ModelPart tail3 = tail2.getChild("tail3");
        tail = new ModelPart[] {tail1, tail2, tail3};
        tailFlame = tail3.getChild("tail_flame");
        legs = new ModelPart[LEGS.length];
        for (int i = 0; i < LEGS.length; i++) {
            legs[i] = salamander.getChild(LEGS[i]);
        }
        crest = new ModelPart[CREST.length];
        for (int i = 0; i < CREST.length; i++) {
            crest[i] = salamander.getChild("crest" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition salamander = mesh.getRoot().addOrReplaceChild("salamander", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        salamander.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -6, -5, 6, 4, 10)
                .texOffs(0, 14).addBox(-2.5F, -2.5F, -4.5F, 5, 1, 9), PartPose.ZERO);

        PartDefinition head = salamander.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-2.5F, -1.5F, -3, 5, 3, 4)
                .texOffs(50, 0).addBox(-1.5F, -0.75F, -5, 3, 2, 3)
                .texOffs(32, 7).addBox(2, -1, -1.5F, 1, 1, 1)
                .texOffs(32, 7).addBox(-3, -1, -1.5F, 1, 1, 1), PartPose.offset(0, -3.75F, -5));

        PartDefinition tail1 = salamander.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(28, 14).addBox(-2, -1.5F, -1, 4, 3, 5), PartPose.offset(0, -3.9F, 5));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(46, 14).addBox(-1.5F, -1, -1, 3, 2, 5), PartPose.offset(0, 0.25F, 4));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1, -1, -1, 2, 2, 5), PartPose.offset(0, 0.2F, 4));
        tail3.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                .texOffs(36, 7).addBox(-0.5F, -2, -0.5F, 1, 2, 1), PartPose.offset(0, -0.5F, 4));

        CubeListBuilder rightLeg = CubeListBuilder.create()
                .texOffs(14, 24).addBox(-1, 0, -1, 2, 3, 2)
                .texOffs(22, 24).addBox(-1.25F, 2.5F, -1.75F, 3, 1, 3);
        CubeListBuilder leftLeg = CubeListBuilder.create()
                .texOffs(14, 24).addBox(-1, 0, -1, 2, 3, 2)
                .texOffs(22, 24).addBox(-1.75F, 2.5F, -1.75F, 3, 1, 3);
        salamander.addOrReplaceChild(LEGS[0], rightLeg, PartPose.offset(3.6F, -3.5F, -3));
        salamander.addOrReplaceChild(LEGS[1], leftLeg, PartPose.offset(-3.6F, -3.5F, -3));
        salamander.addOrReplaceChild(LEGS[2], rightLeg, PartPose.offset(3.6F, -3.5F, 3));
        salamander.addOrReplaceChild(LEGS[3], leftLeg, PartPose.offset(-3.6F, -3.5F, 3));

        CubeListBuilder flame = CubeListBuilder.create().texOffs(40, 7).addBox(-0.5F, -4, -0.5F, 1, 4, 1);
        for (int i = 0; i < CREST.length; i++) {
            salamander.addOrReplaceChild("crest" + i, flame, PartPose.offset(0, -5.5F, 4.5F - 1.5F * i));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SalamanderRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        for (int i = 0; i < tail.length; i++) {
            tail[i].yRot = TAIL_SWAY * Mth.sin(age * 0.125F - 0.8F * i);
        }
        head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * 0.5F + 0.2F * Mth.sin(age * 0.055F);
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;

        int flicker = Mth.floor(age / 2.2F);
        for (int i = 0; i < crest.length; i++) {
            crest[i].yScale = CREST[i] * (0.65F + 0.55F * noise(i, flicker));
        }
        tailFlame.yScale = 0.7F + 0.6F * noise(9, flicker);

        if (state.lying) {
            salamander.y += LYING_DROP;
            for (int i = 0; i < legs.length; i++) {
                legs[i].zRot = i % 2 == 0 ? -LYING_SPREAD : LYING_SPREAD;
            }
        } else {
            float swing = state.walkAnimationPos * 0.6662F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            legs[0].xRot = Mth.cos(swing) * 1.2F * speed;
            legs[3].xRot = Mth.cos(swing) * 1.2F * speed;
            legs[1].xRot = Mth.cos(swing + Mth.PI) * 1.2F * speed;
            legs[2].xRot = Mth.cos(swing + Mth.PI) * 1.2F * speed;
        }
    }

    /** A steady pseudo random number from 0 to 1 for one flame and one flicker step. */
    private static float noise(int flame, int step) {
        return Mth.frac(Mth.sin(flame * 12.9898F + step * 78.233F) * 43758.547F);
    }
}
