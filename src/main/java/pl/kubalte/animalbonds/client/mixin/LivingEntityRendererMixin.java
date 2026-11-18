package pl.kubalte.animalbonds.client.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.kubalte.animalbonds.client.VariantTextures;

/** Swaps the creature's base texture for its own, procedurally recoloured variant. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "getRenderType",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
    private Identifier animalbonds$variantTexture(LivingEntityRenderer self, LivingEntityRenderState state) {
        return VariantTextures.resolve(self.getTextureLocation(state), state);
    }
}
