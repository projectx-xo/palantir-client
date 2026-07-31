package com.perplexddev.shardtracker.tracker;

import com.perplexddev.shardtracker.util.PlayerNameUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure shard membership state machine. Contains no Minecraft types so it can be unit tested.
 *
 * <p>A detection pass is O(n) in the tab-list size and allocates nothing while membership is
 * stable: the membership set is only rebuilt when the comparison says it actually changed, and an
 * idle pass returns the shared {@link ShardUpdate#UNCHANGED} instance.
 *
 * <p>A player is "tracked" (HUD-highlighted, and eligible to raise an arrival notification) when
 * their username or their faction matches the configured tracked lists. A player is "ignored"
 * (excluded from everything: HUD, counts, notifications) when their username or faction matches the
 * configured ignore lists, regardless of whether they are also tracked -- ignore always wins.
 */
public final class ShardTracker {

    /** Normalised names currently considered to be in the shard. */
    private final Set<String> present = new HashSet<>();

    /** Tracked arrivals waiting out the configured stability delay, keyed by normalised name. */
    private final Map<String, PendingArrival> pending = new HashMap<>();

    private TrackerOptions options;
    private boolean initialised;
    private ShardSnapshot snapshot = ShardSnapshot.EMPTY;

    public ShardTracker(TrackerOptions options) {
        this.options = options;
    }

    /** Applies changed configuration without losing the current connection state. */
    public void configure(TrackerOptions options) {
        this.options = options;
        dropPendingArrivalsForUntrackedOrIgnoredPlayers();
        removeNewlyIgnoredFromPresent();
        rebuildSnapshotAfterConfigChange();
    }

    /**
     * Compares the observed tab-list membership against the previous pass.
     *
     * @param observed players the scanner considers to be in the shard
     * @param nowMillis monotonic wall-clock reading used for the stability delay
     */
    public ShardUpdate update(Collection<ObservedPlayer> observed, long nowMillis) {
        boolean initialPass = !initialised;
        initialised = true;

        boolean changed = false;
        int distinctObserved = 0;

        for (ObservedPlayer player : observed) {
            String normalized = player.normalizedName();
            if (normalized.isEmpty() || isIgnored(normalized, player.normalizedFaction())) {
                continue;
            }
            distinctObserved++;
            if (!present.contains(normalized)) {
                changed = true;
                if (shouldNotifyFor(player, initialPass)) {
                    pending.put(normalized, new PendingArrival(player.displayName(), player.faction(), nowMillis));
                }
            }
        }

        // A pure departure leaves every observed name already present, so compare sizes too.
        if (distinctObserved != present.size()) {
            changed = true;
        }

        if (changed) {
            rebuildPresent(observed);
            snapshot = buildSnapshot(observed);
        }

        List<ShardUpdate.TrackedArrival> entered = collectElapsedArrivals(nowMillis);

        if (!changed && entered.isEmpty()) {
            return ShardUpdate.UNCHANGED;
        }
        return new ShardUpdate(changed, entered);
    }

    public ShardSnapshot snapshot() {
        return snapshot;
    }

    /** Clears all state, so the next pass is treated as a fresh connection. */
    public void reset() {
        present.clear();
        pending.clear();
        initialised = false;
        snapshot = ShardSnapshot.EMPTY;
    }

    private boolean shouldNotifyFor(ObservedPlayer player, boolean initialPass) {
        if (initialPass && !options.notifyForPlayersAlreadyPresent()) {
            return false;
        }
        if (player.local() && !options.trackLocalPlayer()) {
            return false;
        }
        return isTracked(player.normalizedName(), player.normalizedFaction());
    }

    private boolean isTracked(String normalizedName, String normalizedFaction) {
        if (options.trackedPlayers().containsNormalized(normalizedName)) {
            return true;
        }
        return !normalizedFaction.isEmpty() && options.trackedFactions().matches(normalizedFaction);
    }

    private boolean isIgnored(String normalizedName, String normalizedFaction) {
        if (options.ignoredPlayers().matches(normalizedName)) {
            return true;
        }
        return !normalizedFaction.isEmpty() && options.ignoredFactions().matches(normalizedFaction);
    }

    private void rebuildPresent(Collection<ObservedPlayer> observed) {
        present.clear();
        for (ObservedPlayer player : observed) {
            String normalized = player.normalizedName();
            if (!normalized.isEmpty() && !isIgnored(normalized, player.normalizedFaction())) {
                present.add(normalized);
            }
        }
    }

    private ShardSnapshot buildSnapshot(Collection<ObservedPlayer> observed) {
        if (observed.isEmpty()) {
            return ShardSnapshot.EMPTY;
        }

        List<ShardPlayerState> players = new ArrayList<>(observed.size());
        int trackedCount = 0;
        for (ObservedPlayer player : observed) {
            String normalized = player.normalizedName();
            if (normalized.isEmpty() || isIgnored(normalized, player.normalizedFaction())) {
                continue;
            }
            boolean tracked = isTracked(normalized, player.normalizedFaction());
            if (tracked) {
                trackedCount++;
            }
            players.add(new ShardPlayerState(player.displayName(), player.faction(), tracked));
        }

        if (players.isEmpty()) {
            return ShardSnapshot.EMPTY;
        }
        return new ShardSnapshot(Collections.unmodifiableList(players), trackedCount);
    }

    /**
     * Returns the tracked arrivals whose stability delay has elapsed, discarding any that left the
     * shard before stabilising.
     */
    private List<ShardUpdate.TrackedArrival> collectElapsedArrivals(long nowMillis) {
        if (pending.isEmpty()) {
            return List.of();
        }

        List<ShardUpdate.TrackedArrival> entered = null;
        Iterator<Map.Entry<String, PendingArrival>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingArrival> entry = iterator.next();
            if (!present.contains(entry.getKey())) {
                iterator.remove();
                continue;
            }
            if (nowMillis - entry.getValue().arrivedAtMillis() >= options.stabilityDelayMs()) {
                if (entered == null) {
                    entered = new ArrayList<>(2);
                }
                PendingArrival arrival = entry.getValue();
                entered.add(new ShardUpdate.TrackedArrival(arrival.displayName(), arrival.faction()));
                iterator.remove();
            }
        }
        return entered == null ? List.of() : Collections.unmodifiableList(entered);
    }

    private void dropPendingArrivalsForUntrackedOrIgnoredPlayers() {
        pending.entrySet().removeIf(entry -> {
            String normalizedName = entry.getKey();
            String normalizedFaction = PlayerNameUtil.normalize(entry.getValue().faction());
            return !isTracked(normalizedName, normalizedFaction) || isIgnored(normalizedName, normalizedFaction);
        });
    }

    /**
     * Drops players from {@code present} whose username or faction has just become ignored, so
     * they stop counting immediately rather than lingering until the next tab-list change. {@code
     * present} only stores normalised names, so their faction is looked up from the current
     * snapshot, which is always built from the same set of players.
     */
    private void removeNewlyIgnoredFromPresent() {
        if (present.isEmpty()) {
            return;
        }
        Map<String, String> normalizedFactionByName = new HashMap<>();
        for (ShardPlayerState player : snapshot.players()) {
            normalizedFactionByName.put(
                    PlayerNameUtil.normalize(player.displayName()), PlayerNameUtil.normalize(player.faction()));
        }
        present.removeIf(normalized -> isIgnored(normalized, normalizedFactionByName.getOrDefault(normalized, "")));
    }

    /**
     * Reapplies tracked flags and drops now-ignored players from the current snapshot, so ignoring
     * someone (by name or faction) who is already visible takes effect immediately rather than
     * waiting for the next tab-list change to trigger a rebuild.
     */
    private void rebuildSnapshotAfterConfigChange() {
        if (snapshot.isEmpty()) {
            return;
        }

        List<ShardPlayerState> players = new ArrayList<>(snapshot.players().size());
        int trackedCount = 0;
        for (ShardPlayerState player : snapshot.players()) {
            String normalizedName = PlayerNameUtil.normalize(player.displayName());
            String normalizedFaction = PlayerNameUtil.normalize(player.faction());
            if (isIgnored(normalizedName, normalizedFaction)) {
                continue;
            }
            boolean tracked = isTracked(normalizedName, normalizedFaction);
            if (tracked) {
                trackedCount++;
            }
            players.add(new ShardPlayerState(player.displayName(), player.faction(), tracked));
        }
        snapshot = players.isEmpty()
                ? ShardSnapshot.EMPTY
                : new ShardSnapshot(Collections.unmodifiableList(players), trackedCount);
    }

    /** A tracked player seen arriving, waiting for the stability delay to elapse. */
    private record PendingArrival(String displayName, String faction, long arrivedAtMillis) {
    }
}
