package com.sebastianhili.horror.logic;

/** Distance keeping while WATCHING. Pure logic. */
public final class WatchRules {
    private WatchRules() {
    }

    public enum Move { APPROACH, RETREAT, HOLD, CATCH_UP }

    /**
     * @param distance current distance to the watched player
     * @param min      preferred minimum distance (he backs away if closer)
     * @param max      preferred maximum distance (he walks closer if further)
     * @param catchUp  beyond this he teleports to catch up (when unseen)
     */
    public static Move decide(double distance, double min, double max, double catchUp) {
        if (distance > catchUp) {
            return Move.CATCH_UP;
        }
        if (distance < min) {
            return Move.RETREAT;
        }
        if (distance > max) {
            return Move.APPROACH;
        }
        return Move.HOLD;
    }
}
