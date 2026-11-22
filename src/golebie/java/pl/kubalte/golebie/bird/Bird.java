package pl.kubalte.golebie.bird;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import pl.kubalte.golebie.Golebie;
import pl.kubalte.golebie.item.DiaperItem;

/**
 * A pet pigeon, modelled on two real birds. Tame ones run up to you with the head low when you crouch, circle in front
 * of you, wait to be stroked and lean into your hand; they fly up onto your shoulder or head and nibble at your face;
 * they answer a quick nod of your head with a bow; they can wear a diaper.
 */
public abstract class Bird extends TamableAnimal implements GeoEntity {
    // ---- what the bird is doing right now (played by the "action" animation controller)
    public static final int A_NONE = 0, A_PECK = 1, A_BOW = 2, A_COO = 3, A_CIRCLE = 4, A_CIRCLE_CCW = 5, A_PETTED = 6, A_NIBBLE_L = 7,
            A_NIBBLE_R = 8, A_PREEN = 9, A_SHAKE = 10, A_STRETCH = 11, A_SCRATCH = 12, A_LOOK_UP = 13, A_STARTLED = 14, A_DRINK = 15,
            A_TAIL_SHIVER = 16, A_HOP = 17, A_BEG = 18, A_STRUT = 19, A_RUNUP = 20;
    static final String[] ACTION_ANIM = {null, "peck", "bow", "coo", "circle", "circle_ccw", "petted", "nibble_l", "nibble_r", "preen", "shake",
            "wing_stretch", "scratch", "look_up", "startled", "drink", "tail_shiver", "hop", "beg", "strut", "runup"};
    static final boolean[] ACTION_LOOPS = {false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false,
            false, false, true, true, true};
    /** Perches on the owner. */
    public static final int PERCH_NONE = 0, PERCH_LEFT = 1, PERCH_RIGHT = 2, PERCH_HEAD = 3;

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_SEQ = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PERCH = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DIAPER = SynchedEntityData.defineId(Bird.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation[] actionAnims = new RawAnimation[ACTION_ANIM.length];
    private final RawAnimation idle, walk, run, fly, glide, sit, sleep;
    private int seenSeq = -1;

    // ---- server state
    int actionTicks;
    int petTicks;
    int bowDelay;
    @Nullable Player bowTo;
    int perchCooldown = 200, perchTicks, perchTime, ownerSneakTicks, nibbleTimer = 60;
    int idleCooldown = 100;
    int runCooldown;
    /** Flight: towards a perch on the owner, or down to the ground. */
    boolean flying;
    int flightTicks;
    int flyPerch;
    @Nullable Vec3 flyGround;
    int awayFromOwner;
    boolean greetPending;

    protected Bird(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        String p = "animation." + species() + ".";
        for (int i = 1; i < ACTION_ANIM.length; i++) {
            String name = ACTION_ANIM[i];
            if (name.equals("tail_shiver") && !hasTailShiver()) name = "shake";
            actionAnims[i] = ACTION_LOOPS[i] ? RawAnimation.begin().thenLoop(p + name) : RawAnimation.begin().thenPlay(p + name);
        }
        idle = RawAnimation.begin().thenLoop(p + "idle");
        walk = RawAnimation.begin().thenLoop(p + "walk");
        run = RawAnimation.begin().thenLoop(p + "run");
        fly = RawAnimation.begin().thenLoop(p + "fly");
        glide = RawAnimation.begin().thenLoop(p + "glide");
        sit = RawAnimation.begin().thenLoop(p + "sit");
        sleep = RawAnimation.begin().thenLoop(p + "sleep");
    }

    /** "pigeon" or "fantail": the model, texture and animation names. */
    public abstract String species();

    protected boolean hasTailShiver() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ACTION, A_NONE);
        b.define(ACTION_SEQ, 0);
        b.define(FLYING, false);
        b.define(SLEEPING, false);
        b.define(PERCH, PERCH_NONE);
        b.define(DIAPER, 0);
    }

    // ------------------------------------------------------------------ accessors
    public int action() {
        return entityData.get(ACTION);
    }

    public int actionSeq() {
        return entityData.get(ACTION_SEQ);
    }

    /** Starts an action; ticks = 0 keeps it until another one replaces it. */
    public void act(int action, int ticks) {
        entityData.set(ACTION, action);
        entityData.set(ACTION_SEQ, entityData.get(ACTION_SEQ) + 1);
        actionTicks = ticks;
    }

    public boolean isFlyingNow() {
        return entityData.get(FLYING);
    }

    public boolean isAsleep() {
        return entityData.get(SLEEPING);
    }

    public int perch() {
        return entityData.get(PERCH);
    }

    /** 0 = no diaper, otherwise 1 + the pattern number. */
    public int diaper() {
        return entityData.get(DIAPER);
    }

    public void setDiaper(int d) {
        entityData.set(DIAPER, d);
    }

    public int petTicks() {
        return petTicks;
    }

    /** Busy with something that should keep the ordinary walking goals still. */
    boolean busy() {
        return flying || perched() || petTicks > 0 || isAsleep() || running;
    }

    /** Sitting on its owner (it follows their shoulder or head; the game does not let anything ride a player). */
    public boolean perched() {
        return perch() != PERCH_NONE;
    }

    @Nullable Player perchPlayer;
    /** Only for the server test, whose fake player is not in the level's player list. */
    public static @Nullable Player testOwner;

    public @Nullable Player perchTarget() {
        if (perchPlayer != null && perchPlayer.isAlive() && perchPlayer.level() == level()) return perchPlayer;
        return ownerPlayer();
    }

    boolean running;

    @Nullable Player ownerPlayer() {
        LivingEntity o = getOwner();
        if (o == null && testOwner != null && isOwnedBy(testOwner)) o = testOwner;
        return o instanceof Player p && p.level() == level() && p.isAlive() && !p.isSpectator() ? p : null;
    }

    // ------------------------------------------------------------------ goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new RunToOwner(this));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 4F, 1.0, 1.4,
                e -> e instanceof Player p && !p.getMainHandItem().is(Items.WHEAT_SEEDS) && !isFood(p.getMainHandItem())) {
            @Override
            public boolean canUse() {
                return !isTame() && !busy() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.15, 8F, 3F) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !busy() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !busy() && action() == A_NONE && super.canUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6F) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }
        });
        goalSelector.addGoal(9, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }
        });
    }

    /**
     * When its owner crouches nearby (or comes back after a long time away), a tame bird runs up with its head low and
     * its tail down, circles in front of them, then waits to be stroked.
     */
    static class RunToOwner extends Goal {
        final Bird b;
        Player owner;
        int phase, timer, repath;

        RunToOwner(Bird b) {
            this.b = b;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!b.isTame() || b.isOrderedToSit() || b.flying || b.perched() || b.petTicks > 0 || b.runCooldown > 0 || b.isBaby()) return false;
            Player o = b.ownerPlayer();
            if (o == null) return false;
            double d = b.distanceTo(o);
            if (d < 2.0 || d > 16) return false;
            if (!(o.isShiftKeyDown() || b.greetPending)) return false;
            owner = o;
            return true;
        }

        @Override
        public void start() {
            b.greetPending = false;
            b.running = true;
            b.entityData.set(SLEEPING, false);
            phase = 0;
            timer = 0;
            repath = 0;
            b.act(A_RUNUP, 0);
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && b.petTicks == 0 && !b.flying && !b.perched() && timer < 400
                    && (phase < 2 || b.distanceTo(owner) < 5);
        }

        @Override
        public void tick() {
            timer++;
            Vec3 front = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(1.1));
            if (phase == 0) {
                if (--repath <= 0) {
                    repath = 8;
                    b.getNavigation().moveTo(front.x, front.y, front.z, 1.9);
                }
                if (b.position().distanceTo(front) < 0.9 || b.distanceTo(owner) < 1.5 || timer > 200) {
                    b.getNavigation().stop();
                    phase = 1;
                    timer = 0;
                    b.act(b.random.nextBoolean() ? A_CIRCLE : A_CIRCLE_CCW, 0);
                    b.playSound(Golebie.BOW_COO.get(), 0.8F, b.getVoicePitch());
                }
            } else if (phase == 1) {
                b.getNavigation().stop();
                if (timer > 2 * b.circleTicks()) {
                    phase = 2;
                    timer = 0;
                    b.act(A_BEG, 0);
                }
            } else {
                b.getNavigation().stop();
                b.getLookControl().setLookAt(owner, 30F, 30F);
                if (timer > 140) timer = 400;
            }
            if (phase >= 1) b.faceTowards(owner);
        }

        @Override
        public void stop() {
            b.running = false;
            b.runCooldown = 200;
            if (b.action() == A_RUNUP || b.action() == A_BEG || b.action() == A_CIRCLE || b.action() == A_CIRCLE_CCW) b.act(A_NONE, 0);
            b.getNavigation().stop();
            owner = null;
        }
    }

    int circleTicks() {
        return 48;
    }

    void faceTowards(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (actionTicks > 0 && --actionTicks == 0) act(A_NONE, 0);
        if (perchCooldown > 0) perchCooldown--;
        if (runCooldown > 0) runCooldown--;
        if (petTicks > 0) {
            petTicks--;
            getNavigation().stop();
            if (petTicks % 20 == 0) {
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 1, 0.15, 0.1, 0.15, 0);
                playSound(Golebie.PURR.get(), 0.7F, getVoicePitch());
            }
            if (petTicks == 0 && action() == A_PETTED) act(A_NONE, 0);
        }
        if (bowDelay > 0 && --bowDelay == 0) {
            if (bowTo != null && !perched() && !flying) faceTowards(bowTo);
            act(A_BOW, 26);
            playSound(Golebie.BOW_COO.get(), 1F, getVoicePitch());
            bowTo = null;
        }
        Player owner = ownerPlayer();
        if (perched()) perchedTick(sl, owner);
        else if (flying) flightTick(sl, owner);
        else groundTick(sl, owner);
        entityData.set(FLYING, flying || (!onGround() && !perched() && !isInWater() && getDeltaMovement().y < -0.1));
        // fluttering down instead of falling
        if (!onGround() && !flying && !perched() && getDeltaMovement().y < -0.12)
            setDeltaMovement(getDeltaMovement().x, -0.12, getDeltaMovement().z);
    }

    void groundTick(ServerLevel sl, @Nullable Player owner) {
        // missed its owner: greets them when they come back
        if (isTame()) {
            if (owner == null || distanceTo(owner) > 24) awayFromOwner++;
            else {
                if (awayFromOwner > 2400 && distanceTo(owner) < 12) greetPending = true;
                awayFromOwner = 0;
            }
        }
        // sleeping at night
        if (tickCount % 40 == 0) {
            boolean sleepy = level().isDarkOutside() && onGround() && !running && petTicks == 0 && getNavigation().isDone()
                    && (owner == null || distanceTo(owner) > 3 || isOrderedToSit());
            if (sleepy != isAsleep() && (sleepy ? random.nextInt(3) == 0 : true)) entityData.set(SLEEPING, sleepy);
        }
        if (isAsleep()) {
            getNavigation().stop();
            return;
        }
        // flies up onto its owner now and then
        if (owner != null && isTame() && !isOrderedToSit() && !isBaby() && perchCooldown == 0 && !running && petTicks == 0 && onGround()
                && distanceTo(owner) < 10 && ownerStill(owner) && !owner.isShiftKeyDown()
                && random.nextInt(Golebie.PERCH_CHANCE) == 0)
            startPerchFlight(owner, 0);
        // little things birds do
        if (!running && petTicks == 0 && action() == A_NONE && getNavigation().isDone() && onGround() && --idleCooldown <= 0) {
            idleCooldown = 120 + random.nextInt(240);
            idleAction(sl);
        }
    }

    @Nullable Vec3 ownerLastPos;
    int ownerStillTicks;

    /** Player movement is decided on the client, so "standing still" is judged by where they are from tick to tick. */
    boolean ownerStill(Player owner) {
        Vec3 now = owner.position();
        if (ownerLastPos != null && ownerLastPos.distanceToSqr(now) < 0.0025) ownerStillTicks++;
        else ownerStillTicks = 0;
        ownerLastPos = now;
        return ownerStillTicks > 30;
    }

    void idleAction(ServerLevel sl) {
        Bird mate = null;
        for (Bird o : level().getEntitiesOfClass(getClass(), getBoundingBox().inflate(5), o -> o != this && !o.isBaby() && !o.busy()))
            mate = o;
        int r = random.nextInt(100);
        if (mate != null && !isBaby() && r < 12) {
            faceTowards(mate);
            act(random.nextBoolean() ? A_STRUT : (random.nextBoolean() ? A_CIRCLE : A_CIRCLE_CCW), 64);
            playSound(Golebie.BOW_COO.get(), 1F, getVoicePitch());
        } else if (r < 45) act(A_PECK, 26);
        else if (r < 55) act(A_PREEN, 52);
        else if (r < 63) act(A_SHAKE, 22);
        else if (r < 70) act(A_STRETCH, 40);
        else if (r < 76) act(A_SCRATCH, 28);
        else if (r < 84) act(A_LOOK_UP, 36);
        else if (r < 92) {
            act(hasTailShiver() ? A_TAIL_SHIVER : A_COO, hasTailShiver() ? 24 : 40);
            playSound(Golebie.COO.get(), 0.9F, getVoicePitch());
        } else act(A_HOP, 10);
    }

    // ------------------------------------------------------------------ flying and perching
    /** Free spot on the owner: a shoulder, otherwise the head (0 if all are taken). */
    static int freePerch(Player p, int wanted) {
        boolean[] used = new boolean[4];
        for (Bird b : p.level().getEntitiesOfClass(Bird.class, p.getBoundingBox().inflate(2), Bird::perched))
            if (b.perchTarget() == p) used[b.perch()] = true;
        if (wanted > 0 && !used[wanted]) return wanted;
        int first = p.getRandom().nextBoolean() ? PERCH_LEFT : PERCH_RIGHT;
        if (!used[first]) return first;
        if (!used[3 - first]) return 3 - first;
        return used[PERCH_HEAD] ? 0 : PERCH_HEAD;
    }

    /** Where a bird sits on a player: on a shoulder or on top of the head. */
    public static Vec3 perchPos(Player p, int slot) {
        return perchPos(p.position(), p.yBodyRot, p.getBbHeight(), slot);
    }

    static Vec3 perchPos(Vec3 feet, float bodyYaw, double h, int slot) {
        float yaw = bodyYaw * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        if (slot == PERCH_HEAD) return feet.add(0, h + 0.02, 0);
        Vec3 side = slot == PERCH_LEFT ? left : left.scale(-1);
        return feet.add(side.scale(0.34)).add(0, h * 0.775, 0);
    }

    /** Glued to the owner's shoulder or head, on both sides, including last tick's place so it moves smoothly. */
    void followPerch(Player p) {
        int slot = perch();
        Vec3 now = perchPos(p, slot);
        Vec3 old = perchPos(new Vec3(p.xo, p.yo, p.zo), p.yBodyRotO, p.getBbHeight(), slot);
        setPos(now.x, now.y, now.z);
        xo = old.x;
        yo = old.y;
        zo = old.z;
        xOld = old.x;
        yOld = old.y;
        zOld = old.z;
        setDeltaMovement(Vec3.ZERO);
        setYRot(p.yBodyRot);
        yRotO = p.yBodyRotO;
        yBodyRot = p.yBodyRot;
        yBodyRotO = p.yBodyRotO;
        yHeadRot = p.yBodyRot;
        yHeadRotO = p.yBodyRotO;
        fallDistance = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (perched()) {
            Player p = perchTarget();
            if (p != null) followPerch(p);
        }
    }

    /** Takes off towards a spot on the owner (0 = whichever is free). Returns false if there is no room. */
    public boolean startPerchFlight(Player owner, int wanted) {
        int slot = freePerch(owner, wanted);
        if (slot == 0) return false;
        flying = true;
        flyPerch = slot;
        flyGround = null;
        flightTicks = 0;
        entityData.set(SLEEPING, false);
        getNavigation().stop();
        setNoGravity(true);
        setDeltaMovement(getDeltaMovement().add(0, 0.42, 0));
        if (action() != A_NONE) act(A_NONE, 0);
        playSound(Golebie.WINGS.get(), 0.8F, 1F);
        return true;
    }

    void flyDownTo(Vec3 ground) {
        flying = true;
        flyPerch = 0;
        flyGround = ground;
        flightTicks = 0;
        setNoGravity(true);
        playSound(Golebie.WINGS.get(), 0.7F, 1.1F);
    }

    void flightTick(ServerLevel sl, @Nullable Player owner) {
        flightTicks++;
        getNavigation().stop();
        Vec3 target;
        if (flyPerch > 0) {
            if (owner == null || flightTicks > 200 || freePerch(owner, flyPerch) != flyPerch) {
                land();
                return;
            }
            target = perchPos(owner, flyPerch);
        } else if (flyGround != null) target = flyGround;
        else {
            land();
            return;
        }
        Vec3 d = target.subtract(position());
        double dist = d.length();
        if (flyPerch > 0 && dist < 0.5) {
            // arrived: sit down on the owner
            flying = false;
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            sitOn(owner, flyPerch);
            perchTime = 1200 + random.nextInt(2400);
            nibbleTimer = 40 + random.nextInt(60);
            return;
        }
        if (flyGround != null && (dist < 0.6 || onGround() && flightTicks > 10 || flightTicks > 160)) {
            land();
            return;
        }
        double speed = Math.min(0.42, 0.12 + dist * 0.25);
        Vec3 want = d.normalize().scale(speed);
        Vec3 v = getDeltaMovement().scale(0.6).add(want.scale(0.4));
        if (horizontalCollision) v = v.add(0, 0.18, 0);
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * (180F / Math.PI)) - 90F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    void sitOn(Player p, int slot) {
        flying = false;
        flyPerch = 0;
        flyGround = null;
        perchPlayer = p;
        entityData.set(PERCH, slot);
        setNoGravity(true);
        noPhysics = true;
        perchTicks = 0;
        ownerSneakTicks = 0;
        getNavigation().stop();
        followPerch(p);
    }

    public void stopFlying() {
        land();
    }

    void land() {
        flying = false;
        flyPerch = 0;
        flyGround = null;
        setNoGravity(false);
    }

    void perchedTick(ServerLevel sl, @Nullable Player owner) {
        Player p = perchTarget();
        getNavigation().stop();
        if (p == null) {
            hopOff(null);
            return;
        }
        perchTicks++;
        ownerSneakTicks = p.isShiftKeyDown() ? ownerSneakTicks + 1 : 0;
        if (ownerSneakTicks >= 12 || perchTicks > perchTime || p.isInWater() || p.isSleeping()) {
            hopOff(p);
            return;
        }
        if (bowDelay > 0 || action() == A_BOW) return;
        if (--nibbleTimer <= 0) {
            nibbleTimer = 100 + random.nextInt(260);
            if (perch() == PERCH_HEAD) {
                act(random.nextBoolean() ? A_LOOK_UP : A_COO, 40);
                playSound(Golebie.COO.get(), 0.7F, getVoicePitch());
            } else {
                act(nibbleAction(), 52);
                playSound(Golebie.PECK.get(), 0.6F, 1F);
            }
        }
    }

    /** The face is on the inner side: from the left shoulder the bird turns to its right. */
    int nibbleAction() {
        return perch() == PERCH_LEFT ? Golebie.NIBBLE_LEFT_SHOULDER : (Golebie.NIBBLE_LEFT_SHOULDER == A_NIBBLE_L ? A_NIBBLE_R : A_NIBBLE_L);
    }

    /** Gets off the owner and flutters down beside them. */
    public void hopOff(@Nullable Player p) {
        int slot = perch();
        entityData.set(PERCH, PERCH_NONE);
        perchPlayer = null;
        noPhysics = false;
        setNoGravity(false);
        perchCooldown = 600 + random.nextInt(600);
        if (action() == A_NIBBLE_L || action() == A_NIBBLE_R) act(A_NONE, 0);
        if (p == null) return;
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = slot == PERCH_RIGHT ? left.scale(-1) : left;
        Vec3 start = perchPos(p, slot == 0 ? PERCH_LEFT : slot);
        setPos(start.x, start.y, start.z);
        flyDownTo(p.position().add(side.scale(1.2)).add(fwd.scale(0.9)));
    }

    // ------------------------------------------------------------------ test hooks
    /** Sits down on the player at once (tests and the client scene). */
    public boolean perchNow(Player p, int slot) {
        if (freePerch(p, slot) != slot) return false;
        sitOn(p, slot);
        perchTime = 100000;
        nibbleTimer = 100000;
        return true;
    }

    /** How many birds sit on this player. */
    public static int perchedOn(Player p) {
        int n = 0;
        for (Bird b : p.level().getEntitiesOfClass(Bird.class, p.getBoundingBox().inflate(2), Bird::perched)) if (b.perchTarget() == p) n++;
        return n;
    }

    public boolean isFlyingTowardsPerch() {
        return flying && flyPerch > 0;
    }

    public boolean isLanding() {
        return flying && flyGround != null;
    }

    public int pendingBow() {
        return bowDelay;
    }

    /** As if it had missed its owner: it runs up to them next. */
    public void greetNow() {
        greetPending = true;
        runCooldown = 0;
    }

    public boolean wouldRunToOwner() {
        return new RunToOwner(this).canUse();
    }

    public void holdPose() {
        perchTime = 100000;
        nibbleTimer = 100000;
        idleCooldown = 100000;
    }

    // ------------------------------------------------------------------ the player's nod
    /** The owner nodded at us: a bow back, after a heartbeat. */
    public void answerNod(Player p) {
        if (bowDelay > 0 || flying || petTicks > 0) return;
        bowTo = p;
        bowDelay = 3 + random.nextInt(10);
        entityData.set(SLEEPING, false);
    }

    // ------------------------------------------------------------------ interaction
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean server = !level().isClientSide();
        if (!isTame()) {
            if (isFood(stack) && !isBaby()) {
                if (server) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    if (random.nextInt(3) == 0) {
                        tame(player);
                        setOrderedToSit(false);
                        level().broadcastEntityEvent(this, (byte) 7);
                        player.sendOverlayMessage(Component.translatable("golebie.tamed", getDisplayName()));
                        playSound(Golebie.COO.get(), 1F, getVoicePitch());
                    } else level().broadcastEntityEvent(this, (byte) 6);
                    act(A_PECK, 26);
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }
        if (!isOwnedBy(player)) return super.mobInteract(player, hand);
        if (stack.getItem() instanceof DiaperItem d) {
            if (server) {
                if (diaper() != 0 && level() instanceof ServerLevel sl) spawnAtLocation(sl, new ItemStack(Golebie.DIAPERS.get(diaper() - 1).get()));
                setDiaper(d.pattern + 1);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8F, 1.3F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS) && diaper() != 0) {
            if (server && level() instanceof ServerLevel sl) {
                spawnAtLocation(sl, new ItemStack(Golebie.DIAPERS.get(diaper() - 1).get()));
                setDiaper(0);
                playSound(SoundEvents.SHEEP_SHEAR, 0.8F, 1.4F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.STICK)) {
            if (server) {
                setOrderedToSit(!isOrderedToSit());
                player.sendOverlayMessage(Component.translatable(isOrderedToSit() ? "golebie.stay" : "golebie.follow", getDisplayName()));
            }
            return InteractionResult.SUCCESS;
        }
        if (isFood(stack)) {
            if (getHealth() < getMaxHealth()) {
                if (server) {
                    heal(2F);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    act(A_PECK, 26);
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);  // breeding
        }
        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) {
                if (server) startPerchFlight(player, 0);
                return InteractionResult.SUCCESS;
            }
            if (server) pet(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Stroked: leans into the hand, purrs, hearts. */
    public void pet(Player player) {
        petTicks = 80;
        entityData.set(SLEEPING, false);
        if (isOrderedToSit()) setInSittingPose(false);
        getNavigation().stop();
        faceTowards(player);
        act(A_PETTED, 80);
        playSound(Golebie.PURR.get(), 0.8F, getVoicePitch());
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 2, 0.15, 0.1, 0.15, 0);
    }

    // ------------------------------------------------------------------ misc
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)) return false;
        if (perched() && source.getEntity() == perchTarget()) return false;
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            entityData.set(SLEEPING, false);
            if (perched()) hopOff(perchTarget());
            else if (!isTame() || !(source.getEntity() instanceof Player)) {
                double a = random.nextDouble() * Math.PI * 2;
                flyDownTo(position().add(Math.cos(a) * 9, 3, Math.sin(a) * 9));
                setDeltaMovement(getDeltaMovement().add(0, 0.5, 0));
            }
        }
        return hurt;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Bird child = (Bird) getType().create(level, EntitySpawnReason.BREEDING);
        if (child != null && isTame() && getOwner() != null) {
            child.setOwner(getOwner());
            child.setTame(true, true);
        }
        return child;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isAsleep() ? null : Golebie.COO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.3F : 0.95F) + random.nextFloat() * 0.1F;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setDiaper(in.getIntOr("Diaper", 0));
        entityData.set(PERCH, PERCH_NONE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Diaper", diaper());
    }

    // ------------------------------------------------------------------ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Bird>("base", 4, this::base));
        controllers.add(new AnimationController<Bird>("action", 3, this::doing));
    }

    private PlayState base(AnimationTest<Bird> t) {
        if (perched()) return t.setAndContinue(idle);
        if (isFlyingNow()) return t.setAndContinue(getY() - yo < -0.06 && !flyingUp() ? glide : fly);
        if (isAsleep()) return t.setAndContinue(sleep);
        if (isInSittingPose()) return t.setAndContinue(sit);
        if (t.isMoving()) return t.setAndContinue(walkAnimation.speed() > 0.75F ? run : walk);
        return t.setAndContinue(idle);
    }

    private boolean flyingUp() {
        return getY() - yo > 0.02;
    }

    private PlayState doing(AnimationTest<Bird> t) {
        int a = action();
        if (a <= A_NONE || a >= actionAnims.length || isFlyingNow() && a != A_BOW) {
            seenSeq = actionSeq();
            return PlayState.STOP;
        }
        if (actionSeq() != seenSeq) {
            seenSeq = actionSeq();
            t.controller().reset();
        }
        return t.setAndContinue(actionAnims[a]);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
