package com.perplexddev.palantir.hud;

import com.perplexddev.palantir.tracker.ShardPlayerState;
import com.perplexddev.palantir.tracker.ShardSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turns a shard snapshot into render-ready HUD rows.
 *
 * <p>Called only when the snapshot or the configuration changes. All text composition, ordering,
 * truncation and measurement happens here so the render callback just draws a prepared list.
 */
public final class HudPanelBuilder {

    public static final int HORIZONTAL_PADDING = 6;
    public static final int VERTICAL_PADDING = 5;
    public static final int LINE_HEIGHT = 10;

    private static final String TITLE = "PALANTIR CLIENT";
    private static final String TRACKED_MARKER = "★ ";
    private static final String UNTRACKED_MARKER = "  ";

    private HudPanelBuilder() {
    }

    public static HudPanel build(ShardSnapshot snapshot, HudOptions options, TextMeasurer measurer) {
        List<HudRow> header = buildHeader(snapshot, options);
        List<HudRow> players = options.showPlayerList()
                ? buildPlayerRows(snapshot, options)
                : List.of();

        if (header.isEmpty() && players.isEmpty()) {
            return HudPanel.EMPTY;
        }

        List<HudRow> rows = new ArrayList<>(header.size() + players.size() + 1);
        rows.addAll(header);
        if (!header.isEmpty() && !players.isEmpty()) {
            rows.add(new HudRow("", options.normalPlayerColor(), false));
        }
        rows.addAll(players);

        return new HudPanel(
                Collections.unmodifiableList(rows),
                measureWidth(rows, measurer),
                rows.size() * LINE_HEIGHT + VERTICAL_PADDING * 2);
    }

    private static List<HudRow> buildHeader(ShardSnapshot snapshot, HudOptions options) {
        List<HudRow> header = new ArrayList<>(3);
        if (options.showTitle()) {
            header.add(new HudRow(TITLE, options.titleColor(), false));
        }
        if (options.showTotalCount()) {
            int total = snapshot.totalCount();
            String word = total == 1 ? " player detected" : " players detected";
            header.add(new HudRow(total + word, options.normalPlayerColor(), false));
        }
        if (options.showTrackedCount()) {
            header.add(new HudRow(snapshot.trackedCount() + " tracked", options.trackedPlayerColor(), false));
        }
        return header;
    }

    private static List<HudRow> buildPlayerRows(ShardSnapshot snapshot, HudOptions options) {
        List<ShardPlayerState> ordered = orderPlayers(snapshot, options.showTrackedFirst());
        if (ordered.isEmpty()) {
            return List.of();
        }

        boolean unlimited = options.maxVisiblePlayers() <= 0;
        int visible = unlimited ? ordered.size() : Math.min(ordered.size(), options.maxVisiblePlayers());
        int hidden = ordered.size() - visible;

        List<HudRow> rows = new ArrayList<>(visible + 1);
        for (int i = 0; i < visible; i++) {
            ShardPlayerState player = ordered.get(i);
            String marker = player.tracked() ? TRACKED_MARKER : UNTRACKED_MARKER;
            String faction = player.faction().isEmpty() ? "" : " [" + player.faction() + "]";
            int color = player.tracked() ? options.trackedPlayerColor() : options.normalPlayerColor();
            rows.add(new HudRow(marker + player.displayName() + faction, color, true));
        }
        if (hidden > 0) {
            rows.add(new HudRow("+" + hidden + " more", options.normalPlayerColor(), false));
        }
        return rows;
    }

    /** Stable partition: tracked players keep tab order, then everyone else keeps tab order. */
    private static List<ShardPlayerState> orderPlayers(ShardSnapshot snapshot, boolean trackedFirst) {
        List<ShardPlayerState> players = snapshot.players();
        if (!trackedFirst || snapshot.trackedCount() == 0 || snapshot.trackedCount() == players.size()) {
            return players;
        }

        List<ShardPlayerState> ordered = new ArrayList<>(players.size());
        for (ShardPlayerState player : players) {
            if (player.tracked()) {
                ordered.add(player);
            }
        }
        for (ShardPlayerState player : players) {
            if (!player.tracked()) {
                ordered.add(player);
            }
        }
        return ordered;
    }

    private static int measureWidth(List<HudRow> rows, TextMeasurer measurer) {
        int widest = 0;
        for (HudRow row : rows) {
            widest = Math.max(widest, measurer.width(row.text()));
        }
        return widest + HORIZONTAL_PADDING * 2;
    }
}
