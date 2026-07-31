package com.perplexddev.palantir.tracker;

/** One player in the current shard snapshot, ready for display. Faction is empty when unknown. */
public record ShardPlayerState(String displayName, String faction, boolean tracked) {
}
