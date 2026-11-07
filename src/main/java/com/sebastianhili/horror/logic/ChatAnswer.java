package com.sebastianhili.horror.logic;

import java.util.Locale;

/** A player's typed reply to "do you have a penny?". Pure logic, no Minecraft classes. */
public enum ChatAnswer {
    YES, NO, NOT_AN_ANSWER;

    /**
     * Parses a chat line. Only "yes" and "no" count (any capitalisation, surrounding spaces
     * and trailing punctuation like "No." or "YES!!" are fine). Anything else is ignored.
     */
    public static ChatAnswer parse(String raw) {
        if (raw == null) {
            return NOT_AN_ANSWER;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[\\p{Punct}\\s]+$", "");
        s = s.replaceAll("^[\\p{Punct}\\s]+", "");
        return switch (s) {
            case "yes" -> YES;
            case "no" -> NO;
            default -> NOT_AN_ANSWER;
        };
    }
}
