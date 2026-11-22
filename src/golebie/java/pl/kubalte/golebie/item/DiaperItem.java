package pl.kubalte.golebie.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A pigeon diaper: right-click a tame bird to put it on, shears take it off. */
public class DiaperItem extends Item {
    public final int pattern;

    public DiaperItem(Properties p, int pattern) {
        super(p);
        this.pattern = pattern;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("item.golebie.diaper.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
