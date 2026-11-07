package com.sebastianhili.horror.item;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebastianEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** An American one-cent coin. Burning one where Sebastian can see it is a very bad idea. */
public class PennyItem extends Item {
    public PennyItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item." + SebHorrorMod.MODID + ".penny.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Called by ItemEntity#hurt right before a dropped penny is destroyed. */
    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource source) {
        super.onDestroyed(itemEntity, source);
        if (itemEntity.level().isClientSide) {
            return;
        }
        boolean fire = source.is(DamageTypeTags.IS_FIRE) || itemEntity.isOnFire() || itemEntity.isInLava();
        SebastianEntity.onPennyDestroyed(itemEntity, fire);
    }
}
