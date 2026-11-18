package pl.kubalte.animalbonds.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import pl.kubalte.animalbonds.AnimalBonds;
import pl.kubalte.animalbonds.bond.BondLevels;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.network.BookEntry;
import pl.kubalte.animalbonds.network.OpenBagsPayload;
import pl.kubalte.animalbonds.network.RenamePayload;

/**
 * The pen book. Top: creature name. Left page: the live creature (its real, current animation).
 * Right page: bond level, mood, needs, gifts. Arrows switch between creatures.
 */
public class BondBookScreen extends Screen {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AnimalBonds.MODID, "textures/gui/bond_book.png");
    public static final int W = 320, H = 200;

    private static final int INK = 0xFF3B2A1A;
    private static final int INK_SOFT = 0xFF6B5340;
    private static final int GOOD = 0xFF2E7D32;
    private static final int BAD = 0xFFB23A2E;

    private final BlockPos pedestal;
    private int area;
    private List<BookEntry> entries;
    private int index;

    private int left, top;
    private EditBox nameBox;
    private Button nameButton, bagsButton, prevButton, nextButton;

    public BondBookScreen(BlockPos pedestal, int area, List<BookEntry> entries) {
        super(Component.translatable("gui.animalbonds.book"));
        this.pedestal = pedestal;
        this.area = area;
        this.entries = entries;
    }

    public BlockPos pedestal() {
        return pedestal;
    }

    /** Refresh with new data from the server, staying on the same creature if possible. */
    public void update(int area, List<BookEntry> newEntries) {
        int currentId = current() != null ? current().entityId() : -1;
        this.area = area;
        this.entries = newEntries;
        this.index = 0;
        for (int i = 0; i < newEntries.size(); i++) {
            if (newEntries.get(i).entityId() == currentId) index = i;
        }
        refreshWidgets();
    }

    private BookEntry current() {
        return entries.isEmpty() ? null : entries.get(Math.floorMod(index, entries.size()));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;

        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> flipPage(-1)).bounds(left + 14, top + H - 30, 20, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> flipPage(1)).bounds(left + W - 34, top + H - 30, 20, 20).build());

        nameBox = new EditBox(font, left + 38, top + H - 54, 84, 16, Component.translatable("gui.animalbonds.name"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);
        nameButton = addRenderableWidget(Button.builder(Component.translatable("gui.animalbonds.set_name"), b -> sendName())
                .bounds(left + 124, top + H - 55, 30, 18).build());
        bagsButton = addRenderableWidget(Button.builder(Component.translatable("gui.animalbonds.bags"), b -> openBags())
                .bounds(left + W / 2 + 12, top + H - 30, 60, 20).build());
        refreshWidgets();
    }

    private void refreshWidgets() {
        if (nameBox == null) return;
        BookEntry e = current();
        boolean many = entries.size() > 1;
        prevButton.visible = many;
        nextButton.visible = many;
        boolean canName = e != null && e.bonded() && e.level() >= 1;
        nameBox.visible = canName;
        nameButton.visible = canName;
        if (canName) nameBox.setValue(e.customName());
        bagsButton.visible = e != null && e.bags();
    }

    public void flipPage(int dir) {
        if (entries.isEmpty()) return;
        index = Math.floorMod(index + dir, entries.size());
        refreshWidgets();
    }

    private void sendName() {
        BookEntry e = current();
        if (e != null) ClientPacketDistributor.sendToServer(new RenamePayload(pedestal, e.entityId(), nameBox.getValue()));
    }

    private void openBags() {
        BookEntry e = current();
        if (e != null) ClientPacketDistributor.sendToServer(new OpenBagsPayload(pedestal, e.entityId()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, W, H, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        BookEntry e = current();
        int midX = left + W / 2;

        if (e == null) {
            centered(g, Component.translatable("gui.animalbonds.empty"), left + W / 4, top + 90, INK);
            centered(g, Component.translatable("gui.animalbonds.area", area), left + 3 * W / 4, top + 90, INK_SOFT);
            return;
        }

        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(e.entityId()) : null;

        // ---- Header: name across the top ----
        Component species = speciesName(e, entity);
        Component title = e.customName().isEmpty() ? species : Component.literal(e.customName());
        centered(g, title.copy().withStyle(ChatFormatting.BOLD), left + W / 4, top + 14, INK);
        if (!e.customName().isEmpty()) centered(g, species, left + W / 4, top + 25, INK_SOFT);
        centered(g, Component.literal((Math.floorMod(index, entries.size()) + 1) + " / " + entries.size()), left + W / 4 + 6, top + H - 24, INK_SOFT);

        // ---- Left page: the live creature ----
        int bx0 = left + 22, by0 = top + 36, bx1 = left + W / 2 - 18, by1 = top + H - 60;
        if (entity instanceof LivingEntity living) {
            float h = Math.max(0.3F, living.getBbHeight());
            float w = Math.max(0.3F, living.getBbWidth());
            int size = (int) Math.max(6, Math.min(45, Math.min(70 / h, 60 / w)));
            float cx = (bx0 + bx1) / 2f, cy = (by0 + by1) / 2f;
            float xAngle = (float) Math.atan((cx - mouseX) / 40.0F);
            float yAngle = (float) Math.atan((cy - mouseY) / 40.0F);
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, bx0, by0, bx1, by1, size, 0.0625F, xAngle, yAngle, living);
        } else {
            centered(g, Component.translatable("gui.animalbonds.out_of_sight"), (bx0 + bx1) / 2, (by0 + by1) / 2, INK_SOFT);
        }
        if (!e.bonded()) {
            centered(g, Component.translatable("gui.animalbonds.hint_bond"), left + W / 4, top + H - 50, INK_SOFT);
        }

        // ---- Right page: bond, mood, needs ----
        int x = midX + 14;
        int y = top + 14;
        int pageW = W / 2 - 30;
        if (!e.bonded()) {
            text(g, Component.translatable("gui.animalbonds.no_bond").withStyle(ChatFormatting.BOLD), x, y, INK);
            y += 14;
            y = wrapped(g, Component.translatable("gui.animalbonds.no_bond_hint"), x, y, pageW, INK_SOFT);
        } else {
            text(g, Component.translatable("gui.animalbonds.level", e.level(), BondLevels.MAX_LEVEL).withStyle(ChatFormatting.BOLD), x, y, INK);
            y += 11;
            // hearts: one per level, xp numbers on the right
            StringBuilder hearts = new StringBuilder();
            for (int i = 1; i <= BondLevels.MAX_LEVEL; i++) hearts.append(i <= e.level() ? '\u2764' : '\u2661');
            text(g, Component.literal(hearts.toString()), x, y, 0xFFC0392B);
            Component xpText = Component.literal(e.xp() + "/" + e.xpTo());
            text(g, xpText, x + pageW - font.width(xpText), y, INK_SOFT);
            y += 10;
            // xp bar
            g.fill(x, y, x + pageW, y + 4, 0xFF8D7B62);
            float frac = e.xpTo() > e.xpFrom() ? (e.xp() - e.xpFrom()) / (float) (e.xpTo() - e.xpFrom()) : 1F;
            g.fill(x, y, x + (int) (pageW * Math.min(1F, Math.max(0F, frac))), y + 4, 0xFFD4A017);
            y += 7;
            int mood = e.happiness();
            Component moodText = mood < 0 ? Component.translatable("gui.animalbonds.mood_unknown")
                    : Component.translatable("gui.animalbonds.mood", mood);
            text(g, moodText, x, y, mood < 0 ? INK_SOFT : mood >= 75 ? GOOD : mood >= 50 ? INK : BAD);
            y += 10;
            if (e.faintTicks() > 0) {
                text(g, Component.translatable("gui.animalbonds.fainted", time(e.faintTicks())), x, y, BAD);
                y += 10;
            } else if (e.giftTicks() >= 0) {
                text(g, Component.translatable("gui.animalbonds.gift", time(e.giftTicks())), x, y, INK_SOFT);
                y += 10;
            }
        }
        y += 2;
        text(g, Component.translatable("gui.animalbonds.needs").withStyle(ChatFormatting.UNDERLINE), x, y, INK);
        y += 10;
        for (int code : e.needs()) {
            Need need = Need.byOrdinal(code / 2);
            boolean ok = (code & 1) == 1 && e.bonded();
            String mark = !e.bonded() ? "• " : ok ? "✔ " : "✘ ";
            MutableComponent line = Component.literal(mark).append(Component.translatable(need.translationKey()));
            text(g, line, x, y, !e.bonded() ? INK_SOFT : ok ? GOOD : BAD);
            y += 9;
            if (y > top + H - 40) break;
        }
        if (!e.favourites().isEmpty() && y < top + H - 40) {
            y += 2;
            MutableComponent fav = Component.translatable("gui.animalbonds.favourite").append(" ");
            String first = e.favourites().get(0);
            if (!first.startsWith("#")) {
                Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(first)).orElse(null);
                if (item != null) fav.append(item.getName(item.getDefaultInstance()));
            } else {
                fav.append(Component.literal(first.substring(first.indexOf(':') + 1)));
            }
            wrapped(g, fav, x, y, pageW, INK_SOFT);
        }
    }

    private Component speciesName(BookEntry e, Entity entity) {
        if (entity != null) return entity.getType().getDescription();
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(e.typeId())).map(EntityType::getDescription).orElse(Component.literal(e.typeId()));
    }

    private static String time(int ticks) {
        int s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private void text(GuiGraphicsExtractor g, Component c, int x, int y, int color) {
        g.text(font, c, x, y, color, false);
    }

    private void centered(GuiGraphicsExtractor g, Component c, int cx, int y, int color) {
        g.text(font, c, cx - font.width(c) / 2, y, color, false);
    }

    private int wrapped(GuiGraphicsExtractor g, Component c, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(c, w)) {
            g.text(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }
}
