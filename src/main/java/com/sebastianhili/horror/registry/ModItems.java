package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.item.PennyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SebHorrorMod.MODID);

    public static final RegistryObject<Item> ZINC_ORE = ITEMS.register("zinc_ore",
            () -> new BlockItem(ModBlocks.ZINC_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEEPSLATE_ZINC_ORE = ITEMS.register("deepslate_zinc_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_ZINC_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RAW_ZINC_BLOCK = ITEMS.register("raw_zinc_block",
            () -> new BlockItem(ModBlocks.RAW_ZINC_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ZINC_BLOCK = ITEMS.register("zinc_block",
            () -> new BlockItem(ModBlocks.ZINC_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAW_ZINC = ITEMS.register("raw_zinc", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ZINC_INGOT = ITEMS.register("zinc_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PENNY = ITEMS.register("penny", () -> new PennyItem(new Item.Properties()));

    public static final RegistryObject<Item> SEBASTIAN_SPAWN_EGG = ITEMS.register("sebastian_hili_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SEBASTIAN_HILI, 0x21402F, 0x33231B, new Item.Properties()));

    private ModItems() {
    }
}
