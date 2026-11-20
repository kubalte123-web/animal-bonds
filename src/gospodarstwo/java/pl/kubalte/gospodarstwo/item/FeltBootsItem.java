package pl.kubalte.gospodarstwo.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Walonki: warm felt boots that let you walk on powder snow like leather boots. */
public class FeltBootsItem extends Item {
    public FeltBootsItem(Properties props) {
        super(props);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return true;
    }
}
