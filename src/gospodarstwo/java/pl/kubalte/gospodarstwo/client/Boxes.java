package pl.kubalte.gospodarstwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Draws simple textured boxes (coordinates in pixels, 16 = one block) with block-model style UVs. */
public final class Boxes {
    private Boxes() {}

    public static void box(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F, d = x1 / 16F, e = y1 / 16F, f = z1 / 16F;
        // UVs from the face's position inside the block, wrapped into the texture
        float u0 = frac(x0), u1 = u0 + (x1 - x0) / 16F, v0 = frac(z0), v1 = v0 + (z1 - z0) / 16F;
        // up / down
        quad(pose, vc, a, e, c, a, e, f, d, e, f, d, e, c, u0, v0, u1, v1, 0, 1, 0, color, light);
        quad(pose, vc, a, b, f, a, b, c, d, b, c, d, b, f, u0, v0, u1, v1, 0, -1, 0, color, light);
        float hu0 = frac(x0), hu1 = hu0 + (x1 - x0) / 16F, hv0 = 1F - (y1 / 16F - (float) Math.floor(y0 / 16F)), hv1 = hv0 + (y1 - y0) / 16F;
        if (hv0 < 0) { hv1 -= hv0; hv0 = 0; }
        // north (-z) / south (+z)
        quad(pose, vc, d, e, c, d, b, c, a, b, c, a, e, c, hu0, hv0, hu1, hv1, 0, 0, -1, color, light);
        quad(pose, vc, a, e, f, a, b, f, d, b, f, d, e, f, hu0, hv0, hu1, hv1, 0, 0, 1, color, light);
        float zu0 = frac(z0), zu1 = zu0 + (z1 - z0) / 16F;
        // west (-x) / east (+x)
        quad(pose, vc, a, e, c, a, b, c, a, b, f, a, e, f, zu0, hv0, zu1, hv1, -1, 0, 0, color, light);
        quad(pose, vc, d, e, f, d, b, f, d, b, c, d, e, c, zu0, hv0, zu1, hv1, 1, 0, 0, color, light);
    }

    private static float frac(float px) {
        float v = px / 16F;
        return v - (float) Math.floor(v);
    }

    /** Corners in order top-left, bottom-left, bottom-right, top-right of the face as seen from outside. */
    private static void quad(PoseStack.Pose pose, VertexConsumer vc, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, float u0, float v0, float u1, float v1,
                             float nx, float ny, float nz, int color, int light) {
        vertex(pose, vc, x1, y1, z1, u0, v0, nx, ny, nz, color, light);
        vertex(pose, vc, x2, y2, z2, u0, v1, nx, ny, nz, color, light);
        vertex(pose, vc, x3, y3, z3, u1, v1, nx, ny, nz, color, light);
        vertex(pose, vc, x4, y4, z4, u1, v0, nx, ny, nz, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v, float nx, float ny, float nz,
                               int color, int light) {
        vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** A double-sided vertical panel in the XY plane at depth z (pixels), with the whole texture stretched over it. */
    public static void panel(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float x1, float y1, float z, int color, int light) {
        float a = x0 / 16F, b = y0 / 16F, d = x1 / 16F, e = y1 / 16F, f = z / 16F;
        quad(pose, vc, a, e, f, a, b, f, d, b, f, d, e, f, 0, 0, 1, 1, 0, 0, 1, color, light);
        quad(pose, vc, d, e, f, d, b, f, a, b, f, a, e, f, 1, 0, 0, 1, 0, 0, -1, color, light);
    }

    /** Two crossed quads (like a sapling or a flower), standing on (cx, y0, cz). */
    public static void cross(PoseStack.Pose pose, VertexConsumer vc, float cx, float y0, float cz, float size, int color, int light) {
        float h = size / 32F, x = cx / 16F, z = cz / 16F, b = y0 / 16F, t = (y0 + size) / 16F;
        for (int s = 0; s < 2; s++) {
            float dx = s == 0 ? h : h, dz = s == 0 ? h : -h;
            quad(pose, vc, x - dx, t, z - dz, x - dx, b, z - dz, x + dx, b, z + dz, x + dx, t, z + dz, 0, 0, 1, 1, 0, 0, 1, color, light);
            quad(pose, vc, x + dx, t, z + dz, x + dx, b, z + dz, x - dx, b, z - dz, x - dx, t, z - dz, 0, 0, 1, 1, 0, 0, -1, color, light);
        }
    }
}
