package pl.kubalte.animalbonds.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.BondConfig;

/**
 * Every creature gets its own colouring, generated from the vanilla texture: same model and shading,
 * different colours and patches. The look is derived from the creature's UUID, so it is stable and
 * the same for every player. Sheep keep their texture (wool colour comes from dye).
 */
public final class VariantTextures {
    private VariantTextures() {}

    public static final ContextKey<Integer> SEED = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "variant_seed"));
    public static final ContextKey<String> TYPE = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "variant_type"));

    /** Distinct colourings per texture; combined with the size variation this makes look-alikes very rare. */
    private static final int VARIANTS = 48;

    private static final Set<String> EXCLUDED = Set.of(
            "minecraft:sheep", "minecraft:player", "minecraft:armor_stand", "minecraft:ender_dragon", "minecraft:wither",
            "minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem", "minecraft:snow_golem",
            "minecraft:mannequin", "minecraft:giant", "minecraft:warden", "minecraft:elder_guardian");

    private static final Map<String, Identifier> CACHE = new HashMap<>();

    public static boolean applies(String type) {
        return BondConfig.TEXTURE_VARIETY.get() && !EXCLUDED.contains(type);
    }

    // ---- palettes (RGB) ----
    private static final int[] FUR = {0xF2EEE4, 0xE8D9B5, 0xC9A06A, 0xB5651D, 0x8B5A2B, 0x5C3A1E, 0x3B2A1E, 0x1E1A18, 0x7A7470, 0xA8A39C};
    private static final int[] FEATHERS = {0xF4F1EA, 0x1C1C1E, 0x6B4226, 0xA0522D, 0xC68642, 0x8A8A8A, 0xE6C9A0};
    private static final int[] PIG = {0xF0A8A8, 0xE8B4A0, 0x2A2420, 0x6B4A3A, 0xC98F7A};
    private static final int[] SPIDER = {0x9B1B1B, 0x1A1A1A, 0x5A3A22, 0xE8E4DC, 0x3A3A3A, 0x6E1E3A};

    private record Style(int[] palette, boolean hueShift, double patchChance) {}

    private static Style style(String type) {
        return switch (type) {
            case "minecraft:cow", "minecraft:mooshroom", "minecraft:horse", "minecraft:donkey", "minecraft:mule",
                 "minecraft:goat", "minecraft:llama", "minecraft:trader_llama", "minecraft:wolf", "minecraft:cat",
                 "minecraft:ocelot", "minecraft:fox", "minecraft:rabbit", "minecraft:panda", "minecraft:camel",
                 "minecraft:polar_bear", "minecraft:armadillo", "minecraft:sniffer", "minecraft:hoglin" -> new Style(FUR, false, 0.75);
            case "minecraft:chicken", "minecraft:parrot", "minecraft:bat" -> new Style(FEATHERS, false, 0.5);
            case "minecraft:pig" -> new Style(PIG, false, 0.6);
            case "minecraft:spider", "minecraft:cave_spider" -> new Style(SPIDER, false, 0.5);
            default -> new Style(null, true, 0.3); // monsters & others: any colours
        };
    }

    /** Called from the renderer mixin with the vanilla texture; returns this creature's variant texture. */
    public static Identifier resolve(Identifier base, LivingEntityRenderState state) {
        Integer seed = state.getRenderData(SEED);
        String type = state.getRenderData(TYPE);
        if (seed == null || type == null || !base.getPath().endsWith(".png")) return base;
        int variant = Math.floorMod(seed, VARIANTS);
        String key = base + "#" + variant;
        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        Identifier out = base;
        try {
            out = generate(base, type, variant);
        } catch (Exception ex) {
            AnimalBonds.LOGGER.warn("Could not create variant texture for {}: {}", base, ex.toString());
        }
        CACHE.put(key, out);
        return out;
    }

    private static Identifier generate(Identifier base, String type, int variant) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var res = mc.getResourceManager().getResource(base);
        if (res.isEmpty()) return base;
        NativeImage img;
        try (InputStream in = res.get().open()) {
            img = NativeImage.read(in);
        }
        recolor(img, style(type), new Random(variant * 7919L + type.hashCode() * 31L + base.hashCode()));
        String path = "variant/" + base.getNamespace() + "/" + base.getPath().replace(".png", "") + "_" + variant;
        Identifier id = Identifier.fromNamespaceAndPath(AnimalBonds.MODID, path);
        mc.getTextureManager().register(id, new DynamicTexture(() -> id.toString(), img));
        return id;
    }

    // ---------------- colour maths ----------------

    private static float[] hsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 1e-5f) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        return new float[] {h, max <= 0 ? 0 : d / max, max};
    }

    private static int fromHsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        s = Math.max(0, Math.min(1, s));
        v = Math.max(0, Math.min(1, v));
        float c = v * s, x = c * (1 - Math.abs((h * 6f) % 2f - 1)), m = v - c;
        float r, g, b;
        int seg = (int) (h * 6f) % 6;
        switch (seg) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private static float luma(int rgb) {
        return (0.299f * ((rgb >> 16) & 255) + 0.587f * ((rgb >> 8) & 255) + 0.114f * (rgb & 255)) / 255f;
    }

    /** Eyes, beaks, combs, noses: small saturated details keep their colour. */
    private static boolean isDetail(float[] hsv) {
        return hsv[1] > 0.55f && hsv[2] > 0.35f;
    }

    private static void recolor(NativeImage img, Style style, Random rnd) {
        int w = img.getWidth(), h = img.getHeight();
        float unit = w / 64f;

        // Random blob mask = where the second colour (patches) goes.
        boolean patches = rnd.nextDouble() < style.patchChance();
        int blobs = 4 + rnd.nextInt(7);
        float[][] circles = new float[blobs][3];
        for (float[] c : circles) {
            c[0] = rnd.nextFloat() * w;
            c[1] = rnd.nextFloat() * h;
            c[2] = (2.5f + rnd.nextFloat() * 7f) * unit;
        }

        if (style.palette() != null) {
            int base = style.palette()[rnd.nextInt(style.palette().length)];
            int patch = style.palette()[rnd.nextInt(style.palette().length)];
            if (patch == base) patch = style.palette()[(rnd.nextInt(style.palette().length - 1) + 1 + indexOf(style.palette(), base)) % style.palette().length];
            // mean luminance of recolourable pixels, to keep the original shading around 1.0
            double sum = 0;
            int n = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getPixel(x, y);
                if ((argb >>> 24) < 8) continue;
                float[] c = hsv(argb & 0xFFFFFF);
                if (isDetail(c)) continue;
                sum += luma(argb & 0xFFFFFF);
                n++;
            }
            float mean = n == 0 ? 0.5f : (float) Math.max(0.15, sum / n);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getPixel(x, y);
                int a = argb >>> 24;
                if (a < 8) continue;
                int rgb = argb & 0xFFFFFF;
                if (isDetail(hsv(rgb))) continue;
                int target = patches && inMask(circles, x, y) ? patch : base;
                float shade = Math.max(0.45f, Math.min(1.45f, 0.35f + 0.65f * luma(rgb) / mean));
                int r = Math.min(255, Math.round(((target >> 16) & 255) * shade));
                int g = Math.min(255, Math.round(((target >> 8) & 255) * shade));
                int b = Math.min(255, Math.round((target & 255) * shade));
                img.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        } else {
            // Monsters: every hue family of the original gets a new random hue (+ small saturation/brightness change).
            float[] shift = new float[12];
            for (int i = 0; i < 12; i++) shift[i] = rnd.nextFloat();
            float satMul = 0.8f + rnd.nextFloat() * 0.5f;
            float valMul = 0.85f + rnd.nextFloat() * 0.3f;
            float patchHue = rnd.nextFloat();
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getPixel(x, y);
                int a = argb >>> 24;
                if (a < 8) continue;
                float[] c = hsv(argb & 0xFFFFFF);
                if (c[1] < 0.12f) continue; // greys/whites (bones, teeth, eyes) stay
                int bin = Math.min(11, (int) (c[0] * 12));
                float hue = shift[bin];
                if (patches && inMask(circles, x, y)) hue = patchHue;
                img.setPixel(x, y, (a << 24) | fromHsv(hue, c[1] * satMul, c[2] * valMul));
            }
        }
    }

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return 0;
    }

    private static boolean inMask(float[][] circles, int x, int y) {
        for (float[] c : circles) {
            float dx = x - c[0], dy = y - c[1];
            if (dx * dx + dy * dy < c[2] * c[2]) return true;
        }
        return false;
    }

    public static List<String> excluded() {
        return List.copyOf(EXCLUDED);
    }
}
