package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Data-driven damage types (see data/sebastianhilihorror/damage_type). Each one carries its own death message. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> PENNY_CHARGE = key("penny_charge");
    public static final ResourceKey<DamageType> PENNY_RAGE = key("penny_rage");
    public static final ResourceKey<DamageType> PENNY_LIE = key("penny_lie");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    private ModDamageTypes() {
    }
}
