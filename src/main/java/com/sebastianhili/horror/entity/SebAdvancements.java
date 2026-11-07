package com.sebastianhili.horror.entity;

import com.sebastianhili.horror.SebHorrorMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Grants the code-triggered advancements (criterion "code" with the minecraft:impossible trigger). */
public final class SebAdvancements {
    public static void award(Player player, String id) {
        if (player instanceof ServerPlayer sp && sp.getServer() != null) {
            Advancement adv = sp.getServer().getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, id));
            if (adv != null) {
                sp.getAdvancements().award(adv, "code");
            }
        }
    }

    private SebAdvancements() {
    }
}
