package com.sebastianhili.horror.logic;

/** Whether a destroyed penny item should send a Sebastian into RAGE. Pure logic. */
public final class BurnRules {
    private BurnRules() {
    }

    /**
     * @param fireDamage  the item was destroyed by fire, lava or another fire damage type
     * @param distanceSq  squared distance from Sebastian's eyes to the item
     * @param range       detection range in blocks
     * @param lineOfSight Sebastian can see the item
     */
    public static boolean shouldEnrage(boolean enabled, boolean fireDamage, double distanceSq, double range,
                                       boolean lineOfSight) {
        return enabled && fireDamage && distanceSq <= range * range && lineOfSight;
    }
}
