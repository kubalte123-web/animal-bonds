package pl.kubalte.gospodarstwo.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pl.kubalte.gospodarstwo.craft.Grooming;

/** Zgrzebło: brushing horses, goats, camels and dogs gives hair and fibre without hurting them. */
public class CurryCombItem extends Item {
    public CurryCombItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return Grooming.brush(stack, player, target, hand);
    }
}
