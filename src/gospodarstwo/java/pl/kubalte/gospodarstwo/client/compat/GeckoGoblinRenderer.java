package pl.kubalte.gospodarstwo.client.compat;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pl.kubalte.gospodarstwo.Gospodarstwo;
import pl.kubalte.gospodarstwo.client.Boxes;
import pl.kubalte.gospodarstwo.goblin.gecko.GeckoGoblin;

/** The GeckoLib goblin: a model per race, keyframed animations, armour on its bones, tools in its hands, the chest on its back. */
public class GeckoGoblinRenderer extends GeoEntityRenderer<GeckoGoblin, GeckoGoblinRenderer.State> {
    static final String[] RACES = {"forest", "stone", "swamp", "cave"};
    static final float[] SCALE = {0.5F, 0.53F, 0.5F, 0.48F};

    public static final class State extends HumanoidRenderState implements GeoRenderState {
        public int race;
        public boolean pack;
    }

    static final List<ItemArmorGeoLayer.RenderData> ARMOR = List.of(ItemArmorGeoLayer.RenderData.head("head"),
            ItemArmorGeoLayer.RenderData.body("body"), ItemArmorGeoLayer.RenderData.rightArm("arm_r"), ItemArmorGeoLayer.RenderData.leftArm("arm_l"),
            ItemArmorGeoLayer.RenderData.rightLeg("leg_r"), ItemArmorGeoLayer.RenderData.leftLeg("leg_l"),
            ItemArmorGeoLayer.RenderData.rightFoot("leg_r"), ItemArmorGeoLayer.RenderData.leftFoot("leg_l"));

    public GeckoGoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.3F;
        withRenderLayer(new ItemInHandGeoLayer<GeckoGoblin, Void, State>(ctx, this, "hand_r", "hand_l"));
        withRenderLayer(new ItemArmorGeoLayer<GeckoGoblin, Void, State>(this, ctx) {
            @Override
            protected List<ItemArmorGeoLayer.RenderData> getRelevantBones(RenderPassInfo<State> info) {
                return ARMOR;
            }
        });
        withRenderLayer(new PackLayer(this));
    }

    @Override
    public State createRenderState(GeckoGoblin goblin, Void nothing) {
        return new State();
    }

    @Override
    public void extractRenderState(GeckoGoblin g, State s, float partialTick) {
        super.extractRenderState(g, s, partialTick);
        s.race = g.race().ordinal();
        s.pack = g.hasPack();
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<State> info, float widthScale, float heightScale) {
        float k = SCALE[info.renderState().race];
        super.scaleModelForRender(info, widthScale * k, heightScale * k);
    }

    static final class Model extends GeoModel<GeckoGoblin> {
        static final Identifier[] GEO = new Identifier[4], TEX = new Identifier[4];

        static {
            for (int i = 0; i < 4; i++) {
                GEO[i] = Gospodarstwo.id("entity/goblin_" + RACES[i]);
                TEX[i] = Gospodarstwo.id("textures/entity/goblin_geo_" + RACES[i] + ".png");
            }
        }

        static final Identifier ANIM = Gospodarstwo.id("entity/goblin");

        static int race(GeoRenderState rs) {
            return rs instanceof State s ? s.race : 0;
        }

        @Override
        public Identifier getModelResource(GeoRenderState rs) {
            return GEO[race(rs)];
        }

        @Override
        public Identifier getTextureResource(GeoRenderState rs) {
            return TEX[race(rs)];
        }

        @Override
        public Identifier getAnimationResource(GeckoGoblin animatable) {
            return ANIM;
        }
    }

    /** A small wooden chest strapped to the back, drawn on the "pack" bone. */
    static final class PackLayer extends GeoRenderLayer<GeckoGoblin, Void, State> {
        static final Identifier PLANKS = Identifier.withDefaultNamespace("textures/block/oak_planks.png");
        static final Identifier IRON = Identifier.withDefaultNamespace("textures/block/iron_block.png");

        PackLayer(GeckoGoblinRenderer r) {
            super(r);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<State> info, BiConsumer<GeoBone, PerBoneRender<State>> consumer) {
            if (!info.renderState().pack) return;
            info.model().getBone("pack").ifPresent(bone -> consumer.accept(bone, (ri, b, col) -> {
                int light = ri.packedLight();
                var ps = ri.poseStack();
                col.submitCustomGeometry(ps, RenderTypes.entityCutout(PLANKS), (pose, vc) -> {
                    Boxes.box(pose, vc, -4.5F, 0F, 0.2F, 4.5F, 6F, 6.2F, 0xFFC89A60, light);
                    Boxes.box(pose, vc, -4.7F, 6F, 0F, 4.7F, 8.5F, 6.4F, 0xFFB08450, light);
                });
                col.submitCustomGeometry(ps, RenderTypes.entityCutout(IRON), (pose, vc) -> {
                    Boxes.box(pose, vc, -0.8F, 4.7F, 6.2F, 0.8F, 7.1F, 6.7F, 0xFFD8C070, light);
                    Boxes.box(pose, vc, -4.8F, 5.8F, -0.1F, 4.8F, 6.2F, 6.5F, 0xFF707070, light);
                });
            }));
        }
    }
}
