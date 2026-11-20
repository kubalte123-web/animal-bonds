package pl.kubalte.gospodarstwo.client.goblin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.kubalte.gospodarstwo.goblin.Goblin;
import pl.kubalte.gospodarstwo.goblin.GoblinMenu;

/** The goblin's menu: its gear on the left, the goblin itself, mode buttons on the right, the backpack and your inventory below. */
public class GoblinScreen extends AbstractContainerScreen<GoblinMenu> {
    static final int W = 176, H = 266;
    static final int PANEL = 0xFFE9DCC0, FRAME = 0xFF5A3E24, SLOT = 0xFF8B7356, SLOT_IN = 0xFF3A2E22, INK = 0xFF3A2A1A, SOFT = 0xFF7A6248;
    private Button follow, base, sit, store;

    public GoblinScreen(GoblinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 98, y = topPos + 18;
        follow = addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo.mode.follow"), b -> press(0)).bounds(x, y, 72, 16).build());
        base = addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo.mode.base"), b -> press(1)).bounds(x, y + 18, 72, 16).build());
        sit = addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo.mode.sit"), b -> press(2)).bounds(x, y + 36, 72, 16).build());
        store = addRenderableWidget(Button.builder(Component.translatable("goblin.gospodarstwo.mode.store"), b -> press(3)).bounds(x, y + 58, 72, 16).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int m = menu.goblin.mode();
        follow.active = m != Goblin.MODE_FOLLOW;
        base.active = m != Goblin.MODE_BASE;
        sit.active = m != Goblin.MODE_SIT;
        store.active = menu.goblin.hasPack() && !menu.goblin.pack.isEmpty();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int l = leftPos, t = topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, FRAME);
        g.fill(l, t, l + W, t + H, PANEL);
        // the goblin, looking at the mouse
        g.fill(l + 30, t + 16, l + 94, t + 112, 0x30000000);
        float ax = (float) Math.atan((l + 62 - mouseX) / 40F), ay = (float) Math.atan((t + 50 - mouseY) / 40F);
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, l + 30, t + 16, l + 94, t + 112, 70, 0.0625F, ax, ay, menu.goblin);
        for (Slot s : menu.slots) {
            boolean on = s.isActive();
            int c = on ? SLOT : 0xFFC8B89A;
            g.fill(l + s.x - 1, t + s.y - 1, l + s.x + 17, t + s.y + 17, c);
            g.fill(l + s.x, t + s.y, l + s.x + 16, t + s.y + 16, on ? SLOT_IN : 0xFFB8A888);
        }
        if (!menu.goblin.hasPack()) {
            g.centeredText(font, Component.translatable("goblin.gospodarstwo.no_pack").withStyle(ChatFormatting.ITALIC), l + W / 2, t + 145, SOFT);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 8, 5, INK, false);
        g.text(font, Component.translatable("goblin.gospodarstwo.hands"), 8, 84, SOFT, false);
        g.text(font, Component.translatable("goblin.gospodarstwo.pack"), 80, 84, SOFT, false);
        g.text(font, Component.translatable("goblin.gospodarstwo.work." + menu.goblin.work()).withStyle(ChatFormatting.ITALIC), 98, 100, SOFT, false);
        g.text(font, Component.translatable("goblin.gospodarstwo.backpack"), 8, 112, SOFT, false);
        g.text(font, playerInventoryTitle, 8, 174, SOFT, false);
    }
}
