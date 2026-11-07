package com.sebastianhili.horror.registry;

import com.sebastianhili.horror.SebHorrorMod;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SebHorrorMod.MODID);

    public static final RegistryObject<Block> ZINC_ORE = BLOCKS.register("zinc_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F),
                    UniformInt.of(0, 1)));
    public static final RegistryObject<Block> DEEPSLATE_ZINC_ORE = BLOCKS.register("deepslate_zinc_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> RAW_ZINC_BLOCK = BLOCKS.register("raw_zinc_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));
    public static final RegistryObject<Block> ZINC_BLOCK = BLOCKS.register("zinc_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
