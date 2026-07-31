package com.perplexddev.shardtracker.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudPlacementTest {

    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 300;
    private static final int PANEL_WIDTH = 100;
    private static final int PANEL_HEIGHT = 60;

    @Test
    void resolvesTopLeftWhenPanelSitsInTheTopLeftCorner() {
        HudPlacement.Resolved resolved =
                HudPlacement.resolve(8, 8, PANEL_WIDTH, PANEL_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        assertEquals(HudAnchor.TOP_LEFT, resolved.anchor());
        assertEquals(8, resolved.offsetX());
        assertEquals(8, resolved.offsetY());
    }

    @Test
    void resolvesBottomRightWhenPanelSitsInTheBottomRightCorner() {
        int x = SCREEN_WIDTH - PANEL_WIDTH - 8;
        int y = SCREEN_HEIGHT - PANEL_HEIGHT - 8;

        HudPlacement.Resolved resolved =
                HudPlacement.resolve(x, y, PANEL_WIDTH, PANEL_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        assertEquals(HudAnchor.BOTTOM_RIGHT, resolved.anchor());
        assertEquals(8, resolved.offsetX());
        assertEquals(8, resolved.offsetY());
    }

    @Test
    void resolvesCenterWhenPanelIsCentered() {
        int x = (SCREEN_WIDTH - PANEL_WIDTH) / 2;
        int y = (SCREEN_HEIGHT - PANEL_HEIGHT) / 2;

        HudPlacement.Resolved resolved =
                HudPlacement.resolve(x, y, PANEL_WIDTH, PANEL_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        assertEquals(HudAnchor.CENTER, resolved.anchor());
        assertEquals(0, resolved.offsetX());
        assertEquals(0, resolved.offsetY());
    }

    @Test
    void fallsBackToCenterColumnWhenThePanelIsWiderThanTheScreen() {
        HudPlacement.Resolved resolved = HudPlacement.resolve(
                -50, 8, SCREEN_WIDTH + 200, PANEL_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

        assertEquals(HudAnchor.TOP_CENTER, resolved.anchor());
    }

    @Test
    void roundTripsThroughHudLayoutForEveryAnchor() {
        int offsetX = 10;
        int offsetY = 12;

        for (HudAnchor anchor : HudAnchor.values()) {
            int x = HudLayout.x(anchor, offsetX, PANEL_WIDTH, SCREEN_WIDTH);
            int y = HudLayout.y(anchor, offsetY, PANEL_HEIGHT, SCREEN_HEIGHT);

            HudPlacement.Resolved resolved =
                    HudPlacement.resolve(x, y, PANEL_WIDTH, PANEL_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);

            assertEquals(anchor, resolved.anchor(), "anchor mismatch for " + anchor);
            assertEquals(offsetX, resolved.offsetX(), "offsetX mismatch for " + anchor);
            assertEquals(offsetY, resolved.offsetY(), "offsetY mismatch for " + anchor);
        }
    }
}
