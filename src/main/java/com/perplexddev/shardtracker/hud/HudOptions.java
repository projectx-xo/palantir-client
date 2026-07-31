package com.perplexddev.shardtracker.hud;

/**
 * Immutable HUD settings derived from the configuration.
 *
 * <p>Colours are already resolved to ARGB ints and the background alpha already has the configured
 * opacity applied, so rendering never parses or blends colours per frame.
 */
public record HudOptions(boolean enabled,
                         HudAnchor anchor,
                         int offsetX,
                         int offsetY,
                         float scaleX,
                         float scaleY,
                         boolean showTitle,
                         boolean showTotalCount,
                         boolean showTrackedCount,
                         boolean showPlayerList,
                         boolean showTrackedFirst,
                         int maxVisiblePlayers,
                         int backgroundColor,
                         int borderColor,
                         int titleColor,
                         int normalPlayerColor,
                         int trackedPlayerColor,
                         boolean roundedCorners,
                         boolean hideWhenEmpty,
                         boolean hideWithDebugScreen) {
}
