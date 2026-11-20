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
        if (s.considering) {
            // looks at the food in its hand
            head.xRot = 0.5F;
            rightArm.xRot = -1.1F;
            rightArm.yRot = -0.3F;
        }
    }
}
