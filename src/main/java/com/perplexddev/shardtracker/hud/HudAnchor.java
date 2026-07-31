package com.perplexddev.shardtracker.hud;

/**
 * Screen anchor used to position the HUD panel.
 *
 * <p>Each anchor carries the fraction of the screen it attaches to and the fraction of the panel
 * that is aligned to that point, so positioning is a single multiply-add per axis with no
 * per-anchor branching.
 */
public enum HudAnchor {
    TOP_LEFT("Top Left", 0.0f, 0.0f),
    TOP_CENTER("Top Center", 0.5f, 0.0f),
    TOP_RIGHT("Top Right", 1.0f, 0.0f),
    CENTER_LEFT("Center Left", 0.0f, 0.5f),
    CENTER("Center", 0.5f, 0.5f),
    CENTER_RIGHT("Center Right", 1.0f, 0.5f),
    BOTTOM_LEFT("Bottom Left", 0.0f, 1.0f),
    BOTTOM_CENTER("Bottom Center", 0.5f, 1.0f),
    BOTTOM_RIGHT("Bottom Right", 1.0f, 1.0f);

    private final String displayName;
    private final float horizontalFraction;
    private final float verticalFraction;

    HudAnchor(String displayName, float horizontalFraction, float verticalFraction) {
        this.displayName = displayName;
        this.horizontalFraction = horizontalFraction;
        this.verticalFraction = verticalFraction;
    }

    public float horizontalFraction() {
        return horizontalFraction;
    }

    public float verticalFraction() {
        return verticalFraction;
    }

    /** Offsets push the panel inwards, away from the edge the anchor sits on. */
    public int horizontalOffsetSign() {
        return horizontalFraction == 1.0f ? -1 : 1;
    }

    public int verticalOffsetSign() {
        return verticalFraction == 1.0f ? -1 : 1;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
