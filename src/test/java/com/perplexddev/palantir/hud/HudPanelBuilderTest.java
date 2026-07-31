package com.perplexddev.palantir.hud;

import com.perplexddev.palantir.tracker.ShardPlayerState;
import com.perplexddev.palantir.tracker.ShardSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPanelBuilderTest {

    /** Deterministic stand-in for Minecraft's TextRenderer. */
    private static final TextMeasurer MEASURER = text -> text.length() * 6;

    private static final int TITLE_COLOR = 0xFF001122;
    private static final int NORMAL_COLOR = 0xFF334455;
    private static final int TRACKED_COLOR = 0xFF667788;

    private static HudOptions options() {
        return new HudOptions(true, HudAnchor.TOP_LEFT, 8, 8, 1.0f, 1.0f,
                true, true, true, true, true, 20,
                0xB0000000, 0xFF111111, TITLE_COLOR, NORMAL_COLOR, TRACKED_COLOR,
                true, false, true);
    }

    private static HudOptions optionsWith(java.util.function.Consumer<HudOptionsMutator> mutation) {
        HudOptionsMutator mutator = new HudOptionsMutator(options());
        mutation.accept(mutator);
        return mutator.build();
    }

    private static ShardSnapshot snapshot(String... names) {
        List<ShardPlayerState> players = new ArrayList<>();
        int tracked = 0;
        for (String name : names) {
            boolean isTracked = name.startsWith("*");
            if (isTracked) {
                tracked++;
            }
            players.add(new ShardPlayerState(isTracked ? name.substring(1) : name, "", isTracked));
        }
        return new ShardSnapshot(List.copyOf(players), tracked);
    }

    private static List<String> textOf(HudPanel panel) {
        List<String> lines = new ArrayList<>();
        for (HudRow row : panel.rows()) {
            lines.add(row.text());
        }
        return lines;
    }

    @Test
    void showsTitleWhenEnabled() {
        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options(), MEASURER);

        assertEquals("PALANTIR CLIENT", panel.rows().get(0).text());
        assertEquals(TITLE_COLOR, panel.rows().get(0).color());
    }

    @Test
    void omitsTitleWhenDisabled() {
        HudOptions options = optionsWith(o -> o.showTitle = false);

        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options, MEASURER);

        assertFalse(textOf(panel).contains("PALANTIR CLIENT"));
    }

    @Test
    void showsTotalPlayerCount() {
        HudPanel panel = HudPanelBuilder.build(snapshot("Alice", "Bob"), options(), MEASURER);

        assertTrue(textOf(panel).contains("2 players detected"));
    }

    @Test
    void usesSingularWordingForOnePlayer() {
        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options(), MEASURER);

        assertTrue(textOf(panel).contains("1 player detected"));
    }

    @Test
    void showsTrackedCount() {
        HudPanel panel = HudPanelBuilder.build(snapshot("*Alice", "Bob"), options(), MEASURER);

        assertTrue(textOf(panel).contains("1 tracked"));
    }

    @Test
    void showsFactionBracketedAfterThePlayerNameWhenPresent() {
        ShardSnapshot snapshot = new ShardSnapshot(
                List.of(new ShardPlayerState("atv444", "Ikea", true)), 1);

        HudPanel panel = HudPanelBuilder.build(snapshot, options(), MEASURER);

        assertTrue(playerLinesOf(panel).get(0).contains("atv444 [Ikea]"));
    }

    @Test
    void omitsFactionBracketWhenPlayerHasNoFaction() {
        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options(), MEASURER);

        assertFalse(playerLinesOf(panel).get(0).contains("["));
    }

    @Test
    void omitsCountsWhenDisabled() {
        HudOptions options = optionsWith(o -> {
            o.showTotalCount = false;
            o.showTrackedCount = false;
        });

        HudPanel panel = HudPanelBuilder.build(snapshot("*Alice", "Bob"), options, MEASURER);

        assertFalse(textOf(panel).contains("2 players detected"));
        assertFalse(textOf(panel).contains("1 tracked"));
    }

    @Test
    void listsTrackedPlayersFirstWhenEnabled() {
        HudPanel panel = HudPanelBuilder.build(
                snapshot("Bob", "*Alice", "Carol"), options(), MEASURER);

        List<String> playerLines = playerLinesOf(panel);
        assertTrue(playerLines.get(0).contains("Alice"), "tracked player should lead: " + playerLines);
    }

    @Test
    void keepsTabListOrderWhenTrackedFirstDisabled() {
        HudOptions options = optionsWith(o -> o.showTrackedFirst = false);

        HudPanel panel = HudPanelBuilder.build(snapshot("Bob", "*Alice"), options, MEASURER);

        List<String> playerLines = playerLinesOf(panel);
        assertTrue(playerLines.get(0).contains("Bob"), "expected tab order: " + playerLines);
    }

    @Test
    void marksTrackedPlayersWithTheTrackedColour() {
        HudPanel panel = HudPanelBuilder.build(snapshot("*Alice", "Bob"), options(), MEASURER);

        HudRow alice = rowContaining(panel, "Alice");
        HudRow bob = rowContaining(panel, "Bob");

        assertEquals(TRACKED_COLOR, alice.color());
        assertEquals(NORMAL_COLOR, bob.color());
    }

    @Test
    void omitsPlayerListWhenDisabled() {
        HudOptions options = optionsWith(o -> o.showPlayerList = false);

        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options, MEASURER);

        assertFalse(textOf(panel).stream().anyMatch(line -> line.contains("Alice")));
    }

    @Test
    void truncatesPlayerListToTheConfiguredMaximum() {
        HudOptions options = optionsWith(o -> o.maxVisiblePlayers = 2);

        HudPanel panel = HudPanelBuilder.build(
                snapshot("Alice", "Bob", "Carol", "Dave"), options, MEASURER);

        assertEquals(2, playerLinesOf(panel).size());
    }

    @Test
    void showsRemainderRowWhenPlayersAreTruncated() {
        HudOptions options = optionsWith(o -> o.maxVisiblePlayers = 2);

        HudPanel panel = HudPanelBuilder.build(
                snapshot("Alice", "Bob", "Carol", "Dave"), options, MEASURER);

        assertTrue(textOf(panel).contains("+2 more"), textOf(panel).toString());
    }

    @Test
    void zeroMaxVisiblePlayersShowsEveryoneWithNoTruncation() {
        HudOptions options = optionsWith(o -> o.maxVisiblePlayers = 0);

        HudPanel panel = HudPanelBuilder.build(
                snapshot("Alice", "Bob", "Carol", "Dave"), options, MEASURER);

        assertEquals(4, playerLinesOf(panel).size());
        assertFalse(textOf(panel).stream().anyMatch(line -> line.contains("more")));
    }

    @Test
    void omitsRemainderRowWhenEverythingFits() {
        HudPanel panel = HudPanelBuilder.build(snapshot("Alice", "Bob"), options(), MEASURER);

        assertFalse(textOf(panel).stream().anyMatch(line -> line.contains("more")));
    }

    @Test
    void widthFitsTheWidestRow() {
        HudPanel panel = HudPanelBuilder.build(
                snapshot("AVeryLongPlayerNameIndeed"), options(), MEASURER);

        int widest = 0;
        for (HudRow row : panel.rows()) {
            widest = Math.max(widest, MEASURER.width(row.text()));
        }

        assertEquals(widest + HudPanelBuilder.HORIZONTAL_PADDING * 2, panel.width());
    }

    @Test
    void heightGrowsWithRowCount() {
        HudPanel small = HudPanelBuilder.build(snapshot("Alice"), options(), MEASURER);
        HudPanel large = HudPanelBuilder.build(
                snapshot("Alice", "Bob", "Carol"), options(), MEASURER);

        assertTrue(large.height() > small.height());
    }

    @Test
    void emptySnapshotStillReportsZeroPlayers() {
        HudPanel panel = HudPanelBuilder.build(ShardSnapshot.EMPTY, options(), MEASURER);

        assertTrue(textOf(panel).contains("0 players detected"));
    }

    @Test
    void panelIsEmptyWhenEverySectionIsDisabled() {
        HudOptions options = optionsWith(o -> {
            o.showTitle = false;
            o.showTotalCount = false;
            o.showTrackedCount = false;
            o.showPlayerList = false;
        });

        HudPanel panel = HudPanelBuilder.build(snapshot("Alice"), options, MEASURER);

        assertTrue(panel.isEmpty());
    }

    private static List<String> playerLinesOf(HudPanel panel) {
        List<String> lines = new ArrayList<>();
        for (HudRow row : panel.rows()) {
            if (row.player()) {
                lines.add(row.text());
            }
        }
        return lines;
    }

    private static HudRow rowContaining(HudPanel panel, String needle) {
        for (HudRow row : panel.rows()) {
            if (row.text().contains(needle)) {
                return row;
            }
        }
        throw new AssertionError("no row containing " + needle + " in " + textOf(panel));
    }

    /** Small mutable builder so each test can vary one option without repeating the whole record. */
    private static final class HudOptionsMutator {
        private final HudOptions base;
        boolean showTitle;
        boolean showTotalCount;
        boolean showTrackedCount;
        boolean showPlayerList;
        boolean showTrackedFirst;
        int maxVisiblePlayers;

        HudOptionsMutator(HudOptions base) {
            this.base = base;
            this.showTitle = base.showTitle();
            this.showTotalCount = base.showTotalCount();
            this.showTrackedCount = base.showTrackedCount();
            this.showPlayerList = base.showPlayerList();
            this.showTrackedFirst = base.showTrackedFirst();
            this.maxVisiblePlayers = base.maxVisiblePlayers();
        }

        HudOptions build() {
            return new HudOptions(base.enabled(), base.anchor(), base.offsetX(), base.offsetY(),
                    base.scaleX(), base.scaleY(), showTitle, showTotalCount, showTrackedCount, showPlayerList,
                    showTrackedFirst, maxVisiblePlayers, base.backgroundColor(), base.borderColor(),
                    base.titleColor(), base.normalPlayerColor(), base.trackedPlayerColor(),
                    base.roundedCorners(), base.hideWhenEmpty(), base.hideWithDebugScreen());
        }
    }
}
