package com.sebastianhili.horror.client;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SebHorrorMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEBASTIAN_HILI.get(), SebastianRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_PENNY.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    private ClientSetup() {
    }
}
