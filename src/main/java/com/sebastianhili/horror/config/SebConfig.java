package com.sebastianhili.horror.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * config/sebastianhilihorror-common.toml. On a server only the server's copy matters,
 * because every bit of Sebastian's behaviour runs server-side.
 */
public final class SebConfig {
    public static final ForgeConfigSpec SPEC;

    // features
    public static final BooleanValue BEGGING;
    public static final BooleanValue CHARGING;
    public static final BooleanValue RAGE;
    public static final BooleanValue RUMMAGING;
    public static final BooleanValue INVENTORY_SEARCH;
    public static final BooleanValue NATURAL_SPAWNING;
    public static final BooleanValue RELOCATION;
    public static final BooleanValue FAKE_JOIN_MESSAGES;
    public static final BooleanValue RAGE_THUNDER;
    public static final BooleanValue HINTS;
    public static final BooleanValue RETALIATION;
    public static final BooleanValue WHISPERS;
    public static final BooleanValue WAKE_UP_SCARE;
    public static final BooleanValue MIMICRY;
    public static final BooleanValue SPARE_CREATIVE;

    // timers (seconds unless stated)
    public static final IntValue BEG_TIMEOUT;
    public static final IntValue BEG_COOLDOWN;
    public static final IntValue SEARCH_MIN_MINUTES;
    public static final IntValue SEARCH_MAX_MINUTES;
    public static final IntValue ANSWER_TIMEOUT;
    public static final IntValue RUMMAGE_MIN;
    public static final IntValue RUMMAGE_MAX;
    public static final IntValue RELOCATE_MIN;
    public static final IntValue RELOCATE_MAX;
    public static final IntValue RAGE_WINDUP;
    public static final IntValue CHARGE_GIVE_UP;

    // ranges (blocks)
    public static final DoubleValue WATCH_MIN;
    public static final DoubleValue WATCH_MAX;
    public static final DoubleValue BEG_RANGE;
    public static final DoubleValue BEG_RUN_DISTANCE;
    public static final DoubleValue RAGE_RANGE;
    public static final IntValue RUMMAGE_RADIUS;
    public static final IntValue CHAT_RADIUS;

    // difficulty
    public static final DoubleValue MAX_HEALTH;
    public static final DoubleValue ARMOR;
    public static final DoubleValue WALK_SPEED;
    public static final DoubleValue CHARGE_SPEED;
    public static final IntValue RETALIATE_HITS;
    public static final DoubleValue REGEN_PER_SECOND;
    public static final DoubleValue TELEPORT_ON_HIT_CHANCE;

    // spawning
    public static final IntValue FIRST_ARRIVAL_MINUTES;
    public static final IntValue RETURN_AFTER_DEATH_MINUTES;
    public static final DoubleValue ARRIVAL_CHANCE_PER_MINUTE;
    public static final IntValue MAX_NATURAL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Turn Sebastian's behaviours on or off").push("features");
        BEGGING = b.comment("He walks up and begs when you hold a penny").define("begging", true);
        CHARGING = b.comment("He charges through (and kills) players who refuse him").define("charging", true);
        RAGE = b.comment("Burning a penny where he can see it makes him kill every player in the dimension").define("rage", true);
        RUMMAGING = b.comment("He searches nearby chests/barrels/containers and removes ONLY pennies").define("rummaging", true);
        INVENTORY_SEARCH = b.comment("Every few minutes he asks a player if they have a penny (answer yes/no in chat)").define("inventorySearch", true);
        NATURAL_SPAWNING = b.comment("He arrives on his own in the Overworld (only one natural Sebastian at a time)").define("naturalSpawning", true);
        RELOCATION = b.comment("He silently reappears somewhere new when you look away").define("relocation", true);
        FAKE_JOIN_MESSAGES = b.comment("Show 'Sebastian Hili joined/left the game' messages").define("fakeJoinMessages", true);
        RAGE_THUNDER = b.comment("Rage turns the weather into a thunderstorm").define("rageThunder", true);
        HINTS = b.comment("Small grey hints telling players how to answer / give pennies").define("hints", true);
        RETALIATION = b.comment("Hitting him repeatedly makes him charge you").define("retaliation", true);
        WHISPERS = b.comment("Private whispers and distant whisper sounds while he watches").define("whispers", true);
        WAKE_UP_SCARE = b.comment("Sometimes he is standing next to your bed when you wake up").define("wakeUpScare", true);
        MIMICRY = b.comment("He copies the watched player's held item and sneaking").define("mimicry", true);
        SPARE_CREATIVE = b.comment("Creative/spectator players are never killed").define("spareCreativePlayers", true);
        b.pop();

        b.comment("Timers").push("timers");
        BEG_TIMEOUT = b.comment("Seconds a player has to hand over the penny while he begs").defineInRange("begTimeoutSeconds", 10, 3, 120);
        BEG_COOLDOWN = b.comment("Seconds before he begs the same player again after being given a penny").defineInRange("begCooldownSeconds", 45, 0, 3600);
        SEARCH_MIN_MINUTES = b.comment("Minimum minutes between inventory searches").defineInRange("searchIntervalMinMinutes", 5, 1, 600);
        SEARCH_MAX_MINUTES = b.comment("Maximum minutes between inventory searches").defineInRange("searchIntervalMaxMinutes", 10, 1, 600);
        ANSWER_TIMEOUT = b.comment("Seconds a player has to answer yes/no").defineInRange("answerTimeoutSeconds", 30, 5, 300);
        RUMMAGE_MIN = b.comment("Minimum seconds between container rummages").defineInRange("rummageIntervalMinSeconds", 60, 5, 3600);
        RUMMAGE_MAX = b.comment("Maximum seconds between container rummages").defineInRange("rummageIntervalMaxSeconds", 180, 5, 3600);
        RELOCATE_MIN = b.comment("Minimum seconds between 'look away and he moved' relocations").defineInRange("relocateIntervalMinSeconds", 15, 3, 3600);
        RELOCATE_MAX = b.comment("Maximum seconds between relocations").defineInRange("relocateIntervalMaxSeconds", 40, 3, 3600);
        RAGE_WINDUP = b.comment("Seconds he screams before the rage killing spree starts").defineInRange("rageWindupSeconds", 3, 0, 30);
        CHARGE_GIVE_UP = b.comment("Seconds of chasing before he blinks right behind his victim").defineInRange("chargeBlinkSeconds", 12, 2, 120);
        b.pop();

        b.comment("Ranges in blocks").push("ranges");
        WATCH_MIN = b.comment("He backs away if you get closer than this while he watches").defineInRange("watchMinDistance", 12.0, 2.0, 64.0);
        WATCH_MAX = b.comment("He walks closer if you get further than this").defineInRange("watchMaxDistance", 24.0, 4.0, 96.0);
        BEG_RANGE = b.comment("How close a penny holder must be (with line of sight) for him to beg").defineInRange("begRange", 16.0, 2.0, 64.0);
        BEG_RUN_DISTANCE = b.comment("Running further than this away from him while he begs counts as running").defineInRange("begRunDistance", 22.0, 4.0, 128.0);
        RAGE_RANGE = b.comment("How close a burning penny must be (with line of sight) to enrage him").defineInRange("rageRange", 32.0, 2.0, 128.0);
        RUMMAGE_RADIUS = b.comment("How far he looks for containers to rummage").defineInRange("rummageRadius", 20, 2, 64);
        CHAT_RADIUS = b.comment("Who sees his chat. 0 = every player on the server (like a real player)").defineInRange("chatRadius", 0, 0, 100000);
        b.pop();

        b.comment("Difficulty").push("difficulty");
        MAX_HEALTH = b.comment("Max health (players have 20)").defineInRange("maxHealth", 300.0, 1.0, 10000.0);
        ARMOR = b.comment("Armor points (diamond armor is 20)").defineInRange("armor", 12.0, 0.0, 30.0);
        WALK_SPEED = b.comment("Movement speed attribute (zombie = 0.23)").defineInRange("walkSpeed", 0.3, 0.05, 1.0);
        CHARGE_SPEED = b.comment("Speed multiplier while charging").defineInRange("chargeSpeedMultiplier", 2.1, 1.0, 5.0);
        RETALIATE_HITS = b.comment("Hits from one player within 15 seconds before he charges them").defineInRange("retaliateHits", 5, 1, 1000);
        REGEN_PER_SECOND = b.comment("Health regenerated per second").defineInRange("regenPerSecond", 0.5, 0.0, 100.0);
        TELEPORT_ON_HIT_CHANCE = b.comment("Chance he blinks away when hit").defineInRange("teleportOnHitChance", 0.25, 0.0, 1.0);
        b.pop();

        b.comment("Natural arrival (Overworld only)").push("spawning");
        FIRST_ARRIVAL_MINUTES = b.comment("World play-time (minutes) before he can first arrive").defineInRange("firstArrivalMinutes", 5, 0, 100000);
        RETURN_AFTER_DEATH_MINUTES = b.comment("Minutes before he comes back after being killed").defineInRange("returnAfterDeathMinutes", 20, 0, 100000);
        ARRIVAL_CHANCE_PER_MINUTE = b.comment("Chance each minute that he arrives when he isn't around").defineInRange("arrivalChancePerMinute", 0.25, 0.0, 1.0);
        MAX_NATURAL = b.comment("How many naturally arrived Sebastians may exist (spawn eggs/commands ignore this)").defineInRange("maxNaturalSebastians", 1, 0, 16);
        b.pop();

        SPEC = b.build();
    }

    private SebConfig() {
    }

    public static int secondsToTicks(IntValue v) {
        return v.get() * 20;
    }
}
