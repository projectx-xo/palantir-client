package com.perplexddev.shardtracker.tracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a faction name from a scoreboard team suffix.
 *
 * <p>Servers observed so far put the faction in the suffix's first bracketed group, e.g.
 * {@code " [Ikea] #1"}, with a shard-instance number trailing outside the brackets -- capturing
 * only the bracket contents already excludes it. A shard number placed inside the brackets instead
 * (e.g. {@code "[Sweden #2]"}) is stripped defensively too, so the parser stays correct if another
 * server formats it that way.
 */
public final class FactionParser {

    private static final Pattern BRACKET = Pattern.compile("\\[([^\\]]+)]");
    private static final Pattern TRAILING_SHARD_NUMBER = Pattern.compile("\\s*#\\d+\\s*$");

    private FactionParser() {
    }

    public static String extractFaction(String teamSuffix) {
        if (teamSuffix == null) {
            return "";
        }
        Matcher matcher = BRACKET.matcher(teamSuffix);
        if (!matcher.find()) {
            return "";
        }
        String faction = matcher.group(1).trim();
        return TRAILING_SHARD_NUMBER.matcher(faction).replaceAll("").trim();
    }
}
