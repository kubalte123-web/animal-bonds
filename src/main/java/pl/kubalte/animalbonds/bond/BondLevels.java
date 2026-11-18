package pl.kubalte.animalbonds.bond;

/** Bond levels 1..10 and the total XP needed to reach each of them. */
public final class BondLevels {
    private BondLevels() {}

    public static final int MAX_LEVEL = 10;
    /** THRESHOLDS[n-1] = total xp required for level n. */
    private static final int[] THRESHOLDS = {0, 60, 160, 300, 480, 720, 1020, 1400, 1860, 2400};

    public static int levelFor(int xp) {
        int lvl = 1;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (xp >= THRESHOLDS[i]) lvl = i + 1;
        }
        return lvl;
    }

    public static int xpForLevel(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) return THRESHOLDS[MAX_LEVEL - 1];
        return THRESHOLDS[level - 1];
    }

    public static int maxXp() {
        return THRESHOLDS[MAX_LEVEL - 1];
    }
}
