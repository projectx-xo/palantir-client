package com.perplexddev.shardtracker.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudResizeMathTest {

    @Test
    void unchangedDistanceKeepsTheSameScale() {
        assertEquals(1.0f, HudResizeMath.resize(1.0f, 100, 100), 0.001f);
    }

    @Test
    void greaterDistanceIncreasesScaleProportionally() {
        assertEquals(2.0f, HudResizeMath.resize(1.0f, 100, 200), 0.001f);
    }

    @Test
    void smallerDistanceDecreasesScaleProportionally() {
        assertEquals(0.5f, HudResizeMath.resize(1.0f, 100, 50), 0.001f);
    }

    @Test
    void clampsToTheMaximumScale() {
        assertEquals(HudResizeMath.MAX_SCALE, HudResizeMath.resize(1.0f, 100, 100_000), 0.001f);
    }

    @Test
    void clampsToTheMinimumScale() {
        assertEquals(HudResizeMath.MIN_SCALE, HudResizeMath.resize(1.0f, 100, 1), 0.001f);
    }

    @Test
    void zeroStartDistanceReturnsTheUnchangedStartScale() {
        assertEquals(1.5f, HudResizeMath.resize(1.5f, 0, 500), 0.001f);
    }

    @Test
    void zeroStartDistanceStillClampsAnOutOfRangeStartScale() {
        assertEquals(HudResizeMath.MAX_SCALE, HudResizeMath.resize(10.0f, 0, 500), 0.001f);
    }
}
