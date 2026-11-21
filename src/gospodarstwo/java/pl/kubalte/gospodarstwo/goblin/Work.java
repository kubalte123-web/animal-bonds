package pl.kubalte.gospodarstwo.goblin;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/** Which tool the goblin holds, and how you told it to use that tool (set in its info page). */
public final class Work {
    public enum Tool { NONE, AXE, PICKAXE, HOE, SHEARS, SWORD, CAN }

    public static Tool toolOf(ItemStack s) {
        if (s.isEmpty()) return Tool.NONE;
        if (s.is(ItemTags.AXES)) return Tool.AXE;
        if (s.is(ItemTags.PICKAXES)) return Tool.PICKAXE;
        if (s.is(ItemTags.HOES) || s.is(Gospodarstwo.SCYTHE.get()) || s.is(Gospodarstwo.SICKLE.get())) return Tool.HOE;
        if (s.is(Items.SHEARS)) return Tool.SHEARS;
        if (s.is(ItemTags.SWORDS) || s.is(Items.TRIDENT) || s.is(Items.MACE)) return Tool.SWORD;
        if (s.is(Gospodarstwo.WATERING_CAN.get())) return Tool.CAN;
        return Tool.NONE;
    }

    // ---- axe: 0 = do nothing, 1 = only chop, 2 = chop and plant saplings again
    public static final int AXE_OFF = 0, AXE_CHOP = 1, AXE_REPLANT = 2;
    // ---- pickaxe: 0 = do nothing, 1 = dig its own mine
    public static final int PICK_OFF = 0, PICK_MINE = 1;
    // ---- hoe: 0 = nothing, 1 = harvest only, 2 = harvest, till and sow
    public static final int HOE_OFF = 0, HOE_HARVEST = 1, HOE_FARM = 2;

    /** Woods the axe may cut (one bit each, all on by default). */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak",
            "crimson", "warped");
    /** What the mine keeps (everything else it digs through is thrown away). */
    public static final List<String> MINE = List.of("coal_ore", "iron_ore", "copper_ore", "gold_ore", "redstone_ore", "lapis_ore", "diamond_ore",
            "emerald_ore", "stone", "deepslate", "granite", "diorite", "andesite", "tuff", "gravel", "dirt", "clay", "amethyst_cluster");

    public int axe = AXE_REPLANT, pick = PICK_MINE, hoe = HOE_FARM;
    public boolean shear = true, guard = true;
    public int woods = (1 << WOODS.size()) - 1;
    /** By default the mine keeps all the ores (the first 8 entries). */
    public long mine = 0xFFL;

    public boolean wood(int i) {
        return (woods >> i & 1) != 0;
    }

    public boolean keeps(int i) {
        return (mine >> i & 1L) != 0;
    }

    /** Which wood (index in WOODS) a log block is, or -1. */
    public static int woodOf(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        for (int i = WOODS.size() - 1; i >= 0; i--) if (id.startsWith(WOODS.get(i) + "_") || id.startsWith("stripped_" + WOODS.get(i) + "_")) return i;
        return -1;
    }

    /** Which mine entry (index in MINE) a block counts as, or -1. Deepslate ores count as the ore. */
    public static int mineOf(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (id.startsWith("deepslate_") && id.endsWith("_ore")) id = id.substring("deepslate_".length());
        if (id.equals("cobblestone")) id = "stone";
        if (id.equals("cobbled_deepslate")) id = "deepslate";
        if (id.equals("nether_gold_ore")) id = "gold_ore";
        return MINE.indexOf(id);
    }

    /** The picture for an entry in the info page. */
    public static Item woodIcon(int i) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(WOODS.get(i) + (i >= 9 ? "_stem" : "_log")));
    }

    public static Item mineIcon(int i) {
        String id = MINE.get(i);
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id.equals("stone") ? "cobblestone" : id.equals("deepslate")
                ? "cobbled_deepslate" : id));
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("axe", axe);
        t.putInt("pick", pick);
        t.putInt("hoe", hoe);
        t.putBoolean("shear", shear);
        t.putBoolean("guard", guard);
        t.putInt("woods", woods);
        t.putLong("mine", mine);
        return t;
    }

    /** Packed for syncing to the client: modes and woods in one int, the mine filter in another. */
    public int packA() {
        return axe | pick << 2 | hoe << 3 | (shear ? 1 : 0) << 5 | (guard ? 1 : 0) << 6 | woods << 7;
    }

    public int packB() {
        return (int) mine;
    }

    public static Work unpack(int a, int b) {
        Work w = new Work();
        w.axe = a & 3;
        w.pick = a >> 2 & 1;
        w.hoe = a >> 3 & 3;
        w.shear = (a >> 5 & 1) != 0;
        w.guard = (a >> 6 & 1) != 0;
        w.woods = a >>> 7;
        w.mine = b & 0xFFFFFFFFL;
        return w;
    }

    public static Work load(CompoundTag t) {
        Work w = new Work();
        w.axe = t.getIntOr("axe", AXE_REPLANT);
        w.pick = t.getIntOr("pick", PICK_MINE);
        w.hoe = t.getIntOr("hoe", HOE_FARM);
        w.shear = t.getBooleanOr("shear", true);
        w.guard = t.getBooleanOr("guard", true);
        w.woods = t.getIntOr("woods", (1 << WOODS.size()) - 1);
        w.mine = t.getLongOr("mine", 0xFFL);
        return w;
    }

    /** Handles a button from the info page; true if it was a work-setting button. */
    public boolean click(int id) {
        if (id >= 10 && id <= 12) axe = id - 10;
        else if (id >= 13 && id <= 14) pick = id - 13;
        else if (id >= 15 && id <= 17) hoe = id - 15;
        else if (id == 18) shear = !shear;
        else if (id == 19) guard = !guard;
        else if (id >= 100 && id < 100 + WOODS.size()) woods ^= 1 << (id - 100);
        else if (id >= 200 && id < 200 + MINE.size()) mine ^= 1L << (id - 200);
        else return false;
        return true;
    }

    static boolean isLog(BlockState s) {
        return s.is(net.minecraft.tags.BlockTags.LOGS);
    }

    static boolean isLeaves(Block b) {
        return b.defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES);
    }
}
