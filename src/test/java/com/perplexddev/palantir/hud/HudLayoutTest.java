package com.perplexddev.palantir.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 300;
    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 60;

    private static int x(HudAnchor anchor, int offset) {
        return HudLayout.x(anchor, offset, PANEL_WIDTH, SCREEN_WIDTH);
    }

    private static int y(HudAnchor anchor, int offset) {
        return HudLayout.y(anchor, offset, PANEL_HEIGHT, SCREEN_HEIGHT);
    }

    @Test
    void leftAnchorsMeasureOffsetFromLeftEdge() {
        assertEquals(8, x(HudAnchor.TOP_LEFT, 8));
        assertEquals(8, x(HudAnchor.CENTER_LEFT, 8));
        assertEquals(8, x(HudAnchor.BOTTOM_LEFT, 8));
    }

    @Test
    void rightAnchorsMeasureOffsetFromRightEdge() {
        assertEquals(SCREEN_WIDTH - PANEL_WIDTH - 8, x(HudAnchor.TOP_RIGHT, 8));
        assertEquals(SCREEN_WIDTH - PANEL_WIDTH - 8, x(HudAnchor.BOTTOM_RIGHT, 8));
    }

    @Test
    void centreAnchorsCentreThePanel() {
        assertEquals((SCREEN_WIDTH - PANEL_WIDTH) / 2, x(HudAnchor.TOP_CENTER, 0));
        assertEquals((SCREEN_HEIGHT - PANEL_HEIGHT) / 2, y(HudAnchor.CENTER, 0));
    }

    @Test
    void topAnchorsMeasureOffsetFromTopEdge() {
        assertEquals(6, y(HudAnchor.TOP_LEFT, 6));
    }

    @Test
    void bottomAnchorsMeasureOffsetFromBottomEdge() {
        assertEquals(SCREEN_HEIGHT - PANEL_HEIGHT - 6, y(HudAnchor.BOTTOM_LEFT, 6));
    }

    @Test
    void clampsPanelInsideScreenWhenOffsetPushesItPastTheRightEdge() {
        int result = x(HudAnchor.TOP_LEFT, 5000);

        assertEquals(SCREEN_WIDTH - PANEL_WIDTH, result);
    }

    @Test
    void clampsPanelInsideScreenWhenOffsetPushesItPastTheTopEdge() {
        int result = y(HudAnchor.BOTTOM_LEFT, 5000);

        assertEquals(0, result);
    }

    @Test
    void clampsToOriginWhenPanelIsWiderThanScreen() {
        int result = HudLayout.x(HudAnchor.TOP_RIGHT, 8, SCREEN_WIDTH + 200, SCREEN_WIDTH);

        assertEquals(0, result);
    }

    @Test
    void clampsToOriginWhenPanelIsTallerThanScreen() {
        int result = HudLayout.y(HudAnchor.BOTTOM_LEFT, 8, SCREEN_HEIGHT + 200, SCREEN_HEIGHT);

        assertEquals(0, result);
    }

    @Test
    void everyAnchorKeepsThePanelOnScreen() {
        for (HudAnchor anchor : HudAnchor.values()) {
            int px = x(anchor, 8);
            int py = y(anchor, 8);

            assertTrue(px >= 0 && px + PANEL_WIDTH <= SCREEN_WIDTH, anchor + " x=" + px);
            assertTrue(py >= 0 && py + PANEL_HEIGHT <= SCREEN_HEIGHT, anchor + " y=" + py);
        }
    }
}
