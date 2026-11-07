package com.sebastianhili.horror.demo;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebastianEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Scripted demo used to record the showcase video. Dev-client only: it lives in the "demo"
 * source set, is not packaged in the release jar, and does nothing unless the JVM was started
 * with -Dsebhorror.demo=true (SEB_DEMO=true ./gradlew runClient).
 *
 * Everything shown is real gameplay: the director only does what a player would do (types
 * commands and chat, looks around, mines, clicks crafting slots, right-clicks).
 */
@Mod.EventBusSubscriber(modid = SebHorrorMod.MODID, value = Dist.CLIENT)
public final class DemoDirector {
    private static final Logger LOG = LoggerFactory.getLogger("SebDemo");
    private static final boolean ENABLED = Boolean.getBoolean("sebhorror.demo");
    private static final String WORLD = "SebDemo";

    private static boolean worldRequested;
    private static int readyTicks = -1;
    private static int t; // ticks since the script started
    private static final List<Step> steps = new ArrayList<>();
    private static int cursor;
    private static BlockPos o; // stage origin
    private static String caption = "";
    private static Supplier<Vec3> lookTarget;
    private static OrbitCam orbit;
    private static boolean finished;

    private record Step(int tick, Runnable action) {
    }

    private record OrbitCam(Vec3 center, double radius, double startAngle, double degPerTick, double y, int startTick) {
    }

    private DemoDirector() {
    }

    // ------------------------------------------------------------------ bootstrapping

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!ENABLED || worldRequested || !(event.getScreen() instanceof TitleScreen)) return;
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            try {
                Path save = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
                if (Files.exists(save)) {
                    try (var walk = Files.walk(save)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                    }
                }
            } catch (IOException e) {
                LOG.warn("could not delete old demo world", e);
            }
            GameRules rules = new GameRules();
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules,
                    WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261107L, false, false),
                    WorldPresets::createNormalWorldDimensions);
        });
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (readyTicks < 0) {
            readyTicks = 0;
        }
        if (readyTicks < 100) { // let the world settle and chunks render
            readyTicks++;
            if (readyTicks == 100) {
                o = p.blockPosition();
                buildScript();
                marker("start");
            }
            return;
        }
        while (cursor < steps.size() && steps.get(cursor).tick <= t) {
            try {
                steps.get(cursor).action.run();
            } catch (RuntimeException e) {
                LOG.error("demo step failed", e);
            }
            cursor++;
        }
        camera(mc);
        t++;
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        if (!ENABLED || caption.isEmpty()) return;
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.font.width(caption);
        g.fill(6, 6, 6 + w + 10, 6 + 18, 0xB0000000);
        g.drawString(mc.font, caption, 11, 11, 0xFFE8C26A, false);
    }

    // ------------------------------------------------------------------ the script

    private static int at; // script time cursor in ticks

    private static void pause(double seconds) {
        at += (int) Math.round(seconds * 20);
    }

    private static void step(Runnable r) {
        steps.add(new Step(at, r));
    }

    private static void cmd(String command) {
        step(() -> Minecraft.getInstance().player.connection.sendCommand(command));
    }

    private static void chat(String message) {
        step(() -> Minecraft.getInstance().player.connection.sendChat(message));
    }

    private static String xyz(int dx, int dy, int dz) {
        return (o.getX() + dx) + " " + (o.getY() + dy) + " " + (o.getZ() + dz);
    }

    private static void scene(String text) {
        step(() -> caption = text);
    }

    private static void lookAtSeb() {
        step(() -> {
            orbit = null;
            lookTarget = () -> {
                SebastianEntity s = seb();
                return s == null ? null : s.getEyePosition().add(0, -0.15, 0);
            };
        });
    }

    private static void lookAtPoint(Vec3 v) {
        step(() -> {
            orbit = null;
            lookTarget = () -> v;
        });
    }

    private static void tpPlayer(int dx, int dy, int dz, float yaw, float pitch) {
        cmd("tp @s " + xyz(dx, dy, dz) + " " + yaw + " " + pitch);
    }

    private static void tpSeb(int dx, int dy, int dz) {
        cmd("tp @e[type=sebastianhilihorror:sebastian_hili] " + xyz(dx, dy, dz));
    }

    private static void hotbar(int slot) {
        step(() -> Minecraft.getInstance().player.getInventory().selected = slot);
    }

    private static void hideGui(boolean hide) {
        step(() -> Minecraft.getInstance().options.hideGui = hide);
    }

    private static void click(int slot, int button, ClickType type) {
        step(() -> {
            Minecraft mc = Minecraft.getInstance();
            AbstractContainerMenu menu = mc.player.containerMenu;
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, mc.player);
        });
    }

    private static void useBlock(int dx, int dy, int dz) {
        step(() -> {
            Minecraft mc = Minecraft.getInstance();
            BlockPos pos = new BlockPos(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
        });
    }

    private static void closeScreen() {
        step(() -> Minecraft.getInstance().player.closeContainer());
    }

    private static void holdKey(KeyMapping key, boolean down) {
        step(() -> KeyMapping.set(key.getKey(), down));
    }

    private static void buildScript() {
        Minecraft mc = Minecraft.getInstance();
        at = 0;
        // ---- set the stage
        cmd("gamerule sendCommandFeedback false");
        cmd("gamerule doDaylightCycle false");
        cmd("gamerule doWeatherCycle false");
        cmd("gamerule doMobSpawning false");
        cmd("gamerule doFireTick false");
        cmd("gamerule keepInventory true");
        cmd("gamerule doImmediateRespawn true");
        cmd("gamerule sendCommandFeedback false");
        cmd("gamerule commandBlockOutput false");
        cmd("gamerule showDeathMessages true");
        cmd("weather clear");
        cmd("time set 6000");
        cmd("fill " + xyz(-22, -4, -22) + " " + xyz(22, -2, 22) + " dirt");
        cmd("fill " + xyz(-22, -1, -22) + " " + xyz(22, -1, 22) + " grass_block");
        cmd("fill " + xyz(-22, 0, -22) + " " + xyz(22, 14, 22) + " air");
        cmd("spawnpoint @s " + xyz(0, 0, 0));
        tpPlayer(0, 0, 0, 0, 0);
        pause(2);

        // ---- title card
        scene("Sebastian Hili Horror  |  Minecraft 1.20.1 Forge");
        cmd("title @s times 5 50 10");
        cmd("title @s subtitle {\"text\":\"a mod about a boy who would like a penny\",\"color\":\"gray\"}");
        cmd("title @s title {\"text\":\"Sebastian Hili Horror\",\"color\":\"dark_red\",\"bold\":true}");
        pause(3.5);

        // ---- 1. skin close-up
        scene("1. The skin, up close");
        cmd("seb spawn");
        pause(0.3);
        cmd("seb freeze true");
        pause(0.5);
        // pause his AI so the camera can circle him and show the back of the skin
        cmd("data merge entity @e[type=sebastianhilihorror:sebastian_hili,limit=1] {NoAI:1b}");
        pause(0.5);
        hideGui(true);
        step(() -> {
            lookTarget = null;
            Minecraft.getInstance().player.getAbilities().flying = true;
            orbit = new OrbitCam(Vec3.atBottomCenterOf(o.offset(0, 0, 4)), 2.3, -90, 1.6, o.getY() + 0.05, t);
        });
        pause(11.5);
        step(() -> orbit = null);
        tpPlayer(0, 0, 2, 0, 5);
        lookAtSeb();
        pause(0.8);
        // ...and then he notices you
        cmd("data merge entity @e[type=sebastianhilihorror:sebastian_hili,limit=1] {NoAI:0b}");
        pause(2.5);
        step(() -> Minecraft.getInstance().player.getAbilities().flying = false);
        hideGui(false);

        // ---- 2. watching
        scene("2. WATCHING: he keeps his distance and stares");
        tpPlayer(0, 0, -14, 0, 0);
        tpSeb(0, 0, 4);
        cmd("seb freeze false");
        lookAtSeb();
        pause(4.5);
        scene("2. ...look away, and he is somewhere else");
        lookAtPoint(Vec3.atCenterOf(o.offset(0, 2, -40)));
        pause(4.0);
        lookAtSeb();
        pause(3.5);
        scene("2. he copies what you hold, and sneaks when you sneak");
        cmd("item replace entity @s weapon.mainhand with minecraft:diamond_sword");
        pause(1.5);
        step(() -> mc.options.keyShift.setDown(true));
        pause(2.5);
        step(() -> mc.options.keyShift.setDown(false));
        pause(1.0);
        cmd("seb freeze true");

        // ---- 3. mining and crafting
        scene("3. Mining zinc ore with a stone pickaxe");
        cmd("clear @s");
        cmd("gamemode survival");
        tpPlayer(0, 0, -14, 180, 35);
        tpSeb(-9, 0, -4);
        cmd("setblock " + xyz(0, 0, -15) + " sebastianhilihorror:zinc_ore");
        cmd("setblock " + xyz(1, 0, -15) + " sebastianhilihorror:deepslate_zinc_ore");
        cmd("item replace entity @s hotbar.0 with minecraft:stone_pickaxe");
        hotbar(0);
        pause(1.0);
        lookAtPoint(Vec3.atCenterOf(o.offset(0, 0, -15)));
        pause(1.0);
        holdKey(mc.options.keyAttack, true);
        pause(2.0);
        holdKey(mc.options.keyAttack, false);
        lookAtPoint(Vec3.atCenterOf(o.offset(1, 0, -15)));
        pause(0.8);
        holdKey(mc.options.keyAttack, true);
        pause(2.6);
        holdKey(mc.options.keyAttack, false);
        pause(1.0);
        scene("3. Raw Zinc smelts into Zinc Ingots. 3 Zinc + 1 Copper = 4 Pennies");
        cmd("clear @s");
        cmd("setblock " + xyz(0, 0, -15) + " minecraft:crafting_table");
        cmd("item replace entity @s hotbar.0 with sebastianhilihorror:zinc_ingot 3");
        cmd("item replace entity @s hotbar.1 with minecraft:copper_ingot 1");
        hotbar(4);
        lookAtPoint(Vec3.atCenterOf(o.offset(0, 0, -15)));
        pause(1.0);
        useBlock(0, 0, -15);
        pause(1.2);
        click(37, 0, ClickType.PICKUP);   // pick up the zinc
        pause(0.6);
        click(1, 1, ClickType.PICKUP);    // one zinc per slot
        pause(0.5);
        click(2, 1, ClickType.PICKUP);
        pause(0.5);
        click(3, 1, ClickType.PICKUP);
        pause(0.6);
        click(38, 0, ClickType.PICKUP);   // copper
        pause(0.6);
        click(5, 1, ClickType.PICKUP);
        pause(1.5);
        click(0, 0, ClickType.QUICK_MOVE); // take the pennies
        pause(1.5);
        closeScreen();
        pause(0.5);

        // ---- 4. begging and giving
        scene("4. BEGGING: hold a Penny near him...");
        tpPlayer(0, 0, 0, 0, 0);
        tpSeb(0, 0, 13);
        lookAtSeb();
        pause(1.0);
        hotbar(8); // the pennies landed in the last hotbar slot
        pause(5.0);
        scene("4. ...right-click him with it to give it");
        step(() -> {
            SebastianEntity s = seb();
            if (s != null) {
                mc.gameMode.interact(mc.player, s, InteractionHand.MAIN_HAND);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        });
        pause(3.0);

        // ---- 5. charge
        scene("5. CHARGING: put the penny away and he runs you through");
        hotbar(4);
        tpPlayer(0, 0, 0, 0, 0);
        tpSeb(0, 0, 16);
        lookAtSeb();
        pause(3.2); // beg cooldown passes
        hotbar(8);  // show him the penny again...
        pause(1.6);
        hotbar(4);  // ...and put it away while he's still far off
        pause(5.5);

        // ---- 6. truthful no
        scene("6. INVENTORY SEARCH: an honest \"no\"");
        cmd("clear @s sebastianhilihorror:penny");
        tpPlayer(0, 0, 0, 0, 0);
        tpSeb(0, 0, 9);
        lookAtSeb();
        cmd("seb state searching Dev");
        pause(3.5);
        chat("no");
        pause(3.5);

        // ---- 7. lying no
        scene("7. INVENTORY SEARCH: a lying \"no\" (2 pennies in the backpack)");
        cmd("item replace entity @s inventory.10 with sebastianhilihorror:penny 2");
        tpSeb(0, 0, 9);
        cmd("seb state searching Dev");
        pause(3.5);
        chat("No");
        pause(1.0);
        lookAtPoint(Vec3.atCenterOf(o.offset(0, 25, 3)));
        pause(1.6);
        lookAtSeb();
        pause(2.0);

        // ---- 8. rummaging
        scene("8. RUMMAGING: he takes ONLY the pennies from your chests");
        tpPlayer(0, 0, 0, 0, 0);
        tpSeb(-2, 0, 14);
        cmd("setblock " + xyz(2, 0, 3) + " minecraft:chest[facing=west]");
        cmd("item replace block " + xyz(2, 0, 3) + " container.0 with sebastianhilihorror:penny 16");
        cmd("item replace block " + xyz(2, 0, 3) + " container.1 with minecraft:diamond 5");
        cmd("item replace block " + xyz(2, 0, 3) + " container.2 with minecraft:gold_ingot 8");
        cmd("item replace block " + xyz(2, 0, 3) + " container.3 with minecraft:bread 6");
        cmd("item replace block " + xyz(2, 0, 3) + " container.4 with sebastianhilihorror:penny 9");
        lookAtPoint(Vec3.atCenterOf(o.offset(2, 0, 3)));
        pause(1.0);
        useBlock(2, 0, 3);
        pause(2.5);
        closeScreen();
        pause(0.3);
        tpPlayer(-3, 0, -1, 0, 0);
        cmd("seb state rummaging");
        step(() -> lookTarget = () -> {
            SebastianEntity s = seb();
            Vec3 chest = Vec3.atCenterOf(o.offset(2, 0, 3));
            return s == null ? chest : s.getEyePosition().add(chest).scale(0.5);
        });
        pause(7.0);
        tpPlayer(0, 0, 0, 0, 0);
        lookAtPoint(Vec3.atCenterOf(o.offset(2, 0, 3)));
        pause(0.6);
        useBlock(2, 0, 3);
        pause(2.5);
        closeScreen();
        pause(0.3);

        // ---- 9. rage
        scene("9. RAGE: never let him see a penny burn");
        cmd("setblock " + xyz(2, 0, 3) + " air");
        cmd("seb freeze true");
        cmd("time set 6000");
        cmd("sebdemo friends " + xyz(-6, 0, 9) + " " + xyz(6, 0, 10));
        cmd("fill " + xyz(-1, -1, 6) + " " + xyz(1, -1, 7) + " lava");
        tpPlayer(0, 0, -3, 0, 0);
        tpSeb(0, 0, 13);
        lookAtPoint(Vec3.atCenterOf(o.offset(0, 0, 8)));
        pause(2.5);
        scene("9. a penny falls into the lava where he can see it...");
        cmd("summon item " + xyz(0, 2, 6) + " {Item:{id:\"sebastianhilihorror:penny\",Count:1b},Motion:[0.0,0.0,0.0]}");
        pause(1.5);
        lookAtSeb();
        pause(15.0);
        cmd("weather clear");
        cmd("sebdemo nofriends");
        cmd("fill " + xyz(-1, -1, 6) + " " + xyz(1, -1, 7) + " grass_block");
        pause(1.0);

        // ---- end card
        scene("Sebastian Hili Horror");
        cmd("time set 6000");
        tpPlayer(0, 0, 0, 0, 0);
        tpSeb(0, 0, 3);
        lookAtSeb();
        pause(0.5);
        cmd("title @s subtitle {\"text\":\"He would like a penny.\",\"color\":\"gray\"}");
        cmd("title @s title {\"text\":\"Sebastian Hili Horror\",\"color\":\"dark_red\",\"bold\":true}");
        pause(5.0);
        step(() -> {
            marker("end");
            finished = true;
            Minecraft.getInstance().stop();
        });
    }

    // ------------------------------------------------------------------ camera

    private static void camera(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (orbit != null) {
            double angle = Math.toRadians(orbit.startAngle + (t - orbit.startTick) * orbit.degPerTick);
            double x = orbit.center.x + Math.cos(angle) * orbit.radius;
            double z = orbit.center.z + Math.sin(angle) * orbit.radius;
            p.setDeltaMovement(Vec3.ZERO);
            p.setPos(x, orbit.y, z);
            SebastianEntity s = seb();
            Vec3 target = s != null ? s.getEyePosition().add(0, -0.2, 0) : orbit.center.add(0, 1.5, 0);
            face(p, target, 1.0F);
            return;
        }
        if (lookTarget != null) {
            Vec3 target = lookTarget.get();
            if (target != null) face(p, target, 0.25F);
        }
    }

    private static void face(LocalPlayer p, Vec3 target, float lerp) {
        Vec3 eye = p.getEyePosition();
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float curYaw = p.getYRot();
        float dy = wrap(yaw - curYaw);
        float newYaw = curYaw + dy * lerp;
        float newPitch = p.getXRot() + (pitch - p.getXRot()) * lerp;
        p.setYRot(newYaw);
        p.setXRot(newPitch);
        p.setYHeadRot(newYaw);
    }

    private static float wrap(float deg) {
        deg %= 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    private static SebastianEntity seb() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        SebastianEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof SebastianEntity s && s.isAlive()) {
                double d = s.distanceToSqr(mc.player);
                if (d < bestD) {
                    bestD = d;
                    best = s;
                }
            }
        }
        return best;
    }

    private static void marker(String name) {
        try {
            Path f = Minecraft.getInstance().gameDirectory.toPath().resolve("demo_markers.txt");
            Files.writeString(f, name + " " + System.currentTimeMillis() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.info("DEMO MARKER {}", name);
        } catch (IOException e) {
            LOG.warn("marker failed", e);
        }
    }
}
