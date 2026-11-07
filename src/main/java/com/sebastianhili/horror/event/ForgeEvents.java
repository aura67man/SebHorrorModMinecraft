package com.sebastianhili.horror.event;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.command.SebCommand;
import com.sebastianhili.horror.entity.SebSpawner;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.logic.ChatAnswer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SebHorrorMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SebCommand.register(event.getDispatcher());
    }

    /** "yes"/"no" typed in chat answers his question. The message still shows in chat for everyone. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ChatAnswer answer = ChatAnswer.parse(event.getRawText());
        if (answer == ChatAnswer.NOT_AN_ANSWER) return;
        ServerPlayer player = event.getPlayer();
        // Handle it on the next server tick, so his reply always appears after the player's message.
        NEXT_TICK.add(() -> SebastianEntity.handleChatAnswer(player, answer));
    }

    private static final java.util.Queue<Runnable> NEXT_TICK = new java.util.concurrent.ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Runnable r;
        while ((r = NEXT_TICK.poll()) != null) {
            r.run();
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel sl && SebSpawner.isOverworld(sl)) {
            SebSpawner.tick(sl);
        }
    }

    /** Waking up in the morning... with company. */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // the wake-up happens mid-tick; wait one tick so the player is standing again
            sp.server.execute(() -> {
                if (sp.level().isDay()) {
                    SebastianEntity.onPlayerWakeUp(sp);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        SebastianEntity.clearActive();
    }

    private ForgeEvents() {
    }
}
