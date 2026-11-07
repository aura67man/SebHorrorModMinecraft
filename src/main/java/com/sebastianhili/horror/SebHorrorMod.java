package com.sebastianhili.horror;

import com.sebastianhili.horror.config.SebConfig;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.registry.ModBlocks;
import com.sebastianhili.horror.registry.ModCreativeTab;
import com.sebastianhili.horror.registry.ModEntities;
import com.sebastianhili.horror.registry.ModItems;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Sebastian Hili Horror. He is watching. He would like a penny. */
@Mod(SebHorrorMod.MODID)
public class SebHorrorMod {
    public static final String MODID = "sebastianhilihorror";

    public SebHorrorMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(this::onAttributes);
        context.registerConfig(ModConfig.Type.COMMON, SebConfig.SPEC);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SEBASTIAN_HILI.get(), SebastianEntity.createAttributes().build());
    }
}
