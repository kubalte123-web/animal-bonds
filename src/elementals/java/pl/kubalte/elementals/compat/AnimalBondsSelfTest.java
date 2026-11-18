package pl.kubalte.elementals.compat;

import static pl.kubalte.elementals.ElementalsSelfTest.check;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import pl.kubalte.animalbonds.api.BondGiftEvent;
import pl.kubalte.animalbonds.block.PedestalBlockEntity;
import pl.kubalte.animalbonds.bond.BondLogic;
import pl.kubalte.animalbonds.bond.Need;
import pl.kubalte.animalbonds.bond.PenScanner;
import pl.kubalte.animalbonds.bond.SpeciesProfiles;
import pl.kubalte.elementals.Element;
import pl.kubalte.elementals.ElementEvents;
import pl.kubalte.elementals.Elementals;

/** Pen bonuses of elements and species, tested with Animal Bonds present. */
public final class AnimalBondsSelfTest {
    private AnimalBondsSelfTest() {}

    public static void run(ServerLevel level, BlockPos origin) {
        for (int cx = (origin.getX() >> 4) - 1; cx <= (origin.getX() >> 4) + 1; cx++)
            for (int cz = (origin.getZ() >> 4) - 1; cz <= (origin.getZ() >> 4) + 1; cz++) level.setChunkForced(cx, cz, true);
        BlockPos ped = pl.kubalte.animalbonds.SelfTest.buildPen(level, origin);
        Mob cow = EntityTypes.COW.spawn(level, origin.offset(3, 0, 3), EntitySpawnReason.COMMAND);
        ElementEvents.apply(cow, Element.FIRE);
        Mob sheep = EntityTypes.SHEEP.spawn(level, origin.offset(4, 0, 3), EntitySpawnReason.COMMAND);
        ElementEvents.apply(sheep, Element.DRAGON);
        Mob moss = Elementals.MOSSLING.get().spawn(level, origin.offset(3, 0, 5), EntitySpawnReason.COMMAND);
        BondLogic.bond(cow);
        BondLogic.bond(sheep);
        PedestalBlockEntity be = (PedestalBlockEntity) level.getBlockEntity(ped);
        PenScanner.Pen pen = be.updatePen(level);
        check(pen != null, "AB: pen found");
        if (pen != null) {
            check(pen.forcedNeeds.contains(Need.WARMTH), "AB: fire creature warms the whole pen");
            check(pen.forcedNeeds.contains(Need.GRASS), "AB: Mchowiec gives the pen grass");
            check(pen.xpMultiplier >= 1.5F && pen.giftBonus >= 1, "AB: dragon creature boosts bond xp and gifts");
        }
        BondGiftEvent ev = NeoForge.EVENT_BUS.post(new BondGiftEvent(sheep, new ItemStack(Items.BEEF, 2)));
        check(ev.getStack().is(Items.COOKED_BEEF), "AB: gifts in a pen with fire come out cooked");
        check(SpeciesProfiles.hasProfile(moss), "AB: Mchowiec has its own needs and gifts");
        check(SpeciesProfiles.isFavourite(moss, new ItemStack(Items.SWEET_BERRIES)), "AB: Mchowiec loves sweet berries");
        // a crop next to the pen grows thanks to the Mchowiec
        BlockPos crop = origin.offset(2, 0, 9);
        level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
        for (int i = 0; i < 30; i++) be.updatePen(level);
        check(level.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0, "AB: Mchowiec makes crops near the pen grow");
        for (Mob m : new Mob[] {cow, sheep, moss}) if (m != null) m.discard();
    }
}
