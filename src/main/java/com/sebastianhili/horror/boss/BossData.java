package com.sebastianhili.horror.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World save data for the boss fight: whether he's gone for good, and the Exhibit A map id. */
public final class BossData extends SavedData {
    /** True once somebody survived the fight: he never arrives on his own again. */
    public boolean gone;
    public int mapId = -1;

    public static BossData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BossData::load, BossData::new, "sebastianhilihorror_boss");
    }

    static BossData load(CompoundTag tag) {
        BossData d = new BossData();
        d.gone = tag.getBoolean("Gone");
        d.mapId = tag.contains("MapId") ? tag.getInt("MapId") : -1;
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Gone", gone);
        tag.putInt("MapId", mapId);
        return tag;
    }
}
