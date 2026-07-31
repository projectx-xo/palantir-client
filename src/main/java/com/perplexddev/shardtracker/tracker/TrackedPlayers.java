package com.perplexddev.shardtracker.tracker;

import com.perplexddev.shardtracker.util.PlayerNameUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, normalised view of the configured tracked-player names.
 *
 * <p>Built once whenever the configuration changes so the per-tick detection pass only has to do
 * {@link HashSet} lookups against already-normalised keys.
 */
public final class TrackedPlayers {

    public static final TrackedPlayers EMPTY = new TrackedPlayers(Set.of(), List.of());

    private final Set<String> normalizedNames;
    private final List<String> displayNames;

    private TrackedPlayers(Set<String> normalizedNames, List<String> displayNames) {
        this.normalizedNames = normalizedNames;
        this.displayNames = displayNames;
    }

    /**
     * Normalises raw configured names: blank entries are dropped, surrounding whitespace trimmed,
     * and case-insensitive duplicates collapsed keeping the first spelling for display.
     */
    public static TrackedPlayers of(Collection<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return EMPTY;
        }

        Map<String, String> byNormalized = new LinkedHashMap<>();
        for (String raw : rawNames) {
            String normalized = PlayerNameUtil.normalize(raw);
            if (normalized.isEmpty()) {
                continue;
            }
            byNormalized.putIfAbsent(normalized, PlayerNameUtil.display(raw));
        }

        if (byNormalized.isEmpty()) {
            return EMPTY;
        }

        return new TrackedPlayers(
                Collections.unmodifiableSet(new HashSet<>(byNormalized.keySet())),
                List.copyOf(byNormalized.values()));
    }

    /** Case-insensitive match against a raw username. */
    public boolean contains(String rawName) {
        return containsNormalized(PlayerNameUtil.normalize(rawName));
    }

    /** Match against an already-normalised username, for hot paths that pre-normalise. */
    public boolean containsNormalized(String normalizedName) {
        return normalizedNames.contains(normalizedName);
    }

    public int size() {
        return normalizedNames.size();
    }

    public boolean isEmpty() {
        return normalizedNames.isEmpty();
    }

    /** Configured names in configuration order, with their original capitalisation. */
    public List<String> displayNames() {
        return displayNames;
    }
}
