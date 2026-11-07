package com.sebastianhili.horror.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.sebastianhili.horror.entity.SebState;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Debug commands (operators only, permission level 2):
 * <pre>
 * /seb spawn                 spawn him 4 blocks in front of you, facing you
 * /seb state &lt;state&gt; [player]   force a state on the nearest Sebastian
 * /seb ask [player]          he asks the question right now (skips walking over)
 * /seb freeze &lt;true|false&gt;   stop/allow wandering, relocating and timers (reactions still work)
 * /seb info                  state, target, pennies, health, next search
 * /seb remove                remove every Sebastian in this dimension
 * </pre>
 */
public final class SebCommand {
    private static final SimpleCommandExceptionType NONE =
            new SimpleCommandExceptionType(Component.literal("No Sebastian Hili in this dimension. Try /seb spawn"));
    private static final SimpleCommandExceptionType FAILED =
            new SimpleCommandExceptionType(Component.literal("He couldn't do that (needs a player, or a container nearby for rummaging)."));

    private static final SuggestionProvider<CommandSourceStack> STATES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(SebState.values()).map(SebState::id), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("seb").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").executes(SebCommand::spawn))
                .then(Commands.literal("info").executes(SebCommand::info))
                .then(Commands.literal("remove").executes(SebCommand::remove))
                .then(Commands.literal("freeze")
                        .then(Commands.argument("frozen", BoolArgumentType.bool()).executes(SebCommand::freeze)))
                .then(Commands.literal("ask")
                        .executes(c -> ask(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> ask(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("map")
                        .executes(c -> giveMap(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> giveMap(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("refusals")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                        .executes(c -> {
                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                            int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "count");
                                            com.sebastianhili.horror.boss.BurnedPennyMap.setRefusals(p, n);
                                            c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " has refused him " + n + " time(s) in a row."), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("gone")
                        .then(Commands.argument("gone", BoolArgumentType.bool()).executes(c -> {
                            boolean gone = BoolArgumentType.getBool(c, "gone");
                            var data = com.sebastianhili.horror.boss.BossData.get(c.getSource().getServer());
                            data.gone = gone;
                            data.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal(gone ? "Sebastian Hili will not arrive on his own any more." : "Sebastian Hili can arrive on his own again."), true);
                            return 1;
                        })))
                .then(Commands.literal("boss")
                        .then(Commands.literal("start").executes(SebCommand::bossStart))
                        .then(Commands.literal("stop").executes(c -> {
                            var f = com.sebastianhili.horror.boss.BossFight.get();
                            if (f == null) throw NO_FIGHT.create();
                            f.cancel();
                            return 1;
                        }))
                        .then(Commands.literal("skip")
                                .then(Commands.argument("seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-4, 399))
                                        .executes(c -> {
                                            var f = com.sebastianhili.horror.boss.BossFight.get();
                                            if (f == null) throw NO_FIGHT.create();
                                            int sec = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "seconds");
                                            f.skipTo(sec * 20);
                                            c.getSource().sendSuccess(() -> Component.literal("Boss fight -> " + sec + "s (the music keeps its own time)"), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("state")
                        .then(Commands.argument("state", com.mojang.brigadier.arguments.StringArgumentType.word()).suggests(STATES)
                                .executes(c -> state(c, c.getSource().getPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> state(c, EntityArgument.getPlayer(c, "player")))))));
    }

    private static final SimpleCommandExceptionType NO_FIGHT =
            new SimpleCommandExceptionType(Component.literal("No boss fight is running."));

    private static int giveMap(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        p.getInventory().add(com.sebastianhili.horror.boss.BurnedPennyMap.create(c.getSource().getServer()));
        c.getSource().sendSuccess(() -> Component.literal("Gave Exhibit A to " + p.getGameProfile().getName()), true);
        return 1;
    }

    /** Starts the fight as if you had shown the nearest Sebastian the map (spawns one if needed). */
    private static int bossStart(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        SebastianEntity seb = SebastianEntity.activeIn(p.serverLevel()).stream()
                .min(Comparator.comparingDouble(s -> s.distanceToSqr(p))).orElse(null);
        if (seb == null) {
            seb = ModEntities.SEBASTIAN_HILI.get().create(p.serverLevel());
            if (seb == null) return 0;
            seb.moveTo(p.getX(), p.getY(), p.getZ() + 3, 180, 0);
            p.serverLevel().addFreshEntity(seb);
        }
        if (!com.sebastianhili.horror.boss.BossFight.start(seb, p)) {
            throw new SimpleCommandExceptionType(Component.literal("Couldn't start the fight (one is already running?)")).create();
        }
        return 1;
    }

    private static SebastianEntity nearest(CommandSourceStack src) throws CommandSyntaxException {
        Vec3 pos = src.getPosition();
        return SebastianEntity.activeIn(src.getLevel()).stream()
                .min(Comparator.comparingDouble(s -> s.distanceToSqr(pos)))
                .orElseThrow(NONE::create);
    }

    private static int spawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        SebastianEntity seb = ModEntities.SEBASTIAN_HILI.get().create(level);
        if (seb == null) return 0;
        ServerPlayer player = src.getPlayer();
        if (player != null) {
            Vec3 look = player.getViewVector(1).multiply(1, 0, 1).normalize();
            Vec3 at = player.position().add(look.scale(4));
            seb.moveTo(at.x, player.getY(), at.z, player.getYRot() + 180, 0);
            seb.setYHeadRot(player.getYRot() + 180);
        } else {
            Vec3 at = src.getPosition();
            seb.moveTo(at.x, at.y, at.z, 0, 0);
        }
        seb.finalizeSpawn(level, level.getCurrentDifficultyAt(seb.blockPosition()), MobSpawnType.COMMAND, null, null);
        level.addFreshEntity(seb);
        src.sendSuccess(() -> Component.literal("Sebastian Hili is here."), true);
        return 1;
    }

    private static int state(CommandContext<CommandSourceStack> c, @Nullable ServerPlayer player) throws CommandSyntaxException {
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "state").toUpperCase(Locale.ROOT);
        SebState s;
        try {
            s = SebState.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown state. Use: watching, begging, charging, rage, rummaging, searching")).create();
        }
        SebastianEntity seb = nearest(c.getSource());
        if (!seb.forceState(s, player)) throw FAILED.create();
        c.getSource().sendSuccess(() -> Component.literal("Sebastian Hili -> " + s.id()), true);
        return 1;
    }

    private static int ask(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        SebastianEntity seb = nearest(c.getSource());
        seb.askNow(player);
        return 1;
    }

    private static int freeze(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        boolean frozen = BoolArgumentType.getBool(c, "frozen");
        for (SebastianEntity seb : SebastianEntity.activeIn(c.getSource().getLevel())) {
            seb.setAutonomous(!frozen);
        }
        c.getSource().sendSuccess(() -> Component.literal(frozen ? "Sebastian Hili will stay put (he still reacts)." : "Sebastian Hili is free to roam."), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        SebastianEntity seb = nearest(c.getSource());
        ServerPlayer t = seb.getTargetPlayer();
        long search = seb.ticksUntilSearch();
        String msg = String.format(Locale.ROOT,
                "Sebastian Hili: state=%s target=%s pennies=%d health=%.0f/%.0f next search in %ds natural=%s roaming=%s at %d %d %d",
                seb.getSebState().id(), t == null ? "-" : t.getGameProfile().getName(), seb.getPennies(),
                seb.getHealth(), seb.getMaxHealth(), Math.max(0, search / 20), seb.isNatural(), seb.isAutonomous(),
                seb.getBlockX(), seb.getBlockY(), seb.getBlockZ());
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (SebastianEntity seb : SebastianEntity.activeIn(c.getSource().getLevel())) {
            seb.discard();
            n++;
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("Removed " + count + " Sebastian Hili."), true);
        return n;
    }

    private SebCommand() {
    }

}
