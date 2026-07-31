package com.perplexddev.palantir.tracker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionParserTest {

    @Test
    void extractsASimpleBracketedFaction() {
        assertEquals("IDF", FactionParser.extractFaction(" [IDF]"));
    }

    @Test
    void extractsFactionWhenAShardNumberFollowsOutsideTheBrackets() {
        assertEquals("Ikea", FactionParser.extractFaction(" [Ikea] #1"));
    }

    @Test
    void extractsFactionWhenAShardNumberIsInsideTheBrackets() {
        assertEquals("Sweden", FactionParser.extractFaction(" [Sweden #2]"));
    }

    @Test
    void returnsEmptyWhenThereAreNoBrackets() {
        assertEquals("", FactionParser.extractFaction("Member"));
    }

    @Test
    void returnsEmptyForAnEmptyString() {
        assertEquals("", FactionParser.extractFaction(""));
    }

    @Test
    void returnsEmptyForNullInput() {
        assertEquals("", FactionParser.extractFaction(null));
    }

    @Test
    void trimsWhitespaceInsideTheBrackets() {
        assertEquals("Pandas", FactionParser.extractFaction("[ Pandas ]"));
    }

    @Test
    void takesTheFirstBracketWhenMultipleArePresent() {
        assertEquals("Ikea", FactionParser.extractFaction(" [Ikea] [extra]"));
    }

    @Test
    void handlesAFactionNameContainingSpaces() {
        assertEquals("Faster Master", FactionParser.extractFaction(" [Faster Master]"));
    }

    @Test
    void handlesAFactionNameContainingASymbol() {
        assertEquals("AusAm", FactionParser.extractFaction(" [AusAm]"));
    }
}
