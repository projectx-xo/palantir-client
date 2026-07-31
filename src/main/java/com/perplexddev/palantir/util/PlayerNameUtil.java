package com.perplexddev.palantir.util;

import java.util.Locale;

/** Username normalisation shared by the tracker and the configuration layer. */
public final class PlayerNameUtil {

    private PlayerNameUtil() {
    }

    /**
     * Returns the case-insensitive comparison key for a username, or an empty string when the name
     * is null or contains only whitespace.
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /** Returns the display form of a username: trimmed, original capitalisation preserved. */
    public static String display(String name) {
        return name == null ? "" : name.trim();
    }

    public static boolean isBlank(String name) {
        return name == null || name.trim().isEmpty();
    }
}
