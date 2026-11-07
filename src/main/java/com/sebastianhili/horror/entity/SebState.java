package com.sebastianhili.horror.entity;

import java.util.Locale;

/** Sebastian's behaviour states. Exactly one is active at a time; see SebastianEntity#customServerAiStep. */
public enum SebState {
    WATCHING,
    BEGGING,
    CHARGING,
    RAGE,
    RUMMAGING,
    SEARCHING;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SebState byId(int id) {
        SebState[] v = values();
        return id >= 0 && id < v.length ? v[id] : WATCHING;
    }
}
