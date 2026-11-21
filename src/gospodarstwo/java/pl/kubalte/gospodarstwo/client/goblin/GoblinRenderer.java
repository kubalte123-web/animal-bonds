package pl.kubalte.gospodarstwo.client.goblin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.client.Boxes;
import pl.kubalte.gospodarstwo.goblin.Goblin;

/** Goblins are drawn at half the size of a player, with armour, held items and the chest on the back scaled with them. */
public class GoblinRenderer extends HumanoidMobRenderer<Goblin, GoblinRenderState, GoblinModel> {
    static final Identifier TEXTURE = Gospodarstwo.id("textures/entity/goblin.png");
    static final Identifier[] RACE_TEXTURES = {TEXTURE, Gospodarstwo.id("textures/entity/goblin_stone.png"),
            Gospodarstwo.id("textures/entity/goblin_swamp.png"), Gospodarstwo.id("textures/entity/goblin_cave.png")};
    static final float[] RACE_SCALE = {0.5F, 0.56F, 0.52F, 0.47F};
    public static final float SCALE = 0.5F;

    public GoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinModel(ctx.bakeLayer(GoblinModel.LAYER)), 0.3F);
        addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(), HumanoidModel::new),
                ctx.getEquipmentRenderer()));
        addLayer(new PackLayer(this));
    }

    @Override
    public GoblinRenderState createRenderState() {
        return new GoblinRenderState();
    }

    @Override
    public void extractRenderState(Goblin g, GoblinRenderState s, float partialTick) {
        super.extractRenderState(g, s, partialTick);
        s.pack = g.hasPack();
        s.tame = g.isTame();
        s.considering = g.considering() > 0;
        s.resting = g.resting();
        s.isCrouching = g.isCrouching() || g.work() == 6;
        s.race = g.race().ordinal();
    }

    @Override
    public Identifier getTextureLocation(GoblinRenderState s) {
        return RACE_TEXTURES[s.race];
    }

    @Override
    protected void scale(GoblinRenderState s, PoseStack ps) {
        float k = RACE_SCALE[s.race];
        ps.scale(k, k, k);
    }

    /** A small wooden chest strapped to the back. */
    static final class PackLayer extends RenderLayer<GoblinRenderState, GoblinModel> {
        static final Identifier PLANKS = Identifier.withDefaultNamespace("textures/block/oak_planks.png");
        static final Identifier IRON = Identifier.withDefaultNamespace("textures/block/iron_block.png");
        static final Identifier STRAP = Identifier.withDefaultNamespace("textures/block/brown_wool.png");

        PackLayer(RenderLayerParent<GoblinRenderState, GoblinModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack ps, SubmitNodeCollector col, int light, GoblinRenderState s, float yRot, float xRot) {
            if (!s.pack) return;
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            // model units: 1/16 block per pixel; the body goes from y 0 (shoulders) to 12, z -2..2
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(PLANKS), (pose, vc) -> {
                Boxes.box(pose, vc, -4.5F, 0.5F, 2.2F, 4.5F, 6.5F, 8.2F, 0xFFC89A60, light);   // body of the chest
                Boxes.box(pose, vc, -4.7F, 6.5F, 2.0F, 4.7F, 9.0F, 8.4F, 0xFFB08450, light);   // lid
            });
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(IRON), (pose, vc) -> {
                Boxes.box(pose, vc, -0.8F, 5.2F, 8.2F, 0.8F, 7.6F, 8.7F, 0xFFD8C070, light);   // latch
                Boxes.box(pose, vc, -4.8F, 6.3F, 1.9F, 4.8F, 6.7F, 8.5F, 0xFF707070, light);   // band
            });
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(STRAP), (pose, vc) -> {
                Boxes.box(pose, vc, -3.2F, -0.2F, -2.3F, -2.2F, 7.5F, 2.3F, 0xFF6A4A2E, light);
                Boxes.box(pose, vc, 2.2F, -0.2F, -2.3F, 3.2F, 7.5F, 2.3F, 0xFF6A4A2E, light);
            });
            ps.popPose();
        }
    }
}
