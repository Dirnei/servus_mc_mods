package io.github.dirnei.mc.servus.alchemy.client.gnome;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeAction;
import io.github.dirnei.mc.servus.alchemy.gnome.GnomeKind;
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
 * A gnome after the elementals sheet: boots and trousers, a coat with belt, a head with nose, beard and moustache under a
 * red hat of six stepped segments that lean back, and arms on pivots. The Old Miner holds a pickaxe, the Lantern Bearer a
 * lantern, the House Gnome a Salt Crystal and a patch on his hat. Model space has y down and the face at negative z; the
 * texture layout is drawn by {@code tools/gnome-textures/gen.py}.
 */
public class GnomeModel extends EntityModel<GnomeRenderState> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome"), "main");

    /** Each kind blinks at its own moment of the same period. */
    private static final float BLINK_PERIOD = 68;
    private static final float BLINK_TICKS = 3;
    private static final float WALK_LEG_SWING = 1.0F;
    private static final float WALK_ARM_SWING = 0.7F;

    private final ModelPart head;
    private final ModelPart eyelids;
    private final ModelPart patch;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart pickaxe;
    private final ModelPart crystal;
    private final ModelPart lantern;

    public GnomeModel(ModelPart root) {
        super(root);
        ModelPart gnome = root.getChild("gnome");
        head = gnome.getChild("head");
        eyelids = head.getChild("eyelids");
        patch = head.getChild("patch");
        rightArm = gnome.getChild("right_arm");
        leftArm = gnome.getChild("left_arm");
        rightLeg = gnome.getChild("right_leg");
        leftLeg = gnome.getChild("left_leg");
        pickaxe = rightArm.getChild("pickaxe");
        crystal = rightArm.getChild("crystal");
        lantern = leftArm.getChild("lantern");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition gnome = mesh.getRoot().addOrReplaceChild("gnome", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        gnome.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(24, 10).addBox(-3, -9.5F, -2, 6, 5, 4)
                .texOffs(0, 23).addBox(-3.15F, -6.15F, -2.15F, 6.3F, 1, 4.3F), PartPose.ZERO);

        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1, 0, -1, 2, 3, 2)
                .texOffs(28, 31).addBox(-1.2F, 3.05F, -1.8F, 2.4F, 1.5F, 3);
        gnome.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.3F, -4.55F, 0));
        gnome.addOrReplaceChild("left_leg", leg, PartPose.offset(1.3F, -4.55F, 0));

        addHead(gnome);

        CubeListBuilder arm = CubeListBuilder.create()
                .texOffs(40, 31).addBox(-1, 0, -1, 2, 4.2F, 2)
                .texOffs(56, 0).addBox(-0.9F, 4.25F, -0.9F, 1.8F, 1.5F, 1.8F);
        PartDefinition rightArm = gnome.addOrReplaceChild("right_arm", arm, PartPose.offset(-4.05F, -9.3F, 0));
        PartDefinition leftArm = gnome.addOrReplaceChild("left_arm", arm, PartPose.offset(4.05F, -9.3F, 0));
        addPickaxe(rightArm);
        rightArm.addOrReplaceChild("crystal", CubeListBuilder.create()
                .texOffs(26, 40).addBox(-0.9F, -2.7F, -0.9F, 1.8F, 3, 1.8F), PartPose.offsetAndRotation(0, 5.8F, -0.6F, 0.3F, -0.785F, -0.2F));
        addLantern(leftArm);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addHead(PartDefinition gnome) {
        PartDefinition head = gnome.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(26, 0).addBox(-3, -5, -2.5F, 6, 5, 5)
                .texOffs(48, 5).addBox(-0.8F, -2.9F, -3.65F, 1.6F, 1.6F, 1.2F)
                .texOffs(44, 10).addBox(-3.2F, -1.5F, -3.95F, 6.4F, 4.5F, 1.4F)
                .texOffs(44, 17).addBox(-2, -1.9F, -4.6F, 4, 1, 0.6F)
                .texOffs(0, 0).addBox(-3.5F, -6.02F, -3, 7, 1, 6)
                .texOffs(24, 23).addBox(-3, -8.2F, -2.5F, 6, 2.2F, 5)
                .texOffs(46, 23).addBox(-2.3F, -10.35F, -1.6F, 5, 2.2F, 4)
                .texOffs(0, 31).addBox(-1.5F, -12.3F, -0.7F, 4, 2, 3.2F)
                .texOffs(16, 31).addBox(-0.6F, -14.25F, 0.3F, 3, 2, 2.4F)
                .texOffs(56, 4).addBox(0.35F, -16, 1.3F, 2, 1.8F, 1.8F)
                .texOffs(0, 40).addBox(1.4F, -17.55F, 2.4F, 1.2F, 1.6F, 1.2F), PartPose.offset(0, -9.45F, 0));
        head.addOrReplaceChild("eyelids", CubeListBuilder.create()
                .texOffs(54, 17).addBox(-2, -3, -2.6F, 1, 1, 0.15F)
                .texOffs(54, 17).addBox(1, -3, -2.6F, 1, 1, 0.15F), PartPose.ZERO);
        head.addOrReplaceChild("patch", CubeListBuilder.create()
                .texOffs(58, 17).addBox(0, -10.05F, -1.92F, 1.6F, 1.6F, 0.3F), PartPose.ZERO);
    }

    private static void addPickaxe(PartDefinition arm) {
        arm.addOrReplaceChild("pickaxe", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-0.45F, -0.45F, -10, 0.9F, 0.9F, 11)
                .texOffs(48, 31).addBox(-0.55F, -3.5F, -10.35F, 1.1F, 7, 1.1F)
                .texOffs(8, 40).addBox(-0.5F, -3.9F, -9.85F, 1, 1.2F, 1.4F)
                .texOffs(8, 40).addBox(-0.5F, 2.7F, -9.85F, 1, 1.2F, 1.4F), PartPose.offset(0, 5, -0.4F));
    }

    private static void addLantern(PartDefinition arm) {
        CubeListBuilder lantern = CubeListBuilder.create()
                .texOffs(24, 19).addBox(-0.2F, 0, -0.2F, 0.4F, 1.65F, 0.4F)
                .texOffs(14, 40).addBox(-1.3F, 1.6F, -1.3F, 2.6F, 0.5F, 2.6F)
                .texOffs(14, 40).addBox(-1.3F, 4.65F, -1.3F, 2.6F, 0.5F, 2.6F)
                .texOffs(56, 31).addBox(-0.8F, 2.2F, -0.8F, 1.6F, 2.4F, 1.6F);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                lantern.texOffs(28, 19).addBox(sx * 1.05F - 0.225F, 2.05F, sz * 1.05F - 0.225F, 0.45F, 2.65F, 0.45F);
            }
        }
        arm.addOrReplaceChild("lantern", lantern, PartPose.offset(0, 5.6F, -0.2F));
    }

    @Override
    public void setupAnim(GnomeRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        eyelids.visible = (age + blinkPhase(state.kind)) % BLINK_PERIOD < BLINK_TICKS;

        float swing = state.walkAnimationPos * 0.6662F;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        rightLeg.xRot = Mth.cos(swing) * WALK_LEG_SWING * speed;
        leftLeg.xRot = Mth.cos(swing + Mth.PI) * WALK_LEG_SWING * speed;
        rightArm.xRot = Mth.cos(swing + Mth.PI) * WALK_ARM_SWING * speed;
        leftArm.xRot = Mth.cos(swing) * WALK_ARM_SWING * speed;

        pickaxe.visible = state.kind == GnomeKind.MINER;
        lantern.visible = state.kind == GnomeKind.LANTERN;
        crystal.visible = state.kind == GnomeKind.HOUSE;
        patch.visible = state.kind == GnomeKind.HOUSE;
        switch (state.kind) {
            case MINER -> poseMiner(state.action, age);
            case LANTERN -> poseLantern(age);
            case HOUSE -> poseHouse(age);
        }
    }

    private void poseMiner(GnomeAction action, float age) {
        switch (action) {
            case DIG -> {
                rightArm.xRot = -0.5F - 0.9F * Math.max(0, Mth.sin(age / 6));
                leftArm.xRot = 0.12F * Mth.sin(age / 12);
            }
            case TAP -> rightArm.xRot = -0.6F - 0.35F * Math.max(0, Mth.sin(age * 0.6F));
            case IDLE -> rightArm.xRot = -0.35F + rightArm.xRot * 0.5F;
        }
    }

    /** The lantern arm stays forward; the lantern hangs from it and sways. */
    private void poseLantern(float age) {
        leftArm.xRot = -0.55F;
        lantern.xRot = 0.55F + 0.12F * Mth.sin(age / 10);
        lantern.zRot = 0.12F * Mth.sin(age / 14);
    }

    /** The Salt Crystal held up and turning; he keeps looking around. */
    private void poseHouse(float age) {
        rightArm.xRot = -0.9F;
        crystal.yRot = -0.785F + age / 28;
        head.yRot += 0.5F * Mth.sin(age / 30);
    }

    private static float blinkPhase(GnomeKind kind) {
        return switch (kind) {
            case MINER -> 0;
            case LANTERN -> 34;
            case HOUSE -> 50;
        };
    }
}
