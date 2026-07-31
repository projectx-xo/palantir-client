package com.perplexddev.shardtracker.tracker;

import com.perplexddev.shardtracker.util.PlayerNameUtil;

/**
 * A single tab-list entry that the scanner considered to be in the shard.
 *
 * <p>The normalised name (and normalised faction, if any) are carried alongside their display
 * forms so the detection pass never has to re-normalise while comparing membership or matching
 * tracked/ignored rules.
 */
public record ObservedPlayer(String displayName, String normalizedName, boolean local,
                             String faction, String normalizedFaction) {

    /** For callers that don't care about faction, e.g. most existing tests. */
    public static ObservedPlayer of(String name, boolean local) {
        return of(name, local, "");
    }

    public static ObservedPlayer of(String name, boolean local, String faction) {
        return new ObservedPlayer(PlayerNameUtil.display(name), PlayerNameUtil.normalize(name), local,
                PlayerNameUtil.display(faction), PlayerNameUtil.normalize(faction));
    }
}
