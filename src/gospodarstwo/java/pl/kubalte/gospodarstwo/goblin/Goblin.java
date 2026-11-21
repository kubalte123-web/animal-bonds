package pl.kubalte.gospodarstwo.goblin;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import pl.kubalte.gospodarstwo.GConfig;
import pl.kubalte.gospodarstwo.Gospodarstwo;

/**
 * Goblin: a small, shy forest creature. Wild ones watch you from behind trees and run when you come close. Throw it food
 * and walk away: it picks the food up, thinks it over and either eats it and follows you, or throws it away.
 * A tame goblin travels with you (picking up loose things into the chest on its back) or stays at the base and works.
 */
public class Goblin extends TamableAnimal {
    public static final int MODE_FOLLOW = 0, MODE_BASE = 1, MODE_SIT = 2;
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PACK = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESTING = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIDING = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WORK = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RACE = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_SEQ = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEEDS = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAITS = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SETTINGS = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SETTINGS_MINE = SynchedEntityData.defineId(Goblin.class, EntityDataSerializers.INT);

    /** Animation actions (shown by the renderer). */
    public static final int A_NONE = 0, A_WINDUP = 1, A_LEAP = 2, A_STRIKE = 3, A_DODGE_L = 4, A_DODGE_R = 5, A_DODGE_B = 6, A_BLOCK = 7,
            A_DANCE1 = 8, A_DANCE2 = 9, A_DANCE3 = 10, A_CHOP = 11, A_MINE = 12, A_HOE = 13, A_SHEAR = 14, A_WORK = 15, A_CHURN = 16,
            A_BUILD = 17, A_PET = 18, A_PETTED = 19, A_POKE = 20, A_ANNOYED = 21, A_EAT = 22, A_WAVE = 23, A_SULK = 24, A_CHEER = 25,
            A_PLAY = 26, A_SCARED = 27, A_PUNCH = 28;

    public final SimpleContainer pack = new SimpleContainer(27);
    private Traits traits;
    private Work settings = new Work();
    /** Needs, 0..100: hunger (full = 100), energy, fun, company. */
    float hunger = 80, energy = 90, fun = 70, social = 70;
    private int actionTicks, noise;
    @Nullable BlockPos hut;
    @Nullable BlockPos mine;
    int mineDepth;
    /** While another goblin plays with it, its own goals wait. */
    int socialLock;
    /** Ticks left of holding the shield up. */
    int blockTicks;
    private BlockPos home;
    private int restTicks, considerTicks, refuseCooldown, depositTimer;
    private @Nullable UUID offeredBy;
    private @Nullable Player offeredPlayer;
    boolean depositing;

    public Goblin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(MODE, MODE_FOLLOW);
        b.define(PACK, false);
        b.define(RESTING, false);
        b.define(HIDING, false);
        b.define(WORK, 0);
        b.define(RACE, 0);
        b.define(ACTION, 0);
        b.define(ACTION_SEQ, 0);
        b.define(NEEDS, pack4(80, 90, 70, 70));
        b.define(TRAITS, 0);
        b.define(SETTINGS, new Work().packA());
        b.define(SETTINGS_MINE, new Work().packB());
    }

    static int pack4(float a, float b, float c, float d) {
        return Math.round(Math.max(0, Math.min(100, a))) | Math.round(Math.max(0, Math.min(100, b))) << 8
                | Math.round(Math.max(0, Math.min(100, c))) << 16 | Math.round(Math.max(0, Math.min(100, d))) << 24;
    }

    // ------------------------------------------------------------------ race, character, needs, settings
    public Race race() {
        return Race.byId(entityData.get(RACE));
    }

    public void setRace(Race r) {
        entityData.set(RACE, r.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(r.health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(r.speed);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(r.attack);
        setHealth(getMaxHealth());
    }

    public Traits traits() {
        if (level().isClientSide()) {
            int t = entityData.get(TRAITS);
            return t == 0 ? new Traits() : Traits.unpack(t);
        }
        if (traits == null) {
            int t = entityData.get(TRAITS);
            traits = t == 0 ? Traits.roll(race(), random) : Traits.unpack(t);
            if (!level().isClientSide() && t == 0) entityData.set(TRAITS, traits.pack());
            if (traits.fears(Traits.Fear.WATER)) setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1F);
        }
        return traits;
    }

    void setTraitsForTest(Traits t) {
        traits = t;
        entityData.set(TRAITS, t.pack());
    }

    public Work settings() {
        if (level().isClientSide()) return Work.unpack(entityData.get(SETTINGS), entityData.get(SETTINGS_MINE));
        return settings;
    }

    /** A button from the info page. */
    public boolean settingsClick(int id) {
        if (!settings.click(id)) return false;
        if (id >= 13 && id <= 14 && settings.pick == Work.PICK_OFF) mine = null;
        settingsChanged();
        return true;
    }

    void settingsChanged() {
        entityData.set(SETTINGS, settings.packA());
        entityData.set(SETTINGS_MINE, settings.packB());
    }

    /** hunger, energy, fun, company as synced to the client (0..100). */
    public int[] needs() {
        int n = entityData.get(NEEDS);
        return new int[] {n & 255, n >> 8 & 255, n >> 16 & 255, n >>> 24 & 255};
    }

    public void addNeeds(float h, float e, float f, float s) {
        hunger = Math.max(0, Math.min(100, hunger + h));
        energy = Math.max(0, Math.min(100, energy + e));
        fun = Math.max(0, Math.min(100, fun + f));
        social = Math.max(0, Math.min(100, social + s));
    }

    /** 0..100: how it feels overall, after what bothers it right now. */
    public int mood() {
        int[] n = needs();
        float m = (n[0] + n[1] + n[2] + n[3]) / 4F;
        return Math.max(0, Math.min(100, Math.round(m - bother() - noise / 4F)));
    }

    /** How much the things it dislikes bother it right now. */
    float bother() {
        Traits t = traits();
        float b = 0;
        if (t.bothered(Traits.Bother.RAIN) && level().isRainingAt(blockPosition().above())) b += 15;
        if (t.bothered(Traits.Bother.DARKNESS) && level().getMaxLocalRawBrightness(blockPosition()) < 5) b += 15;
        if (t.bothered(Traits.Bother.WATER) && isInWater()) b += 20;
        if (t.bothered(Traits.Bother.SUN) && level().isBrightOutside() && level().canSeeSky(blockPosition().above())) b += 20;
        if (t.bothered(Traits.Bother.LONELINESS) && social < 40) b += 10;
        return b;
    }

    /** Something loud went off nearby (explosions): goblins who mind noise sulk for a while. */
    public void heardNoise() {
        if (traits().bothered(Traits.Bother.NOISE)) noise = 400;
        else noise = Math.max(noise, 80);
    }

    /** Liking of a food for this goblin: its favourites always win, the food it hates never. */
    public float likingOf(ItemStack s) {
        if (traits().likes(s)) return 0.95F;
        if (traits().hates(s)) return 0F;
        return liking(s);
    }

    // ------------------------------------------------------------------ animations
    public int action() {
        return entityData.get(ACTION);
    }

    public int actionSeq() {
        return entityData.get(ACTION_SEQ);
    }

    /** Starts an animation that lasts the given ticks (0 = until replaced). */
    public void act(int action, int ticks) {
        entityData.set(ACTION, action);
        entityData.set(ACTION_SEQ, entityData.get(ACTION_SEQ) + 1);
        actionTicks = ticks;
    }

    /** Ticks left of the current timed animation. */
    public int actionLeft() {
        return actionTicks;
    }

    public Work.Tool tool() {
        return Work.toolOf(getMainHandItem());
    }

    // ------------------------------------------------------------------ state
    public int mode() {
        return entityData.get(MODE);
    }

    public void setMode(int m) {
        entityData.set(MODE, m);
        setOrderedToSit(m == MODE_SIT);
        if (m == MODE_BASE) home = blockPosition();
        getNavigation().stop();
    }

    public boolean hasPack() {
        return entityData.get(PACK);
    }

    public void setPack(boolean on) {
        entityData.set(PACK, on);
    }

    public boolean resting() {
        return entityData.get(RESTING);
    }

    public boolean hiding() {
        return entityData.get(HIDING);
    }

    public void setHiding(boolean h) {
        entityData.set(HIDING, h);
    }

    /** What it is doing right now (shown in the menu): 0 idle, 1 playing, 2 harvesting, 3 cooking, 4 collecting, 5 storing. */
    public int work() {
        return entityData.get(WORK);
    }

    void setWork(int w) {
        entityData.set(WORK, w);
    }

    public @Nullable BlockPos home() {
        return home;
    }

    public int considering() {
        return considerTicks;
    }

    public void startDeposit() {
        depositing = true;
        depositTimer = 1200;
    }

    // ------------------------------------------------------------------ AI
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RestGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 6F, 1.0, 1.5) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new GoblinNeeds.FleeFears(this));
        goalSelector.addGoal(4, new GoblinCombat.Fight(this));
        goalSelector.addGoal(5, new GoblinNeeds.Sleep(this));
        goalSelector.addGoal(5, new GoblinGoals.TakeOffer(this));
        goalSelector.addGoal(5, new GoblinGoals.Deposit(this));
        goalSelector.addGoal(6, new GoblinGoals.Hide(this));
        goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.25, 6F, 2.5F) {
            @Override
            public boolean canUse() {
                return mode() == MODE_FOLLOW && !depositing && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return mode() == MODE_FOLLOW && !depositing && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new GoblinNeeds.Dance(this));
        goalSelector.addGoal(7, new GoblinNeeds.Social(this));
        goalSelector.addGoal(8, new GoblinGoals.Collect(this));
        goalSelector.addGoal(8, new GoblinHut.Build(this));
        goalSelector.addGoal(9, new GoblinTools.ToolWork(this));
        goalSelector.addGoal(9, new GoblinGoals.BaseWork(this));
        goalSelector.addGoal(10, new GoblinGoals.GoHome(this));
        goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override
            public boolean canUse() {
                return !(isTame() && mode() == MODE_FOLLOW) && super.canUse();
            }
        });
        goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(13, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new GoblinCombat.Guard(this));
    }

    /** While it rests after fainting (or sleeps at night) nothing else happens. */
    static final class RestGoal extends Goal {
        private final Goblin g;

        RestGoal(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return g.resting() || g.isSleeping();
        }

        @Override
        public void start() {
            g.getNavigation().stop();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (refuseCooldown > 0) refuseCooldown--;
        if (depositing && --depositTimer <= 0) depositing = false;
        if (considerTicks > 0 && --considerTicks == 0) decide(sl, null);
        if (resting()) {
            if (--restTicks <= 0) wakeUp();
        } else if (isSleeping() && isTame() && !GoblinGoals.night(level())) {
            stopSleeping();
        }
        if (tickCount % 20 == 0 && isTame() && getHealth() < getMaxHealth() && (resting() || isSleeping())) heal(1F);
        if (actionTicks > 0 && --actionTicks == 0) entityData.set(ACTION, A_NONE);
        if (noise > 0) noise--;
        if (socialLock > 0) socialLock--;
        if (tickCount % 20 == 0) needsTick(sl);
        GoblinCombat.shieldTick(this, sl);
    }

    /** Once a second: needs go down (or up while sleeping, dancing, in company...). */
    void needsTick(ServerLevel sl) {
        boolean busy = work() != 0 || getTarget() != null;
        boolean asleep = isSleeping() || resting() || GoblinGoals.inHut(this);
        hunger -= busy ? 0.05F : 0.025F;
        if (asleep) energy += 0.6F;
        else energy -= busy ? 0.05F : 0.03F;
        fun -= 0.05F;
        boolean company = !sl.getEntitiesOfClass(Goblin.class, getBoundingBox().inflate(8), g -> g != this).isEmpty()
                || getOwner() != null && getOwner().distanceTo(this) < 8;
        social += company ? 0.25F : (traits().bothered(Traits.Bother.LONELINESS) ? -0.06F : -0.035F);
        addNeeds(0, 0, 0, 0);
        entityData.set(NEEDS, pack4(hunger, energy, fun, social));
        if (isTame() && hunger < 30 && action() != A_EAT) GoblinNeeds.eatSomething(this);
    }

    @Override
    public net.minecraft.world.entity.@Nullable SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.world.entity.@Nullable SpawnGroupData data) {
        setRace(raceFor(level, blockPosition()));
        traits = null;
        entityData.set(TRAITS, 0);
        traits();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Cave goblins spawn on stone in the dark of lush and dripstone caves. */
    public static boolean caveSpawn(net.minecraft.world.level.ServerLevelAccessor level, BlockPos pos) {
        return raceFor(level, pos) == Race.CAVE && level.getRawBrightness(pos, 0) < 8 && level.getBlockState(pos.below()).isSolid()
                && !level.canSeeSky(pos);
    }

    /** Where it was born decides the race: mountains make stone goblins, swamps bog goblins, caves pale cave goblins. */
    static Race raceFor(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        var biome = level.getBiome(pos);
        String id = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("");
        if (id.contains("swamp") || id.contains("mangrove")) return Race.SWAMP;
        if (id.contains("cave") || id.contains("deep_dark") || pos.getY() < 40 && !level.canSeeSky(pos)) return Race.CAVE;
        if (id.contains("peak") || id.contains("slope") || id.contains("mountain") || id.contains("hills") || id.contains("stony")
                || id.contains("windswept") || id.contains("grove") || id.contains("meadow")) return Race.STONE;
        return Race.FOREST;
    }

    // ------------------------------------------------------------------ taming
    /** Chance that it likes the food: home cooking works best. */
    public static float liking(ItemStack s) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        if (id.startsWith("gospodarstwo:")) {
            for (String dish : new String[] {"pierogi", "bigos", "kielbasa", "smoked", "pancakes", "apple_pie", "bread_with_butter", "jam"})
                if (id.contains(dish)) return 0.8F;
            return 0.35F;
        }
        if (id.contains("cooked") || s.is(Items.BREAD) || s.is(Items.PUMPKIN_PIE) || s.is(Items.COOKIE) || s.is(Items.CAKE)) return 0.35F;
        return 0.15F;
    }

    public static boolean isOffer(ItemStack s) {
        return s.get(net.minecraft.core.component.DataComponents.FOOD) != null;
    }

    public boolean willingToTakeOffer() {
        return !isTame() && refuseCooldown <= 0 && considerTicks <= 0 && getMainHandItem().isEmpty();
    }

    /** Takes a piece of thrown food into its hand and thinks it over for a few seconds. */
    public void takeOffer(ItemEntity item) {
        ItemStack s = item.getItem().split(1);
        if (item.getItem().isEmpty()) item.discard();
        setItemSlot(EquipmentSlot.MAINHAND, s);
        setDropChance(EquipmentSlot.MAINHAND, 0F);
        Entity owner = item.getOwner();
        offeredBy = owner instanceof Player p ? p.getUUID() : null;
        offeredPlayer = owner instanceof Player p ? p : null;
        considerTicks = 60 + random.nextInt(40);
        playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
    }

    /** Tests / screenshots: rests (asleep if in a bed) for the given time. */
    public void debugRest(int ticks) {
        entityData.set(RESTING, true);
        restTicks = ticks;
    }

    /** Tests / screenshots: holds food and thinks it over. */
    public void debugConsider(ItemStack food) {
        setItemSlot(EquipmentSlot.MAINHAND, food);
        considerTicks = 100000;
    }

    /** Eats the food and takes the giver as its friend, or throws it away. forced = test override (null = random). */
    public void decide(ServerLevel level, @Nullable Boolean forced) {
        ItemStack food = getMainHandItem();
        if (food.isEmpty()) return;
        considerTicks = 0;
        Player giver = offeredBy == null ? null : level.getPlayerByUUID(offeredBy);
        if (giver == null && offeredPlayer != null && offeredPlayer.isAlive()) giver = offeredPlayer;
        boolean yes = giver != null && (forced != null ? forced : random.nextFloat() < likingOf(food));
        if (yes) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.getItem()), getX(), getEyeY(), getZ(), 12, 0.2, 0.1, 0.2, 0.05);
            playSound(SoundEvents.GENERIC_EAT.value(), 1F, 1.3F);
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            act(A_EAT, 30);
            addNeeds(30, 0, traits().likes(food) ? 20 : 5, 10);
            tame(giver);
            setMode(MODE_FOLLOW);
            level.broadcastEntityEvent(this, (byte) 7);
            giver.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.tamed"));
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ItemEntity thrown = new ItemEntity(level, getX(), getEyeY() - 0.2, getZ(), food);
            Vec3 look = getLookAngle().scale(-0.35);
            thrown.setDeltaMovement(look.x, 0.3, look.z);
            thrown.setPickUpDelay(40);
            level.addFreshEntity(thrown);
            playSound(SoundEvents.VILLAGER_NO, 1F, 1.6F);
            level.broadcastEntityEvent(this, (byte) 6);
            refuseCooldown = 1200;
        }
        offeredBy = null;
        offeredPlayer = null;
    }

    // ------------------------------------------------------------------ interaction
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() || !isOwnedBy(player)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // leaves sneak-click to other mods (Animal Bonds)
        if (stack.is(Items.CHEST) && !hasPack()) {
            if (!level().isClientSide()) {
                setPack(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        }
        if (isOffer(stack) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                heal(4F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                playSound(SoundEvents.GENERIC_EAT.value(), 1F, 1.3F);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) GoblinMenu.open(sp, this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // goblins don't breed
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /** Adds to the pack (if it carries one); returns what did not fit. */
    public ItemStack stow(ItemStack s) {
        if (!hasPack() || s.isEmpty()) return s;
        return pack.addItem(s);
    }

    public boolean packHasRoom(ItemStack s) {
        return hasPack() && pack.canAddItem(s);
    }

    // ------------------------------------------------------------------ fainting
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isTame() && amount >= getHealth() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            faint(level);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /** Instead of dying: falls over, comes back to where its friend last slept and rests there for a day. */
    public void faint(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        playSound(SoundEvents.FOX_HURT, 1F, 1.4F);
        setHealth(getMaxHealth());
        clearFire();
        setTarget(null);
        getNavigation().stop();
        Player owner = getOwner() instanceof Player p ? p : null;
        BlockPos dest = null;
        if (owner != null) {
            GlobalPos bed = owner.getData(Gospodarstwo.LAST_BED.get()).orElse(null);
            if (bed != null && bed.dimension() == level.dimension()) dest = bed.pos();
        }
        if (dest == null && home != null) dest = home;
        if (dest != null) {
            BlockState s = level.getBlockState(dest);
            if (s.getBlock() instanceof BedBlock && !s.getValue(BedBlock.OCCUPIED)) {
                BlockPos head = s.getValue(BedBlock.PART) == BedPart.HEAD ? dest : dest.relative(s.getValue(BedBlock.FACING));
                teleportTo(head.getX() + 0.5, head.getY() + 0.6, head.getZ() + 0.5);
                startSleeping(head);
            } else {
                teleportTo(dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5);
            }
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
        entityData.set(RESTING, true);
        restTicks = GConfig.GOBLIN_REST.get();
        if (owner != null) owner.sendSystemMessage(Component.translatable("goblin.gospodarstwo.fainted", getDisplayName()));
    }

    void wakeUp() {
        entityData.set(RESTING, false);
        restTicks = 0;
        if (isSleeping()) stopSleeping();
        playSound(SoundEvents.FOX_AMBIENT, 1F, 1.5F);
    }

    // ------------------------------------------------------------------ misc
    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (int i = 0; i < pack.getContainerSize(); i++) spawnAtLocation(level, pack.removeItemNoUpdate(i));
        if (hasPack()) spawnAtLocation(level, new ItemStack(Items.CHEST));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTame() ? SoundEvents.FOX_AMBIENT : SoundEvents.FOX_SNIFF;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F + random.nextFloat() * 0.2F;
    }

    @Override
    public Pose getPose() {
        return super.getPose();
    }

    @Override
    public boolean isCrouching() {
        return hiding() || super.isCrouching();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(MODE, in.getIntOr("GoblinMode", MODE_FOLLOW));
        entityData.set(PACK, in.getBooleanOr("Pack", false));
        entityData.set(RESTING, in.getBooleanOr("Resting", false));
        restTicks = in.getIntOr("RestTicks", 0);
        refuseCooldown = in.getIntOr("Refuse", 0);
        home = in.read("Home", BlockPos.CODEC).orElse(null);
        pack.clearContent();
        in.read("PackItems", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size() && i < pack.getContainerSize(); i++) pack.setItem(i, l.get(i));
        });
        setOrderedToSit(mode() == MODE_SIT);
        entityData.set(RACE, in.getIntOr("Race", 0));
        in.read("Traits", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(t -> {
            traits = Traits.load(t);
            entityData.set(TRAITS, traits.pack());
        });
        in.read("Settings", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(t -> settings = Work.load(t));
        settingsChanged();
        hunger = in.getFloatOr("Hunger", 80);
        energy = in.getFloatOr("Energy", 90);
        fun = in.getFloatOr("Fun", 70);
        social = in.getFloatOr("Social", 70);
        hut = in.read("Hut", BlockPos.CODEC).orElse(null);
        mine = in.read("Mine", BlockPos.CODEC).orElse(null);
        mineDepth = in.getIntOr("MineDepth", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("GoblinMode", mode());
        out.putBoolean("Pack", hasPack());
        out.putBoolean("Resting", resting());
        out.putInt("RestTicks", restTicks);
        out.putInt("Refuse", refuseCooldown);
        if (home != null) out.store("Home", BlockPos.CODEC, home);
        java.util.List<ItemStack> l = new java.util.ArrayList<>();
        for (int i = 0; i < pack.getContainerSize(); i++) l.add(pack.getItem(i));
        out.store("PackItems", ItemStack.OPTIONAL_CODEC.listOf(), l);
        out.putInt("Race", entityData.get(RACE));
        out.store("Traits", net.minecraft.nbt.CompoundTag.CODEC, traits().save());
        out.store("Settings", net.minecraft.nbt.CompoundTag.CODEC, settings.save());
        out.putFloat("Hunger", hunger);
        out.putFloat("Energy", energy);
        out.putFloat("Fun", fun);
        out.putFloat("Social", social);
        if (hut != null) out.store("Hut", BlockPos.CODEC, hut);
        if (mine != null) out.store("Mine", BlockPos.CODEC, mine);
        out.putInt("MineDepth", mineDepth);
    }

    /** Test helper: is a player within r blocks? */
    boolean playerNear(double r) {
        return !level().getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(r), p -> !p.isSpectator() && !p.isCreative() || p == getOwner())
                .isEmpty();
    }
}
