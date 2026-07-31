package com.perplexddev.palantir.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationAnimationTest {

    private static final long DURATION = 5000L;
    private static final long FADE_IN = 200L;
    private static final long FADE_OUT = 400L;

    private static float opacity(long elapsed) {
        return NotificationAnimation.opacity(elapsed, DURATION, FADE_IN, FADE_OUT);
    }

    @Test
    void startsTransparent() {
        assertEquals(0.0f, opacity(0), 0.001f);
    }

    @Test
    void rampsUpDuringFadeIn() {
        assertEquals(0.5f, opacity(100), 0.001f);
    }

    @Test
    void isFullyOpaqueBetweenFades() {
        assertEquals(1.0f, opacity(FADE_IN), 0.001f);
        assertEquals(1.0f, opacity(2500), 0.001f);
    }

    @Test
    void ramrpsDownDuringFadeOut() {
        assertEquals(0.5f, opacity(DURATION - FADE_OUT / 2), 0.001f);
    }

    @Test
    void isTransparentOnceExpired() {
        assertEquals(0.0f, opacity(DURATION), 0.001f);
        assertEquals(0.0f, opacity(DURATION + 1000), 0.001f);
    }

    @Test
    void handlesZeroFadeDurations() {
        assertEquals(1.0f, NotificationAnimation.opacity(0, DURATION, 0, 0), 0.001f);
        assertEquals(1.0f, NotificationAnimation.opacity(DURATION - 1, DURATION, 0, 0), 0.001f);
    }

    @Test
    void neverExceedsFullOpacityWhenFadesOverlap() {
        for (long elapsed = 0; elapsed <= 1000; elapsed += 50) {
            float value = NotificationAnimation.opacity(elapsed, 1000, 800, 800);

            assertTrue(value >= 0.0f && value <= 1.0f, "opacity out of range at " + elapsed + ": " + value);
        }
    }
}
