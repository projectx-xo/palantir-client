package com.perplexddev.palantir.notification;

/**
 * Corner-relative placement and stacking of notifications, clamped to the scaled screen.
 *
 * <p>The clamped base position is kept separate from the slide offset: clamping guarantees a
 * configured offset can never push a notification off-screen, while the slide is deliberately
 * allowed to carry it past the edge as it animates in and out.
 */
public final class NotificationLayout {

    /** How far a notification travels while sliding in or out, in scaled pixels. */
    public static final int SLIDE_DISTANCE = 12;

    private NotificationLayout() {
    }

    public static int baseX(NotificationCorner corner, int offsetX, int width, int screenWidth) {
        int aligned = corner.isRight() ? screenWidth - width - offsetX : offsetX;
        return clamp(aligned, screenWidth - width);
    }

    public static int slideOffset(NotificationCorner corner, float opacity) {
        float hidden = 1.0f - Math.max(0.0f, Math.min(1.0f, opacity));
        return Math.round(SLIDE_DISTANCE * hidden * corner.slideDirection());
    }

    public static int y(NotificationCorner corner, int offsetY, int height, int screenHeight,
                        int stackIndex, int spacing) {
        int step = height + spacing;
        int aligned = corner.isBottom()
                ? screenHeight - height - offsetY - stackIndex * step
                : offsetY + stackIndex * step;
        return clamp(aligned, screenHeight - height);
    }

    private static int clamp(int value, int maxPosition) {
        if (maxPosition <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxPosition, value));
    }
}
