package com.sebastianhili.horror.entity;

import net.minecraft.util.RandomSource;

/** Everything Sebastian says. He types in lowercase, mostly. */
public final class SebLines {
    public static final String[] BEG_START = {
            "is that a penny?", "oh. oh, you have a penny.", "hey. hey. that's a penny, right?",
            "can i see that penny?", "you're holding a penny."};
    public static final String[] BEG_PLEAD = {
            "can i have it?", "please. just the one.", "i'll take good care of it.", "i collect them. you know that.",
            "it's only one cent. it's nothing to you.", "please please please", "just right click me with it. it's easy."};
    public static final String[] THANKS = {
            "thank you. thank you. thank you.", "oh. it's warm.", "i'll keep it safe. forever.",
            "you're my favourite now.", "one cent. perfect.", "i'm going to look at this one for a while."};
    public static final String[] CHARGE_TIMEOUT = {"fine. i'll take it.", "that's a no, then.", "time's up."};
    public static final String[] CHARGE_PUT_AWAY = {"why did you put it away?", "don't hide it from me."};
    public static final String[] CHARGE_RAN = {"don't run.", "you can't outrun me. nobody can.", "where are you going with my penny?"};
    public static final String[] CHARGE_KILL = {"got it.", "mine now.", "you should have just given it to me.",
            "sorry. it was only a penny."};
    public static final String[] TOO_LATE = {"too late.", "no. too late now."};
    public static final String[] RAGE_START = {"WHO BURNED IT", "THAT WAS A PENNY", "YOU MELTED IT. YOU MELTED IT.",
            "ONE CENT. IT WAS ONE CENT."};
    public static final String[] RAGE_KILL = {"next.", "who's next.", "everyone pays.", "you all watched."};
    public static final String[] RAGE_END = {"...okay. i feel better now.", "...sorry. i don't know what came over me.",
            "don't burn pennies."};
    public static final String[] SEARCH_ASK = {"hey. do you have a penny?", "do you have a penny? be honest.",
            "quick question. any pennies on you?", "do you have a penny? i won't be mad."};
    public static final String[] SEARCH_REMIND = {"well?", "yes or no.", "i asked you a question."};
    public static final String[] SEARCH_HONEST_YES = {"thank you. i'll just take one.", "honest. i like that."};
    public static final String[] SEARCH_HONEST_NO = {"okay.", "okay. i believe you.", "...okay. sorry to bother you."};
    public static final String[] SEARCH_LIE = {"liar.", "i can hear it jingling.", "you shouldn't lie to me."};
    public static final String[] SEARCH_FALSE_YES = {"you don't have one. why would you say that?", "that's not funny."};
    public static final String[] SEARCH_SILENT_HAS = {"fine. i'll look myself."};
    public static final String[] SEARCH_SILENT_EMPTY = {"...rude.", "you could have just said no."};
    public static final String[] RUMMAGE_FOUND = {"mine.", "there you are.", "who leaves pennies in a chest?"};
    public static final String[] HURT = {"ow.", "why would you do that?", "that hurt.", "stop.", "i only want pennies."};
    public static final String[] RETALIATE = {"okay. that's enough.", "you've made your choice."};
    public static final String[] DEATH = {"...my pennies.", "i'll be back. i always come back."};
    public static final String[] WHISPERS = {"i see you.", "you look away first.", "do you have a penny?",
            "i'm not going anywhere.", "you blinked.", "i like watching you work.", "nice glasses. oh wait.",
            "keep mining. zinc is good."};
    public static final String[] WAKE = {"good morning.", "you sleep with your mouth open.", "sweet dreams?"};

    public static String pick(RandomSource random, String[] lines) {
        return lines[random.nextInt(lines.length)];
    }

    private SebLines() {
    }
}
