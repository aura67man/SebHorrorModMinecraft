package com.sebastianhili.horror.demo;

import com.mojang.authlib.GameProfile;
import com.sebastianhili.horror.SebHorrorMod;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Demo only: "/sebdemo friends pos1 pos2" puts two stand-in players into the world so the
 * rage scene can show him going through several players one by one. They are ordinary
 * ServerPlayers on an in-memory connection (the same trick the GameTests use).
 */
@Mod.EventBusSubscriber(modid = SebHorrorMod.MODID)
public final class DemoFriends {
    private static final List<ServerPlayer> FRIENDS = new ArrayList<>();
    private static final String[] NAMES = {"Friend_Matt", "Friend_Lucas"};

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("sebhorror.demo")) return;
        event.getDispatcher().register(Commands.literal("sebdemo").requires(s -> s.hasPermission(2))
                .then(Commands.literal("friends")
                        .then(Commands.argument("a", BlockPosArgument.blockPos())
                                .then(Commands.argument("b", BlockPosArgument.blockPos())
                                        .executes(c -> {
                                            spawn(c.getSource().getLevel(), NAMES[0], BlockPosArgument.getLoadedBlockPos(c, "a"));
                                            spawn(c.getSource().getLevel(), NAMES[1], BlockPosArgument.getLoadedBlockPos(c, "b"));
                                            return 1;
                                        }))))
                .then(Commands.literal("nofriends").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    for (ServerPlayer f : FRIENDS) server.getPlayerList().remove(f);
                    FRIENDS.clear();
                    return 1;
                })));
    }

    private static void spawn(ServerLevel level, String name, BlockPos pos) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(id, name));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, p);
        p.setGameMode(GameType.SURVIVAL);
        p.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180, 0);
        FRIENDS.add(p);
    }

    private DemoFriends() {
    }
}
