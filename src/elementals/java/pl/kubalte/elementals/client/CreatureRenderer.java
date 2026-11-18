package pl.kubalte.elementals.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.Elementals;

/** Renderer used without GeckoLib: the same model and element textures, procedural animation. */
public class CreatureRenderer<T extends Mob> extends MobRenderer<T, LivingEntityRenderState, GeoJsonModel.CreatureModel> {
    private final String creature;

    public static ModelLayerLocation layer(String creature) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Elementals.MODID, creature), "main");
    }

    public CreatureRenderer(EntityRendererProvider.Context ctx, String creature, float shadow) {
        super(ctx, new GeoJsonModel.CreatureModel(ctx.bakeLayer(layer(creature)), creature), shadow);
        this.creature = creature;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.setRenderData(ElementVisuals.ELEMENT, Elementals.element(entity));
        state.setRenderData(ElementVisuals.OWN, creature);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        Element el = state.getRenderData(ElementVisuals.ELEMENT);
        return texture(creature, el == null ? Element.NONE : el);
    }

    public static Identifier texture(String creature, Element el) {
        return Identifier.fromNamespaceAndPath(Elementals.MODID, "textures/entity/" + creature + "_" + el.id + ".png");
    }

    public static Identifier glowmask(String creature, Element el) {
        return Identifier.fromNamespaceAndPath(Elementals.MODID, "textures/entity/" + creature + "_" + el.id + "_glowmask.png");
    }
}
