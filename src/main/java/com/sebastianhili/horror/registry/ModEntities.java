package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebastianEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SebHorrorMod.MODID);

    public static final RegistryObject<EntityType<SebastianEntity>> SEBASTIAN_HILI = ENTITIES.register("sebastian_hili",
            () -> EntityType.Builder.of(SebastianEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build(SebHorrorMod.MODID + ":sebastian_hili"));

    private ModEntities() {
    }
}
