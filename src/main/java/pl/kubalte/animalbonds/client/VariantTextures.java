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

    public static final ContextKey<Long> SEED = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "variant_seed"));
    /** true = animal-like creature (natural colours), false = monster/other (any colours). Used for modded creatures. */
    public static final ContextKey<Boolean> NATURAL = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "variant_natural"));
    public static final ContextKey<String> TYPE = new ContextKey<>(Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "variant_type"));

    /** Generated textures kept in memory; the least recently seen ones are released. */
    private static final int MAX_TEXTURES = 1024;
    /** New textures generated per frame at most (avoids a hitch when a herd comes into view). */
    private static final int MAX_NEW_PER_FRAME = 12;
    private static int newThisFrame;
    private static long frameMillis;
    private static int generatedTotal;

    private record Entry(Identifier id, long[] lastUsed) {}

    private static final Set<String> EXCLUDED = Set.of(
            "minecraft:sheep", "minecraft:player", "minecraft:armor_stand", "minecraft:ender_dragon", "minecraft:wither",
            "minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem", "minecraft:snow_golem",
            "minecraft:mannequin", "minecraft:giant", "minecraft:warden", "minecraft:elder_guardian");

    private static final Map<String, Entry> CACHE = new java.util.LinkedHashMap<>(256, 0.75f, true);
    private static final Map<Identifier, NativeImage> BASE_IMAGES = new HashMap<>();

    public static boolean applies(String type) {
        return BondConfig.TEXTURE_VARIETY.get() && !EXCLUDED.contains(type) && !pl.kubalte.animalbonds.api.AnimalBondsApi.isExcludedFromTextureVariety(type);
    }

    // ---- palettes (RGB) ----
    private static final int[] FUR = {0xF2EEE4, 0xE8D9B5, 0xC9A06A, 0xB5651D, 0x8B5A2B, 0x5C3A1E, 0x3B2A1E, 0x1E1A18, 0x7A7470, 0xA8A39C};
    private static final int[] FEATHERS = {0xF4F1EA, 0x1C1C1E, 0x6B4226, 0xA0522D, 0xC68642, 0x8A8A8A, 0xE6C9A0};
    private static final int[] PIG = {0xF0A8A8, 0xE8B4A0, 0x2A2420, 0x6B4A3A, 0xC98F7A};
    private static final int[] SPIDER = {0x9B1B1B, 0x1A1A1A, 0x5A3A22, 0xE8E4DC, 0x3A3A3A, 0x6E1E3A};

    private record Style(int[] palette, boolean hueShift, double patchChance) {}

    private static Style style(String type, boolean natural) {
        return switch (type) {
            case "minecraft:cow", "minecraft:mooshroom", "minecraft:horse", "minecraft:donkey", "minecraft:mule",
                 "minecraft:goat", "minecraft:llama", "minecraft:trader_llama", "minecraft:wolf", "minecraft:cat",
                 "minecraft:ocelot", "minecraft:fox", "minecraft:rabbit", "minecraft:panda", "minecraft:camel",
                 "minecraft:polar_bear", "minecraft:armadillo", "minecraft:sniffer", "minecraft:hoglin" -> new Style(FUR, false, 0.75);
            case "minecraft:chicken", "minecraft:parrot", "minecraft:bat" -> new Style(FEATHERS, false, 0.5);
            case "minecraft:pig" -> new Style(PIG, false, 0.6);
            case "minecraft:spider", "minecraft:cave_spider" -> new Style(SPIDER, false, 0.5);
            // unknown (also modded) creatures: animals get natural colours, monsters and others any colours
            default -> natural ? new Style(FUR, false, 0.6) : new Style(null, true, 0.3);
        };
    }

    /**
     * Called from the renderer mixin with the vanilla (or modded) texture; returns this creature's own texture.
     * The colouring is computed from the creature's UUID hash, so every creature is unique, stable across
     * sessions and identical for every player - and it works for creatures from any mod.
     */
    public static Identifier resolve(Identifier base, net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        Long seed = state.getRenderData(SEED);
        String type = state.getRenderData(TYPE);
        Boolean natural = state.getRenderData(NATURAL);
        if (seed == null || type == null) return base;
        return resolve(base, seed, type, natural != null && natural);
    }

    /** Variant of {@code base} for the given seed, coloured in the style of creature type {@code type}. */
    public static Identifier resolve(Identifier base, long seed, String type, boolean natural) {
        if (!base.getPath().endsWith(".png") || !BondConfig.TEXTURE_VARIETY.get()) return base;
        long now = System.currentTimeMillis();
        if (now - frameMillis > 15) {
            frameMillis = now;
            newThisFrame = 0;
        }
        String key = base + "#" + Long.toHexString(seed) + "#" + type;
        Entry cached = CACHE.get(key);
        if (cached != null) {
            cached.lastUsed()[0] = now;
            return cached.id();
        }
        if (newThisFrame >= MAX_NEW_PER_FRAME) return base; // generated in one of the next frames
        newThisFrame++;
        Identifier out = base;
        try {
            out = generate(base, type, natural, seed);
            generatedTotal++;
        } catch (Exception ex) {
            AnimalBonds.LOGGER.warn("Could not create variant texture for {}: {}", base, ex.toString());
        }
        CACHE.put(key, new Entry(out, new long[] {now}));
        evict(now);
        return out;
    }

    /** Releases textures of creatures that haven't been seen for a while once the cache is full. */
    private static void evict(long now) {
        if (CACHE.size() <= MAX_TEXTURES) return;
        var it = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_TEXTURES && it.hasNext()) {
            Entry e = it.next().getValue();
            if (now - e.lastUsed()[0] < 10_000) break; // access-ordered: the rest is newer
            if (e.id().getNamespace().equals(AnimalBonds.MODID)) Minecraft.getInstance().getTextureManager().release(e.id());
            it.remove();
        }
    }

    public static int generatedTotal() {
        return generatedTotal;
    }

    private static Identifier generate(Identifier base, String type, boolean natural, long seed) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        NativeImage src = BASE_IMAGES.get(base);
        if (src == null) {
            var res = mc.getResourceManager().getResource(base);
            if (res.isEmpty()) return base;
            try (InputStream in = res.get().open()) {
                src = NativeImage.read(in);
            }
            BASE_IMAGES.put(base, src);
        }
        NativeImage img = new NativeImage(src.getWidth(), src.getHeight(), false);
        img.copyFrom(src);
        recolor(img, style(type, natural), new Random(seed ^ base.hashCode() * 0x9E3779B97F4A7C15L));
        String path = "variant/" + Long.toHexString(seed) + "/" + type.replace(':', '_') + "/" + base.getNamespace() + "/" + base.getPath().replace(".png", "");
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
            // any colour between two neighbouring palette colours, slightly jittered: no two creatures alike
            int base = paletteColor(style.palette(), rnd);
            int patch = paletteColor(style.palette(), rnd);
            for (int tries = 0; tries < 5 && colorDistance(base, patch) < 90; tries++) patch = paletteColor(style.palette(), rnd);
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

    private static int paletteColor(int[] pal, Random rnd) {
        int i = rnd.nextInt(pal.length);
        int a = pal[i], b = pal[(i + 1) % pal.length];
        float t = rnd.nextFloat();
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        float[] hsv = hsv((r << 16) | (g << 8) | bl);
        return fromHsv(hsv[0] + (rnd.nextFloat() - 0.5f) * 0.03f, hsv[1] * (0.9f + rnd.nextFloat() * 0.2f), hsv[2] * (0.92f + rnd.nextFloat() * 0.16f));
    }

    private static int colorDistance(int a, int b) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255));
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
