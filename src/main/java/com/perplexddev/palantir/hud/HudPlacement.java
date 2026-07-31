package com.perplexddev.palantir.hud;

/**
 * Resolves a freely-dragged panel position back into an anchor and offset.
 *
 * <p>The panel's centre decides which of the nine anchors it snaps to: the screen is divided into
 * thirds along each axis, and the anchor whose grid cell contains the panel's centre is chosen.
 * The offset is then the exact inverse of {@link HudLayout}'s placement formula, so resolving a
 * position and feeding the result back through {@code HudLayout} reproduces the same coordinates.
 */
public final class HudPlacement {

    private HudPlacement() {
    }

    public record Resolved(HudAnchor anchor, int offsetX, int offsetY) {
    }

    private static final HudAnchor[][] ANCHOR_GRID = {
            {HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT},
            {HudAnchor.CENTER_LEFT, HudAnchor.CENTER, HudAnchor.CENTER_RIGHT},
            {HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_RIGHT}
    };

    public static Resolved resolve(int x, int y, int panelWidth, int panelHeight,
                                   int screenWidth, int screenHeight) {
        HudAnchor anchor = nearestAnchor(x, y, panelWidth, panelHeight, screenWidth, screenHeight);
        int offsetX = inverseOffset(anchor.horizontalFraction(), anchor.horizontalOffsetSign(),
                x, panelWidth, screenWidth);
        int offsetY = inverseOffset(anchor.verticalFraction(), anchor.verticalOffsetSign(),
                y, panelHeight, screenHeight);
        return new Resolved(anchor, offsetX, offsetY);
    }

    private static HudAnchor nearestAnchor(int x, int y, int panelWidth, int panelHeight,
                                           int screenWidth, int screenHeight) {
        int column = third(centerFraction(x, panelWidth, screenWidth));
        int row = third(centerFraction(y, panelHeight, screenHeight));
        return ANCHOR_GRID[row][column];
    }

    private static double centerFraction(int position, int panelSize, int screenSize) {
        if (screenSize <= panelSize) {
            return 0.5;
        }
        return (position + panelSize / 2.0) / screenSize;
    }

    private static int third(double fraction) {
        if (fraction < 1.0 / 3.0) {
            return 0;
        }
        if (fraction < 2.0 / 3.0) {
            return 1;
        }
        return 2;
    }

    private static int inverseOffset(float fraction, int sign, int position, int panelSize, int screenSize) {
        int aligned = Math.round((screenSize - panelSize) * fraction);
        return Math.round((position - aligned) * sign);
    }
}
