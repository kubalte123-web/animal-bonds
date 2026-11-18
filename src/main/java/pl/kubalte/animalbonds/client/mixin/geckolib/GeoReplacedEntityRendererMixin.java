package pl.kubalte.animalbonds.client.mixin.geckolib;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.kubalte.animalbonds.client.VariantTextures;

/** GeckoLib creatures: render with the creature's own unique texture (generated from its UUID hash). */
@Mixin(value = GeoReplacedEntityRenderer.class, remap = false)
public abstract class GeoReplacedEntityRendererMixin {
    private static final ThreadLocal<Boolean> ANIMALBONDS_GUARD = ThreadLocal.withInitial(() -> false);

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void animalbonds$variantTexture(EntityRenderState state, Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ANIMALBONDS_GUARD.get()) return;
        Identifier variant = VariantTextures.resolve(texture, state);
        if (variant == texture) return;
        ANIMALBONDS_GUARD.set(true);
        try {
            cir.setReturnValue(((GeoReplacedEntityRenderer) (Object) this).getRenderType(state, variant));
        } finally {
            ANIMALBONDS_GUARD.set(false);
        }
    }
}
