package pl.kubalte.gospodarstwo.client.goblin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import pl.kubalte.gospodarstwo.goblin.Goblin;
import pl.kubalte.gospodarstwo.goblin.GoblinMenu;
import pl.kubalte.gospodarstwo.goblin.Traits;
import pl.kubalte.gospodarstwo.goblin.Work;

/**
 * Everything about one goblin: its race and mood, its needs, what it likes, dislikes and fears, and - when it holds a
 * tool - how it should work with it (with pictures to tick which woods to cut or which ores to keep).
 */
public class GoblinInfoScreen extends Screen {
    static final int W = 300, H = 236;
    static final int PANEL = 0xFFE9DCC0, FRAME = 0xFF5A3E24, INK = 0xFF3A2A1A, SOFT = 0xFF7A6248, GOOD = 0xFF4E8A3A, BAD = 0xFFA8423A;
    private final GoblinScreen parent;
    private final GoblinMenu menu;
    private int left, top;
    private final List<Button> grid = new ArrayList<>();
    private final List<Integer> gridIds = new ArrayList<>();
    private final List<ItemStack> gridIcons = new ArrayList<>();
    private Work.Tool shown;

    public GoblinInfoScreen(GoblinScreen parent, GoblinMenu menu) {
        super(Component.translatable("goblin.gospodarstwo.info"));
        this.parent = parent;
        this.menu = menu;
    }

    Goblin g() {
        return menu.goblin;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo.back"), b -> onClose()).bounds(left + W - 58, top + H - 22, 52, 16)
                .build());
        grid.clear();
        gridIds.clear();
        gridIcons.clear();
        shown = g().tool();
        int x = left + 160, y = top + 52;
        switch (shown) {
            case AXE -> {
                modes(x, y, 10, "axe", 3);
                for (int i = 0; i < Work.WOODS.size(); i++) icon(x + (i % 6) * 22, y + 44 + (i / 6) * 22, 100 + i, new ItemStack(Work.woodIcon(i)));
            }
            case PICKAXE -> {
                modes(x, y, 13, "pick", 2);
                for (int i = 0; i < Work.MINE.size(); i++) icon(x + (i % 6) * 22, y + 26 + (i / 6) * 22, 200 + i, new ItemStack(Work.mineIcon(i)));
            }
            case HOE -> modes(x, y, 15, "hoe", 3);
            case SHEARS -> addRenderableWidget(Button.builder(Component.empty(), b -> press(18)).bounds(x, y, 132, 16).build());
            case SWORD -> addRenderableWidget(Button.builder(Component.empty(), b -> press(19)).bounds(x, y, 132, 16).build());
            default -> {}
        }
    }

    void modes(int x, int y, int firstId, String key, int n) {
        for (int i = 0; i < n; i++) {
            final int id = firstId + i;
            addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo." + key + "." + i), b -> press(id))
                    .bounds(x + (i % 2) * 68, y + (i / 2) * 18, 64, 16).build());
        }
    }

    void icon(int x, int y, int id, ItemStack stack) {
        Button b = addRenderableWidget(Button.builder(Component.empty(), btn -> press(id)).bounds(x, y, 20, 20).build());
        grid.add(b);
        gridIds.add(id);
        gridIcons.add(stack);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (g().tool() != shown) init(Minecraft.getInstance(), width, height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, FRAME);
        g.fill(left, top, left + W, top + H, PANEL);
        g.fill(left + 152, top + 8, left + 153, top + H - 8, 0xFFC8B89A);
    }

    static MutableComponent tr(String key) {
        return Component.translatable("goblin.gospodarstwo." + key);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Goblin gob = g();
        Traits t = gob.traits();
        int x = left + 10, y = top + 10;
        g.text(font, gob.getDisplayName().copy().withStyle(ChatFormatting.BOLD), x, y, INK, false);
        g.text(font, tr("race." + gob.race().id()), x, y + 11, SOFT, false);
        // mood
        int mood = gob.mood();
        String face = mood >= 70 ? "happy" : mood >= 40 ? "ok" : mood >= 15 ? "sad" : "sulking";
        g.text(font, tr("mood").append(": ").append(tr("mood." + face)), x, y + 26, mood >= 40 ? GOOD : BAD, false);
        // needs
        int[] n = gob.needs();
        String[] names = {"need.hunger", "need.energy", "need.fun", "need.company"};
        for (int i = 0; i < 4; i++) {
            int yy = y + 40 + i * 12;
            g.text(font, tr(names[i]), x, yy, INK, false);
            int bx = x + 62;
            g.fill(bx, yy + 1, bx + 70, yy + 8, 0xFF8B7356);
            int c = n[i] >= 60 ? GOOD : n[i] >= 30 ? 0xFFC8A040 : BAD;
            g.fill(bx + 1, yy + 2, bx + 1 + Math.round(68 * n[i] / 100F), yy + 7, c);
        }
        // likes
        int yy = y + 92;
        g.text(font, tr("likes").withStyle(ChatFormatting.BOLD), x, yy, GOOD, false);
        int ix = x;
        for (String id : t.likedFoods) {
            g.item(new ItemStack(Traits.item(id)), ix, yy + 10);
            ix += 18;
        }
        g.text(font, tr("pastime." + t.pastime.name().toLowerCase()), ix + 2, yy + 14, INK, false);
        // dislikes
        yy += 32;
        g.text(font, tr("dislikes").withStyle(ChatFormatting.BOLD), x, yy, BAD, false);
        ix = x;
        for (String id : t.hatedFoods) {
            g.item(new ItemStack(Traits.item(id)), ix, yy + 10);
            ix += 18;
        }
        StringBuilder sb = new StringBuilder();
        List<Component> bothers = new ArrayList<>();
        for (Traits.Bother b : t.bothers) bothers.add(tr("bother." + b.name().toLowerCase()));
        g.textWithWordWrap(font, join(bothers), ix + 2, yy + 11, 140 - (ix - x), INK, false);
        // fears
        yy += 34;
        g.text(font, tr("fears").withStyle(ChatFormatting.BOLD), x, yy, 0xFF6A3FA0, false);
        List<Component> fears = new ArrayList<>();
        for (Traits.Fear f : t.fears) fears.add(tr("fear." + f.name().toLowerCase()));
        g.textWithWordWrap(font, join(fears), x, yy + 11, 138, INK, false);
        // now doing
        g.text(font, tr("doing").append(" ").append(tr("work." + gob.work())).withStyle(ChatFormatting.ITALIC), x, top + H - 18, SOFT, false);

        // ---- work with the tool
        int rx = left + 160, ry = top + 10;
        g.text(font, tr("work_title").withStyle(ChatFormatting.BOLD), rx, ry, INK, false);
        ItemStack held = gob.getMainHandItem();
        Work w = gob.settings();
        if (shown == Work.Tool.NONE || shown == Work.Tool.CAN) {
            g.textWithWordWrap(font, tr(shown == Work.Tool.CAN ? "work_can" : "work_hands"), rx, ry + 16, 132, SOFT, false);
            return;
        }
        g.item(held, rx, ry + 14);
        g.text(font, held.getHoverName(), rx + 20, ry + 18, INK, false);
        int mode = switch (shown) {
            case AXE -> w.axe;
            case PICKAXE -> w.pick;
            case HOE -> w.hoe;
            default -> -1;
        };
        if (shown == Work.Tool.SHEARS || shown == Work.Tool.SWORD) {
            boolean on = shown == Work.Tool.SHEARS ? w.shear : w.guard;
            g.centeredText(font, tr((shown == Work.Tool.SHEARS ? "shear." : "guard.") + (on ? "on" : "off")), rx + 66, top + 56, on ? 0xFF2E5A20 : 0xFF7A2020);
            g.textWithWordWrap(font, tr(shown == Work.Tool.SHEARS ? "work_shears" : "work_sword"), rx, top + 76, 134, SOFT, false);
            return;
        }
        String key = shown == Work.Tool.AXE ? "axe" : shown == Work.Tool.PICKAXE ? "pick" : "hoe";
        g.text(font, tr("chosen").append(": ").append(tr(key + "." + mode)), rx, ry + 34 + (shown == Work.Tool.PICKAXE ? 0 : 0) - 2, GOOD, false);
        if (shown == Work.Tool.AXE) g.text(font, tr("woods"), rx, top + 52 + 34, SOFT, false);
        if (shown == Work.Tool.PICKAXE) g.text(font, tr("keep"), rx, top + 52 + 17, SOFT, false);
        if (shown == Work.Tool.HOE) g.textWithWordWrap(font, tr("work_hoe"), rx, top + 92, 134, SOFT, false);
        for (int i = 0; i < grid.size(); i++) {
            Button b = grid.get(i);
            int id = gridIds.get(i);
            boolean on = id >= 200 ? w.keeps(id - 200) : w.wood(id - 100);
            g.item(gridIcons.get(i), b.getX() + 2, b.getY() + 2);
            if (!on) g.fill(b.getX() + 1, b.getY() + 1, b.getX() + 19, b.getY() + 19, 0xA0303030);
            else g.outline(b.getX(), b.getY(), 20, 20, 0xFF4EC03A);
            if (b.isHovered()) g.setTooltipForNextFrame(font, gridIcons.get(i), mouseX, mouseY);
        }
    }

    static Component join(List<Component> parts) {
        MutableComponent c = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) c.append(", ");
            c.append(parts.get(i));
        }
        return c;
    }
}
