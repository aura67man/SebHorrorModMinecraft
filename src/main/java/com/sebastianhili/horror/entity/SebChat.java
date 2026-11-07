package com.sebastianhili.horror.entity;

import com.sebastianhili.horror.config.SebConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Chat that looks exactly like a real player typing: "<Sebastian Hili> message". */
public final class SebChat {
    public static final String NAME = "Sebastian Hili";

    public static Component name() {
        return Component.literal(NAME);
    }

    /** Public chat. Everyone on the server sees it unless chatRadius is set in the config. */
    public static void say(Entity seb, String message) {
        if (!(seb.level() instanceof ServerLevel level)) {
            return;
        }
        Component line = Component.translatable("chat.type.text", name(), Component.literal(message));
        int radius = SebConfig.CHAT_RADIUS.get();
        if (radius <= 0) {
            level.getServer().getPlayerList().broadcastSystemMessage(line, false);
        } else {
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(seb) <= (double) radius * radius) {
                    p.sendSystemMessage(line);
                }
            }
            level.getServer().sendSystemMessage(line);
        }
    }

    /** A private whisper, formatted like /msg. */
    public static void whisper(ServerPlayer to, String message) {
        to.sendSystemMessage(Component.translatable("commands.message.display.incoming", name(), Component.literal(message))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Grey helper text for the player who needs to act. */
    public static void hint(ServerPlayer to, String message) {
        if (SebConfig.HINTS.get()) {
            to.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    public static void actionBar(ServerPlayer to, Component message) {
        if (SebConfig.HINTS.get()) {
            to.displayClientMessage(message, true);
        }
    }

    public static void joinMessage(MinecraftServer server, boolean joined) {
        if (SebConfig.FAKE_JOIN_MESSAGES.get()) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable(
                    joined ? "multiplayer.player.joined" : "multiplayer.player.left", name()).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    private SebChat() {
    }
}
