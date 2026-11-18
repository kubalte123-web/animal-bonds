package pl.kubalte.animalbonds.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import pl.kubalte.animalbonds.ModRegistry;
import pl.kubalte.animalbonds.bond.HybridData;
import pl.kubalte.animalbonds.bond.Hybrids;
import pl.kubalte.animalbonds.network.OpenAltarPayload;
import pl.kubalte.animalbonds.network.SplicePayload;

/**
 * The Splicing Altar: parent A on the left, parent B on the right, a live preview of the hybrid in the middle.
 * For every part you choose whose it is.
 */
public class AltarScreen extends Screen {
    private static final int W = 340, H = 214;
    private static final int PANEL = 0xF0201A2E, FRAME = 0xFF8E6CC8, INK = 0xFFEDE6FF, SOFT = 0xFFB9AEDB, BAD = 0xFFFF8A80;

    private final BlockPos altar;
    private List<OpenAltarPayload.Candidate> all;
    private List<OpenAltarPayload.Candidate> eligible;
    private String cost;
    private int ia = 0, ib = 1;
    private boolean bodyB, headB, legsB, extrasB, movementB;
    private int colors = 2;
    private int left, top;
    private Button bodyBtn, headBtn, legsBtn, extrasBtn, colorsBtn, moveBtn, spliceBtn;
    private Mob preview;
    private String previewKey = "";

    public AltarScreen(OpenAltarPayload p) {
        super(Component.translatable("gui.animalbonds.altar"));
        this.altar = p.altar();
        setData(p);
    }

    public BlockPos altar() {
        return altar;
    }

    public void setData(OpenAltarPayload p) {
        this.all = p.candidates();
        this.eligible = all.stream().filter(c -> c.problem().isEmpty()).toList();
        this.cost = p.cost();
        if (ia >= eligible.size()) ia = 0;
        if (ib >= eligible.size() || ib == ia) ib = eligible.size() > 1 ? (ia + 1) % eligible.size() : 0;
        previewKey = "";
    }

    private OpenAltarPayload.Candidate a() {
        return eligible.isEmpty() ? null : eligible.get(ia);
    }

    private OpenAltarPayload.Candidate b() {
        return eligible.size() < 2 ? null : eligible.get(ib);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), x -> cycleA(-1)).bounds(left + 8, top + 104, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), x -> cycleA(1)).bounds(left + 86, top + 104, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("<"), x -> cycleB(-1)).bounds(left + W - 102, top + 104, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), x -> cycleB(1)).bounds(left + W - 24, top + 104, 16, 16).build());

        int bx = left + 10, by = top + 128, bw = 104, bh = 18, gap = 4;
        bodyBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { bodyB = !bodyB; refresh(); }).bounds(bx, by, bw, bh).build());
        headBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { headB = !headB; refresh(); }).bounds(bx + bw + gap, by, bw, bh).build());
        legsBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { legsB = !legsB; refresh(); }).bounds(bx + 2 * (bw + gap), by, bw, bh).build());
        extrasBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { extrasB = !extrasB; refresh(); }).bounds(bx, by + bh + gap, bw, bh).build());
        colorsBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { colors = (colors + 1) % 3; refresh(); }).bounds(bx + bw + gap, by + bh + gap, bw, bh).build());
        moveBtn = addRenderableWidget(Button.builder(Component.empty(), x -> { movementB = !movementB; refresh(); }).bounds(bx + 2 * (bw + gap), by + bh + gap, bw, bh).build());
        spliceBtn = addRenderableWidget(Button.builder(Component.translatable("gui.animalbonds.splice").withStyle(ChatFormatting.BOLD), x -> splice())
                .bounds(left + W / 2 - 50, top + H - 26, 100, 20).build());
        refresh();
    }

    private void cycleA(int d) {
        if (eligible.isEmpty()) return;
        do ia = Math.floorMod(ia + d, eligible.size()); while (ia == ib && eligible.size() > 1);
        refresh();
    }

    private void cycleB(int d) {
        if (eligible.size() < 2) return;
        do ib = Math.floorMod(ib + d, eligible.size()); while (ib == ia);
        refresh();
    }

    private Component ab(String key, boolean fromB) {
        return Component.translatable(key).append(": ").append(Component.literal(fromB ? "B" : "A").withStyle(fromB ? ChatFormatting.AQUA : ChatFormatting.GOLD));
    }

    private void refresh() {
        if (bodyBtn == null) return;
        bodyBtn.setMessage(ab("gui.animalbonds.part_body", bodyB));
        headBtn.setMessage(ab("gui.animalbonds.part_head", headB));
        legsBtn.setMessage(ab("gui.animalbonds.part_legs", legsB));
        extrasBtn.setMessage(ab("gui.animalbonds.part_extras", extrasB));
        moveBtn.setMessage(ab("gui.animalbonds.part_move", movementB));
        colorsBtn.setMessage(Component.translatable("gui.animalbonds.part_colors").append(": ").append(Component.translatable(
                colors == 0 ? "gui.animalbonds.colors_a" : colors == 1 ? "gui.animalbonds.colors_b" : "gui.animalbonds.colors_mix")));
        spliceBtn.active = a() != null && b() != null;
    }

    private Hybrids.Choices choices() {
        return new Hybrids.Choices(bodyB, headB, legsB, extrasB, colors, movementB);
    }

    /** Used by the CI visual test. */
    public void testChoose(boolean head, boolean legs, boolean extras, boolean movement) {
        headB = head;
        legsB = legs;
        extrasB = extras;
        movementB = movement;
        refresh();
    }

    public void testSplice() {
        splice();
    }

    private void splice() {
        if (a() == null || b() == null) return;
        ClientPacketDistributor.sendToServer(new SplicePayload(altar, a().entityId(), b().entityId(), choices()));
    }

    /** A client-only creature showing what the hybrid will look like. */
    private Mob preview() {
        OpenAltarPayload.Candidate ca = a(), cb = b();
        if (ca == null || cb == null || Minecraft.getInstance().level == null) return null;
        String key = ca.entityId() + "/" + cb.entityId() + "/" + choices();
        if (key.equals(previewKey)) return preview;
        previewKey = key;
        OpenAltarPayload.Candidate body = bodyB ? cb : ca, donor = bodyB ? ca : cb;
        preview = null;
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(body.typeId()));
        if (type.isEmpty()) return null;
        Entity e = type.get().create(Minecraft.getInstance().level, EntitySpawnReason.LOAD);
        if (!(e instanceof Mob m)) return null;
        boolean head = headB != bodyB, legs = legsB != bodyB, extras = extrasB != bodyB, move = movementB != bodyB;
        int col = colors == 2 ? 2 : ((colors == 1) != bodyB ? 1 : 0);
        m.setData(ModRegistry.HYBRID.get(), new HybridData(donor.typeId(), head, legs, extras, col, move, ""));
        preview = m;
        return m;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, FRAME);
        g.fill(left, top, left + W, top + H, PANEL);
        g.fill(left + 6, top + 22, left + 104, top + 122, 0x40FFFFFF);
        g.fill(left + W - 104, top + 22, left + W - 6, top + 122, 0x40FFFFFF);
        g.fill(left + 112, top + 22, left + W - 112, top + 122, 0x60B48CFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        centered(g, title.copy().withStyle(ChatFormatting.BOLD), left + W / 2, top + 7, INK);
        OpenAltarPayload.Candidate ca = a(), cb = b();
        if (eligible.size() < 2) {
            centered(g, Component.translatable("gui.animalbonds.altar_need_two"), left + W / 2, top + 60, BAD);
            int y = top + 74;
            for (OpenAltarPayload.Candidate c : all) {
                if (c.problem().isEmpty() || y > top + 118) continue;
                centered(g, Component.literal(c.name() + ": ").append(Component.translatable(c.problem())), left + W / 2, y, SOFT);
                y += 10;
            }
        }
        drawParent(g, ca, "A", left + 6, ChatFormatting.GOLD, mouseX, mouseY);
        drawParent(g, cb, "B", left + W - 104, ChatFormatting.AQUA, mouseX, mouseY);

        Mob p = preview();
        if (p != null) {
            centered(g, Component.translatable("gui.animalbonds.hybrid_preview"), left + W / 2, top + 25, INK);
            float spin = (System.currentTimeMillis() % 6000L) / 6000f * (float) (Math.PI * 2);
            float h = Math.max(0.4F, p.getBbHeight()), w = Math.max(0.4F, p.getBbWidth());
            int size = (int) Math.max(8, Math.min(40, Math.min(70 / h, 70 / w)));
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, left + 114, top + 34, left + W - 114, top + 120, size, 0.0625F,
                    (float) Math.sin(spin) * 0.9F, -0.15F, p);
        }
        centered(g, Component.translatable("gui.animalbonds.splice_cost", cost), left + W / 2, top + H - 38, SOFT);
    }

    private void drawParent(GuiGraphicsExtractor g, OpenAltarPayload.Candidate c, String label, int x, ChatFormatting color, int mouseX, int mouseY) {
        centered(g, Component.literal(label).withStyle(color, ChatFormatting.BOLD), x + 49, top + 25, INK);
        if (c == null) return;
        Entity e = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(c.entityId()) : null;
        if (e instanceof LivingEntity le) {
            float h = Math.max(0.4F, le.getBbHeight()), w = Math.max(0.4F, le.getBbWidth());
            int size = (int) Math.max(8, Math.min(32, Math.min(60 / h, 60 / w)));
            float cx = x + 49, cy = top + 70;
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 4, top + 34, x + 94, top + 104, size, 0.0625F,
                    (float) Math.atan((cx - mouseX) / 40.0F), (float) Math.atan((cy - mouseY) / 40.0F), le);
        }
        centered(g, Component.literal(c.name()), x + 49, top + 104, INK);
        centered(g, Component.translatable("gui.animalbonds.level_short", c.level()), x + 49, top + 113, SOFT);
    }

    private void centered(GuiGraphicsExtractor g, Component c, int cx, int y, int color) {
        g.text(font, c, cx - font.width(c) / 2, y, color, false);
    }
}
