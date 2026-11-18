package pl.kubalte.animalbonds.bond;

/** Needs a creature can have. Labels are translated via "need.animalbonds.<id>". */
public enum Need {
    TROUGH("trough"),       // feeding trough with food in the pen
    BED("bed"),             // enough animal beds in the pen
    ROOF("roof"),           // roof above its head
    WATER("water"),         // water in the pen
    GRASS("grass"),         // grass ground in the pen
    SPACE("space"),         // enough space per creature
    COMPANY("company"),     // another creature of the same species
    TREAT("treat"),         // hand-fed its favourite food today
    AFFECTION("affection"), // petted today
    DARKNESS("darkness"),   // dark place
    LIGHT("light"),         // bright place
    HAY("hay"),             // hay bale in the pen
    SAND("sand"),           // sandy ground
    FLOWERS("flowers"),     // flowers in the pen
    SNOW("snow"),           // snow or ice in the pen
    WARMTH("warmth");       // lava, magma or campfire in the pen

    public final String id;

    Need(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "need.animalbonds." + id;
    }

    private static final Need[] VALUES = values();

    public static Need byOrdinal(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : TROUGH;
    }
}
