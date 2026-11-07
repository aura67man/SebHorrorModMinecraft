package com.sebastianhili.horror.entity;

import com.sebastianhili.horror.boss.BossFight;
import com.sebastianhili.horror.boss.BurnedPennyMap;
import com.sebastianhili.horror.config.SebConfig;
import com.sebastianhili.horror.logic.BurnRules;
import com.sebastianhili.horror.logic.ChatAnswer;
import com.sebastianhili.horror.logic.LieDetector;
import com.sebastianhili.horror.logic.PennyLogic;
import com.sebastianhili.horror.logic.WatchRules;
import com.sebastianhili.horror.registry.ModDamageTypes;
import com.sebastianhili.horror.registry.ModItems;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.IdentityHashMap;

/**
 * Sebastian Hili. All behaviour runs on the server inside {@link #customServerAiStep()}, which
 * dispatches to one tick method per {@link SebState}. Clients only render him and some particles.
 */
public class SebastianEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(SebastianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOSS =
            SynchedEntityData.defineId(SebastianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FADE =
            SynchedEntityData.defineId(SebastianEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Every Sebastian currently loaded on the server (used for chat answers and burning pennies).
     * Identity-based on purpose: Entity#equals compares entity ids, and in single-player the client
     * copy has the same id as the server copy, so a normal set would let the client's unload remove
     * the server entity.
     */
    private static final Set<SebastianEntity> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());

    // ---- persistent
    private int pennies;
    private boolean natural;
    private long nextSearchTime = -1;

    // ---- shared state machine bookkeeping (server only)
    private SebState state = SebState.WATCHING;
    private int stateTicks;
    @Nullable
    private UUID targetId;
    private boolean autonomous = true;

    // watching
    private int relocateCooldown = 200;
    private int rummageCooldown = 1200;
    private int stareTicks;
    private int whisperCooldown = 600;
    private int heartbeatCooldown;
    private int lonelyTicks;
    private final Map<UUID, Long> begCooldowns = new HashMap<>();

    // begging
    private int begLineCooldown;

    // charging / rage
    private boolean rageCharge;
    private double bestDistance;
    private int stuckTicks;
    private int passTicks;
    private Vec3 passDir = Vec3.ZERO;
    private final Deque<UUID> rageQueue = new ArrayDeque<>();
    private final Set<UUID> rageKilled = new HashSet<>();
    private int ragePause;

    // rummaging
    @Nullable
    private BlockPos containerPos;
    private int rummageOpenTicks = -1;
    private final Map<BlockPos, Long> visitedContainers = new HashMap<>();

    // searching
    private boolean asked;
    private int askedAt;
    private boolean reminded;

    // lie lightning: kill shortly after the bolt so the dropped loot isn't destroyed by it
    @Nullable
    private UUID smiteVictim;
    private int smiteTicks;

    // retaliation
    private final Map<UUID, int[]> hitsByPlayer = new HashMap<>(); // {hits, tickOfFirstHit}
    private int hurtLineCooldown;
    private boolean duplicateChecked;

    public SebastianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.setPersistenceRequired();
        this.setCustomName(SebChat.name());
        this.setCustomNameVisible(false);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.xpReward = 50;
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, SebState.WATCHING.ordinal());
        this.entityData.define(DATA_BOSS, false);
        this.entityData.define(DATA_FADE, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // He opens your doors. And closes them behind him.
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    /** While staring, his whole body snaps to face you instead of turning naturally. */
    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                SebState s = getSebState();
                if (s == SebState.WATCHING || s == SebState.BEGGING || s == SebState.SEARCHING || s == SebState.RAGE) {
                    SebastianEntity.this.yBodyRot = SebastianEntity.this.yHeadRot;
                } else {
                    super.clientTick();
                }
            }
        };
    }

    @Override
    public int getMaxHeadYRot() {
        return 90;
    }

    @Override
    public int getHeadRotSpeed() {
        return 180;
    }

    // ------------------------------------------------------------------ public API

    public SebState getSebState() {
        return SebState.byId(this.entityData.get(DATA_STATE));
    }

    /** The boss-fight Sebastian in The Red. Driven by BossFight instead of the normal state machine. */
    public boolean isBoss() {
        return this.entityData.get(DATA_BOSS);
    }

    public void setBoss(boolean boss) {
        this.entityData.set(DATA_BOSS, boss);
        this.setInvulnerable(boss);
        this.autonomous = !boss;
        this.natural = false;
        if (boss) {
            var health = getAttribute(Attributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(1024.0D); // the most Minecraft allows
            setHealth(getMaxHealth());
        }
    }

    /** 1 = solid, 0 = gone. He fades out when he tires at the end of the song. */
    public float getFade() {
        return this.entityData.get(DATA_FADE);
    }

    public void setFade(float fade) {
        this.entityData.set(DATA_FADE, Mth.clamp(fade, 0.0F, 1.0F));
    }

    public int getPennies() {
        return pennies;
    }

    public boolean isNatural() {
        return natural;
    }

    public void setNatural(boolean natural) {
        this.natural = natural;
    }

    /** Tests and /seb freeze: when false he only reacts (begging, burns, answers) and never wanders or schedules things. */
    public void setAutonomous(boolean autonomous) {
        this.autonomous = autonomous;
    }

    public boolean isAutonomous() {
        return autonomous;
    }

    @Nullable
    public ServerPlayer getTargetPlayer() {
        if (targetId == null || !(level() instanceof ServerLevel sl)) {
            return null;
        }
        Player p = sl.getPlayerByUUID(targetId);
        return p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator() ? sp : null;
    }

    public long ticksUntilSearch() {
        return nextSearchTime < 0 ? -1 : nextSearchTime - level().getGameTime();
    }

    public static List<SebastianEntity> activeIn(Level level) {
        List<SebastianEntity> out = new ArrayList<>();
        synchronized (ACTIVE) {
            for (SebastianEntity s : ACTIVE) {
                if (s.level() == level && s.isAlive() && !s.isBoss()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    public static List<SebastianEntity> allActive() {
        synchronized (ACTIVE) {
            List<SebastianEntity> out = new ArrayList<>();
            for (SebastianEntity s : ACTIVE) {
                if (s.isAlive() && !s.isBoss()) {
                    out.add(s);
                }
            }
            return out;
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            synchronized (ACTIVE) {
                ACTIVE.add(this);
            }
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!level().isClientSide) {
            synchronized (ACTIVE) {
                ACTIVE.remove(this);
            }
        }
    }

    /** Called when the server stops, so nothing from an old world lingers in single-player. */
    public static void clearActive() {
        synchronized (ACTIVE) {
            ACTIVE.clear();
        }
    }

    /** Players he may kill. Creative/spectator players are spared unless the config says otherwise. */
    public static boolean isTouchable(Player p) {
        if (!p.isAlive() || p.isSpectator()) {
            return false;
        }
        return !(p.isCreative() && SebConfig.SPARE_CREATIVE.get());
    }

    // ------------------------------------------------------------------ state machine core

    private void setState(SebState newState) {
        if (newState != state) {
            if (state == SebState.RUMMAGING) {
                closeContainer();
            }
            if (state == SebState.RAGE) {
                this.removeEffect(MobEffects.GLOWING);
            }
        }
        this.state = newState;
        this.stateTicks = 0;
        this.entityData.set(DATA_STATE, newState.ordinal());
        this.getNavigation().stop();
        this.setSprinting(false);
        if (newState != SebState.WATCHING) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.setPose(Pose.STANDING);
            this.setShiftKeyDown(false);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isBoss()) {
            // BossFight moves and attacks with him. A leftover boss (e.g. after a restart) just leaves.
            BossFight fight = BossFight.get();
            if (fight == null || fight.boss() != this) discard();
            if (hurtLineCooldown > 0) hurtLineCooldown--;
            return;
        }
        ServerLevel level = (ServerLevel) level();
        stateTicks++;
        if (hurtLineCooldown > 0) hurtLineCooldown--;

        if (!duplicateChecked) {
            duplicateChecked = true;
            if (SebSpawner.isStaleDuplicate(this)) {
                this.discard();
                return;
            }
        }
        if (natural && tickCount % 100 == 0) {
            SebSpawner.markSeen(this);
        }
        if (tickCount % 20 == 0) {
            applyConfigAttributes();
            float regen = SebConfig.REGEN_PER_SECOND.get().floatValue();
            if (regen > 0 && getHealth() < getMaxHealth()) {
                heal(regen);
            }
        }
        if (nextSearchTime < 0) {
            scheduleNextSearch();
        }
        tickSmite(level);

        switch (state) {
            case WATCHING -> tickWatching(level);
            case BEGGING -> tickBegging(level);
            case CHARGING -> tickCharging(level);
            case RAGE -> tickRage(level);
            case RUMMAGING -> tickRummaging(level);
            case SEARCHING -> tickSearching(level);
        }
    }

    private void applyConfigAttributes() {
        var health = getAttribute(Attributes.MAX_HEALTH);
        double wantHealth = SebConfig.MAX_HEALTH.get();
        if (health != null && health.getBaseValue() != wantHealth) {
            health.setBaseValue(wantHealth);
            if (getHealth() > wantHealth) setHealth((float) wantHealth);
        }
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getBaseValue() != SebConfig.ARMOR.get()) armor.setBaseValue(SebConfig.ARMOR.get());
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getBaseValue() != SebConfig.WALK_SPEED.get()) speed.setBaseValue(SebConfig.WALK_SPEED.get());
    }

    // ------------------------------------------------------------------ WATCHING

    private void tickWatching(ServerLevel level) {
        ServerPlayer target = nearestPlayer(level, 160);
        this.targetId = target == null ? null : target.getUUID();

        if (target == null) {
            // Nobody close. Drift toward the players if there are any, otherwise poke around containers.
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (autonomous && ++lonelyTicks > 400 && SebConfig.RELOCATION.get()) {
                ServerPlayer anyone = randomTouchablePlayer(level);
                if (anyone != null && blinkNear(anyone, SebConfig.WATCH_MIN.get() + 4, SebConfig.WATCH_MAX.get(), true)) {
                    lonelyTicks = 0;
                }
            }
            if (autonomous && --rummageCooldown <= 0) {
                tryStartRummage(level);
            }
            return;
        }
        lonelyTicks = 0;

        // Head locked on, unblinking.
        lockLookOn(target);

        // Begging takes priority over everything else.
        if (SebConfig.BEGGING.get()) {
            ServerPlayer holder = findPennyHolder(level);
            if (holder != null) {
                startBegging(holder);
                return;
            }
        }
        if (!autonomous) {
            getNavigation().stop();
            return;
        }

        // Inventory search on its own timer.
        if (SebConfig.INVENTORY_SEARCH.get() && level.getGameTime() >= nextSearchTime) {
            ServerPlayer victim = isTouchable(target) ? target : randomTouchablePlayer(level);
            if (victim != null) {
                startSearching(victim);
                return;
            }
            scheduleNextSearch();
        }
        if (SebConfig.RUMMAGING.get() && --rummageCooldown <= 0) {
            if (tryStartRummage(level)) {
                return;
            }
        }

        double dist = distanceTo(target);
        boolean seen = isInView(target, this);

        // Distance keeping. He walks backwards while staring if you approach.
        if (tickCount % 10 == 0) {
            double min = SebConfig.WATCH_MIN.get();
            double max = Math.max(min + 2, SebConfig.WATCH_MAX.get());
            switch (WatchRules.decide(dist, min, max, max * 2.2)) {
                case RETREAT -> {
                    Vec3 away = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(6);
                    Vec3 dest = position().add(away);
                    getNavigation().moveTo(dest.x, dest.y, dest.z, 1.15D);
                }
                case APPROACH -> getNavigation().moveTo(target, 1.0D);
                case CATCH_UP -> {
                    if (!seen && SebConfig.RELOCATION.get()) {
                        blinkNear(target, min, max, true);
                    } else {
                        getNavigation().moveTo(target, 1.1D);
                    }
                }
                case HOLD -> getNavigation().stop();
            }
        }

        // When you look away, he is somewhere else.
        if (SebConfig.RELOCATION.get() && --relocateCooldown <= 0) {
            if (!seen) {
                blinkNear(target, SebConfig.WATCH_MIN.get(), SebConfig.WATCH_MAX.get(), true);
                relocateCooldown = randomTicks(SebConfig.RELOCATE_MIN.get(), SebConfig.RELOCATE_MAX.get());
            } else {
                relocateCooldown = 20;
            }
        }

        // Mimicry: he holds what you hold and sneaks when you sneak.
        if (SebConfig.MIMICRY.get() && tickCount % 10 == 0) {
            ItemStack held = target.getMainHandItem();
            ItemStack copy = held.isEmpty() || PennyLogic.isPenny(held) ? ItemStack.EMPTY : held.copyWithCount(1);
            if (!ItemStack.isSameItemSameTags(copy, getMainHandItem())) {
                setItemSlot(EquipmentSlot.MAINHAND, copy);
            }
            boolean sneak = target.isShiftKeyDown();
            setShiftKeyDown(sneak);
            setPose(sneak ? Pose.CROUCHING : Pose.STANDING);
        }

        // Staring contest: heartbeat while you look at him, a whisper if you keep looking.
        if (seen && dist < 20) {
            stareTicks++;
            if (--heartbeatCooldown <= 0) {
                heartbeatCooldown = dist < 14 ? 18 : 28;
                target.playNotifySound(ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, (float) Mth.clamp(1.2 - dist / 25, 0.25, 1.0), 1.0F);
            }
            if (SebConfig.WHISPERS.get() && stareTicks > 100 && --whisperCooldown <= 0) {
                SebChat.whisper(target, SebLines.pick(random, SebLines.WHISPERS));
                whisperCooldown = 900 + random.nextInt(900);
            }
        } else {
            stareTicks = 0;
            if (SebConfig.WHISPERS.get() && random.nextInt(2400) == 0) {
                // A whisper from directly behind you.
                Vec3 behind = target.position().subtract(target.getViewVector(1).multiply(1, 0, 1).normalize().scale(3));
                playSoundTo(target, ModSounds.WHISPER.get(), behind, 0.7F, 0.8F + random.nextFloat() * 0.3F);
            }
        }
    }

    private void lockLookOn(LivingEntity target) {
        this.getLookControl().setLookAt(target, 360.0F, 360.0F);
    }

    @Nullable
    private ServerPlayer nearestPlayer(ServerLevel level, double range) {
        ServerPlayer best = null;
        double bestD = range * range;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    @Nullable
    private ServerPlayer randomTouchablePlayer(ServerLevel level) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (isTouchable(p)) list.add(p);
        }
        return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
    }

    @Nullable
    private ServerPlayer findPennyHolder(ServerLevel level) {
        double range = SebConfig.BEG_RANGE.get();
        long now = level.getGameTime();
        ServerPlayer best = null;
        double bestD = range * range;
        for (ServerPlayer p : level.players()) {
            if (!isTouchable(p) || !PennyLogic.isHoldingPenny(p)) continue;
            Long until = begCooldowns.get(p.getUUID());
            if (until != null && until > now) continue;
            double d = p.distanceToSqr(this);
            if (d <= bestD && hasLineOfSight(p)) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** True when the player is looking roughly toward the entity and could actually see it. */
    public static boolean isInView(Player p, LivingEntity e) {
        Vec3 look = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(e.getX() - p.getX(), e.getEyeY() - p.getEyeY(), e.getZ() - p.getZ());
        if (to.lengthSqr() < 1.0E-4) return true;
        return look.dot(to.normalize()) > 0.35 && p.hasLineOfSight(e);
    }

    // ------------------------------------------------------------------ BEGGING

    public void startBegging(ServerPlayer player) {
        setState(SebState.BEGGING);
        this.targetId = player.getUUID();
        this.begLineCooldown = 50;
        SebChat.say(this, SebLines.pick(random, SebLines.BEG_START));
        SebChat.hint(player, "(Right-click Sebastian Hili while holding the penny to give it to him. Quickly.)");
    }

    private void tickBegging(ServerLevel level) {
        ServerPlayer p = getTargetPlayer();
        if (p == null) {
            setState(SebState.WATCHING);
            return;
        }
        lockLookOn(p);
        int timeout = SebConfig.secondsToTicks(SebConfig.BEG_TIMEOUT);
        double dist = distanceTo(p);

        if (dist > SebConfig.BEG_RUN_DISTANCE.get()) {
            refused(p, SebLines.CHARGE_RAN);
            return;
        }
        if (!PennyLogic.isHoldingPenny(p) && stateTicks > 10) {
            refused(p, SebLines.CHARGE_PUT_AWAY);
            return;
        }
        if (stateTicks >= timeout) {
            refused(p, SebLines.CHARGE_TIMEOUT);
            return;
        }

        if (dist > 2.2) {
            if (tickCount % 5 == 0) getNavigation().moveTo(p, 1.25D);
        } else {
            getNavigation().stop();
        }

        int left = timeout - stateTicks;
        if (left == 60) SebChat.say(this, "three.");
        else if (left == 40) SebChat.say(this, "two.");
        else if (left == 20) SebChat.say(this, "one.");
        else if (left > 70 && --begLineCooldown <= 0) {
            SebChat.say(this, SebLines.pick(random, SebLines.BEG_PLEAD));
            begLineCooldown = 50 + random.nextInt(20);
        }
        if (stateTicks % 10 == 0) {
            SebChat.actionBar(p, Component.literal("Give Sebastian Hili the penny: " + Mth.ceil(left / 20.0F) + "s")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** The player refused to hand the penny over. Usually he charges; the Nth refusal in a row earns Exhibit A instead. */
    private void refused(ServerPlayer p, String[] lines) {
        if (BurnedPennyMap.onRefusal(this, p)) {
            begCooldowns.put(p.getUUID(), level().getGameTime() + 20L * 60);
            setState(SebState.WATCHING);
            return;
        }
        startCharge(p, lines);
    }

    /** Right-clicking him with a penny hands it over. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (BurnedPennyMap.isMap(stack) && !isBoss()) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (BossFight.isActive()) {
                SebChat.say(this, "not now.");
            } else if (!SebConfig.BOSS_FIGHT.get()) {
                SebChat.say(this, "...put that away. i don't want to look at it.");
            } else if (player instanceof ServerPlayer sp) {
                ItemStack shown = stack.copy();
                stack.shrink(1); // he takes it from you
                if (!BossFight.start(this, sp)) {
                    player.getInventory().add(shown);
                }
            }
            return InteractionResult.CONSUME;
        }
        if (!PennyLogic.isPenny(stack)) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state == SebState.CHARGING || state == SebState.RAGE) {
            if (hurtLineCooldown <= 0) {
                SebChat.say(this, SebLines.pick(random, SebLines.TOO_LATE));
                hurtLineCooldown = 40;
            }
            return InteractionResult.CONSUME;
        }
        acceptPenny(player, stack);
        return InteractionResult.CONSUME;
    }

    public void acceptPenny(Player player, ItemStack stack) {
        stack.shrink(1);
        pennies++;
        BurnedPennyMap.resetRefusals(player);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.PENNY_CLINK.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getEyeY() + 0.4, getZ(), 6, 0.3, 0.2, 0.3, 0);
        SebChat.say(this, SebLines.pick(random, SebLines.THANKS));
        SebAdvancements.award(player, "spare_change");
        begCooldowns.put(player.getUUID(), level().getGameTime() + SebConfig.secondsToTicks(SebConfig.BEG_COOLDOWN));
        swing(InteractionHand.MAIN_HAND);
        if (state == SebState.BEGGING || state == SebState.WATCHING || state == SebState.RUMMAGING) {
            setState(SebState.WATCHING);
        }
    }

    // ------------------------------------------------------------------ CHARGING (and the shared chase)

    public void startCharge(ServerPlayer player, @Nullable String[] lines) {
        if (!SebConfig.CHARGING.get() && lines != null) {
            // charging disabled: he just gives up
            SebChat.say(this, "...fine.");
            setState(SebState.WATCHING);
            return;
        }
        setState(SebState.CHARGING);
        this.targetId = player.getUUID();
        this.rageCharge = false;
        resetChase();
        if (lines != null) {
            SebChat.say(this, SebLines.pick(random, lines));
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.STING.get(), SoundSource.HOSTILE, 1.5F, 1.4F);
    }

    private void resetChase() {
        bestDistance = Double.MAX_VALUE;
        stuckTicks = 0;
        passTicks = 0;
    }

    private void tickCharging(ServerLevel level) {
        if (passTicks > 0) {
            continueThrough();
            if (passTicks == 0) {
                setState(SebState.WATCHING);
            }
            return;
        }
        ServerPlayer p = getTargetPlayer();
        if (p == null) {
            setState(SebState.WATCHING);
            return;
        }
        if (!isTouchable(p)) {
            SebChat.say(this, "...you're not even real.");
            setState(SebState.WATCHING);
            return;
        }
        if (chase(p)) {
            impale(p, ModDamageTypes.PENNY_CHARGE);
            SebChat.say(this, SebLines.pick(random, SebLines.CHARGE_KILL));
        }
    }

    /** Sprints straight at the player. Returns true once he is close enough to run them through. */
    private boolean chase(ServerPlayer p) {
        setSprinting(true);
        lockLookOn(p);
        double dist = distanceTo(p);
        if (dist < 1.4 || getBoundingBox().inflate(0.35).intersects(p.getBoundingBox())) {
            return true;
        }
        double speed = SebConfig.CHARGE_SPEED.get();
        boolean direct = dist < 28 && Math.abs(p.getY() - getY()) < 1.5 && hasLineOfSight(p);
        if (direct) {
            getNavigation().stop();
            // Overshoot the target so he runs *through* them rather than stopping in front.
            Vec3 dir = p.position().subtract(position()).normalize();
            Vec3 aim = p.position().add(dir.scale(2));
            getMoveControl().setWantedPosition(aim.x, p.getY(), aim.z, speed);
        } else if (tickCount % 5 == 0) {
            getNavigation().moveTo(p, speed);
        }
        // Stuck or outrun: he blinks right behind you.
        if (dist < bestDistance - 0.5) {
            bestDistance = dist;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        int giveUp = SebConfig.secondsToTicks(SebConfig.CHARGE_GIVE_UP);
        if (stuckTicks > 50 || dist > 48 || stateTicks > giveUp) {
            Vec3 behind = p.position().subtract(p.getViewVector(1).multiply(1, 0, 1).normalize().scale(3));
            BlockPos spot = findStandable(BlockPos.containing(behind), 4);
            if (spot != null) {
                blinkTo(Vec3.atBottomCenterOf(spot));
            } else {
                blinkTo(p.position());
            }
            stuckTicks = 0;
            bestDistance = distanceTo(p);
            if (stateTicks > giveUp) {
                stateTicks = 0;
            }
        }
        return false;
    }

    private void impale(ServerPlayer p, ResourceKey<DamageType> type) {
        ServerLevel level = (ServerLevel) level();
        if (PennyLogic.takeOnePenny(p)) {
            pennies++;
        }
        passDir = p.position().subtract(position()).multiply(1, 0, 1);
        passDir = passDir.lengthSqr() < 1.0E-4 ? getViewVector(1).multiply(1, 0, 1).normalize() : passDir.normalize();
        swing(InteractionHand.MAIN_HAND);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.5F, 0.5F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY(0.6), p.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY(0.5), p.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        if (isTouchable(p)) {
            SebAdvancements.award(p, "over_a_penny");
            p.hurt(ModDamageTypes.source(level, type, this), Float.MAX_VALUE);
        }
        passTicks = 12;
        setDeltaMovement(passDir.scale(0.8));
    }

    private void continueThrough() {
        passTicks--;
        Vec3 aim = position().add(passDir.scale(3));
        getMoveControl().setWantedPosition(aim.x, getY(), aim.z, SebConfig.CHARGE_SPEED.get());
        if (passTicks == 0) {
            setSprinting(false);
            getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ RAGE

    /** Called when a penny item is destroyed. Any Sebastian that saw it burn goes into a rage. */
    public static void onPennyDestroyed(ItemEntity item, boolean fire) {
        for (SebastianEntity seb : activeIn(item.level())) {
            if (seb.state == SebState.RAGE) continue;
            boolean los = seb.canSee(item);
            double range = SebConfig.RAGE_RANGE.get();
            if (BurnRules.shouldEnrage(SebConfig.RAGE.get(), fire, seb.distanceToSqr(item), range, los)) {
                Player culprit = item.getOwner() instanceof Player p ? p : item.level().getNearestPlayer(item, 16);
                if (culprit != null) {
                    SebAdvancements.award(culprit, "money_to_burn");
                }
                seb.startRage();
            }
        }
    }

    /**
     * Line of sight to an item, forgiving enough to count a penny that has sunk into a lava pool
     * or fire: he can see either the item itself or the spot just above it.
     */
    public boolean canSee(net.minecraft.world.entity.Entity e) {
        if (e.level() != level()) return false;
        Vec3 eye = getEyePosition();
        for (Vec3 target : new Vec3[]{e.position().add(0, e.getBbHeight() * 0.5, 0), new Vec3(e.getX(), Math.floor(e.getY()) + 1.1, e.getZ()),
                e.position().add(0, 1.0, 0)}) {
            if (target.distanceToSqr(eye) > 128 * 128) continue;
            var hit = level().clip(new net.minecraft.world.level.ClipContext(eye, target, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;
        }
        return false;
    }

    public void startRage() {
        ServerLevel level = (ServerLevel) level();
        setState(SebState.RAGE);
        rageQueue.clear();
        rageKilled.clear();
        targetId = null;
        ragePause = SebConfig.secondsToTicks(SebConfig.RAGE_WINDUP);
        resetChase();

        List<ServerPlayer> victims = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (isTouchable(p)) victims.add(p);
        }
        victims.sort(Comparator.comparingDouble(this::distanceToSqr));
        victims.forEach(v -> rageQueue.add(v.getUUID()));

        SebChat.say(this, SebLines.pick(random, SebLines.RAGE_START));
        addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 5, 0, false, false));
        level.playSound(null, getX(), getY(), getZ(), ModSounds.RAGE_SCREAM.get(), SoundSource.HOSTILE, 4.0F, 0.9F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 12, 0.6, 0.4, 0.6, 0);
        level.sendParticles(ParticleTypes.LAVA, getX(), getY(1.0), getZ(), 20, 0.4, 0.4, 0.4, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 30, 0.5, 0.8, 0.5, 0.02);
        for (ServerPlayer p : level.players()) {
            p.playNotifySound(ModSounds.RAGE_SCREAM.get(), SoundSource.HOSTILE, 0.7F, 0.8F);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("HE SAW THAT").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("someone burned a penny").withStyle(ChatFormatting.RED)));
            if (isTouchable(p)) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            }
        }
        if (SebConfig.RAGE_THUNDER.get()) {
            level.setWeatherParameters(0, 20 * 90, true, true);
        }
    }

    private void tickRage(ServerLevel level) {
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.4, getZ(), 1, 0.3, 0.1, 0.3, 0);
        }
        if (passTicks > 0) {
            continueThrough();
            return;
        }
        if (ragePause > 0) {
            ragePause--;
            getNavigation().stop();
            ServerPlayer near = nearestPlayer(level, 64);
            if (near != null) lockLookOn(near);
            return;
        }
        ServerPlayer victim = getTargetPlayer();
        if (victim == null || rageKilled.contains(victim.getUUID()) || !isTouchable(victim)) {
            victim = nextRageVictim(level);
            if (victim == null) {
                SebChat.say(this, SebLines.pick(random, SebLines.RAGE_END));
                setState(SebState.WATCHING);
                return;
            }
            targetId = victim.getUUID();
            resetChase();
            stateTicks = 0;
            if (distanceTo(victim) > 40) {
                blinkNear(victim, 9, 14, false);
            }
        }
        if (chase(victim)) {
            rageKilled.add(victim.getUUID());
            impale(victim, ModDamageTypes.PENNY_RAGE);
            SebChat.say(this, SebLines.RAGE_KILL[(rageKilled.size() - 1) % SebLines.RAGE_KILL.length]);
            targetId = null;
            ragePause = 20;
        }
    }

    @Nullable
    private ServerPlayer nextRageVictim(ServerLevel level) {
        while (!rageQueue.isEmpty()) {
            UUID id = rageQueue.poll();
            Player p = level.getPlayerByUUID(id);
            if (p instanceof ServerPlayer sp && isTouchable(sp) && !rageKilled.contains(id)) {
                return sp;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ RUMMAGING

    public boolean tryStartRummage(ServerLevel level) {
        rummageCooldown = randomTicks(SebConfig.RUMMAGE_MIN.get(), SebConfig.RUMMAGE_MAX.get());
        if (!SebConfig.RUMMAGING.get()) return false;
        BlockPos found = findContainer(level, SebConfig.RUMMAGE_RADIUS.get());
        if (found == null) return false;
        startRummage(found);
        return true;
    }

    public void startRummage(BlockPos pos) {
        setState(SebState.RUMMAGING);
        this.containerPos = pos.immutable();
        this.rummageOpenTicks = -1;
    }

    @Nullable
    public BlockPos findContainer(ServerLevel level, int radius) {
        long now = level.getGameTime();
        visitedContainers.values().removeIf(t -> t < now);
        BlockPos me = blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int minCx = (me.getX() - radius) >> 4, maxCx = (me.getX() + radius) >> 4;
        int minCz = (me.getZ() - radius) >> 4, maxCz = (me.getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = e.getKey();
                    double d = pos.distSqr(me);
                    if (d > (double) radius * radius || d >= bestD) continue;
                    if (visitedContainers.containsKey(pos)) continue;
                    if (!isRummageable(e.getValue())) continue;
                    best = pos;
                    bestD = d;
                }
            }
        }
        return best;
    }

    private static boolean isRummageable(BlockEntity be) {
        if (be instanceof RandomizableContainerBlockEntity r && r.saveWithoutMetadata().contains("LootTable")) {
            return false; // never generate dungeon loot early
        }
        return be instanceof Container || be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    private void tickRummaging(ServerLevel level) {
        if (containerPos == null) {
            setState(SebState.WATCHING);
            return;
        }
        if (SebConfig.BEGGING.get()) {
            ServerPlayer holder = findPennyHolder(level);
            if (holder != null) {
                startBegging(holder);
                return;
            }
        }
        BlockEntity be = level.getBlockEntity(containerPos);
        if (be == null || !isRummageable(be) && rummageOpenTicks < 0) {
            finishRummage(level);
            return;
        }
        Vec3 center = Vec3.atCenterOf(containerPos);
        getLookControl().setLookAt(center.x, center.y, center.z, 360F, 360F);

        if (rummageOpenTicks < 0) {
            double d = position().distanceTo(center);
            if (d <= 2.6) {
                getNavigation().stop();
                rummageOpenTicks = 0;
                openContainer(level, be);
            } else {
                if (tickCount % 10 == 0) {
                    BlockPos stand = standingSpotNear(containerPos);
                    if (stand != null) {
                        getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.05D);
                    } else {
                        getNavigation().moveTo(center.x, center.y, center.z, 1.05D);
                    }
                }
                if (stateTicks > 400) { // can't get there
                    visitedContainers.put(containerPos, level.getGameTime() + 6000);
                    setState(SebState.WATCHING);
                }
            }
            return;
        }

        rummageOpenTicks++;
        if (rummageOpenTicks % 8 == 0 && rummageOpenTicks < 36) {
            swing(InteractionHand.MAIN_HAND);
            level.playSound(null, containerPos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.5F, 0.7F + random.nextFloat() * 0.3F);
        }
        if (rummageOpenTicks == 25) {
            int taken = takePenniesFrom(be);
            if (taken > 0) {
                pennies += taken;
                level.playSound(null, containerPos, ModSounds.PENNY_CLINK.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
                if (nearestPlayer(level, 48) != null) {
                    SebChat.say(this, SebLines.pick(random, SebLines.RUMMAGE_FOUND));
                }
            }
        }
        if (rummageOpenTicks >= 45) {
            finishRummage(level);
        }
    }

    @Nullable
    private BlockPos standingSpotNear(BlockPos container) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos spot = findStandable(container.offset(dx, 0, dz), 2);
                if (spot == null) continue;
                double d = spot.distSqr(blockPosition());
                if (d < bestD) {
                    bestD = d;
                    best = spot;
                }
            }
        }
        return best;
    }

    /** Removes ONLY pennies. Public so the GameTests can check nothing else is touched. */
    public static int takePenniesFrom(BlockEntity be) {
        if (be instanceof Container c) {
            return PennyLogic.removePennies(c);
        }
        Optional<IItemHandler> handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        return handler.map(PennyLogic::removePennies).orElse(0);
    }

    private void finishRummage(ServerLevel level) {
        if (containerPos != null) {
            visitedContainers.put(containerPos, level.getGameTime() + 20 * 60 * 5);
        }
        setState(SebState.WATCHING); // closes the lid via setState
    }

    private void openContainer(ServerLevel level, BlockEntity be) {
        BlockState bs = be.getBlockState();
        if (be instanceof ChestBlockEntity) {
            level.blockEvent(containerPos, bs.getBlock(), 1, 1);
            level.playSound(null, containerPos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F);
        } else if (be instanceof BarrelBlockEntity && bs.hasProperty(BarrelBlock.OPEN)) {
            level.setBlock(containerPos, bs.setValue(BarrelBlock.OPEN, true), 3);
            level.playSound(null, containerPos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F);
        } else if (be instanceof ShulkerBoxBlockEntity) {
            level.blockEvent(containerPos, bs.getBlock(), 1, 1);
            level.playSound(null, containerPos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F);
        } else {
            level.playSound(null, containerPos, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
        level.gameEvent(this, GameEvent.CONTAINER_OPEN, containerPos);
    }

    private void closeContainer() {
        if (containerPos == null || rummageOpenTicks < 0 || !(level() instanceof ServerLevel level)) {
            containerPos = null;
            rummageOpenTicks = -1;
            return;
        }
        BlockEntity be = level.getBlockEntity(containerPos);
        if (be != null) {
            BlockState bs = be.getBlockState();
            if (be instanceof ChestBlockEntity) {
                level.blockEvent(containerPos, bs.getBlock(), 1, 0);
                level.playSound(null, containerPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F);
            } else if (be instanceof BarrelBlockEntity && bs.hasProperty(BarrelBlock.OPEN)) {
                level.setBlock(containerPos, bs.setValue(BarrelBlock.OPEN, false), 3);
                level.playSound(null, containerPos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F);
            } else if (be instanceof ShulkerBoxBlockEntity) {
                level.blockEvent(containerPos, bs.getBlock(), 1, 0);
                level.playSound(null, containerPos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F);
            }
            level.gameEvent(this, GameEvent.CONTAINER_CLOSE, containerPos);
        }
        containerPos = null;
        rummageOpenTicks = -1;
    }

    // ------------------------------------------------------------------ SEARCHING (inventory search over chat)

    public void startSearching(ServerPlayer player) {
        setState(SebState.SEARCHING);
        this.targetId = player.getUUID();
        this.asked = false;
        this.reminded = false;
        scheduleNextSearch();
    }

    public boolean isWaitingForAnswerFrom(Player p) {
        return state == SebState.SEARCHING && asked && p.getUUID().equals(targetId);
    }

    private void tickSearching(ServerLevel level) {
        ServerPlayer p = getTargetPlayer();
        if (p == null) {
            setState(SebState.WATCHING);
            return;
        }
        lockLookOn(p);
        double dist = distanceTo(p);
        if (!asked) {
            if (dist > 40 || stateTicks > 600) {
                blinkNear(p, 2.5, 4, !isInView(p, this));
            }
            if (dist > 3.0) {
                if (tickCount % 5 == 0) getNavigation().moveTo(p, 1.15D);
                return;
            }
            getNavigation().stop();
            ask(p);
            return;
        }
        if (dist > 4.5) {
            if (tickCount % 5 == 0) getNavigation().moveTo(p, 1.15D);
        } else {
            getNavigation().stop();
        }
        int elapsed = stateTicks - askedAt;
        int timeout = SebConfig.secondsToTicks(SebConfig.ANSWER_TIMEOUT);
        if (!reminded && elapsed >= timeout * 2 / 3) {
            reminded = true;
            SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_REMIND));
        }
        if (elapsed % 10 == 0) {
            SebChat.actionBar(p, Component.literal("Type yes or no in chat: " + Mth.ceil((timeout - elapsed) / 20.0F) + "s")
                    .withStyle(ChatFormatting.GOLD));
        }
        if (elapsed >= timeout) {
            resolveSearch(p, LieDetector.judgeTimeout(PennyLogic.hasPenny(p)));
        }
    }

    private void ask(ServerPlayer p) {
        asked = true;
        askedAt = stateTicks;
        SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_ASK));
        SebChat.hint(p, "(Answer by typing yes or no in chat. You have " + SebConfig.ANSWER_TIMEOUT.get() + " seconds. Don't lie.)");
    }

    /** Forces the question immediately (commands / tests), skipping the walk over. */
    public void askNow(ServerPlayer p) {
        startSearching(p);
        ask(p);
    }

    /** Routes a yes/no typed in chat. Returns true if some Sebastian was waiting for this player's answer. */
    public static boolean handleChatAnswer(ServerPlayer player, ChatAnswer answer) {
        if (answer == ChatAnswer.NOT_AN_ANSWER) return false;
        for (SebastianEntity seb : activeIn(player.level())) {
            if (seb.isWaitingForAnswerFrom(player)) {
                seb.receiveAnswer(player, answer);
                return true;
            }
        }
        return false;
    }

    public void receiveAnswer(ServerPlayer p, ChatAnswer answer) {
        resolveSearch(p, LieDetector.judge(answer, PennyLogic.hasPenny(p)));
    }

    private void resolveSearch(ServerPlayer p, LieDetector.Verdict verdict) {
        ServerLevel level = (ServerLevel) level();
        asked = false;
        scheduleNextSearch();
        switch (verdict) {
            case HONEST_YES -> {
                if (PennyLogic.takeOnePenny(p)) pennies++;
                level.playSound(null, getX(), getY(), getZ(), ModSounds.PENNY_CLINK.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_HONEST_YES));
                SebAdvancements.award(p, "spare_change");
                setState(SebState.WATCHING);
            }
            case FALSE_YES -> {
                SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_FALSE_YES));
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 15, 0));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 30, 1));
                setState(SebState.WATCHING);
            }
            case HONEST_NO -> {
                SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_HONEST_NO));
                SebAdvancements.award(p, "honest_answer");
                setState(SebState.WATCHING);
            }
            case LYING_NO -> {
                SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_LIE));
                smite(p);
                setState(SebState.WATCHING);
            }
            case SILENT_WITH_PENNY -> startCharge(p, SebLines.SEARCH_SILENT_HAS);
            case SILENT_EMPTY -> {
                SebChat.say(this, SebLines.pick(random, SebLines.SEARCH_SILENT_EMPTY));
                setState(SebState.WATCHING);
            }
        }
    }

    /** Real lightning on the liar. He also takes every penny they lied about. */
    private void smite(ServerPlayer p) {
        ServerLevel level = (ServerLevel) level();
        while (PennyLogic.takeOnePenny(p)) {
            pennies++;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(p.blockPosition()));
            level.addFreshEntity(bolt);
        }
        SebAdvancements.award(p, "pants_on_fire");
        smiteVictim = p.getUUID();
        smiteTicks = 6; // finish them once the bolt is done so their dropped items survive
    }

    private void tickSmite(ServerLevel level) {
        if (smiteVictim == null) return;
        if (--smiteTicks > 0) return;
        Player p = level.getPlayerByUUID(smiteVictim);
        smiteVictim = null;
        if (p != null && p.isAlive() && isTouchable(p)) {
            p.hurt(ModDamageTypes.source(level, ModDamageTypes.PENNY_LIE, this), Float.MAX_VALUE);
        }
    }

    public boolean hasPendingSmite() {
        return smiteVictim != null;
    }

    private void scheduleNextSearch() {
        int min = SebConfig.SEARCH_MIN_MINUTES.get();
        int max = Math.max(min, SebConfig.SEARCH_MAX_MINUTES.get());
        nextSearchTime = level().getGameTime() + randomTicks(min * 60, max * 60);
    }

    // ------------------------------------------------------------------ commands / tests

    /** Forces a state for /seb state and the GameTests. */
    public boolean forceState(SebState s, @Nullable ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        switch (s) {
            case WATCHING -> setState(SebState.WATCHING);
            case BEGGING -> {
                if (player == null) return false;
                startBegging(player);
            }
            case CHARGING -> {
                if (player == null) return false;
                startCharge(player, SebLines.CHARGE_TIMEOUT);
            }
            case RAGE -> startRage();
            case RUMMAGING -> {
                BlockPos pos = findContainer(level, Math.max(32, SebConfig.RUMMAGE_RADIUS.get()));
                if (pos == null) return false;
                startRummage(pos);
            }
            case SEARCHING -> {
                if (player == null) return false;
                startSearching(player);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ teleporting

    /** Finds a spot near the player and silently appears there. */
    public boolean blinkNear(Player p, double minD, double maxD, boolean outOfView) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle;
            Vec3 look = p.getViewVector(1).multiply(1, 0, 1);
            double lookYaw = Math.atan2(look.z, look.x);
            if (outOfView) {
                // behind or beside the player, never where they are looking
                angle = lookYaw + Math.PI + (random.nextDouble() - 0.5) * Math.PI * 1.1;
            } else {
                angle = random.nextDouble() * Math.PI * 2;
            }
            double d = minD + random.nextDouble() * Math.max(0.1, maxD - minD);
            BlockPos guess = BlockPos.containing(p.getX() + Math.cos(angle) * d, p.getY(), p.getZ() + Math.sin(angle) * d);
            BlockPos spot = findStandable(guess, 8);
            if (spot == null) continue;
            Vec3 v = Vec3.atBottomCenterOf(spot);
            if (outOfView) {
                Vec3 to = v.add(0, 1.6, 0).subtract(p.getEyePosition()).normalize();
                if (p.getViewVector(1).normalize().dot(to) > 0.2) continue;
            }
            blinkTo(v);
            return true;
        }
        return false;
    }

    private void blinkTo(Vec3 v) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 15, 0.25, 0.6, 0.25, 0.01);
        }
        getNavigation().stop();
        teleportTo(v.x, v.y, v.z);
    }

    @Nullable
    private BlockPos findStandable(BlockPos around, int vertical) {
        Level level = level();
        for (int dy = vertical; dy >= -vertical; dy--) {
            BlockPos pos = around.above(dy);
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            AABB box = getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos));
            if (level.noCollision(this, box)) {
                return pos;
            }
        }
        return null;
    }

    private void playSoundTo(ServerPlayer p, SoundEvent sound, Vec3 at, float volume, float pitch) {
        p.connection.send(new ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.HOSTILE, at.x, at.y, at.z, volume, pitch, random.nextLong()));
    }

    private int randomTicks(int minSeconds, int maxSeconds) {
        int lo = Math.min(minSeconds, maxSeconds), hi = Math.max(minSeconds, maxSeconds);
        return (lo + random.nextInt(hi - lo + 1)) * 20;
    }

    // ------------------------------------------------------------------ the wake-up scare

    public static void onPlayerWakeUp(ServerPlayer player) {
        if (!SebConfig.WAKE_UP_SCARE.get() || !isTouchable(player)) return;
        for (SebastianEntity seb : activeIn(player.level())) {
            if (seb.state != SebState.WATCHING || seb.distanceToSqr(player) > 128 * 128) continue;
            if (seb.random.nextFloat() > 0.6F) return;
            if (seb.blinkNear(player, 1.5, 2.5, false)) {
                seb.lockLookOn(player);
                player.playNotifySound(ModSounds.STING.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                SebChat.whisper(player, SebLines.pick(seb.random, SebLines.WAKE));
                SebAdvancements.award(player, "good_morning");
            }
            return;
        }
    }

    // ------------------------------------------------------------------ combat, damage, death

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (isBoss() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof ServerPlayer && hurtLineCooldown <= 0) {
                SebChat.say(this, SebLines.pick(random, new String[]{"that tickles.", "you can't hurt me here.", "keep swinging.", "no."}));
                hurtLineCooldown = 100;
            }
            return false;
        }
        if (source.is(DamageTypes.IN_WALL)) {
            ServerPlayer p = getTargetPlayer();
            if (p != null) blinkNear(p, SebConfig.WATCH_MIN.get(), SebConfig.WATCH_MAX.get(), true);
            else teleportTo(getX(), getY() + 1, getZ());
            return false;
        }
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof ServerPlayer attacker && isAlive()
                && state != SebState.CHARGING && state != SebState.RAGE) {
            int now = tickCount;
            int[] rec = hitsByPlayer.computeIfAbsent(attacker.getUUID(), k -> new int[]{0, now});
            if (now - rec[1] > 300) {
                rec[0] = 0;
                rec[1] = now;
            }
            rec[0]++;
            if (SebConfig.RETALIATION.get() && rec[0] >= SebConfig.RETALIATE_HITS.get() && isTouchable(attacker)) {
                hitsByPlayer.remove(attacker.getUUID());
                startCharge(attacker, SebLines.RETALIATE);
                return true;
            }
            if (hurtLineCooldown <= 0) {
                SebChat.say(this, SebLines.pick(random, SebLines.HURT));
                hurtLineCooldown = 60;
            }
            if (random.nextFloat() < SebConfig.TELEPORT_ON_HIT_CHANCE.get()) {
                blinkNear(attacker, 7, 12, true);
            }
        }
        return result;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.DROWN) || source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        // lightning is his, it doesn't hurt him
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved() && !dead && !isBoss()) {
            SebChat.say(this, SebLines.pick(random, SebLines.DEATH));
            closeContainer();
            if (natural) {
                SebSpawner.markDeath(this);
            }
            if (getServer() != null) {
                SebChat.joinMessage(getServer(), false);
            }
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int left = pennies;
        while (left > 0) {
            int n = Math.min(64, left);
            spawnAtLocation(new ItemStack(ModItems.PENNY.get(), n));
            left -= n;
        }
        pennies = 0;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // silent footsteps
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        applyConfigAttributes();
        setHealth(getMaxHealth());
        rummageCooldown = randomTicks(SebConfig.RUMMAGE_MIN.get(), SebConfig.RUMMAGE_MAX.get());
        if (getServer() != null && reason != MobSpawnType.CHUNK_GENERATION) {
            SebChat.joinMessage(getServer(), true);
        }
        return result;
    }

    // ------------------------------------------------------------------ client effects

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            SebState s = getSebState();
            if (s == SebState.RAGE) {
                if (random.nextInt(2) == 0) {
                    level().addParticle(ParticleTypes.FLAME, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.03, 0);
                }
                if (random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.5), getY(1.0), getRandomZ(0.5), 0, 0.05, 0);
                }
            } else if (s == SebState.CHARGING && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.CRIT, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pennies", pennies);
        tag.putBoolean("NaturalArrival", natural);
        tag.putLong("NextSearch", nextSearchTime);
        tag.putBoolean("Autonomous", autonomous);
        tag.putBoolean("Boss", isBoss());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pennies = tag.getInt("Pennies");
        natural = tag.getBoolean("NaturalArrival");
        nextSearchTime = tag.contains("NextSearch") ? tag.getLong("NextSearch") : -1;
        autonomous = !tag.contains("Autonomous") || tag.getBoolean("Autonomous");
        if (tag.getBoolean("Boss")) entityData.set(DATA_BOSS, true);
        setCustomName(SebChat.name());
    }
}
