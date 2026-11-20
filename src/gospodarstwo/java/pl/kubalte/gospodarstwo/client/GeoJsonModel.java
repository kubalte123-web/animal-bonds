package pl.kubalte.gospodarstwo.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Builds a vanilla model from the creature's GeckoLib geo.json, so the creatures look the same without GeckoLib
 * (animations are then simple procedural ones instead of the keyframed GeckoLib ones).
 */
public final class GeoJsonModel {
    private GeoJsonModel() {}

    /** Bone name -> path of child names from the root. */
    public record Parsed(LayerDefinition layer, Map<String, List<String>> paths) {}

    private static final Map<String, Parsed> CACHE = new HashMap<>();

    public static Parsed load(String creature) {
        return CACHE.computeIfAbsent(creature, GeoJsonModel::parse);
    }

    private static Parsed parse(String creature) {
        String res = "/assets/gospodarstwo/geckolib/models/entity/" + creature + ".geo.json";
        try (InputStream in = GeoJsonModel.class.getResourceAsStream(res)) {
            if (in == null) throw new IllegalStateException("missing " + res);
            JsonObject geo = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject desc = geo.getAsJsonObject("description");
            int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            Map<String, PartDefinition> parts = new HashMap<>();
            Map<String, float[]> pivots = new HashMap<>();
            Map<String, List<String>> paths = new HashMap<>();
            for (JsonElement el : geo.getAsJsonArray("bones")) {
                JsonObject b = el.getAsJsonObject();
                String name = b.get("name").getAsString();
                String parent = b.has("parent") ? b.get("parent").getAsString() : null;
                float[] p = vec(b.getAsJsonArray("pivot"));
                // bedrock -> java: x mirrored, y measured downwards from 24
                float[] j = {-p[0], 24 - p[1], p[2]};
                float[] pj = parent == null ? new float[3] : pivots.get(parent);
                float[] r = b.has("rotation") ? vec(b.getAsJsonArray("rotation")) : new float[3];
                CubeListBuilder cubes = CubeListBuilder.create();
                if (b.has("cubes")) {
                    for (JsonElement ce : b.getAsJsonArray("cubes")) {
                        JsonObject c = ce.getAsJsonObject();
                        float[] o = vec(c.getAsJsonArray("origin")), s = vec(c.getAsJsonArray("size"));
                        JsonArray uv = c.getAsJsonArray("uv");
                        float minX = -(o[0] + s[0]), minY = 24 - (o[1] + s[1]), minZ = o[2];
                        cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                                .addBox(minX - j[0], minY - j[1], minZ - j[2], s[0], s[1], s[2]);
                    }
                }
                PartPose pose = PartPose.offsetAndRotation(j[0] - pj[0], j[1] - pj[1], j[2] - pj[2],
                        -r[0] * Mth.DEG_TO_RAD, -r[1] * Mth.DEG_TO_RAD, r[2] * Mth.DEG_TO_RAD);
                PartDefinition parentDef = parent == null ? root : parts.get(parent);
                parts.put(name, parentDef.addOrReplaceChild(name, cubes, pose));
                pivots.put(name, j);
                List<String> path = new ArrayList<>(parent == null ? List.of() : paths.get(parent));
                path.add(name);
                paths.put(name, path);
            }
            return new Parsed(LayerDefinition.create(mesh, tw, th), paths);
        } catch (Exception e) {
            throw new RuntimeException("Could not read model of " + creature, e);
        }
    }

    private static float[] vec(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    /** The vanilla model of a creature with simple procedural animation of its named bones. */
    public static class CreatureModel extends EntityModel<LivingEntityRenderState> {
        private final Map<String, ModelPart> bones = new HashMap<>();
        private final boolean hovering;

        public CreatureModel(ModelPart root, String creature) {
            super(root);
            hovering = false;
            for (var e : load(creature).paths().entrySet()) {
                ModelPart p = root;
                for (String n : e.getValue()) p = p.getChild(n);
                bones.put(e.getKey(), p);
            }
        }

        private ModelPart bone(String n) {
            return bones.get(n);
        }

        @Override
        public void setupAnim(LivingEntityRenderState s) {
            super.setupAnim(s);
            float pos = s.walkAnimationPos, spd = Math.min(1F, s.walkAnimationSpeed), age = s.ageInTicks;
            float swing = Mth.cos(pos * 0.6662F) * 1.1F * spd;
            ModelPart head = bone("head");
            if (head != null) {
                head.xRot += s.xRot * Mth.DEG_TO_RAD;
                head.yRot += s.yRot * Mth.DEG_TO_RAD;
            }
            for (String n : new String[] {"leg_fl", "leg_br", "leg_left"}) {
                ModelPart p = bone(n);
                if (p != null) p.xRot += swing;
            }
            for (String n : new String[] {"leg_fr", "leg_bl", "leg_right"}) {
                ModelPart p = bone(n);
                if (p != null) p.xRot -= swing;
            }
            ModelPart wl = bone("wing_left"), wr = bone("wing_right");
            if (wl != null && wr != null) {
                float flap = Mth.sin(age * (spd > 0.1F ? 1.3F : 0.2F)) * (spd > 0.1F ? 0.6F : 0.08F);
                wl.zRot -= flap;
                wr.zRot += flap;
            }
            ModelPart al = bone("arm_left"), ar = bone("arm_right");
            if (al != null && ar != null) {
                al.xRot += swing * 0.5F;
                ar.xRot -= swing * 0.5F;
                al.zRot -= Mth.sin(age * 0.08F) * 0.08F;
                ar.zRot += Mth.sin(age * 0.08F) * 0.08F;
            }
            for (String n : new String[] {"tail", "wisp"}) {
                ModelPart t = bone(n);
                if (t != null) t.yRot += Mth.sin(age * 0.1F) * 0.15F + Mth.cos(pos * 0.6662F) * 0.35F * spd;
            }
            ModelPart tip = bone("tail_tip");
            if (tip != null) tip.yRot += Mth.sin(age * 0.1F + 1F) * 0.2F + Mth.cos(pos * 0.6662F + 1F) * 0.4F * spd;
            ModelPart body = bone("body");
            if (body != null) {
                if (hovering) {
                    body.y += Mth.sin(age * 0.1F) * 1.2F;
                    body.xRot += spd * 0.15F;
                } else {
                    body.y += Math.abs(Mth.sin(pos * 0.6662F)) * -0.6F * spd;
                }
            }
            if (s.isBaby) {
                ModelPart r = root();
                r.xScale = r.yScale = r.zScale = 0.55F;
                r.y += 24F * (1F - 0.55F);
            }
        }
    }
}
