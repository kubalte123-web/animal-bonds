package pl.kubalte.golebie;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import pl.kubalte.golebie.bird.Bird;

/**
 * Watches how players move their heads. A quick nod (looking down and straight back up within about half a second)
 * makes the tame pigeons around bow back, as the real ones do.
 */
public final class Nods {
    private Nods() {}

    static final int HISTORY = 16;
    static final Map<UUID, float[]> PITCH = new HashMap<>();
    static final Map<UUID, Integer> COOLDOWN = new HashMap<>();

    static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide()) return;
        float[] h = PITCH.computeIfAbsent(p.getUUID(), u -> {
            float[] a = new float[HISTORY];
            java.util.Arrays.fill(a, p.getXRot());
            return a;
        });
        System.arraycopy(h, 1, h, 0, HISTORY - 1);
        h[HISTORY - 1] = p.getXRot();
        int cd = COOLDOWN.getOrDefault(p.getUUID(), 0);
        if (cd > 0) {
            COOLDOWN.put(p.getUUID(), cd - 1);
            return;
        }
        if (nodded(h)) {
            COOLDOWN.put(p.getUUID(), 30);
            PITCH.remove(p.getUUID());
            answer(p);
        }
    }

    /** Down by at least 15 degrees and back up by at least 12, each within 8 ticks (pitch grows when looking down). */
    public static boolean nodded(float[] h) {
        for (int j = 1; j < h.length - 1; j++) {
            float down = 0, up = 0;
            for (int i = Math.max(0, j - 8); i < j; i++) down = Math.max(down, h[j] - h[i]);
            for (int k = j + 1; k < Math.min(h.length, j + 9); k++) up = Math.max(up, h[j] - h[k]);
            if (down >= 15 && up >= 12) return true;
        }
        return false;
    }

    /** Every tame bird within 12 blocks bows back. */
    public static int answer(Player p) {
        int n = 0;
        for (Bird b : p.level().getEntitiesOfClass(Bird.class, p.getBoundingBox().inflate(12), Bird::isTame)) {
            b.answerNod(p);
            n++;
        }
        return n;
    }

    /** Birds sitting on a player are not saved with them, so they hop off when the player leaves. */
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Player p = e.getEntity();
        for (Entity passenger : java.util.List.copyOf(p.getPassengers())) if (passenger instanceof Bird b) b.hopOff(p);
        PITCH.remove(p.getUUID());
        COOLDOWN.remove(p.getUUID());
    }
}
