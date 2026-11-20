package pl.kubalte.gospodarstwo.goblin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * The goblin's menu: its clothes and armour, the things in its hands, the chest on its back and what is in it, and
 * buttons for what it should do (follow / stay at the base / wait / store everything in the chests).
 */
public class GoblinMenu extends AbstractContainerMenu {
    public static final int EQUIP = 6, PACK_SLOT = 6, PACK0 = 7, PACK_N = 27, PLAYER0 = PACK0 + PACK_N;
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    public final Goblin goblin;
    private final Container gear, packFlag;

    public static void open(ServerPlayer player, Goblin g) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GoblinMenu(id, inv, g), g.getDisplayName()), buf -> buf.writeVarInt(g.getId()));
    }

    /** Client side: finds the goblin by its id. */
    public static GoblinMenu client(int id, Inventory inv, int entityId) {
        Entity e = inv.player.level().getEntity(entityId);
        return new GoblinMenu(id, inv, e instanceof Goblin g ? g : new Goblin(Gospodarstwo.GOBLIN.get(), inv.player.level()));
    }

    public GoblinMenu(int id, Inventory inv, Goblin g) {
        super(Gospodarstwo.GOBLIN_MENU.get(), id);
        this.goblin = g;
        this.gear = new GearContainer(g);
        this.packFlag = new PackFlag(g);
        // armour column on the left, hands below it
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot slot = SLOTS[i];
            addSlot(new Slot(gear, i, 8, 18 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    var eq = s.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                    return eq != null && eq.slot() == slot;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addSlot(new Slot(gear, 4, 8, 94));
        addSlot(new Slot(gear, 5, 26, 94));
        // the chest on its back
        addSlot(new Slot(packFlag, 0, 80, 94) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(Items.CHEST);
            }

            @Override
            public boolean mayPickup(Player p) {
                return goblin.pack.isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) {
            addSlot(new Slot(g.pack, c + r * 9, 8 + c * 18 + 0, 122 + r * 18) {
                @Override
                public boolean isActive() {
                    return goblin.hasPack();
                }

                @Override
                public boolean mayPlace(ItemStack s) {
                    return goblin.hasPack();
                }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 184 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 242));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!goblin.isOwnedBy(player)) return false;
        switch (id) {
            case 0 -> goblin.setMode(Goblin.MODE_FOLLOW);
            case 1 -> goblin.setMode(Goblin.MODE_BASE);
            case 2 -> goblin.setMode(Goblin.MODE_SIT);
            case 3 -> {
                if (!goblin.hasPack() || goblin.pack.isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.nothing"));
                    return true;
                }
                goblin.startDeposit();
                player.closeContainer();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < PLAYER0) {
            if (!moveItemStackTo(s, PLAYER0, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i <= PACK_SLOT && !moved; i++) {
                Slot t = slots.get(i);
                if (!t.hasItem() && t.mayPlace(s)) moved = moveItemStackTo(s, i, i + 1, false);
            }
            if (!moved && goblin.hasPack()) moved = moveItemStackTo(s, PACK0, PLAYER0, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return goblin.isAlive() && player.distanceToSqr(goblin) < 64;
    }

    /** The goblin's equipment as a container. */
    static final class GearContainer implements Container {
        private final Goblin g;

        GearContainer(Goblin g) {
            this.g = g;
        }

        @Override
        public int getContainerSize() {
            return SLOTS.length;
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot s : SLOTS) if (!g.getItemBySlot(s).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int i) {
            return g.getItemBySlot(SLOTS[i]);
        }

        @Override
        public ItemStack removeItem(int i, int n) {
            ItemStack s = g.getItemBySlot(SLOTS[i]);
            ItemStack r = s.split(n);
            g.setItemSlot(SLOTS[i], s);
            return r;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = g.getItemBySlot(SLOTS[i]);
            g.setItemSlot(SLOTS[i], ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setItem(int i, ItemStack s) {
            g.setItemSlot(SLOTS[i], s);
            g.setDropChance(SLOTS[i], 2F); // always drops what you gave it
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player p) {
            return g.isAlive();
        }

        @Override
        public void clearContent() {
            for (EquipmentSlot s : SLOTS) g.setItemSlot(s, ItemStack.EMPTY);
        }
    }

    /** One slot that holds the chest the goblin carries on its back. */
    static final class PackFlag implements Container {
        private final Goblin g;

        PackFlag(Goblin g) {
            this.g = g;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return !g.hasPack();
        }

        @Override
        public ItemStack getItem(int i) {
            return g.hasPack() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int i, int n) {
            if (!g.hasPack() || !g.pack.isEmpty()) return ItemStack.EMPTY;
            g.setPack(false);
            return new ItemStack(Items.CHEST);
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            return removeItem(i, 1);
        }

        @Override
        public void setItem(int i, ItemStack s) {
            g.setPack(s.is(Items.CHEST));
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player p) {
            return g.isAlive();
        }

        @Override
        public void clearContent() {
            if (g.pack.isEmpty()) g.setPack(false);
        }
    }
}
