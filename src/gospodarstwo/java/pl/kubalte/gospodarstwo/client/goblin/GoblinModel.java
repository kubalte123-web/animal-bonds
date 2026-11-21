package pl.kubalte.gospodarstwo.client.goblin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.goblin.Goblin;

/** A humanoid body (so armour and held items fit) with long pointed ears and a big nose. */
public class GoblinModel extends HumanoidModel<GoblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Gospodarstwo.id("goblin"), "main");
    private final ModelPart leftEar, rightEar;

    public GoblinModel(ModelPart root) {
        super(root);
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0F);
        PartDefinition head = mesh.getRoot().getChild("head");
        // ears: thin flaps that stick out sideways and a bit up, pointed by a smaller tip piece
        PartDefinition le = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 32).addBox(0F, -1.5F, -0.5F, 5, 3, 1)
                .texOffs(12, 32).addBox(5F, -1F, -0.5F, 2, 2, 1), PartPose.offsetAndRotation(4F, -4.5F, 0F, 0F, -0.3F, -0.35F));
        PartDefinition re = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 36).addBox(-5F, -1.5F, -0.5F, 5, 3, 1)
                .texOffs(12, 36).addBox(-7F, -1F, -0.5F, 2, 2, 1), PartPose.offsetAndRotation(-4F, -4.5F, 0F, 0F, 0.3F, 0.35F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(20, 32).addBox(-1F, -1F, -3F, 2, 3, 3),
                PartPose.offsetAndRotation(0F, -3.5F, -4F, 0.25F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Simple versions of the GeckoLib animations, for when GeckoLib is not installed. */
    void actions(GoblinRenderState s) {
        float a = s.actionAge, t = s.ageInTicks;
        float sw = Mth.sin(a * 0.35F);
        switch (s.action) {
            case Goblin.A_WINDUP -> {
                float k = Math.min(1F, a / 8F);
                rightArm.xRot = leftArm.xRot = -2.9F * k;
                body.xRot = 0.5F * k;
                head.xRot = -0.4F * k;
            }
            case Goblin.A_LEAP -> {
                rightArm.xRot = leftArm.xRot = -3.2F;
                rightLeg.xRot = 0.7F;
                leftLeg.xRot = -0.6F;
            }
            case Goblin.A_STRIKE -> {
                float k = Math.min(1F, a / 4F);
                rightArm.xRot = leftArm.xRot = -3.2F + 3.0F * k;
                body.xRot = 0.6F * k;
            }
            case Goblin.A_PUNCH -> rightArm.xRot = -1.6F * Mth.sin(Math.min(1F, a / 6F) * (float) Math.PI);
            case Goblin.A_DODGE_L, Goblin.A_DODGE_R -> {
                float k = Math.max(0F, 1F - a / 7F) * (s.action == Goblin.A_DODGE_L ? 1 : -1);
                body.zRot = 0.4F * k;
                rightArm.zRot = 1.0F * Math.abs(k);
                leftArm.zRot = -1.0F * Math.abs(k);
            }
            case Goblin.A_DODGE_B -> {
                body.xRot = -0.4F * Math.max(0F, 1F - a / 7F);
                rightArm.xRot = leftArm.xRot = -1.2F;
            }
            case Goblin.A_BLOCK -> {
                leftArm.xRot = -1.4F;
                leftArm.yRot = 0.5F;
                body.xRot = 0.25F;
            }
            case Goblin.A_DANCE1 -> {
                rightArm.zRot = 2.4F + 0.4F * Mth.sin(t * 0.6F);
                leftArm.zRot = -2.4F + 0.4F * Mth.sin(t * 0.6F);
                rightLeg.xRot = Math.min(0F, Mth.sin(t * 0.3F)) * 0.9F;
                leftLeg.xRot = Math.min(0F, -Mth.sin(t * 0.3F)) * 0.9F;
                head.zRot = 0.2F * Mth.sin(t * 0.3F);
            }
            case Goblin.A_DANCE2 -> {
                rightArm.zRot = 1.5F;
                leftArm.zRot = -1.5F;
                body.yRot = t * 0.2F % (float) (Math.PI * 2);
                head.zRot = 0.2F * Mth.sin(t * 0.4F);
            }
            case Goblin.A_DANCE3 -> {
                rightArm.xRot = leftArm.xRot = -1.3F;
                rightArm.yRot = 0.6F * Mth.sin(t * 0.5F);
                leftArm.yRot = -0.6F * Mth.sin(t * 0.5F);
                rightLeg.xRot = Math.min(0F, Mth.sin(t * 0.25F)) * 0.8F;
            }
            case Goblin.A_CHOP, Goblin.A_BUILD, Goblin.A_MINE -> {
                float k = (Mth.sin(a * 0.5F) + 1F) / 2F;
                rightArm.xRot = -2.6F + 2.0F * k;
                if (s.action == Goblin.A_CHOP) leftArm.xRot = rightArm.xRot;
                body.xRot = 0.3F * k;
            }
            case Goblin.A_HOE, Goblin.A_WORK, Goblin.A_CHURN -> {
                rightArm.xRot = leftArm.xRot = -1.0F + 0.4F * sw;
                body.xRot = 0.3F;
            }
            case Goblin.A_SHEAR, Goblin.A_PET -> {
                rightArm.xRot = -1.1F;
                rightArm.yRot = 0.3F * Mth.sin(t * 0.8F);
                head.xRot = 0.35F;
            }
            case Goblin.A_EAT -> {
                rightArm.xRot = -1.9F + 0.2F * Mth.sin(t * 0.9F);
                head.xRot = 0.2F;
            }
            case Goblin.A_WAVE -> rightArm.zRot = 2.6F + 0.35F * Mth.sin(t * 0.8F);
            case Goblin.A_CHEER -> {
                rightArm.zRot = 2.8F + 0.2F * Mth.sin(t);
                leftArm.zRot = -2.8F - 0.2F * Mth.sin(t);
            }
            case Goblin.A_SULK -> {
                head.xRot = 0.6F;
                body.xRot = 0.3F;
            }
            case Goblin.A_SCARED -> {
                rightArm.xRot = leftArm.xRot = -2.4F;
                rightArm.zRot = 0.05F * Mth.sin(t * 3F);
                body.xRot = 0.4F;
            }
            case Goblin.A_PETTED -> head.zRot = 0.3F + 0.08F * Mth.sin(t * 0.4F);
            case Goblin.A_ANNOYED -> {
                head.yRot = 0.35F * Mth.sin(t * 1.2F);
                rightArm.xRot = leftArm.xRot = -1.1F;
                rightArm.yRot = 0.6F;
                leftArm.yRot = -0.6F;
            }
            case Goblin.A_PLAY -> {
                rightArm.zRot = 0.7F + 0.4F * Mth.sin(t * 0.8F);
                leftArm.zRot = -0.7F - 0.4F * Mth.sin(t * 0.8F);
            }
            default -> {}
        }
    }

    @Override
    public void setupAnim(GoblinRenderState s) {
        super.setupAnim(s);
        hat.visible = false;
        // the ears twitch now and then, and droop while it hides
        float t = s.ageInTicks;
        float twitch = (t % 70F) < 4F ? Mth.sin(t * 2.5F) * 0.25F : 0F;
        float droop = s.isCrouching ? 0.35F : 0F;
        leftEar.zRot = -0.35F + droop + twitch;
        rightEar.zRot = 0.35F - droop - twitch;
        actions(s);
        if (s.considering) {
            // looks at the food in its hand
            head.xRot = 0.5F;
            rightArm.xRot = -1.1F;
            rightArm.yRot = -0.3F;
        }
    }
}
