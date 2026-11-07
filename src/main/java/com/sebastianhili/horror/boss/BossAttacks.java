package com.sebastianhili.horror.boss;

import com.sebastianhili.horror.config.SebConfig;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.entity.ThrownPenny;
import com.sebastianhili.horror.registry.ModDamageTypes;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sebastian's boss attacks, scheduled against the song. Every attack is telegraphed (a sound,
 * particles on the ground, or a line) about a second before it lands, so it can always be dodged.
 */
final class BossAttacks {
    /** Sections of "The World Looks Red", in seconds, matched to the music's loud and quiet parts. */
    enum Phase {
        ASSAULT(42, 110), GROOVE(110, 148), BUILD(148, 170), DARK(170, 192), RETURN(192, 212),
        CLIMAX(212, 298), BREATHER(298, 340), DROP(340, 362), FINAL(362, 384);

        final int from, to;

        Phase(int fromSec, int toSec) {
            this.from = fromSec * 20;
            this.to = toSec * 20;
        }

        static Phase at(int t) {
            for (Phase p : values()) if (t < p.to) return p;
            return FINAL;
        }
    }

    enum Kind { FAN, DASH, GEYSER, BLINK, WAVE, LIGHTNING, RAIN }

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.9F, 0.04F, 0.04F), 1.7F);
    private static final DustParticleOptions DARK_RED = new DustParticleOptions(new Vector3f(0.45F, 0.0F, 0.0F), 2.2F);

    private final BossFight fight;
    private final SebastianEntity boss;
    private final ServerLevel level;
    private final RandomSource rng;

    private record Task(int at, Runnable run) {
    }

    private final List<Task> tasks = new ArrayList<>();
    private int now;
    /** Counts calls (one per server tick) for effects that only need a steady rhythm, not the song. */
    private int frame;
    private int lastStorm;
    private int nextAttack;
    private int busyUntil;
    @Nullable
    private Kind last;
    @Nullable
    private Phase phase;
    private int idleCooldown;

    // dash
    private Vec3 dashDir = Vec3.ZERO;
    private int dashLeft;
    private final Set<UUID> dashHit = new HashSet<>();
    // leap and slam
    private boolean slamPending;
    private int airTicks;
    private int slamsLeft;

    private static final class Wave {
        final Vec3 c;
        double r = 1.0;
        final Set<UUID> hit = new HashSet<>();

        Wave(Vec3 c) {
            this.c = c;
        }
    }

    private final List<Wave> waves = new ArrayList<>();
    // the dark
    @Nullable
    private UUID stalkTarget;
    private int stalkBlink;
    private int stalkHitCooldown;

    BossAttacks(BossFight fight) {
        this.fight = fight;
        this.boss = fight.boss();
        this.level = fight.arena();
        this.rng = level.getRandom();
    }

    void stopAll() {
        tasks.clear();
        dashLeft = 0;
        slamPending = false;
        waves.clear();
        boss.setInvisible(false);
        boss.setPose(Pose.STANDING);
    }

    // ------------------------------------------------------------------ main loop

    void tick(int t) {
        now = t;
        frame++;
        Phase p = Phase.at(t);
        if (p != phase) {
            if (phase != null) leave(phase);
            phase = p;
            enter(p);
        }
        runTasks();
        tickDash();
        tickSlam();
        tickWaves();
        if (stalkHitCooldown > 0) stalkHitCooldown--;
        if (phase == Phase.DARK) {
            tickStalk();
            return;
        }
        idle();
        if (t >= nextAttack && t >= busyUntil && dashLeft <= 0 && !slamPending) {
            launch();
        }
        // a background storm during the loudest stretches
        boolean peak = t >= 256 * 20 && t < 278 * 20;
        if ((peak && t - lastStorm >= 70) || (phase == Phase.FINAL && t - lastStorm >= 80)) {
            lastStorm = t;
            lightning(Math.max(2, fight.alive().size()), 30);
        }
    }

    private void enter(Phase p) {
        switch (p) {
            case GROOVE -> fight.say("you're still standing.");
            case BUILD -> fight.say("the sky is coming down.");
            case DARK -> {
                fight.say("lights out.");
                boss.setInvisible(true);
                for (ServerPlayer pl : fight.alive()) {
                    pl.addEffect(new MobEffectInstance(MobEffects.DARKNESS, (p.to - p.from) + 20, 0, false, false, true));
                    pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, true, true));
                }
                stalkBlink = 30;
            }
            case RETURN -> fight.say("there you are.");
            case CLIMAX -> {
                fight.say("NOW.");
                later(44 * 20, () -> fight.say("WHY WON'T YOU FALL"));
            }
            case BREATHER -> {
                for (ServerPlayer pl : fight.alive()) {
                    pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0, false, true, true));
                }
                boss.getNavigation().moveTo(BossFight.CENTER.getX() + 0.5, BossFight.CENTER.getY() + 1, BossFight.CENTER.getZ() + 0.5, 1.0);
                later(10, () -> fight.say("why won't you just stop."));
                later(250, () -> fight.say("it was one cent. one."));
                later(500, () -> fight.say("i can hear the song ending. can you?"));
                later(780, () -> fight.say("...no. not yet."));
            }
            case DROP -> fight.say("NOT YET.");
            case FINAL -> fight.say("EVERYTHING. ALL OF IT.");
            default -> {
            }
        }
    }

    private void leave(Phase p) {
        if (p == Phase.DARK) {
            boss.setInvisible(false);
            for (ServerPlayer pl : fight.alive()) pl.removeEffect(MobEffects.DARKNESS);
            fx(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.5), boss.getZ(), 30, 0.5, 0.02);
        }
    }

    private void launch() {
        List<Kind> pool = switch (phase) {
            case ASSAULT -> List.of(Kind.FAN, Kind.DASH, Kind.GEYSER, Kind.BLINK);
            case GROOVE -> List.of(Kind.WAVE, Kind.BLINK, Kind.FAN);
            case BUILD -> List.of(Kind.LIGHTNING, Kind.FAN, Kind.LIGHTNING);
            case RETURN -> List.of(Kind.DASH, Kind.WAVE);
            case CLIMAX -> List.of(Kind.FAN, Kind.DASH, Kind.GEYSER, Kind.BLINK, Kind.WAVE, Kind.LIGHTNING, Kind.RAIN);
            case BREATHER -> List.of(Kind.WAVE);
            case DROP -> List.of(Kind.DASH, Kind.WAVE);
            case FINAL -> List.of(Kind.RAIN, Kind.LIGHTNING, Kind.DASH, Kind.FAN, Kind.GEYSER);
            default -> List.of(Kind.FAN);
        };
        Kind k = pool.get(rng.nextInt(pool.size()));
        if (k == last && pool.size() > 2) k = pool.get(rng.nextInt(pool.size()));
        last = k;
        boolean hard = phase == Phase.CLIMAX || phase == Phase.FINAL;
        switch (k) {
            case FAN -> fan(hard ? 2 : 1);
            case DASH -> dash(phase == Phase.DROP ? 3 : (phase == Phase.RETURN || hard) ? 2 : 1);
            case GEYSER -> geyser(hard);
            case BLINK -> blink(hard ? 2 : 1);
            case WAVE -> slam(phase == Phase.DROP ? 2 : 1);
            case LIGHTNING -> {
                float progress = phase == Phase.BUILD ? (now - phase.from) / (float) (phase.to - phase.from) : 1.0F;
                int n = phase == Phase.BUILD ? 2 + Math.round(progress * 4) : hard ? 5 : 3;
                lightning(n, 26);
            }
            case RAIN -> rain(50);
        }
        nextAttack = now + cadence();
    }

    private int cadence() {
        return switch (phase) {
            case ASSAULT -> 60;
            case GROOVE -> 64;
            case BUILD -> (int) Mth.lerp((now - phase.from) / (float) (phase.to - phase.from), 56, 40);
            case RETURN -> 54;
            case CLIMAX -> now >= 256 * 20 && now < 278 * 20 ? 36 : 46;
            case BREATHER -> 130;
            case DROP -> 44;
            case FINAL -> 36;
            default -> 60;
        };
    }

    /** Between attacks he prowls toward the nearest player, staring. */
    private void idle() {
        ServerPlayer near = fight.nearest();
        if (near != null && dashLeft <= 0) boss.getLookControl().setLookAt(near, 360, 360);
        if (now < busyUntil || dashLeft > 0 || slamPending || near == null || phase == Phase.BREATHER) return;
        if (--idleCooldown > 0) return;
        idleCooldown = 25;
        Vec3 toMe = boss.position().subtract(near.position()).multiply(1, 0, 1);
        Vec3 dir = toMe.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : toMe.normalize();
        double ang = (rng.nextDouble() - 0.5) * 1.6;
        Vec3 rot = new Vec3(dir.x * Math.cos(ang) - dir.z * Math.sin(ang), 0, dir.x * Math.sin(ang) + dir.z * Math.cos(ang));
        Vec3 dest = clampToIsland(near.position().add(rot.scale(7 + rng.nextDouble() * 4)), 20);
        boss.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.25);
    }

    // ------------------------------------------------------------------ attacks

    /** A fan of pennies thrown at one player. */
    private void fan(int volleys) {
        ServerPlayer target = randomTarget();
        if (target == null) return;
        busy(18 + volleys * 16);
        boss.getNavigation().stop();
        for (int v = 0; v < volleys; v++) {
            later(v * 16, () -> {
                boss.getLookControl().setLookAt(target, 360, 360);
                boss.swing(InteractionHand.MAIN_HAND);
                sound(boss.position(), ModSounds.PENNY_CLINK.get(), 1.4F, 0.7F);
                fx(ParticleTypes.CRIT, boss.getX(), boss.getEyeY(), boss.getZ(), 8, 0.4, 0.1);
            });
            later(v * 16 + 11, () -> shootFan(target, 7, 64));
        }
    }

    private void shootFan(ServerPlayer target, int n, double spreadDeg) {
        if (!target.isAlive()) return;
        Vec3 eye = boss.getEyePosition();
        Vec3 to = new Vec3(target.getX(), target.getY(0.55), target.getZ()).subtract(eye);
        double baseYaw = Math.atan2(to.z, to.x);
        double pitch = Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z));
        for (int i = 0; i < n; i++) {
            double yaw = baseYaw + Math.toRadians((i - (n - 1) / 2.0) * spreadDeg / (n - 1));
            ThrownPenny tp = new ThrownPenny(level, boss);
            tp.setPos(eye.x, eye.y - 0.3, eye.z);
            tp.shoot(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch) + 0.03, Math.sin(yaw) * Math.cos(pitch), 1.15F, 0.6F);
            tp.setDamage(dmg(4));
            level.addFreshEntity(tp);
        }
        sound(boss.position(), SoundEvents.WITCH_THROW, 1.0F, 0.6F);
    }

    /** He crouches, a red line shows where he will go, then he charges straight through. */
    private void dash(int count) {
        busy(count * 32 + 10);
        for (int i = 0; i < count; i++) later(i * 32, this::dashOnce);
    }

    private void dashOnce() {
        ServerPlayer target = randomTarget();
        if (target == null) return;
        boss.getNavigation().stop();
        boss.setPose(Pose.CROUCHING);
        Vec3 from = boss.position();
        Vec3 flat = target.position().subtract(from).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : flat.normalize();
        double len = 24;
        while (len > 8 && from.add(dir.scale(len)).subtract(Vec3.atBottomCenterOf(BossFight.CENTER)).multiply(1, 0, 1).length() > 23) {
            len -= 1;
        }
        double length = len;
        sound(from, SoundEvents.RAVAGER_ROAR, 1.3F, 1.5F);
        for (int k = 0; k < 18; k += 3) {
            later(k, () -> {
                for (double s = 0; s <= length; s += 0.8) {
                    Vec3 pt = from.add(dir.scale(s));
                    fx(RED, pt.x, pt.y + 0.15, pt.z, 1, 0.05, 0);
                }
            });
        }
        later(18, () -> {
            boss.setPose(Pose.STANDING);
            dashDir = dir;
            dashLeft = (int) Math.ceil(length / 1.9);
            dashHit.clear();
            sound(boss.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
            sound(boss.position(), SoundEvents.FIRECHARGE_USE, 1.2F, 0.8F);
        });
    }

    private void tickDash() {
        if (dashLeft <= 0) return;
        dashLeft--;
        boss.setDeltaMovement(dashDir.x * 1.9, boss.getDeltaMovement().y, dashDir.z * 1.9);
        boss.hasImpulse = true;
        float yaw = (float) (Math.toDegrees(Math.atan2(dashDir.z, dashDir.x)) - 90);
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
        fx(ParticleTypes.FLAME, boss.getX(), boss.getY(0.5), boss.getZ(), 6, 0.3, 0.02);
        fx(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.3), boss.getZ(), 2, 0.2, 0.01);
        AABB box = boss.getBoundingBox().inflate(1.0, 0.4, 1.0);
        for (ServerPlayer p : fight.alive()) {
            if (!dashHit.contains(p.getUUID()) && box.intersects(p.getBoundingBox())) {
                dashHit.add(p.getUUID());
                hurt(p, ModDamageTypes.BOSS_DASH, 10);
                knock(p, dashDir.x * 1.6, 0.55, dashDir.z * 1.6);
            }
        }
        if (dashLeft == 0) boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
    }

    /** Fire rings open under everyone's feet, then erupt. */
    private void geyser(boolean extra) {
        busy(42);
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : fight.alive()) spots.add(new Vec3(p.getX(), Math.floor(p.getY()), p.getZ()));
        if (extra) {
            for (int i = 0; i < 3; i++) spots.add(randomIslandSpot());
        }
        for (Vec3 s : spots) {
            sound(s, SoundEvents.LAVA_AMBIENT, 1.5F, 0.7F);
            for (int k = 0; k < 30; k += 3) {
                later(k, () -> {
                    ring(ParticleTypes.FLAME, s.add(0, 0.1, 0), 1.7, 14);
                    fx(ParticleTypes.LAVA, s.x, s.y + 0.2, s.z, 1, 0.3, 0);
                });
            }
            later(30, () -> {
                fx(ParticleTypes.LAVA, s.x, s.y + 0.5, s.z, 25, 0.6, 0);
                for (int h = 0; h < 6; h++) fx(ParticleTypes.FLAME, s.x, s.y + h, s.z, 8, 0.35, 0.03);
                fx(ParticleTypes.LARGE_SMOKE, s.x, s.y + 3, s.z, 12, 0.5, 0.05);
                sound(s, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.7F);
                sound(s, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
                for (ServerPlayer p : fight.alive()) {
                    double dx = p.getX() - s.x, dz = p.getZ() - s.z, dy = p.getY() - s.y;
                    if (dx * dx + dz * dz < 1.9 * 1.9 && dy > -1 && dy < 4) {
                        hurt(p, ModDamageTypes.BOSS_ERUPTION, 8);
                        p.setSecondsOnFire(4);
                        knock(p, 0, 1.05, 0);
                    }
                }
            });
        }
    }

    /** He appears behind you, winds up for a moment, and slashes. Keep moving. */
    private void blink(int count) {
        busy(count * 28 + 6);
        for (int i = 0; i < count; i++) later(i * 28, this::blinkOnce);
    }

    private void blinkOnce() {
        ServerPlayer target = randomTarget();
        if (target == null) return;
        Vec3 back = target.getViewVector(1).multiply(1, 0, 1);
        back = back.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : back.normalize();
        Vec3 spot = standable(target.position().subtract(back.scale(2.3)), target.position());
        fx(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.5), boss.getZ(), 15, 0.4, 0.02);
        boss.getNavigation().stop();
        boss.teleportTo(spot.x, spot.y, spot.z);
        boss.getLookControl().setLookAt(target, 360, 360);
        target.playNotifySound(ModSounds.STING.get(), SoundSource.HOSTILE, 1.0F, 1.4F);
        target.displayClientMessage(Component.literal("behind you.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        for (int k = 0; k < 15; k += 3) {
            later(k, () -> fx(RED, boss.getX(), boss.getY(0.75), boss.getZ(), 4, 0.5, 0));
        }
        later(8, () -> boss.swing(InteractionHand.MAIN_HAND));
        later(15, () -> {
            Vec3 front = boss.position().add(boss.getViewVector(1).multiply(1, 0, 1).normalize().scale(1.3));
            fx(ParticleTypes.SWEEP_ATTACK, front.x, boss.getY(0.6), front.z, 3, 0.4, 0);
            sound(boss.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.5F);
            if (target.isAlive() && target.level() == level && boss.distanceTo(target) < 3.4) {
                hurt(target, ModDamageTypes.BOSS_SLASH, 11);
                Vec3 push = target.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
                knock(target, push.x * 1.1, 0.35, push.z * 1.1);
            }
        });
    }

    /** He leaps and slams the ground: a ring of red rolls outward. Jump over it. */
    private void slam(int count) {
        slamsLeft = count;
        busy(count * 50 + 20);
        startLeap();
    }

    private void startLeap() {
        boss.getNavigation().stop();
        boss.setDeltaMovement(0, 0.95, 0);
        boss.hasImpulse = true;
        slamPending = true;
        airTicks = 0;
        sound(boss.position(), SoundEvents.RAVAGER_ROAR, 1.4F, 0.6F);
    }

    private void tickSlam() {
        if (!slamPending) return;
        airTicks++;
        if ((airTicks > 5 && boss.onGround()) || airTicks > 40) {
            slamPending = false;
            Vec3 c = boss.position();
            waves.add(new Wave(c));
            sound(c, SoundEvents.GENERIC_EXPLODE, 1.6F, 0.5F);
            sound(c, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.6F);
            fx(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 0.8, 0);
            if (--slamsLeft > 0) later(14, this::startLeap);
        }
    }

    private void tickWaves() {
        Iterator<Wave> it = waves.iterator();
        while (it.hasNext()) {
            Wave w = it.next();
            w.r += 0.7;
            if (w.r > 27) {
                it.remove();
                continue;
            }
            int points = Mth.clamp((int) (w.r * 4.5), 16, 72);
            for (int i = 0; i < points; i++) {
                double a = i * Math.PI * 2 / points;
                double x = w.c.x + Math.cos(a) * w.r, z = w.c.z + Math.sin(a) * w.r;
                fx(i % 3 == 0 ? ParticleTypes.FLAME : DARK_RED, x, w.c.y + 0.25, z, 1, 0.02, 0);
            }
            for (ServerPlayer p : fight.alive()) {
                if (w.hit.contains(p.getUUID())) continue;
                double d = Math.sqrt((p.getX() - w.c.x) * (p.getX() - w.c.x) + (p.getZ() - w.c.z) * (p.getZ() - w.c.z));
                if (Math.abs(d - w.r) < 0.9 && Math.abs(p.getY() - w.c.y) < 1.1 && p.onGround()) {
                    w.hit.add(p.getUUID());
                    hurt(p, ModDamageTypes.BOSS_WAVE, 7);
                    Vec3 out = p.position().subtract(w.c).multiply(1, 0, 1).normalize();
                    knock(p, out.x * 0.7, 0.6, out.z * 0.7);
                }
            }
        }
    }

    /** Sparks mark the ground, then red lightning hits each mark. */
    private void lightning(int n, int delay) {
        busy(Math.max(busyUntil - now, 20));
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : fight.alive()) {
            Vec3 lead = p.getDeltaMovement().multiply(1, 0, 1).scale(10);
            spots.add(clampToIsland(new Vec3(p.getX() + lead.x, Math.floor(p.getY()), p.getZ() + lead.z), 23));
        }
        while (spots.size() < n) spots.add(randomIslandSpot());
        for (Vec3 s : spots) {
            sound(s, SoundEvents.BEACON_POWER_SELECT, 1.0F, 2.0F);
            for (int k = 0; k < delay; k += 3) {
                later(k, () -> {
                    ring(ParticleTypes.ELECTRIC_SPARK, s.add(0, 0.15, 0), 1.8, 12);
                    fx(ParticleTypes.END_ROD, s.x, s.y + 0.5, s.z, 1, 0.1, 0.05);
                });
            }
            later(delay, () -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.setVisualOnly(true);
                    bolt.moveTo(s.x, s.y, s.z);
                    level.addFreshEntity(bolt);
                }
                fx(DARK_RED, s.x, s.y + 1, s.z, 30, 0.6, 0);
                for (ServerPlayer p : fight.alive()) {
                    double dx = p.getX() - s.x, dz = p.getZ() - s.z;
                    if (dx * dx + dz * dz < 2.3 * 2.3 && Math.abs(p.getY() - s.y) < 4) {
                        hurt(p, ModDamageTypes.BOSS_LIGHTNING, 8);
                        p.setSecondsOnFire(3);
                    }
                }
            });
        }
    }

    /** Pennies rain down around everyone. Keep moving. */
    private void rain(int duration) {
        busy(duration + 10);
        sound(boss.position(), ModSounds.PENNY_CLINK.get(), 2.0F, 0.5F);
        for (int k = 0; k < duration; k += 2) {
            later(k, () -> {
                for (ServerPlayer p : fight.alive()) {
                    for (int i = 0; i < 2; i++) {
                        double x = p.getX() + (rng.nextDouble() - 0.5) * 12, z = p.getZ() + (rng.nextDouble() - 0.5) * 12;
                        ThrownPenny tp = new ThrownPenny(level, boss);
                        tp.setPos(x, p.getY() + 16, z);
                        tp.shoot(0, -1, 0, 1.0F, 1.5F);
                        tp.setDamage(dmg(3));
                        level.addFreshEntity(tp);
                        fx(ParticleTypes.SMOKE, x, Math.floor(p.getY()) + 0.1, z, 2, 0.1, 0);
                    }
                }
            });
        }
    }

    /** The Dark: he is invisible except for his eyes, and only a heartbeat tells you he's close. */
    private void tickStalk() {
        List<ServerPlayer> alive = fight.alive();
        if (alive.isEmpty()) return;
        ServerPlayer target = null;
        for (ServerPlayer p : alive) if (p.getUUID().equals(stalkTarget)) target = p;
        if (target == null || --stalkBlink <= 0) {
            target = alive.get(rng.nextInt(alive.size()));
            stalkTarget = target.getUUID();
            stalkBlink = 100;
            Vec3 back = target.getViewVector(1).multiply(1, 0, 1);
            back = back.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : back.normalize();
            Vec3 spot = standable(clampToIsland(target.position().subtract(back.scale(10)), 22), target.position());
            boss.teleportTo(spot.x, spot.y, spot.z);
        }
        boss.getLookControl().setLookAt(target, 360, 360);
        if (frame % 10 == 0) boss.getNavigation().moveTo(target, 1.1);
        // two tiny flames where his eyes are: the only thing you can see of him in the dark
        Vec3 look = boss.getViewVector(1);
        Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(0.15);
        Vec3 eyes = boss.getEyePosition().add(0, 0.35, 0).add(look.multiply(1, 0, 1).normalize().scale(0.4));
        fx(ParticleTypes.SMALL_FLAME, eyes.x + side.x, eyes.y, eyes.z + side.z, 1, 0, 0);
        fx(ParticleTypes.SMALL_FLAME, eyes.x - side.x, eyes.y, eyes.z - side.z, 1, 0, 0);
        double d = boss.distanceTo(target);
        int beat = Mth.clamp((int) (d * 1.6), 6, 30);
        if (frame % beat == 0) {
            target.playNotifySound(ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, (float) Mth.clamp(1.4 - d / 14, 0.3, 1.0), 1.0F);
        }
        if (d < 1.7 && stalkHitCooldown <= 0) {
            stalkHitCooldown = 30;
            hurt(target, ModDamageTypes.BOSS_STALK, 6);
            target.displayClientMessage(Component.literal("found you.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            stalkBlink = 8;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void later(int delay, Runnable r) {
        tasks.add(new Task(now + delay, r));
    }

    private void runTasks() {
        if (tasks.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = tasks.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.at <= now) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) task.run.run();
    }

    private void busy(int ticks) {
        busyUntil = Math.max(busyUntil, now + ticks);
    }

    @Nullable
    private ServerPlayer randomTarget() {
        List<ServerPlayer> alive = fight.alive();
        return alive.isEmpty() ? null : alive.get(rng.nextInt(alive.size()));
    }

    private float dmg(float base) {
        return (float) (base * SebConfig.BOSS_DAMAGE.get());
    }

    private void hurt(ServerPlayer p, ResourceKey<DamageType> type, float base) {
        p.hurt(ModDamageTypes.source(level, type, boss), dmg(base));
    }

    private static void knock(ServerPlayer p, double x, double y, double z) {
        p.push(x, y, z);
        p.hurtMarked = true;
    }

    private void fx(ParticleOptions type, double x, double y, double z, int count, double spread, double speed) {
        for (ServerPlayer p : fight.alive()) {
            level.sendParticles(p, type, true, x, y, z, count, spread, spread, spread, speed);
        }
    }

    private void ring(ParticleOptions type, Vec3 c, double r, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            fx(type, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0.02, 0);
        }
    }

    private void sound(Vec3 at, SoundEvent s, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, volume, pitch);
    }

    private Vec3 randomIslandSpot() {
        double a = rng.nextDouble() * Math.PI * 2, r = rng.nextDouble() * 20;
        return new Vec3(BossFight.CENTER.getX() + 0.5 + Math.cos(a) * r, BossFight.CENTER.getY() + 1, BossFight.CENTER.getZ() + 0.5 + Math.sin(a) * r);
    }

    private static Vec3 clampToIsland(Vec3 v, double maxR) {
        double cx = BossFight.CENTER.getX() + 0.5, cz = BossFight.CENTER.getZ() + 0.5;
        double dx = v.x - cx, dz = v.z - cz, r = Math.sqrt(dx * dx + dz * dz);
        if (r <= maxR) return v;
        return new Vec3(cx + dx / r * maxR, v.y, cz + dz / r * maxR);
    }

    /** A spot he can stand on near {@code want}, falling back to {@code fallback}. */
    private Vec3 standable(Vec3 want, Vec3 fallback) {
        BlockPos base = BlockPos.containing(want);
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos pos = base.above(dy);
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getFluidState(pos).isEmpty()
                    && level.noCollision(boss, boss.getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)))) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return fallback;
    }
}
