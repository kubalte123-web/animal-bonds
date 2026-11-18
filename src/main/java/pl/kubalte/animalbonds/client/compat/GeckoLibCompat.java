package pl.kubalte.animalbonds.client.compat;

import com.geckolib.event.entity.GeoEntityPreRenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.client.BondRendering;

/**
 * GeckoLib creatures don't go through the vanilla living-entity renderer, so fainting and saddlebags are
 * handled here, from GeckoLib's own pre-render event. (Unique textures come from the GeckoLib mixins.)
 * Only loaded when GeckoLib is installed.
 */
public final class GeckoLibCompat {
    private GeckoLibCompat() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(GeckoLibCompat::preRender);
        AnimalBonds.LOGGER.info("Animal Bonds: GeckoLib integration active");
    }

    static void preRender(GeoEntityPreRenderEvent<?, ?> e) {
        EntityRenderState state = e.getRenderState();
        PoseStack ps = e.getPoseStack();
        float[] bags = state.getRenderData(BondRendering.BAGS);
        int light = state.lightCoords;
        if (bags != null) {
            // The pose is still at the entity's feet, world-aligned: GeckoLib rotates/scales afterwards.
            float w = bags[0], h = bags[1], bodyRot = bags[2];
            float ch = Math.max(0.2F, Math.min(0.7F, h * 0.35F));
            float ct = ch * 0.4F;
            float cy = h * 0.55F;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
            final float y0 = cy - ch / 2, y1 = cy + ch / 2, z0 = -ch / 2, z1 = ch / 2, side = w / 2;
            e.getRenderTasks().submitCustomGeometry(ps, RenderTypes.entityCutout(BondRendering.CHEST_TEXTURE), (pose, vc) -> {
                BondRendering.chest(pose, vc, light, side, side + ct, y0, y1, z0, z1, true);
                BondRendering.chest(pose, vc, light, -side - ct, -side, y0, y1, z0, z1, false);
            });
            ps.popPose();
        }
        if (Boolean.TRUE.equals(state.getRenderData(BondRendering.FAINTED))) {
            // lie on the side; GeckoLib pops this pose at the end of its render pass
            ps.translate(0.0F, 0.15F, 0.0F);
            ps.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
