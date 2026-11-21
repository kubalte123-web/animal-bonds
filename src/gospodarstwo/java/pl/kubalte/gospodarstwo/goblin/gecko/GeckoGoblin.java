package pl.kubalte.gospodarstwo.goblin.gecko;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import pl.kubalte.gospodarstwo.goblin.Goblin;

/**
 * The goblin as GeckoLib sees it: same entity, plus two animation controllers. "base" plays how it stands or moves
 * (idle, walk, run, sneak, swim, sit, sleep, peek, think), "action" plays what the AI tells it to do on top.
 * Only created when GeckoLib is installed (see {@link Goblin#create}).
 */
public class GeckoGoblin extends Goblin implements GeoEntity {
    private static RawAnimation loop(String n) {
        return RawAnimation.begin().thenLoop(n);
    }

    private static RawAnimation once(String n) {
        return RawAnimation.begin().thenPlay(n);
    }

    private static RawAnimation hold(String n) {
        return RawAnimation.begin().thenPlayAndHold(n);
    }

    static final RawAnimation IDLE = loop("idle"), WALK = loop("walk"), RUN = loop("run"), SNEAK = loop("sneak"), SWIM = loop("swim"),
            SIT = loop("sit"), SLEEP = loop("sleep"), SLEEP_BED = loop("sleep_bed"), PEEK = loop("peek"), CONSIDER = loop("consider");

    /** Indexed by the Goblin.A_* action numbers. */
    static final RawAnimation[] ACTIONS = {null, hold("windup"), hold("leap"), once("strike"), once("dodge_l"), once("dodge_r"),
            once("dodge_b"), hold("block"), loop("dance1"), loop("dance2"), loop("dance3"), loop("chop"), loop("mine"), loop("hoe"),
            loop("shear"), loop("work"), loop("churn"), loop("build"), once("pet"), once("petted"), once("poke"), once("annoyed"),
            once("eat"), once("wave"), loop("sulk"), once("cheer"), loop("play"), loop("scared"), once("punch")};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int seenSeq = -1;

    public GeckoGoblin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeckoGoblin>("base", 4, this::base));
        controllers.add(new AnimationController<GeckoGoblin>("action", 3, this::doing));
    }

    private PlayState base(AnimationTest<GeckoGoblin> t) {
        if (isSleeping()) return t.setAndContinue(SLEEP_BED);
        if (work() == 6) return t.setAndContinue(SLEEP);
        if (hiding()) return t.setAndContinue(PEEK);
        if (considering() > 0) return t.setAndContinue(CONSIDER);
        if (!level().getFluidState(BlockPos.containing(getX(), getY() + 0.5, getZ())).isEmpty()) return t.setAndContinue(SWIM);
        if (isOrderedToSit() || resting()) return t.setAndContinue(SIT);
        if (t.isMoving()) {
            if (isCrouching()) return t.setAndContinue(SNEAK);
            return t.setAndContinue(walkAnimation.speed() > 0.8F ? RUN : WALK);
        }
        return t.setAndContinue(IDLE);
    }

    private PlayState doing(AnimationTest<GeckoGoblin> t) {
        int a = action();
        if (a <= A_NONE || a >= ACTIONS.length) {
            seenSeq = actionSeq();
            return PlayState.STOP;
        }
        if (actionSeq() != seenSeq) {
            // the same action asked for again (another blow, another dodge) starts from the beginning
            seenSeq = actionSeq();
            t.controller().reset();
        }
        return t.setAndContinue(ACTIONS[a]);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
