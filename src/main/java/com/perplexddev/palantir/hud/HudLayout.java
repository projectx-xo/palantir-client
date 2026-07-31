package com.perplexddev.palantir.hud;

/**
 * Anchor-relative placement of the HUD panel, clamped to the scaled screen.
 *
 * <p>Both axes use the same formula, so no anchor is special-cased: the panel is aligned to the
 * anchor's fraction of the screen, nudged inwards by the configured offset, then clamped so it can
 * never render partially off-screen.
 */
public final class HudLayout {

    private HudLayout() {
    }

    public static int x(HudAnchor anchor, int offsetX, int panelWidth, int screenWidth) {
        return place(anchor.horizontalFraction(), anchor.horizontalOffsetSign(),
                offsetX, panelWidth, screenWidth);
    }

    public static int y(HudAnchor anchor, int offsetY, int panelHeight, int screenHeight) {
        return place(anchor.verticalFraction(), anchor.verticalOffsetSign(),
                offsetY, panelHeight, screenHeight);
    }

    private static int place(float fraction, int offsetSign, int offset, int panelSize, int screenSize) {
        int aligned = Math.round((screenSize - panelSize) * fraction);
        return clamp(aligned + offset * offsetSign, screenSize - panelSize);
    }

    private static int clamp(int value, int maxPosition) {
        if (maxPosition <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxPosition, value));
    }
}
