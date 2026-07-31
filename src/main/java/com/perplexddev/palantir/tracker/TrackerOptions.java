package com.perplexddev.palantir.tracker;

/**
 * Immutable detection settings, rebuilt only when the configuration changes.
 *
 * @param trackedPlayers                 pre-normalised tracked usernames
 * @param ignoredPlayers                 pre-compiled ignore patterns for usernames
 * @param trackedFactions                pre-compiled patterns for factions to treat as tracked
 * @param ignoredFactions                pre-compiled patterns for factions to hide entirely; matches
 *                                       (by username or faction) are excluded before anything else
 *                                       runs, so an ignored player never appears in the HUD, counts,
 *                                       or notifications, even if also tracked
 * @param notifyForPlayersAlreadyPresent whether the first snapshot after connecting may notify
 * @param trackLocalPlayer               whether the local player may trigger notifications
 * @param stabilityDelayMs               how long a tracked player must stay present before notifying
 */
public record TrackerOptions(TrackedPlayers trackedPlayers,
                             WildcardNameMatcher ignoredPlayers,
                             WildcardNameMatcher trackedFactions,
                             WildcardNameMatcher ignoredFactions,
                             boolean notifyForPlayersAlreadyPresent,
                             boolean trackLocalPlayer,
                             int stabilityDelayMs) {

    public static final TrackerOptions DEFAULTS = new TrackerOptions(TrackedPlayers.EMPTY,
            WildcardNameMatcher.EMPTY, WildcardNameMatcher.EMPTY, WildcardNameMatcher.EMPTY, false, false, 0);
}
