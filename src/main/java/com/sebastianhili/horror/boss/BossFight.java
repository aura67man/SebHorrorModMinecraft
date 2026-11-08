package com.sebastianhili.horror.boss;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebAdvancements;
import com.sebastianhili.horror.entity.SebChat;
import com.sebastianhili.horror.entity.SebSpawner;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.registry.ModEntities;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The boss fight. Everything is timed against the song "The World Looks Red" (6:39):
 * <pre>
 *  -4.0s  arrive in The Red, frozen, Sebastian a little way in front of you
 *  -2.5s  "you..."
 *   0:00  the song starts; he talks for 42 seconds
 *   0:42  the climax hits: boss bar, you can move, survive
 *   6:24  he tires and starts to fade
 *   6:39  the song ends: everyone still alive goes home, and he is gone for good
 * </pre>
 * Song time is counted in server ticks ({@link #time()}), 0 = the first note.
 */
public final class BossFight {
    public static final ResourceKey<Level> THE_RED =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, "the_red"));
    public static final BlockPos CENTER = new BlockPos(0, 64, 0);

    public static final int T_ARRIVE = -80;
    public static final int T_YOU = -50;
    public static final int T_FIGHT = 42 * 20;
    public static final int T_TIRED = 384 * 20;
    public static final int T_END = 399 * 20 + 10;

    private static final String RETURN_TAG = "SebBossReturn";

    @Nullable
    private static BossFight active;
    private static boolean allowTeleport;
    /** Inventories of players who died in The Red, given back when they respawn. */
    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();

    private record Snapshot(ListTag inventory, int xp) {
    }

    private record ReturnPoint(ResourceKey<Level> dim, double x, double y, double z, float yaw, float pitch) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", dim.location().toString());
            t.putDouble("X", x);
            t.putDouble("Y", y);
            t.putDouble("Z", z);
            t.putFloat("Yaw", yaw);
            t.putFloat("Pitch", pitch);
            return t;
        }

        static ReturnPoint load(CompoundTag t) {
            return new ReturnPoint(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))),
                    t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), t.getFloat("Yaw"), t.getFloat("Pitch"));
        }
    }

    private final MinecraftServer server;
    private final ServerLevel arena;
    private final Map<UUID, ReturnPoint> participants = new LinkedHashMap<>();
    private final Set<UUID> inArena = new LinkedHashSet<>();
    private final Map<UUID, Vec3> frozenAt = new HashMap<>();
    private final SebastianEntity boss;
    @Nullable
    private ServerBossEvent bar;
    @Nullable
    private BossAttacks attacks;
    private int t = T_ARRIVE;
    private int lastT = T_ARRIVE;
    /** Wall-clock start of the fight. Song time follows the real clock, not server ticks, so a lagging
     *  server can't fall behind the music (which the clients play in real time). */
    private final long startNanos = System.nanoTime();
    private int skipOffset;
    private boolean begun;
    private boolean tiredDone;
    private boolean finished;
    private int fallen;

    private BossFight(MinecraftServer server, ServerLevel arena, SebastianEntity boss) {
        this.server = server;
        this.arena = arena;
        this.boss = boss;
    }

    // ------------------------------------------------------------------ queries

    @Nullable
    public static BossFight get() {
        return active;
    }

    public static boolean isActive() {
        return active != null;
    }

    public int time() {
        return t;
    }

    public SebastianEntity boss() {
        return boss;
    }

    public ServerLevel arena() {
        return arena;
    }

    public boolean isInArena(Player p) {
        return inArena.contains(p.getUUID());
    }

    /** Players still alive in the arena. */
    public List<ServerPlayer> alive() {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : inArena) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive() && p.level() == arena) out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------------ starting

    /** Someone showed him Exhibit A. Everyone online is pulled into The Red. */
    public static boolean start(SebastianEntity trigger, ServerPlayer shower) {
        if (active != null) return false;
        MinecraftServer server = shower.server;
        ServerLevel arena = server.getLevel(THE_RED);
        if (arena == null) {
            shower.sendSystemMessage(Component.literal("The Red dimension is missing (is the mod's data pack disabled?)").withStyle(ChatFormatting.RED));
            return false;
        }
        RedArena.build(arena, CENTER, shower.getRandom().nextLong());

        SebastianEntity boss = ModEntities.SEBASTIAN_HILI.get().create(arena);
        if (boss == null) return false;
        boss.setBoss(true);
        boss.moveTo(CENTER.getX() + 0.5, CENTER.getY() + 1, CENTER.getZ() - 7.5, 0, 0);
        boss.setYHeadRot(0);
        arena.addFreshEntity(boss);

        BossFight fight = new BossFight(server, arena, boss);
        active = fight;

        if (trigger.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.LARGE_SMOKE, trigger.getX(), trigger.getY(1), trigger.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        }
        SebChat.say(trigger, "...where did you get that.");
        trigger.discard();

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && p.isAlive()) players.add(p);
        }
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer p = players.get(i);
            ReturnPoint rp = new ReturnPoint(p.level().dimension(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
            fight.participants.put(p.getUUID(), rp);
            persisted(p).put(RETURN_TAG, rp.save());
            double x = CENTER.getX() + 0.5 + (i - (players.size() - 1) / 2.0) * 2.0;
            double z = CENTER.getZ() + 7.5;
            teleport(p, arena, x, CENTER.getY() + 1, z, 180, 0);
            p.setHealth(p.getMaxHealth());
            p.getFoodData().eat(20, 20);
            p.clearFire();
            fight.inArena.add(p.getUUID());
            fight.frozenAt.put(p.getUUID(), new Vec3(x, CENTER.getY() + 1, z));
        }
        return true;
    }

    // ------------------------------------------------------------------ the timeline

    /** Called every server tick. */
    public static void tickActive() {
        if (active != null) active.tick();
    }

    private void tick() {
        if (finished) return;
        lastT = t;
        t = clock() + skipOffset;
        inArena.removeIf(id -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            return p == null || !p.isAlive() || p.level() != arena;
        });
        if (boss.isRemoved()) {
            end(false);
            return;
        }
        if (inArena.isEmpty()) {
            end(false);
            return;
        }
        keepBossInBounds();

        if (t < T_FIGHT) {
            intro();
        } else if (!begun) {
            begun = true;
            begin();
        } else if (t < T_TIRED) {
            if (attacks != null) attacks.tick(t);
        } else if (!tiredDone) {
            tiredDone = true;
            tired();
        } else if (t < T_END) {
            fading();
        } else {
            end(true);
        }
    }

    /** True once when song time passes {@code mark} (song time can jump by more than one tick). */
    private boolean crossed(int mark) {
        return lastT < mark && t >= mark;
    }

    private void intro() {
        for (ServerPlayer p : alive()) freeze(p);
        boss.getNavigation().stop();
        ServerPlayer near = nearest();
        if (near != null) boss.getLookControl().setLookAt(near, 360, 360);
        if (crossed(T_YOU)) say("you.");
        if (crossed(0)) {
            for (ServerPlayer p : alive()) startMusic(p);
        }
        for (Map.Entry<Integer, String> e : INTRO.entrySet()) {
            if (crossed(e.getKey())) say(e.getValue());
        }
        if (t > 36 * 20 && t % 4 == 0) { // he shakes as he builds up to it
            arena.sendParticles(ParticleTypes.ANGRY_VILLAGER, boss.getX(), boss.getEyeY() + 0.5, boss.getZ(), 1, 0.4, 0.2, 0.4, 0);
            arena.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY(0.5), boss.getZ(), 3, 0.4, 0.8, 0.4, 0.01);
        }
    }

    /** What he says over the first 42 seconds of the song (key = song tick). */
    private static final Map<Integer, String> INTRO = Map.ofEntries(
            Map.entry(40, "five times."),
            Map.entry(120, "five times you held it out where i could see it. and put it back."),
            Map.entry(220, "you didn't need it. you just wanted me to watch you keep it."),
            Map.entry(320, "and then you bring me this."),
            Map.entry(420, "i was patient with you. every single day."),
            Map.entry(520, "i asked. i always asked. you always laughed."),
            Map.entry(620, "nobody is coming for you here."),
            Map.entry(720, "i don't want your penny anymore."),
            Map.entry(800, "i want you."));

    private void begin() {
        bar = new ServerBossEvent(Component.literal("Sebastian Hili").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal("   ❤ 50,000").withStyle(ChatFormatting.DARK_RED)),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
        bar.setProgress(1.0F);
        bar.setDarkenScreen(true);
        bar.setCreateWorldFog(true);
        for (ServerPlayer p : alive()) {
            bar.addPlayer(p);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("SURVIVE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("you can't hurt him. last until the song ends.").withStyle(ChatFormatting.RED)));
        }
        frozenAt.clear();
        boss.playSound(ModSounds.RAGE_SCREAM.get(), 4.0F, 0.7F);
        attacks = new BossAttacks(this);
    }

    private void tired() {
        if (attacks != null) attacks.stopAll();
        boss.setInvisible(false);
        boss.getNavigation().stop();
        boss.setPose(Pose.CROUCHING);
        boss.setShiftKeyDown(true);
        for (ServerPlayer p : alive()) p.removeEffect(MobEffects.DARKNESS);
        say("...hah...");
    }

    private void fading() {
        int since = t - T_TIRED;
        int before = lastT - T_TIRED;
        float fade = 1.0F - Math.min(1.0F, since / (float) (T_END - 30 - T_TIRED));
        boss.setFade(fade);
        boss.getNavigation().stop();
        ServerPlayer near = nearest();
        if (near != null) boss.getLookControl().setLookAt(near, 10, 10);
        if (before < 80 && since >= 80) say("why won't you... just...");
        if (before < 160 && since >= 160) say("this doesn't make us even.");
        if (before < 240 && since >= 240) say("keep it. keep your penny.");
        if (t % 3 == 0) {
            arena.sendParticles(ParticleTypes.ASH, boss.getX(), boss.getY(0.6), boss.getZ(), 6, 0.4, 0.8, 0.4, 0.01);
            arena.sendParticles(ParticleTypes.SMOKE, boss.getX(), boss.getY(0.9), boss.getZ(), 2, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // ------------------------------------------------------------------ ending

    private void end(boolean won) {
        if (finished) return;
        finished = true;
        active = null;
        if (attacks != null) attacks.stopAll();
        if (bar != null) bar.removeAllPlayers();
        for (Map.Entry<UUID, ReturnPoint> e : participants.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) continue;
            stopMusic(p);
            p.removeEffect(MobEffects.DARKNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (p.level() == arena && p.isAlive()) {
                sendHome(p, e.getValue());
            }
            persisted(p).remove(RETURN_TAG);
            if (won) {
                SebAdvancements.award(p, "the_world_looks_red");
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("YOU SURVIVED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Sebastian Hili is gone. For good.").withStyle(ChatFormatting.GRAY)));
            }
        }
        boss.discard();
        if (won) {
            BossData data = BossData.get(server);
            data.gone = true;
            data.setDirty();
            for (ServerLevel level : server.getAllLevels()) {
                for (SebastianEntity s : SebastianEntity.activeIn(level)) s.discard();
            }
            SebChat.joinMessage(server, false);
        } else {
            server.getPlayerList().broadcastSystemMessage(Component.literal("<Sebastian Hili> ").append(
                    Component.literal("next time.").withStyle(ChatFormatting.DARK_RED)), false);
            // he goes back to normal: he turns up again next to one of you
            for (UUID id : participants.keySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null && p.isAlive() && p.level().dimension() == Level.OVERWORLD) {
                    SebSpawner.spawnNear(p.serverLevel(), p, false);
                    break;
                }
            }
        }
    }

    /** /seb boss stop */
    public void cancel() {
        end(false);
    }

    /** /seb boss skip: jump the timeline (the music can't seek, so it is restarted only when jumping to 0). */
    public void skipTo(int songTick) {
        if (songTick >= T_FIGHT && !begun) {
            begun = true;
            begin();
        }
        skipOffset = songTick - clock();
        t = songTick;
        lastT = songTick;
    }

    /** Song time in ticks from the real clock: T_ARRIVE at the moment the fight started. */
    private int clock() {
        return T_ARRIVE + (int) ((System.nanoTime() - startNanos) / 50_000_000L);
    }

    // ------------------------------------------------------------------ deaths, logins, travel

    /** Dying in The Red: you go home and keep your things (they are handed back on respawn). */
    public static void onArenaDeath(ServerPlayer p) {
        if (p.level().dimension() != THE_RED) return;
        ListTag inv = p.getInventory().save(new ListTag());
        SNAPSHOTS.put(p.getUUID(), new Snapshot(inv, p.totalExperience));
        p.getInventory().clearContent();
        p.skipDropExperience();
        stopMusic(p);
        persisted(p).remove(RETURN_TAG);
        if (active != null) {
            active.fallen++;
            String[] n = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
            active.say(active.fallen <= n.length ? n[active.fallen - 1] + "." : active.fallen + ".");
        }
    }

    public static void onRespawn(ServerPlayer p) {
        Snapshot s = SNAPSHOTS.remove(p.getUUID());
        if (s != null) {
            p.getInventory().load(s.inventory());
            p.setExperiencePoints(0);
            p.setExperienceLevels(0);
            p.giveExperiencePoints(s.xp());
        }
    }

    /** Logging back in inside The Red with no fight going on: you are sent back where you came from. */
    public static void onLogin(ServerPlayer p) {
        if (p.level().dimension() != THE_RED) return;
        if (active != null && active.inArena.contains(p.getUUID())) return;
        CompoundTag rt = persisted(p).getCompound(RETURN_TAG);
        persisted(p).remove(RETURN_TAG);
        if (!rt.isEmpty()) {
            sendHome(p, ReturnPoint.load(rt));
        } else {
            ServerLevel ow = p.server.overworld();
            BlockPos spawn = ow.getSharedSpawnPos();
            teleport(p, ow, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
        }
    }

    public static void onLogout(ServerPlayer p) {
        if (active != null) active.inArena.remove(p.getUUID());
    }

    /** No escaping through portals mid-fight. */
    public static boolean shouldBlockTravel(Entity e) {
        return !allowTeleport && active != null && e instanceof ServerPlayer p && active.inArena.contains(p.getUUID());
    }

    public static void onServerStopping() {
        active = null;
        SNAPSHOTS.clear();
    }

    // ------------------------------------------------------------------ helpers

    void say(String line) {
        Component msg = Component.translatable("chat.type.text", SebChat.name(), Component.literal(line).withStyle(ChatFormatting.RED));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    @Nullable
    ServerPlayer nearest() {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer p : alive()) {
            double d = p.distanceToSqr(boss);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private void freeze(ServerPlayer p) {
        Vec3 at = frozenAt.get(p.getUUID());
        if (at == null) return;
        if (p.position().distanceToSqr(at) > 0.0025) {
            p.connection.teleport(at.x, at.y, at.z, 0, 0, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
        }
        p.setDeltaMovement(Vec3.ZERO);
        if (t % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, false, false, false));
        }
    }

    private void keepBossInBounds() {
        double dx = boss.getX() - (CENTER.getX() + 0.5), dz = boss.getZ() - (CENTER.getZ() + 0.5);
        if (boss.getY() < CENTER.getY() - 3 || dx * dx + dz * dz > 30 * 30) {
            arena.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.5), boss.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
            boss.teleportTo(CENTER.getX() + 0.5, CENTER.getY() + 1, CENTER.getZ() + 0.5);
            boss.setDeltaMovement(Vec3.ZERO);
        }
    }

    static void startMusic(ServerPlayer p) {
        p.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.BOSS_MUSIC.get()),
                SoundSource.RECORDS, p, 1.0F, 1.0F, 0L));
    }

    static void stopMusic(ServerPlayer p) {
        p.connection.send(new ClientboundStopSoundPacket(ModSounds.BOSS_MUSIC.getId(), SoundSource.RECORDS));
    }

    private static void sendHome(ServerPlayer p, ReturnPoint rp) {
        ServerLevel level = p.server.getLevel(rp.dim());
        if (level == null || level.dimension() == THE_RED) {
            level = p.server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            teleport(p, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, rp.yaw(), rp.pitch());
            return;
        }
        teleport(p, level, rp.x(), rp.y(), rp.z(), rp.yaw(), rp.pitch());
    }

    private static void teleport(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        allowTeleport = true;
        try {
            p.teleportTo(level, x, y, z, yaw, pitch);
        } finally {
            allowTeleport = false;
        }
    }

    private static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
