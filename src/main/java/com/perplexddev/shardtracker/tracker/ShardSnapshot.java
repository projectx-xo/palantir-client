package com.perplexddev.shardtracker.tracker;

import java.util.List;

/**
 * Immutable view of who is currently in the shard.
 *
 * <p>Rebuilt only when membership actually changes, so renderers can cache derived layout against
 * snapshot identity.
 */
public final class ShardSnapshot {

    public static final ShardSnapshot EMPTY = new ShardSnapshot(List.of(), 0);

    private final List<ShardPlayerState> players;
    private final int trackedCount;

    public ShardSnapshot(List<ShardPlayerState> players, int trackedCount) {
        this.players = players;
        this.trackedCount = trackedCount;
    }

    /** Players in tab-list order. */
    public List<ShardPlayerState> players() {
        return players;
    }

    public int totalCount() {
        return players.size();
    }

    public int trackedCount() {
        return trackedCount;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }
}
