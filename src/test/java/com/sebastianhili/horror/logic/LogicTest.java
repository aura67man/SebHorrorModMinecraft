package com.sebastianhili.horror.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain JUnit tests for the logic that doesn't need Minecraft running (./gradlew test). */
class LogicTest {
    @Test
    void parsesYesAndNoCaseInsensitively() {
        for (String s : new String[]{"yes", "YES", "Yes", "  yEs  ", "yes!", "yes.", "Yes?!"}) {
            assertEquals(ChatAnswer.YES, ChatAnswer.parse(s), s);
        }
        for (String s : new String[]{"no", "NO", "No", " no ", "no.", "NO!!!", "...no"}) {
            assertEquals(ChatAnswer.NO, ChatAnswer.parse(s), s);
        }
    }

    @Test
    void ignoresAnythingElse() {
        for (String s : new String[]{"", "   ", "nope", "yeah", "no way", "yes please", "y", "n", "know", "eyes", null}) {
            assertEquals(ChatAnswer.NOT_AN_ANSWER, ChatAnswer.parse(s), String.valueOf(s));
        }
    }

    @Test
    void lieDetectorTruthTable() {
        assertEquals(LieDetector.Verdict.HONEST_YES, LieDetector.judge(ChatAnswer.YES, true));
        assertEquals(LieDetector.Verdict.FALSE_YES, LieDetector.judge(ChatAnswer.YES, false));
        assertEquals(LieDetector.Verdict.HONEST_NO, LieDetector.judge(ChatAnswer.NO, false));
        assertEquals(LieDetector.Verdict.LYING_NO, LieDetector.judge(ChatAnswer.NO, true));
        assertEquals(LieDetector.Verdict.SILENT_WITH_PENNY, LieDetector.judgeTimeout(true));
        assertEquals(LieDetector.Verdict.SILENT_EMPTY, LieDetector.judgeTimeout(false));
    }

    @Test
    void burnRules() {
        assertTrue(BurnRules.shouldEnrage(true, true, 31 * 31, 32, true));
        assertFalse(BurnRules.shouldEnrage(true, true, 33 * 33, 32, true), "too far");
        assertFalse(BurnRules.shouldEnrage(true, true, 4, 32, false), "no line of sight");
        assertFalse(BurnRules.shouldEnrage(true, false, 4, 32, true), "not fire (cactus, explosion...)");
        assertFalse(BurnRules.shouldEnrage(false, true, 4, 32, true), "disabled in config");
    }

    @Test
    void watchDistanceKeeping() {
        assertEquals(WatchRules.Move.RETREAT, WatchRules.decide(5, 12, 24, 50));
        assertEquals(WatchRules.Move.HOLD, WatchRules.decide(18, 12, 24, 50));
        assertEquals(WatchRules.Move.APPROACH, WatchRules.decide(30, 12, 24, 50));
        assertEquals(WatchRules.Move.CATCH_UP, WatchRules.decide(80, 12, 24, 50));
    }
}
