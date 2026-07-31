package com.perplexddev.palantir.tracker;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackedPlayersTest {

    @Test
    void matchesConfiguredNameIgnoringCase() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("PlayerOne"));

        assertTrue(tracked.contains("playerone"));
        assertTrue(tracked.contains("PLAYERONE"));
        assertTrue(tracked.contains("PlAyErOnE"));
    }

    @Test
    void doesNotMatchUnconfiguredName() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("PlayerOne"));

        assertFalse(tracked.contains("PlayerTwo"));
    }

    @Test
    void ignoresBlankAndWhitespaceOnlyEntries() {
        TrackedPlayers tracked = TrackedPlayers.of(Arrays.asList("PlayerOne", "", "   ", "\t"));

        assertEquals(1, tracked.size());
        assertEquals(List.of("PlayerOne"), tracked.displayNames());
    }

    @Test
    void ignoresNullEntries() {
        TrackedPlayers tracked = TrackedPlayers.of(Arrays.asList("PlayerOne", null));

        assertEquals(1, tracked.size());
    }

    @Test
    void trimsSurroundingWhitespace() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("  PlayerOne  "));

        assertTrue(tracked.contains("PlayerOne"));
        assertEquals(List.of("PlayerOne"), tracked.displayNames());
    }

    @Test
    void removesDuplicatesDifferingOnlyByCase() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("PlayerOne", "playerone", "PLAYERONE"));

        assertEquals(1, tracked.size());
    }

    @Test
    void preservesDisplayCapitalisationOfFirstOccurrence() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("FactionLeader", "factionleader"));

        assertEquals(List.of("FactionLeader"), tracked.displayNames());
    }

    @Test
    void emptyConfigurationMatchesNothing() {
        TrackedPlayers tracked = TrackedPlayers.of(Collections.emptyList());

        assertTrue(tracked.isEmpty());
        assertFalse(tracked.contains("PlayerOne"));
    }

    @Test
    void containsNormalizedSkipsRepeatedNormalisation() {
        TrackedPlayers tracked = TrackedPlayers.of(List.of("PlayerOne"));

        assertTrue(tracked.containsNormalized("playerone"));
        assertFalse(tracked.containsNormalized("PlayerOne"));
    }
}
