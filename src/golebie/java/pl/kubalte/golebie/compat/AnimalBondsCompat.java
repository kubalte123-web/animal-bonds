package pl.kubalte.golebie.compat;

import pl.kubalte.animalbonds.api.AnimalBondsApi;

/** With Animal Bonds installed: the birds keep their own colours (they are portraits of two real pigeons). */
public final class AnimalBondsCompat {
    private AnimalBondsCompat() {}

    public static void init() {
        AnimalBondsApi.excludeFromTextureVariety("golebie:pigeon");
        AnimalBondsApi.excludeFromTextureVariety("golebie:fantail");
    }
}
