package com.perplexddev.shardtracker.notification;

/**
 * Screen corner used to position the notification stack.
 *
 * <p>The vertical fraction also decides the stacking direction: notifications anchored to a top
 * corner grow downwards, notifications anchored to a bottom corner grow upwards.
 */
public enum NotificationCorner {
    TOP_LEFT("Top Left", false, false),
    TOP_RIGHT("Top Right", true, false),
    BOTTOM_LEFT("Bottom Left", false, true),
    BOTTOM_RIGHT("Bottom Right", true, true);

    private final String displayName;
    private final boolean right;
    private final boolean bottom;

    NotificationCorner(String displayName, boolean right, boolean bottom) {
        this.displayName = displayName;
        this.right = right;
        this.bottom = bottom;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isBottom() {
        return bottom;
    }

    /** Notifications slide in from the nearest horizontal screen edge. */
    public int slideDirection() {
        return right ? 1 : -1;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
