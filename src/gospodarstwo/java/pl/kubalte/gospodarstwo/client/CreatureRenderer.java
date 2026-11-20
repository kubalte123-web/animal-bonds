package pl.kubalte.gospodarstwo.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Renderer used without GeckoLib: the model from the geo.json with simple procedural animation. */
public class CreatureRenderer<T extends Mob> extends MobRenderer<T, LivingEntityRenderState, GeoJsonModel.CreatureModel> {
    private final Identifier texture;

    public static ModelLayerLocation layer(String creature) {
        return new ModelLayerLocation(Gospodarstwo.id(creature), "main");
    }

    public CreatureRenderer(EntityRendererProvider.Context ctx, String creature, float shadow) {
        super(ctx, new GeoJsonModel.CreatureModel(ctx.bakeLayer(layer(creature)), creature), shadow);
        this.texture = texture(creature);
    }

    public static Identifier texture(String creature) {
        return Gospodarstwo.id("textures/entity/" + creature + ".png");
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return texture;
    }
}
