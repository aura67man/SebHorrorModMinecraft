package com.sebastianhili.horror.logic;

/** Decides what Sebastian does with an answer to "do you have a penny?". Pure logic. */
public final class LieDetector {
    private LieDetector() {
    }

    public enum Verdict {
        /** Said yes and has one: he takes exactly one penny. */
        HONEST_YES,
        /** Said yes but has none: he is annoyed (darkness + slowness), nobody dies. */
        FALSE_YES,
        /** Said no and really has none: nothing happens. */
        HONEST_NO,
        /** Said no but has a penny: real lightning, death. */
        LYING_NO,
        /** Ignored him and has a penny: he charges and takes it. */
        SILENT_WITH_PENNY,
        /** Ignored him and has none: he sulks off. */
        SILENT_EMPTY
    }

    public static Verdict judge(ChatAnswer answer, boolean hasPenny) {
        return switch (answer) {
            case YES -> hasPenny ? Verdict.HONEST_YES : Verdict.FALSE_YES;
            case NO -> hasPenny ? Verdict.LYING_NO : Verdict.HONEST_NO;
            case NOT_AN_ANSWER -> hasPenny ? Verdict.SILENT_WITH_PENNY : Verdict.SILENT_EMPTY;
        };
    }

    public static Verdict judgeTimeout(boolean hasPenny) {
        return judge(ChatAnswer.NOT_AN_ANSWER, hasPenny);
    }
}
