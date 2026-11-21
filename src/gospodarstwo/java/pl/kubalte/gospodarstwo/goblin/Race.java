package pl.kubalte.gospodarstwo.goblin;

/** Goblin races: each is built differently, has its own strengths and its own leanings in what it likes and fears. */
public enum Race {
    // health, speed, attack, mining, chopping, farming, dodge chance
    FOREST(16, 0.32, 2.0, 1.0F, 1.5F, 1.3F, 0.35F, 0x6E9A46),
    STONE(26, 0.26, 3.5, 1.6F, 1.0F, 0.9F, 0.15F, 0x8A8A86),
    SWAMP(16, 0.30, 2.0, 0.8F, 1.0F, 1.4F, 0.30F, 0x6C7A38),
    CAVE(13, 0.33, 2.5, 1.3F, 0.8F, 0.8F, 0.55F, 0xC8C2B0);

    public final double health, speed, attack;
    public final float mining, chopping, farming, dodge;
    public final int skin;

    Race(double health, double speed, double attack, float mining, float chopping, float farming, float dodge, int skin) {
        this.health = health;
        this.speed = speed;
        this.attack = attack;
        this.mining = mining;
        this.chopping = chopping;
        this.farming = farming;
        this.dodge = dodge;
        this.skin = skin;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Race byId(int i) {
        Race[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
