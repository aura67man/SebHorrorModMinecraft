package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SebHorrorMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + SebHorrorMod.MODID))
            .icon(() -> new ItemStack(ModItems.PENNY.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.PENNY.get());
                out.accept(ModItems.ZINC_INGOT.get());
                out.accept(ModItems.RAW_ZINC.get());
                out.accept(ModItems.ZINC_ORE.get());
                out.accept(ModItems.DEEPSLATE_ZINC_ORE.get());
                out.accept(ModItems.RAW_ZINC_BLOCK.get());
                out.accept(ModItems.ZINC_BLOCK.get());
                out.accept(ModItems.SEBASTIAN_SPAWN_EGG.get());
            })
            .build());

    private ModCreativeTab() {
    }
}
