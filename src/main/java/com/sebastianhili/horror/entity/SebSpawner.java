package com.sebastianhili.horror.entity;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.config.SebConfig;
import com.sebastianhili.horror.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Natural arrival. Instead of biome spawns (which would quickly make a crowd of him), the server
 * remembers "the" natural Sebastian. If he isn't around, he eventually arrives near a player,
 * out of sight, with a fake "Sebastian Hili joined the game" message.
 */
public final class SebSpawner {
    private static final long STRANDED_TICKS = 20L * 60 * 10;

    public static void tick(ServerLevel overworld) {
        if (!SebConfig.NATURAL_SPAWNING.get()) return;
        long time = overworld.getGameTime();
        if (time % 1200 != 600) return; // once a minute
        if (overworld.players().isEmpty()) return;
        if (time < SebConfig.FIRST_ARRIVAL_MINUTES.get() * 1200L) return;

        Data data = Data.get(overworld);
        if (data.lastDeath > 0 && time - data.lastDeath < SebConfig.RETURN_AFTER_DEATH_MINUTES.get() * 1200L) return;

        long loadedNatural = SebastianEntity.allActive().stream().filter(SebastianEntity::isNatural).count();
        if (loadedNatural >= SebConfig.MAX_NATURAL.get()) return;
        // He exists somewhere unloaded and was seen recently: don't make another one yet.
        if (loadedNatural == 0 && data.current != null && time - data.lastSeen < STRANDED_TICKS) return;
        if (overworld.random.nextDouble() > SebConfig.ARRIVAL_CHANCE_PER_MINUTE.get()) return;

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : overworld.players()) {
            if (SebastianEntity.isTouchable(p)) candidates.add(p);
        }
        if (candidates.isEmpty()) return;
        ServerPlayer near = candidates.get(overworld.random.nextInt(candidates.size()));
        SebastianEntity seb = spawnNear(overworld, near, true);
        if (seb != null) {
            data.current = seb.getUUID();
            data.lastSeen = time;
            data.setDirty();
        }
    }

    @Nullable
    public static SebastianEntity spawnNear(ServerLevel level, ServerPlayer player, boolean natural) {
        SebastianEntity seb = ModEntities.SEBASTIAN_HILI.get().create(level);
        if (seb == null) return null;
        seb.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot() + 180, 0);
        seb.setNatural(natural);
        if (!seb.blinkNear(player, 24, 40, true) && !seb.blinkNear(player, 12, 24, false)) {
            return null;
        }
        seb.finalizeSpawn(level, level.getCurrentDifficultyAt(seb.blockPosition()), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(seb);
        return seb;
    }

    static boolean isStaleDuplicate(SebastianEntity seb) {
        if (!seb.isNatural() || !(seb.level() instanceof ServerLevel sl)) return false;
        Data data = Data.get(sl.getServer().overworld());
        if (data.current == null) {
            data.current = seb.getUUID();
            data.lastSeen = sl.getGameTime();
            data.setDirty();
            return false;
        }
        if (data.current.equals(seb.getUUID())) return false;
        // Another natural Sebastian is the "real" one. If he's loaded, this old copy leaves.
        for (SebastianEntity other : SebastianEntity.allActive()) {
            if (other != seb && other.getUUID().equals(data.current)) return true;
        }
        // The registered one is gone; this one takes over.
        data.current = seb.getUUID();
        data.lastSeen = sl.getGameTime();
        data.setDirty();
        return false;
    }

    static void markSeen(SebastianEntity seb) {
        if (!(seb.level() instanceof ServerLevel sl)) return;
        Data data = Data.get(sl.getServer().overworld());
        if (seb.getUUID().equals(data.current)) {
            data.lastSeen = sl.getServer().overworld().getGameTime();
            data.setDirty();
        }
    }

    static void markDeath(SebastianEntity seb) {
        if (!(seb.level() instanceof ServerLevel sl)) return;
        Data data = Data.get(sl.getServer().overworld());
        if (seb.getUUID().equals(data.current)) {
            data.current = null;
        }
        data.lastDeath = sl.getServer().overworld().getGameTime();
        data.setDirty();
    }

    /** Stored in the world save (data/sebastianhilihorror.dat). */
    public static final class Data extends SavedData {
        @Nullable
        UUID current;
        long lastSeen;
        long lastDeath;

        static Data get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(Data::load, Data::new, SebHorrorMod.MODID);
        }

        static Data load(CompoundTag tag) {
            Data d = new Data();
            if (tag.hasUUID("Current")) d.current = tag.getUUID("Current");
            d.lastSeen = tag.getLong("LastSeen");
            d.lastDeath = tag.getLong("LastDeath");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            if (current != null) tag.putUUID("Current", current);
            tag.putLong("LastSeen", lastSeen);
            tag.putLong("LastDeath", lastDeath);
            return tag;
        }
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    private SebSpawner() {
    }
}
