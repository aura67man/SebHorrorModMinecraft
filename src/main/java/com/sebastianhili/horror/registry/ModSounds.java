package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SebHorrorMod.MODID);

    public static final RegistryObject<SoundEvent> HEARTBEAT = register("heartbeat");
    public static final RegistryObject<SoundEvent> WHISPER = register("whisper");
    public static final RegistryObject<SoundEvent> RAGE_SCREAM = register("rage_scream");
    public static final RegistryObject<SoundEvent> PENNY_CLINK = register("penny_clink");
    public static final RegistryObject<SoundEvent> STING = register("sting");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, name)));
    }

    private ModSounds() {
    }
}
