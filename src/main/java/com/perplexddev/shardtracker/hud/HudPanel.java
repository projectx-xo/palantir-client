package com.perplexddev.shardtracker.hud;

import java.util.List;

/**
 * Immutable, render-ready HUD contents.
 *
 * <p>Rebuilt only when the shard snapshot or the configuration changes, so the render callback does
 * no text measurement or list building of its own.
 */
public record HudPanel(List<HudRow> rows, int width, int height) {

    public static final HudPanel EMPTY = new HudPanel(List.of(), 0, 0);

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
