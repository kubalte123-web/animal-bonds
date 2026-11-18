package pl.kubalte.animalbonds.api;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/** Fired when a bonded creature is about to give a gift. Change the stack (or set it empty) to modify it. */
public class BondGiftEvent extends Event {
    private final Mob creature;
    private ItemStack stack;

    public BondGiftEvent(Mob creature, ItemStack stack) {
        this.creature = creature;
        this.stack = stack;
    }

    public Mob getCreature() {
        return creature;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }
}
