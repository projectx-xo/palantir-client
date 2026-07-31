package com.perplexddev.shardtracker.tracker;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildcardNameMatcherTest {

    @Test
    void matchesExactNameIgnoringCase() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("Hoodcartel1"));

        assertTrue(ignored.matches("hoodcartel1"));
    }

    @Test
    void doesNotMatchUnrelatedName() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("Hoodcartel1"));

        assertFalse(ignored.matches("someoneelse"));
    }

    @Test
    void wildcardPrefixMatchesAnySuffix() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("hoodcartel*"));

        assertTrue(ignored.matches("hoodcartel18"));
        assertTrue(ignored.matches("hoodcartelfuckyoudie"));
    }

    @Test
    void wildcardPrefixDoesNotMatchDifferentPrefix() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("hoodcartel*"));

        assertFalse(ignored.matches("somethingelse18"));
    }

    @Test
    void wildcardSuffixMatchesAnyPrefixIncludingEmpty() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("*cartel"));

        assertTrue(ignored.matches("hoodcartel"));
        assertTrue(ignored.matches("cartel"));
        assertFalse(ignored.matches("cartelx"));
    }

    @Test
    void wildcardOnBothSidesMatchesAnywhere() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("*cartel*"));

        assertTrue(ignored.matches("xcartely"));
    }

    @Test
    void exactPatternWithoutWildcardRequiresFullMatch() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("cartel"));

        assertFalse(ignored.matches("cartel18"));
    }

    @Test
    void multipleWildcardsInOnePatternAllMustMatchInOrder() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("a*b*c"));

        assertTrue(ignored.matches("aXbYc"));
        assertTrue(ignored.matches("abc"));
        assertFalse(ignored.matches("ac"));
    }

    @Test
    void blankAndWhitespaceOnlyEntriesAreIgnored() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(Arrays.asList("", "   ", "hoodcartel*"));

        assertEquals(1, ignored.size());
        assertTrue(ignored.matches("hoodcartel1"));
    }

    @Test
    void ignoresNullEntries() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(Arrays.asList("hoodcartel*", null));

        assertEquals(1, ignored.size());
    }

    @Test
    void trimsSurroundingWhitespace() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("  hoodcartel*  "));

        assertTrue(ignored.matches("hoodcartel99"));
    }

    @Test
    void wildcardMatchingIsCaseInsensitive() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("HoodCartel*"));

        assertTrue(ignored.matches("hoodcartel5"));
    }

    @Test
    void duplicatePatternsAreDeduplicated() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("hoodcartel*", "hoodcartel*", "HOODCARTEL*"));

        assertEquals(1, ignored.size());
    }

    @Test
    void emptyConfigurationMatchesNothing() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of());

        assertTrue(ignored.isEmpty());
        assertFalse(ignored.matches("anything"));
    }

    @Test
    void mixOfExactAndWildcardPatternsBothApply() {
        WildcardNameMatcher ignored = WildcardNameMatcher.of(List.of("exactname", "hoodcartel*"));

        assertTrue(ignored.matches("exactname"));
        assertTrue(ignored.matches("hoodcartel42"));
        assertFalse(ignored.matches("exactname42"));
    }
}
