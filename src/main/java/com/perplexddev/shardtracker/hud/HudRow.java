package com.perplexddev.shardtracker.hud;

/**
 * One render-ready line of the HUD panel.
 *
 * @param text   the fully composed line, including any tracked-player marker
 * @param color  ARGB colour resolved from the configuration at build time
 * @param player whether this line represents a player rather than a heading or summary
 */
public record HudRow(String text, int color, boolean player) {
}
