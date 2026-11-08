package com.sebastianhili.horror.boss;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.config.SebConfig;
import com.sebastianhili.horror.entity.SebAdvancements;
import com.sebastianhili.horror.entity.SebChat;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * "Exhibit A": a locked map showing a burned penny. Refuse Sebastian a penny enough times in a row
 * and he hands it to you. Show it to him (right-click him with it) to start the boss fight.
 */
public final class BurnedPennyMap {
    private static final Logger LOG = LoggerFactory.getLogger("SebHorror");
    public static final String TAG = "SebExhibitA";
    private static final String REFUSALS = "SebRefusals";
    private static final ResourceLocation ART = ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, "map_art/burned_penny.png");

    public static boolean isMap(ItemStack stack) {
        return stack.is(Items.FILLED_MAP) && stack.getTag() != null && stack.getTag().getBoolean(TAG);
    }

    /** Persisted player data survives death, which matters because refusing him usually kills you. */
    private static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int getRefusals(Player p) {
        return persisted(p).getInt(REFUSALS);
    }

    public static void setRefusals(Player p, int n) {
        persisted(p).putInt(REFUSALS, Math.max(0, n));
    }

    public static void resetRefusals(Player p) {
        setRefusals(p, 0);
    }

    /**
     * Called each time a player refuses to hand over a penny while he begs.
     * @return true if this refusal earned the map (then he does NOT charge)
     */
    public static boolean onRefusal(SebastianEntity seb, ServerPlayer p) {
        if (!SebConfig.BOSS_FIGHT.get()) {
            return false;
        }
        int n = getRefusals(p) + 1;
        if (n < SebConfig.REFUSALS_FOR_MAP.get()) {
            setRefusals(p, n);
            return false;
        }
        setRefusals(p, 0);
        SebChat.say(seb, "..." + numberWord(n) + ".");
        SebChat.say(seb, "keep it, then. i hope it was worth it.");
        ItemStack map = create(p.server);
        if (!p.getInventory().add(map)) {
            p.drop(map, false);
        }
        p.playNotifySound(ModSounds.STING.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
        SebChat.hint(p, "(Something was slipped into your pocket: Exhibit A. Show it to him by right-clicking him with it. If you dare.)");
        SebAdvancements.award(p, "evidence");
        return true;
    }

    private static String numberWord(int n) {
        String[] w = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
        return n < w.length ? w[n] : Integer.toString(n);
    }

    /** Creates the map item. The map picture is painted once per world and reused. */
    public static ItemStack create(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BossData data = BossData.get(server);
        MapItemSavedData md = data.mapId >= 0 ? overworld.getMapData(MapItem.makeKey(data.mapId)) : null;
        if (md == null) {
            int id = overworld.getFreeMapId();
            md = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, Level.OVERWORLD).locked();
            paint(md, server);
            overworld.setMapData(MapItem.makeKey(id), md);
            data.mapId = id;
            data.setDirty();
        }
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("map", data.mapId);
        tag.putBoolean(TAG, true);
        stack.setHoverName(Component.translatable("item." + SebHorrorMod.MODID + ".burned_penny_map")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("item." + SebHorrorMod.MODID + ".burned_penny_map.lore")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))));
        display.put("Lore", lore);
        return stack;
    }

    /** Converts the 128x128 PNG into the nearest Minecraft map colours. */
    static void paint(MapItemSavedData md, MinecraftServer server) {
        BufferedImage img;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(ART).open()) {
            img = ImageIO.read(in);
        } catch (Exception e) {
            LOG.error("Could not read the Exhibit A map art", e);
            return;
        }
        List<int[]> palette = new ArrayList<>(); // {r, g, b, packedColour}
        for (int id = 1; id < 64; id++) {
            MapColor c = MapColor.byId(id);
            if (c == null || c == MapColor.NONE) continue;
            for (MapColor.Brightness b : MapColor.Brightness.values()) {
                int r = (c.col >> 16 & 255) * b.modifier / 255;
                int g = (c.col >> 8 & 255) * b.modifier / 255;
                int bl = (c.col & 255) * b.modifier / 255;
                palette.add(new int[]{r, g, bl, id * 4 + b.id});
            }
        }
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int rgb = img.getRGB(x * img.getWidth() / 128, y * img.getHeight() / 128);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                int best = 0;
                long bestD = Long.MAX_VALUE;
                for (int[] p : palette) {
                    long dr = r - p[0], dg = g - p[1], db = b - p[2];
                    long d = 3 * dr * dr + 4 * dg * dg + 2 * db * db;
                    if (d < bestD) {
                        bestD = d;
                        best = p[3];
                    }
                }
                md.setColor(x, y, (byte) best);
            }
        }
    }

    private BurnedPennyMap() {
    }
}
