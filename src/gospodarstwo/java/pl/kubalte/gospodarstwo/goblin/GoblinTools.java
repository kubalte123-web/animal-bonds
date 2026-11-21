package pl.kubalte.gospodarstwo.goblin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.kubalte.gospodarstwo.tools.WateringCanItem;

/** Work with the tool in its hand, at the base: chop trees, dig its own mine, farm, shear sheep, water crops. */
public final class GoblinTools {
    private GoblinTools() {}

    public static final int RADIUS = 16;

    /** Would one more use break the tool? Goblins stop before that and tell you. */
    static boolean worn(Goblin g) {
        ItemStack s = g.getMainHandItem();
        return s.isDamageableItem() && s.getMaxDamage() - s.getDamageValue() <= 2;
    }

    static void use(Goblin g) {
        ItemStack s = g.getMainHandItem();
        if (s.isDamageableItem()) s.hurtAndBreak(1, g, EquipmentSlot.MAINHAND);
        g.swing(InteractionHand.MAIN_HAND);
    }

    static void keep(Goblin g, BlockPos at, List<ItemStack> drops) {
        for (ItemStack d : drops) {
            ItemStack left = g.stow(d);
            if (!left.isEmpty()) Block.popResource(g.level(), at, left);
        }
    }

    static final class ToolWork extends Goal {
        private final Goblin g;
        private Task task;
        private int cooldown, warned;

        ToolWork(Goblin g) {
            this.g = g;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!g.isTame() || g.mode() != Goblin.MODE_BASE || g.depositing || g.resting() || g.isSleeping() || g.getTarget() != null) return false;
            if (g.tool() == Work.Tool.NONE || g.tool() == Work.Tool.SWORD || g.mood() < 15) return false;
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            if (g.tickCount % 20 != 0) return false;
            if (worn(g)) {
                if (warned-- <= 0 && g.getOwner() instanceof Player p) {
                    p.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.tool_worn", g.getDisplayName()));
                    warned = 30;
                }
                return false;
            }
            if (g.hasPack() && g.pack.getItem(g.pack.getContainerSize() - 1).isEmpty() == false && full(g)) {
                // pack is full: go home and put things away
                if (g.home() != null && g.blockPosition().distSqr(g.home()) > 25) {
                    task = new GoHome();
                    return true;
                }
                g.startDeposit();
                return false;
            }
            Work w = g.settings();
            task = switch (g.tool()) {
                case AXE -> w.axe == Work.AXE_OFF ? null : Chop.find(g);
                case PICKAXE -> w.pick == Work.PICK_OFF ? null : new Mine();
                case HOE -> w.hoe == Work.HOE_OFF ? null : Farm.find(g);
                case SHEARS -> w.shear ? Shear.find(g) : null;
                case CAN -> WaterCrops.find(g);
                default -> null;
            };
            if (task == null) cooldown = 60;
            return task != null;
        }

        @Override
        public boolean canContinueToUse() {
            return task != null && !task.done && g.mode() == Goblin.MODE_BASE && !g.depositing && g.getTarget() == null && !worn(g)
                    && g.tool() != Work.Tool.NONE;
        }

        @Override
        public void start() {
            task.start(g);
        }

        @Override
        public void tick() {
            task.tick(g);
        }

        @Override
        public void stop() {
            g.setWork(0);
            if (task != null) task.stop(g);
            task = null;
            cooldown = 20 + g.getRandom().nextInt(40);
        }
    }

    static boolean full(Goblin g) {
        for (int i = 0; i < g.pack.getContainerSize(); i++) if (g.pack.getItem(i).isEmpty()) return false;
        return true;
    }

    abstract static class Task {
        boolean done;
        int time;

        void start(Goblin g) {}

        abstract void tick(Goblin g);

        void stop(Goblin g) {}

        static boolean walkTo(Goblin g, BlockPos p, double reach) {
            if (g.position().distanceToSqr(Vec3.atBottomCenterOf(p)) < reach * reach) {
                g.getNavigation().stop();
                return true;
            }
            if (g.getNavigation().isDone() || g.tickCount % 30 == 0) g.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.1);
            return false;
        }
    }

    static final class GoHome extends Task {
        @Override
        void tick(Goblin g) {
            time++;
            BlockPos h = g.home();
            if (h == null || walkTo(g, h, 3) || time > 400) {
                if (h != null && time > 400) g.teleportTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5);
                g.startDeposit();
                done = true;
            }
        }
    }

    // ------------------------------------------------------------------ axe
    static final class Chop extends Task {
        final List<BlockPos> logs;
        final BlockPos base;
        final int wood;
        int next, hit;

        Chop(BlockPos base, List<BlockPos> logs, int wood) {
            this.base = base;
            this.logs = logs;
            this.wood = wood;
        }

        /** A real tree (a trunk standing on soil with leaves on top) of a wood it may cut, close to home. */
        static Chop find(Goblin g) {
            Level level = g.level();
            BlockPos c = g.home() != null ? g.home() : g.blockPosition();
            Work w = g.settings();
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-RADIUS, -4, -RADIUS), c.offset(RADIUS, 6, RADIUS))) {
                BlockState s = level.getBlockState(p);
                if (!s.is(BlockTags.LOGS)) continue;
                int wood = Work.woodOf(s);
                if (wood < 0 || !w.wood(wood)) continue;
                BlockState below = level.getBlockState(p.below());
                if (!below.is(BlockTags.DIRT) && !below.is(Blocks.NETHERRACK) && !below.is(BlockTags.NYLIUM)) continue;
                double d = p.distSqr(g.blockPosition());
                if (d < bd && hasLeaves(level, p)) {
                    bd = d;
                    best = p.immutable();
                }
            }
            if (best == null) return null;
            List<BlockPos> logs = trunk(level, best);
            return logs.size() >= 2 ? new Chop(best, logs, Work.woodOf(level.getBlockState(best))) : null;
        }

        static boolean hasLeaves(Level level, BlockPos base) {
            for (int y = 1; y < 12; y++) {
                for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, y, -2), base.offset(2, y, 2)))
                    if (level.getBlockState(q).is(BlockTags.LEAVES) || level.getBlockState(q).is(BlockTags.WART_BLOCKS)) return true;
            }
            return false;
        }

        /** All logs of the tree, from the bottom up (connected, also diagonally, at most 80). */
        static List<BlockPos> trunk(Level level, BlockPos base) {
            List<BlockPos> out = new ArrayList<>();
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            q.add(base);
            while (!q.isEmpty() && out.size() < 80) {
                BlockPos p = q.poll();
                if (!seen.add(p) || !level.getBlockState(p).is(BlockTags.LOGS) || p.getY() < base.getY()) continue;
                out.add(p);
                for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0) q.add(p.offset(dx, dy, dz));
            }
            out.sort((a, b) -> a.getY() != b.getY() ? Integer.compare(a.getY(), b.getY()) : Integer.compare(a.hashCode(), b.hashCode()));
            return out;
        }

        @Override
        void start(Goblin g) {
            g.setWork(7);
        }

        @Override
        void tick(Goblin g) {
            time++;
            if (time > 2400) {
                done = true;
                return;
            }
            if (!walkTo(g, base, 1.8)) return;
            g.getLookControl().setLookAt(Vec3.atCenterOf(logs.get(Math.min(next, logs.size() - 1))));
            int speed = Math.max(6, Math.round(22 / g.race().chopping));
            if (g.action() != Goblin.A_CHOP) g.act(Goblin.A_CHOP, 0);
            if (++hit < speed) {
                if (hit % 6 == 0) g.level().playSound(null, base, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1F);
                return;
            }
            hit = 0;
            // fell the trunk log by log, from the bottom - the goblin is short, the rest comes down to it
            while (next < logs.size() && !g.level().getBlockState(logs.get(next)).is(BlockTags.LOGS)) next++;
            if (next >= logs.size()) {
                finish(g);
                return;
            }
            BlockPos p = logs.get(next++);
            ServerLevel sl = (ServerLevel) g.level();
            BlockState s = sl.getBlockState(p);
            List<ItemStack> drops = Block.getDrops(s, sl, p, sl.getBlockEntity(p), g, g.getMainHandItem());
            sl.destroyBlock(p, false, g);
            keep(g, base, drops);
            use(g);
            g.addNeeds(-0.2F, -0.3F, g.traits().pastime == Traits.Pastime.CHOPPING ? 2 : 0, 0);
        }

        void finish(Goblin g) {
            g.act(Goblin.A_NONE, 1);
            if (g.settings().axe == Work.AXE_REPLANT && wood >= 0) {
                String w = Work.WOODS.get(wood);
                String sap = w.equals("mangrove") ? "mangrove_propagule" : w.equals("crimson") ? "crimson_fungus" : w.equals("warped") ? "warped_fungus"
                        : w + "_sapling";
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(sap));
                for (int i = 0; i < g.pack.getContainerSize(); i++) {
                    ItemStack s = g.pack.getItem(i);
                    if (s.is(item) && item instanceof BlockItem bi) {
                        BlockState st = bi.getBlock().defaultBlockState();
                        if (g.level().getBlockState(base).isAir() && st.canSurvive(g.level(), base)) {
                            g.level().setBlockAndUpdate(base, st);
                            g.level().playSound(null, base, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1F, 1F);
                            s.shrink(1);
                        }
                        break;
                    }
                }
            }
            done = true;
        }

        @Override
        void stop(Goblin g) {
            if (g.action() == Goblin.A_CHOP) g.act(Goblin.A_NONE, 1);
        }
    }

    // ------------------------------------------------------------------ pickaxe: its own mine
    /**
     * The goblin digs a staircase (two blocks high, so you can walk it too) down from a spot near home to deep
     * underground, then a long straight tunnel, taking the ores (and whatever else you marked) from the walls.
     */
    static final class Mine extends Task {
        static final int DEEP_Y = -52;
        BlockPos cell;
        int hit;

        @Override
        void start(Goblin g) {
            if (g.mine == null) g.mine = entrance(g);
            g.setWork(8);
        }

        static BlockPos entrance(Goblin g) {
            BlockPos h = g.home() != null ? g.home() : g.blockPosition();
            Level level = g.level();
            for (int r = 3; r < 10; r++) for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h.relative(d, r));
                if (level.getBlockState(p.below()).isSolid() && level.getBlockState(p).isAir()) return p.immutable();
            }
            return h;
        }

        /** Direction of the mine (along x, away from home). */
        Direction dir(Goblin g) {
            BlockPos h = g.home() != null ? g.home() : g.mine;
            return g.mine.getX() >= h.getX() ? Direction.EAST : Direction.WEST;
        }

        /** The step-th cell of the mine (its floor position): stairs going down one per step, then flat. */
        BlockPos at(Goblin g, int step) {
            Direction d = dir(g);
            int down = Math.min(step, Math.max(0, g.mine.getY() - DEEP_Y));
            return g.mine.relative(d, step + 1).below(down + 1);
        }

        @Override
        void tick(Goblin g) {
            time++;
            if (time > 4000) {
                done = true;
                return;
            }
            ServerLevel sl = (ServerLevel) g.level();
            if (cell == null) {
                // the next block to dig: the two cells of the next step, then the ores in its walls
                BlockPos floor = at(g, g.mineDepth);
                cell = nextBlock(g, sl, floor);
                if (cell == null) {
                    g.mineDepth++;
                    if (g.mineDepth % 8 == 0) torch(g, sl, at(g, g.mineDepth - 1));
                    if (g.mineDepth > 400) {
                        done = true;
                        if (g.getOwner() instanceof Player p) p.sendOverlayMessage(Component.translatable("goblin.gospodarstwo.mine_done", g.getDisplayName()));
                    }
                    return;
                }
                hit = 0;
            }
            BlockPos stand = at(g, Math.max(0, g.mineDepth - 1));
            if (g.mineDepth > 0 && !walkTo(g, stand, 1.4)) {
                if (time % 200 == 199) g.teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                return;
            }
            g.getLookControl().setLookAt(Vec3.atCenterOf(cell));
            if (g.action() != Goblin.A_MINE) g.act(Goblin.A_MINE, 0);
            BlockState s = sl.getBlockState(cell);
            float hardness = s.getDestroySpeed(sl, cell);
            if (hardness < 0) {
                cell = null;
                g.mineDepth = 9999;
                return;
            }
            int need = Math.max(5, Math.round((8 + hardness * 14) / g.race().mining));
            if (++hit % 5 == 0) sl.playSound(null, cell, s.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.7F, 1F);
            if (hit < need) return;
            dig(g, sl, cell);
            cell = null;
        }

        /** Blocks still to dig at this step: the two tunnel cells first, then wanted ores showing in the walls. */
        BlockPos nextBlock(Goblin g, ServerLevel sl, BlockPos floor) {
            for (BlockPos p : new BlockPos[] {floor.above(), floor.above(2)}) {
                BlockState s = sl.getBlockState(p);
                if (!s.isAir() && s.getFluidState().isEmpty() && s.getDestroySpeed(sl, p) >= 0) return p;
            }
            for (BlockPos t : new BlockPos[] {floor.above(), floor.above(2)}) for (Direction d : Direction.values()) {
                BlockPos p = t.relative(d);
                int k = Work.mineOf(sl.getBlockState(p));
                if (k >= 0 && k < 8 && g.settings().keeps(k)) return p;
            }
            return null;
        }

        void dig(Goblin g, ServerLevel sl, BlockPos p) {
            BlockState s = sl.getBlockState(p);
            int k = Work.mineOf(s);
            List<ItemStack> drops = Block.getDrops(s, sl, p, sl.getBlockEntity(p), g, g.getMainHandItem());
            sl.destroyBlock(p, false, g);
            if (k >= 0 && g.settings().keeps(k)) keep(g, p, drops);
            use(g);
            seal(g, sl, p);
            g.addNeeds(-0.2F, -0.3F, g.traits().pastime == Traits.Pastime.DIGGING ? 1.5F : 0, 0);
        }

        /** Water or lava next to the new hole gets walled off with stone (or the block it just dug). */
        void seal(Goblin g, ServerLevel sl, BlockPos hole) {
            for (Direction d : Direction.values()) {
                BlockPos n = hole.relative(d);
                if (sl.getFluidState(n).isEmpty()) continue;
                sl.setBlockAndUpdate(n, Blocks.COBBLESTONE.defaultBlockState());
            }
        }

        void torch(Goblin g, ServerLevel sl, BlockPos floor) {
            for (int i = 0; i < g.pack.getContainerSize(); i++) {
                ItemStack s = g.pack.getItem(i);
                if (!s.is(Items.TORCH)) continue;
                BlockPos at = floor.above();
                Direction side = dir(g).getClockWise();
                if (sl.getBlockState(at).isAir() && sl.getBlockState(at.relative(side)).isFaceSturdy(sl, at.relative(side), side.getOpposite())) {
                    sl.setBlockAndUpdate(at, Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING,
                            side.getOpposite()));
                    s.shrink(1);
                }
                return;
            }
        }

        @Override
        void stop(Goblin g) {
            if (g.action() == Goblin.A_MINE) g.act(Goblin.A_NONE, 1);
        }
    }

    // ------------------------------------------------------------------ hoe / scythe / sickle
    static final class Farm extends Task {
        final BlockPos at;
        final int kind; // 0 harvest, 1 till, 2 sow

        Farm(BlockPos at, int kind) {
            this.at = at;
            this.kind = kind;
        }

        static Farm find(Goblin g) {
            Level level = g.level();
            BlockPos c = g.home() != null ? g.home() : g.blockPosition();
            boolean farm = g.settings().hoe == Work.HOE_FARM;
            Item seed = farm ? seedIn(g) : null;
            Farm till = null, sow = null;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -2, -12), c.offset(12, 2, 12))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) return new Farm(p.immutable(), 0);
                if (!farm) continue;
                if (seed != null && sow == null && s.getBlock() instanceof FarmlandBlock && level.getBlockState(p.above()).isAir())
                    sow = new Farm(p.above().immutable(), 2);
                if (seed != null && till == null && (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)) && level.getBlockState(p.above()).isAir()
                        && nextToFarmland(level, p)) till = new Farm(p.immutable(), 1);
            }
            return sow != null ? sow : till;
        }

        static boolean nextToFarmland(Level level, BlockPos p) {
            for (Direction d : Direction.Plane.HORIZONTAL) if (level.getBlockState(p.relative(d)).getBlock() instanceof FarmlandBlock) return true;
            return false;
        }

        static Item seedIn(Goblin g) {
            for (int i = 0; i < g.pack.getContainerSize(); i++) {
                ItemStack s = g.pack.getItem(i);
                if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock) return s.getItem();
            }
            return null;
        }

        @Override
        void start(Goblin g) {
            g.setWork(2);
        }

        @Override
        void tick(Goblin g) {
            time++;
            if (time > 400) {
                done = true;
                return;
            }
            if (!walkTo(g, at, 1.6)) return;
            g.getLookControl().setLookAt(Vec3.atCenterOf(at));
            if (time % 4 == 0 && g.action() != Goblin.A_HOE) g.act(Goblin.A_HOE, 20);
            if (g.action() == Goblin.A_HOE && g.actionLeft() > 4) return;
            ServerLevel sl = (ServerLevel) g.level();
            switch (kind) {
                case 0 -> {
                    if (g.getMainHandItem().is(pl.kubalte.gospodarstwo.Gospodarstwo.SCYTHE.get()))
                        pl.kubalte.gospodarstwo.tools.ScytheItem.reap(sl, at, null, g.getMainHandItem());
                    else GoblinGoals.harvest(g, at);
                }
                case 1 -> {
                    sl.setBlockAndUpdate(at, Blocks.FARMLAND.defaultBlockState());
                    sl.playSound(null, at, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1F, 1F);
                }
                default -> {
                    Item seed = seedIn(g);
                    if (seed instanceof BlockItem bi && sl.getBlockState(at).isAir()) {
                        sl.setBlockAndUpdate(at, bi.getBlock().defaultBlockState());
                        sl.playSound(null, at, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1F, 1F);
                        for (int i = 0; i < g.pack.getContainerSize(); i++) if (g.pack.getItem(i).is(seed)) {
                            g.pack.getItem(i).shrink(1);
                            break;
                        }
                    }
                }
            }
            use(g);
            g.addNeeds(-0.1F, -0.2F, g.traits().pastime == Traits.Pastime.FARMING ? 3 : 0, 0);
            done = true;
        }
    }

    // ------------------------------------------------------------------ shears
    static final class Shear extends Task {
        final Animal sheep;

        Shear(Animal sheep) {
            this.sheep = sheep;
        }

        static Shear find(Goblin g) {
            BlockPos c = g.home() != null ? g.home() : g.blockPosition();
            List<Animal> l = g.level().getEntitiesOfClass(Animal.class, new AABB(c).inflate(RADIUS), a -> a instanceof Shearable sh
                    && sh.readyForShearing() && a.isAlive());
            if (l.isEmpty()) return null;
            l.sort((a, b) -> Double.compare(a.distanceToSqr(g), b.distanceToSqr(g)));
            return new Shear(l.get(0));
        }

        @Override
        void start(Goblin g) {
            g.setWork(9);
        }

        @Override
        void tick(Goblin g) {
            time++;
            if (time > 400 || !sheep.isAlive()) {
                done = true;
                return;
            }
            if (g.distanceToSqr(sheep) > 2.5) {
                if (g.getNavigation().isDone() || time % 20 == 0) g.getNavigation().moveTo(sheep, 1.1);
                return;
            }
            g.getNavigation().stop();
            sheep.getNavigation().stop();
            g.getLookControl().setLookAt(sheep);
            if (g.action() != Goblin.A_SHEAR) {
                g.act(Goblin.A_SHEAR, 24);
                return;
            }
            if (g.actionLeft() > 2) return;
            if (sheep instanceof Shearable sh && sh.readyForShearing()) {
                sh.shear((ServerLevel) g.level(), SoundSource.NEUTRAL, g.getMainHandItem());
                use(g);
                g.addNeeds(0, -0.2F, g.traits().pastime == Traits.Pastime.ANIMALS ? 3 : 1, 0);
            }
            done = true;
        }
    }

    // ------------------------------------------------------------------ watering can
    static final class WaterCrops extends Task {
        final BlockPos at;
        final boolean refill;

        WaterCrops(BlockPos at, boolean refill) {
            this.at = at;
            this.refill = refill;
        }

        static WaterCrops find(Goblin g) {
            Level level = g.level();
            BlockPos c = g.home() != null ? g.home() : g.blockPosition();
            boolean empty = WateringCanItem.water(g.getMainHandItem()) <= 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -2, -12), c.offset(12, 2, 12))) {
                BlockState s = level.getBlockState(p);
                if (empty) {
                    if (s.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && s.getFluidState().isSource()) return new WaterCrops(p.immutable(), true);
                } else if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s) && g.getRandom().nextInt(6) == 0) {
                    return new WaterCrops(p.immutable(), false);
                }
            }
            return null;
        }

        @Override
        void tick(Goblin g) {
            time++;
            if (time > 400) {
                done = true;
                return;
            }
            if (!walkTo(g, at, refill ? 2.2 : 1.8)) return;
            g.getLookControl().setLookAt(Vec3.atCenterOf(at));
            if (g.action() != Goblin.A_WORK) {
                g.act(Goblin.A_WORK, 20);
                return;
            }
            if (g.actionLeft() > 2) return;
            ItemStack can = g.getMainHandItem();
            if (refill) {
                WateringCanItem.setWater(can, WateringCanItem.max());
                g.level().playSound(null, at, SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.8F, 1.2F);
            } else {
                WateringCanItem.waterArea((ServerLevel) g.level(), at, g.getRandom());
                WateringCanItem.setWater(can, WateringCanItem.water(can) - 1);
                g.level().playSound(null, at, SoundEvents.WEATHER_RAIN, SoundSource.NEUTRAL, 0.5F, 1.6F);
                g.addNeeds(0, -0.1F, g.traits().pastime == Traits.Pastime.FARMING ? 2 : 0, 0);
            }
            done = true;
        }
    }
}
