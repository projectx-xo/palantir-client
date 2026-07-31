package com.perplexddev.shardtracker.tracker;

import java.util.List;

/**
 * Result of one detection pass.
 *
 * @param membershipChanged whether the shard membership differs from the previous pass
 * @param enteredTracked    tracked players that should raise a notification, in arrival order
 */
public record ShardUpdate(boolean membershipChanged, List<TrackedArrival> enteredTracked) {

    /** Shared instance for the common case, so an idle tick allocates nothing. */
    public static final ShardUpdate UNCHANGED = new ShardUpdate(false, List.of());

    public boolean hasNotifications() {
        return !enteredTracked.isEmpty();
    }

    /** A tracked player's arrival, with their faction if one is known (empty otherwise). */
    public record TrackedArrival(String displayName, String faction) {
    }
}
