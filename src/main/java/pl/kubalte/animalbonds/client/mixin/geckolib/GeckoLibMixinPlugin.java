package pl.kubalte.animalbonds.client.mixin.geckolib;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Applies the GeckoLib mixins only when GeckoLib is installed. */
public class GeckoLibMixinPlugin implements IMixinConfigPlugin {
    private static boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        present = GeckoLibMixinPlugin.class.getClassLoader().getResource("com/geckolib/renderer/GeoEntityRenderer.class") != null
                || Thread.currentThread().getContextClassLoader().getResource("com/geckolib/renderer/GeoEntityRenderer.class") != null;
        System.out.println("[animalbonds] GeckoLib " + (present ? "found: enabling integration" : "not installed: integration off"));
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return present;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        System.out.println("[animalbonds] GeckoLib integration applied to " + targetClassName);
    }
}
