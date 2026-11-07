package com.sebastianhili.horror.entity;

import com.sebastianhili.horror.registry.ModDamageTypes;
import com.sebastianhili.horror.registry.ModEntities;
import com.sebastianhili.horror.registry.ModItems;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A penny thrown by Sebastian Hili during the boss fight. Hurts, then vanishes (no item drop). */
public class ThrownPenny extends ThrowableItemProjectile {
    private float damage = 4.0F;

    public ThrownPenny(EntityType<? extends ThrownPenny> type, Level level) {
        super(type, level);
    }

    public ThrownPenny(Level level, LivingEntity owner) {
        super(ModEntities.THROWN_PENNY.get(), owner, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PENNY.get();
    }

    @Override
    protected float getGravity() {
        return 0.012F;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof SebastianEntity) && !(target instanceof ThrownPenny) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity owner = getOwner();
        result.getEntity().hurt(ModDamageTypes.source(level(), ModDamageTypes.BOSS_PENNY, owner == null ? this : owner), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.PENNY_CLINK.get(), SoundSource.HOSTILE, 0.4F, 1.4F);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 6; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem())), getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5) * 0.1, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 120) {
            discard();
        }
    }
}
