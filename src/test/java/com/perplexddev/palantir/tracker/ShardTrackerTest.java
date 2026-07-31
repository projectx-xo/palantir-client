package com.perplexddev.palantir.tracker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardTrackerTest {

    private static final long T0 = 1_000L;

    private static TrackerOptions options(String... trackedNames) {
        return fullOptions(List.of(trackedNames), List.of(), List.of(), List.of(), false, false, 0);
    }

    private static TrackerOptions optionsWithIgnored(String[] trackedNames, String... ignoredPatterns) {
        return fullOptions(List.of(trackedNames), List.of(ignoredPatterns), List.of(), List.of(), false, false, 0);
    }

    private static TrackerOptions fullOptions(List<String> trackedNames, List<String> ignoredPlayerPatterns,
                                              List<String> trackedFactionPatterns, List<String> ignoredFactionPatterns,
                                              boolean notifyAlreadyPresent, boolean trackLocal, int stabilityDelayMs) {
        return new TrackerOptions(
                TrackedPlayers.of(trackedNames),
                WildcardNameMatcher.of(ignoredPlayerPatterns),
                WildcardNameMatcher.of(trackedFactionPatterns),
                WildcardNameMatcher.of(ignoredFactionPatterns),
                notifyAlreadyPresent, trackLocal, stabilityDelayMs);
    }

    private static ObservedPlayer player(String name) {
        return ObservedPlayer.of(name, false);
    }

    private static ObservedPlayer localPlayer(String name) {
        return ObservedPlayer.of(name, true);
    }

    private static ObservedPlayer playerWithFaction(String name, String faction) {
        return ObservedPlayer.of(name, false, faction);
    }

    private static ShardUpdate.TrackedArrival arrival(String name) {
        return new ShardUpdate.TrackedArrival(name, "");
    }

    private static ShardUpdate.TrackedArrival arrival(String name, String faction) {
        return new ShardUpdate.TrackedArrival(name, faction);
    }

    /** Consumes the initial snapshot, which never notifies by default. */
    private static ShardTracker connectedWith(TrackerOptions options, ObservedPlayer... initial) {
        ShardTracker tracker = new ShardTracker(options);
        tracker.update(List.of(initial), T0);
        return tracker;
    }

    @Test
    void detectsPlayerEnteringShard() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 50);

        assertTrue(update.membershipChanged());
        assertEquals(List.of(arrival("PlayerOne")), update.enteredTracked());
        assertEquals(1, tracker.snapshot().totalCount());
    }

    @Test
    void detectsPlayerLeavingShard() {
        ShardTracker tracker = connectedWith(options("PlayerOne"), player("PlayerOne"));

        ShardUpdate update = tracker.update(List.of(), T0 + 50);

        assertTrue(update.membershipChanged());
        assertTrue(update.enteredTracked().isEmpty());
        assertEquals(0, tracker.snapshot().totalCount());
    }

    @Test
    void reportsNoChangeWhenMembershipIsIdentical() {
        ShardTracker tracker = connectedWith(options("PlayerOne"), player("PlayerOne"));
        ShardSnapshot before = tracker.snapshot();

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 50);

        assertFalse(update.membershipChanged());
        assertTrue(update.enteredTracked().isEmpty());
        assertSame(before, tracker.snapshot(), "snapshot must not be rebuilt when nothing changed");
    }

    @Test
    void doesNotNotifyForPlayersAlreadyPresentOnFirstSnapshot() {
        ShardTracker tracker = new ShardTracker(options("PlayerOne"));

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0);

        assertTrue(update.membershipChanged(), "HUD still needs the first snapshot");
        assertTrue(update.enteredTracked().isEmpty());
    }

    @Test
    void notifiesForPlayersAlreadyPresentWhenExplicitlyEnabled() {
        TrackerOptions options = fullOptions(List.of("PlayerOne"), List.of(), List.of(), List.of(), true, false, 0);
        ShardTracker tracker = new ShardTracker(options);

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0);

        assertEquals(List.of(arrival("PlayerOne")), update.enteredTracked());
    }

    @Test
    void notifiesOnlyForTrackedPlayers() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));

        ShardUpdate update = tracker.update(
                List.of(player("PlayerOne"), player("SomeoneElse")), T0 + 50);

        assertEquals(List.of(arrival("PlayerOne")), update.enteredTracked());
        assertEquals(2, tracker.snapshot().totalCount());
        assertEquals(1, tracker.snapshot().trackedCount());
    }

    @Test
    void doesNotNotifyAgainWhileTrackedPlayerRemainsPresent() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));
        tracker.update(List.of(player("PlayerOne")), T0 + 50);

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 100);

        assertTrue(update.enteredTracked().isEmpty());
    }

    @Test
    void doesNotNotifyWhenOnlyCapitalisationChanges() {
        ShardTracker tracker = connectedWith(options("PlayerOne"), player("PlayerOne"));

        ShardUpdate update = tracker.update(List.of(player("PLAYERONE")), T0 + 50);

        assertFalse(update.membershipChanged());
        assertTrue(update.enteredTracked().isEmpty());
    }

    @Test
    void notifiesAgainAfterTrackedPlayerLeavesAndReturns() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));
        tracker.update(List.of(player("PlayerOne")), T0 + 50);
        tracker.update(List.of(), T0 + 100);

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 150);

        assertEquals(List.of(arrival("PlayerOne")), update.enteredTracked());
    }

    @Test
    void doesNotNotifyForLocalPlayerByDefault() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));

        ShardUpdate update = tracker.update(List.of(localPlayer("PlayerOne")), T0 + 50);

        assertTrue(update.enteredTracked().isEmpty());
        assertEquals(1, tracker.snapshot().totalCount(), "local player still shows on the HUD");
    }

    @Test
    void notifiesForLocalPlayerWhenTrackLocalPlayerEnabled() {
        TrackerOptions options = fullOptions(List.of("PlayerOne"), List.of(), List.of(), List.of(), false, true, 0);
        ShardTracker tracker = new ShardTracker(options);
        tracker.update(List.of(), T0);

        ShardUpdate update = tracker.update(List.of(localPlayer("PlayerOne")), T0 + 50);

        assertEquals(List.of(arrival("PlayerOne")), update.enteredTracked());
    }

    @Test
    void resetMakesTheNextSnapshotInitialAgain() {
        ShardTracker tracker = connectedWith(options("PlayerOne"), player("PlayerOne"));

        tracker.reset();
        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 50);

        assertTrue(update.enteredTracked().isEmpty(), "reconnecting must not replay notifications");
        assertEquals(1, tracker.snapshot().totalCount());
    }

    @Test
    void resetClearsSnapshot() {
        ShardTracker tracker = connectedWith(options("PlayerOne"), player("PlayerOne"));

        tracker.reset();

        assertEquals(0, tracker.snapshot().totalCount());
    }

    @Test
    void stabilityDelayDefersNotificationUntilItElapses() {
        TrackerOptions options = fullOptions(List.of("PlayerOne"), List.of(), List.of(), List.of(), false, false, 500);
        ShardTracker tracker = new ShardTracker(options);
        tracker.update(List.of(), T0);

        ShardUpdate atArrival = tracker.update(List.of(player("PlayerOne")), T0 + 50);
        ShardUpdate tooEarly = tracker.update(List.of(player("PlayerOne")), T0 + 300);
        ShardUpdate elapsed = tracker.update(List.of(player("PlayerOne")), T0 + 550);

        assertTrue(atArrival.enteredTracked().isEmpty());
        assertTrue(tooEarly.enteredTracked().isEmpty());
        assertEquals(List.of(arrival("PlayerOne")), elapsed.enteredTracked());
    }

    @Test
    void stabilityDelayDiscardsPlayersThatLeaveBeforeItElapses() {
        TrackerOptions options = fullOptions(List.of("PlayerOne"), List.of(), List.of(), List.of(), false, false, 500);
        ShardTracker tracker = new ShardTracker(options);
        tracker.update(List.of(), T0);
        tracker.update(List.of(player("PlayerOne")), T0 + 50);

        tracker.update(List.of(), T0 + 100);
        ShardUpdate afterDelayWindow = tracker.update(List.of(), T0 + 900);

        assertTrue(afterDelayWindow.enteredTracked().isEmpty());
    }

    @Test
    void snapshotMarksTrackedPlayers() {
        ShardTracker tracker = connectedWith(options("PlayerOne"));
        tracker.update(List.of(player("SomeoneElse"), player("PlayerOne")), T0 + 50);

        List<ShardPlayerState> players = tracker.snapshot().players();

        assertEquals(2, players.size());
        assertTrue(players.stream().anyMatch(p -> p.displayName().equals("PlayerOne") && p.tracked()));
        assertTrue(players.stream().anyMatch(p -> p.displayName().equals("SomeoneElse") && !p.tracked()));
    }

    @Test
    void configureAppliesNewTrackedNamesWithoutReconnecting() {
        ShardTracker tracker = connectedWith(options(), player("PlayerOne"));

        tracker.configure(options("SomeoneElse"));
        ShardUpdate update = tracker.update(
                List.of(player("PlayerOne"), player("SomeoneElse")), T0 + 50);

        assertEquals(List.of(arrival("SomeoneElse")), update.enteredTracked());
    }

    @Test
    void configureRefreshesSnapshotTrackedFlags() {
        ShardTracker tracker = connectedWith(options(), player("PlayerOne"));

        tracker.configure(options("PlayerOne"));

        assertEquals(1, tracker.snapshot().trackedCount());
    }

    @Test
    void ignoredPlayerNeverAppearsInTheSnapshot() {
        TrackerOptions options = optionsWithIgnored(new String[0], "Hoodcartel1");

        ShardTracker tracker = connectedWith(options, player("Hoodcartel1"), player("SomeoneElse"));

        assertEquals(1, tracker.snapshot().totalCount());
        assertTrue(tracker.snapshot().players().stream().noneMatch(p -> p.displayName().equals("Hoodcartel1")));
    }

    @Test
    void ignoredWildcardMatchesAnySuffix() {
        TrackerOptions options = optionsWithIgnored(new String[0], "hoodcartel*");

        ShardTracker tracker = connectedWith(options, player("hoodcartel18"), player("hoodcartelfuckyoudie"));

        assertEquals(0, tracker.snapshot().totalCount());
    }

    @Test
    void ignoredPlayerNeverCountsAsAnArrivalEvenWhenAlsoTracked() {
        TrackerOptions options = optionsWithIgnored(new String[] {"Hoodcartel1"}, "Hoodcartel1");
        ShardTracker tracker = connectedWith(options);

        ShardUpdate update = tracker.update(List.of(player("Hoodcartel1")), T0 + 50);

        assertTrue(update.enteredTracked().isEmpty());
        assertEquals(0, tracker.snapshot().totalCount());
    }

    @Test
    void configureImmediatelyHidesAPlayerNewlyAddedToTheIgnoreList() {
        ShardTracker tracker = connectedWith(options(), player("Hoodcartel1"), player("SomeoneElse"));
        assertEquals(2, tracker.snapshot().totalCount());

        tracker.configure(optionsWithIgnored(new String[0], "Hoodcartel1"));

        assertEquals(1, tracker.snapshot().totalCount());
        assertTrue(tracker.snapshot().players().stream().noneMatch(p -> p.displayName().equals("Hoodcartel1")));
    }

    @Test
    void configureDropsAPendingArrivalForAPlayerNewlyIgnored() {
        TrackerOptions withDelay = fullOptions(List.of("PlayerOne"), List.of(), List.of(), List.of(), false, false, 500);
        ShardTracker tracker = new ShardTracker(withDelay);
        tracker.update(List.of(), T0);
        tracker.update(List.of(player("PlayerOne")), T0 + 50);

        tracker.configure(optionsWithIgnored(new String[] {"PlayerOne"}, "PlayerOne"));
        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 600);

        assertTrue(update.enteredTracked().isEmpty(), "the pending arrival must not fire after being ignored");
    }

    @Test
    void trackedFactionRaisesNotificationForAnyMemberOfThatFaction() {
        TrackerOptions options = fullOptions(List.of(), List.of(), List.of("Ikea"), List.of(), false, false, 0);
        ShardTracker tracker = connectedWith(options);

        ShardUpdate update = tracker.update(List.of(playerWithFaction("atv444", "Ikea")), T0 + 50);

        assertEquals(List.of(arrival("atv444", "Ikea")), update.enteredTracked());
    }

    @Test
    void trackedFactionWildcardMatchesAnyMemberOfMatchingFactions() {
        TrackerOptions options = fullOptions(List.of(), List.of(), List.of("Swe*"), List.of(), false, false, 0);
        ShardTracker tracker = connectedWith(options);

        ShardUpdate update = tracker.update(List.of(playerWithFaction("Sv2Wildc2i", "Sweden")), T0 + 50);

        assertEquals(List.of(arrival("Sv2Wildc2i", "Sweden")), update.enteredTracked());
    }

    @Test
    void snapshotMarksFactionTrackedPlayersAsTracked() {
        TrackerOptions options = fullOptions(List.of(), List.of(), List.of("Ikea"), List.of(), false, false, 0);
        ShardTracker tracker = connectedWith(options);

        tracker.update(List.of(playerWithFaction("atv444", "Ikea"), player("SomeoneElse")), T0 + 50);

        List<ShardPlayerState> players = tracker.snapshot().players();
        assertTrue(players.stream().anyMatch(p -> p.displayName().equals("atv444") && p.tracked()));
        assertTrue(players.stream().anyMatch(p -> p.displayName().equals("SomeoneElse") && !p.tracked()));
    }

    @Test
    void ignoredFactionHidesEveryMemberOfThatFaction() {
        TrackerOptions options = fullOptions(List.of(), List.of(), List.of(), List.of("Hoodlums"), false, false, 0);

        ShardTracker tracker = connectedWith(options,
                playerWithFaction("Hoodcartel1", "Hoodlums"), playerWithFaction("Hoodcartel2", "Hoodlums"),
                player("SomeoneElse"));

        assertEquals(1, tracker.snapshot().totalCount());
        assertTrue(tracker.snapshot().players().stream().allMatch(p -> p.displayName().equals("SomeoneElse")));
    }

    @Test
    void ignoredFactionOverridesIndividuallyTrackedUsername() {
        TrackerOptions options = fullOptions(List.of("atv444"), List.of(), List.of(), List.of("Ikea"), false, false, 0);
        ShardTracker tracker = connectedWith(options);

        ShardUpdate update = tracker.update(List.of(playerWithFaction("atv444", "Ikea")), T0 + 50);

        assertTrue(update.enteredTracked().isEmpty(), "faction ignore must override individual tracking");
        assertEquals(0, tracker.snapshot().totalCount());
    }

    @Test
    void playerWithNoFactionIsUnaffectedByFactionRules() {
        TrackerOptions options = fullOptions(List.of(), List.of(), List.of("Ikea"), List.of("Hoodlums"), false, false, 0);
        ShardTracker tracker = connectedWith(options);

        ShardUpdate update = tracker.update(List.of(player("PlayerOne")), T0 + 50);

        assertTrue(update.enteredTracked().isEmpty());
        assertEquals(1, tracker.snapshot().totalCount());
    }

    @Test
    void configureImmediatelyHidesAPlayerWhoseFactionBecomesIgnored() {
        ShardTracker tracker = connectedWith(options(), playerWithFaction("atv444", "Ikea"), player("SomeoneElse"));
        assertEquals(2, tracker.snapshot().totalCount());

        tracker.configure(fullOptions(List.of(), List.of(), List.of(), List.of("Ikea"), false, false, 0));

        assertEquals(1, tracker.snapshot().totalCount());
        assertTrue(tracker.snapshot().players().stream().noneMatch(p -> p.displayName().equals("atv444")));
    }
}
