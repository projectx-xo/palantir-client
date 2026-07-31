package com.perplexddev.palantir.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationLayoutTest {

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 300;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 28;
    private static final int OFFSET = 8;
    private static final int SPACING = 4;

    private static int y(NotificationCorner corner, int stackIndex) {
        return NotificationLayout.y(corner, OFFSET, HEIGHT, SCREEN_HEIGHT, stackIndex, SPACING);
    }

    @Test
    void leftCornersMeasureFromTheLeftEdge() {
        assertEquals(OFFSET, NotificationLayout.baseX(NotificationCorner.TOP_LEFT, OFFSET, WIDTH, SCREEN_WIDTH));
    }

    @Test
    void rightCornersMeasureFromTheRightEdge() {
        assertEquals(SCREEN_WIDTH - WIDTH - OFFSET,
                NotificationLayout.baseX(NotificationCorner.BOTTOM_RIGHT, OFFSET, WIDTH, SCREEN_WIDTH));
    }

    @Test
    void clampsHorizontallyWhenOffsetPushesItOffScreen() {
        int result = NotificationLayout.baseX(NotificationCorner.TOP_LEFT, 5000, WIDTH, SCREEN_WIDTH);

        assertEquals(SCREEN_WIDTH - WIDTH, result);
    }

    @Test
    void clampsToOriginWhenWiderThanScreen() {
        int result = NotificationLayout.baseX(NotificationCorner.TOP_RIGHT, OFFSET, SCREEN_WIDTH + 100, SCREEN_WIDTH);

        assertEquals(0, result);
    }

    @Test
    void topCornersStackDownwards() {
        assertEquals(OFFSET, y(NotificationCorner.TOP_LEFT, 0));
        assertEquals(OFFSET + HEIGHT + SPACING, y(NotificationCorner.TOP_LEFT, 1));
    }

    @Test
    void bottomCornersStackUpwards() {
        int first = y(NotificationCorner.BOTTOM_RIGHT, 0);
        int second = y(NotificationCorner.BOTTOM_RIGHT, 1);

        assertEquals(SCREEN_HEIGHT - HEIGHT - OFFSET, first);
        assertEquals(first - HEIGHT - SPACING, second);
    }

    @Test
    void clampsVerticallyWhenTheStackOverflowsTheScreen() {
        int result = y(NotificationCorner.TOP_LEFT, 100);

        assertTrue(result + HEIGHT <= SCREEN_HEIGHT, "expected on-screen, got " + result);
    }

    @Test
    void isFullyVisibleAtFullOpacity() {
        int slide = NotificationLayout.slideOffset(NotificationCorner.BOTTOM_RIGHT, 1.0f);

        assertEquals(0, slide);
    }

    @Test
    void slidesOutToTheRightForRightCorners() {
        int slide = NotificationLayout.slideOffset(NotificationCorner.BOTTOM_RIGHT, 0.0f);

        assertTrue(slide > 0, "right-corner notifications should slide towards the right edge");
    }

    @Test
    void slidesOutToTheLeftForLeftCorners() {
        int slide = NotificationLayout.slideOffset(NotificationCorner.TOP_LEFT, 0.0f);

        assertTrue(slide < 0, "left-corner notifications should slide towards the left edge");
    }

    @Test
    void slideShrinksAsOpacityRises() {
        int early = Math.abs(NotificationLayout.slideOffset(NotificationCorner.TOP_LEFT, 0.25f));
        int late = Math.abs(NotificationLayout.slideOffset(NotificationCorner.TOP_LEFT, 0.75f));

        assertTrue(early > late, "slide " + early + " should exceed " + late);
    }
}
