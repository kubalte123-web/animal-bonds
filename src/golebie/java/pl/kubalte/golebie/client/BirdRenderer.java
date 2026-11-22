package pl.kubalte.golebie.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import pl.kubalte.golebie.Golebie;
import pl.kubalte.golebie.bird.Bird;

/** GeckoLib renderer for both birds. The diaper is part of the model, shown by picking the texture that paints it. */
public class BirdRenderer extends GeoEntityRenderer<Bird, BirdRenderer.State> {
    final float scale;

    public static final class State extends LivingEntityRenderState implements GeoRenderState {
        public int diaper;
        public boolean baby;
    }

    public BirdRenderer(EntityRendererProvider.Context ctx, String species, float scale) {
        super(ctx, new Model(species));
        this.scale = scale;
        this.shadowRadius = 0.2F;
    }

    @Override
    public State createRenderState(Bird bird, Void nothing) {
        return new State();
    }

    @Override
    public void extractRenderState(Bird bird, State s, float partialTick) {
        super.extractRenderState(bird, s, partialTick);
        s.diaper = bird.diaper();
        s.baby = bird.isBaby();
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<State> info, float widthScale, float heightScale) {
        float k = scale * (info.renderState().baby ? 0.6F : 1F);
        super.scaleModelForRender(info, widthScale * k, heightScale * k);
    }

    static final class Model extends GeoModel<Bird> {
        final Identifier geo, anim;
        final Identifier[] textures = new Identifier[Golebie.DIAPER_PATTERNS.length + 1];

        Model(String species) {
            geo = Golebie.id("entity/" + species);
            anim = Golebie.id("entity/" + species);
            textures[0] = Golebie.id("textures/entity/" + species + ".png");
            for (int i = 0; i < Golebie.DIAPER_PATTERNS.length; i++)
                textures[i + 1] = Golebie.id("textures/entity/" + species + "_" + Golebie.DIAPER_PATTERNS[i] + ".png");
        }

        @Override
        public Identifier getModelResource(GeoRenderState rs) {
            return geo;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState rs) {
            int d = rs instanceof State s ? s.diaper : 0;
            return textures[d >= 0 && d < textures.length ? d : 0];
        }

        @Override
        public Identifier getAnimationResource(Bird bird) {
            return anim;
        }
    }
}
