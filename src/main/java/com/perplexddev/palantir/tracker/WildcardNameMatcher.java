package com.perplexddev.palantir.tracker;

import com.perplexddev.palantir.util.PlayerNameUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable, normalised view of a configured list of wildcard-capable patterns -- used for ignored
 * usernames, tracked factions, and ignored factions alike.
 *
 * <p>A pattern with no {@code *} is an exact, case-insensitive match, checked with an O(1)
 * {@link HashSet} lookup. A pattern containing {@code *} is compiled once into a regex (any number
 * of characters at each {@code *}) and checked only for the patterns that actually use one, so a
 * configuration with no wildcards costs the same as {@link TrackedPlayers}.
 */
public final class WildcardNameMatcher {

    public static final WildcardNameMatcher EMPTY = new WildcardNameMatcher(Set.of(), List.of());

    private final Set<String> exactNames;
    private final List<Pattern> wildcardPatterns;

    private WildcardNameMatcher(Set<String> exactNames, List<Pattern> wildcardPatterns) {
        this.exactNames = exactNames;
        this.wildcardPatterns = wildcardPatterns;
    }

    public static WildcardNameMatcher of(Collection<String> rawPatterns) {
        if (rawPatterns == null || rawPatterns.isEmpty()) {
            return EMPTY;
        }

        Set<String> exact = new HashSet<>();
        Set<String> wildcardSources = new LinkedHashSet<>();
        for (String raw : rawPatterns) {
            String normalized = PlayerNameUtil.normalize(raw);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.indexOf('*') >= 0) {
                wildcardSources.add(normalized);
            } else {
                exact.add(normalized);
            }
        }

        if (exact.isEmpty() && wildcardSources.isEmpty()) {
            return EMPTY;
        }

        List<Pattern> compiled = new ArrayList<>(wildcardSources.size());
        for (String source : wildcardSources) {
            compiled.add(compileWildcard(source));
        }

        return new WildcardNameMatcher(Set.copyOf(exact), List.copyOf(compiled));
    }

    private static Pattern compileWildcard(String normalizedPattern) {
        String[] parts = normalizedPattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parts[i]));
        }
        return Pattern.compile(regex.toString());
    }

    /** Match against an already-normalised username or faction, for the per-tick detection pass. */
    public boolean matches(String normalizedValue) {
        if (exactNames.contains(normalizedValue)) {
            return true;
        }
        for (Pattern pattern : wildcardPatterns) {
            if (pattern.matcher(normalizedValue).matches()) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return exactNames.size() + wildcardPatterns.size();
    }

    public boolean isEmpty() {
        return exactNames.isEmpty() && wildcardPatterns.isEmpty();
    }
}
