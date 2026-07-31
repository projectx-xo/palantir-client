package com.perplexddev.palantir.tracker;

import com.perplexddev.palantir.util.PlayerNameUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads shard membership from the multiplayer tab list.
 *
 * <p>This is the only place that touches Minecraft types on the detection path. A player counts as
 * in the shard when their {@link PlayerListEntry} has a non-null scoreboard team. Their faction, if
 * any, is parsed from that team's suffix via {@link FactionParser}.
 *
 * <p>The scan reuses one buffer and caches {@link ObservedPlayer} instances per username, so a
 * steady-state tick allocates nothing. A cached entry is invalidated if the player's faction has
 * changed since it was cached, which is the only thing about an already-seen name that can change
 * from one scan to the next. The returned list is owned by the scanner and is only valid until the
 * next scan.
 */
public final class ShardScanner {

    /** Guards against unbounded growth on servers that cycle through very many usernames. */
    private static final int MAX_CACHED_NAMES = 512;

    private final List<ObservedPlayer> buffer = new ArrayList<>();
    private final Map<String, ObservedPlayer> cache = new HashMap<>();

    /**
     * @param localPlayerUuid uuid of the client's own player, or null when unknown
     * @return players currently in the shard, in tab-list order
     */
    public List<ObservedPlayer> scan(ClientPlayNetworkHandler networkHandler, UUID localPlayerUuid) {
        buffer.clear();

        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            Team team = entry.getScoreboardTeam();
            if (team == null) {
                continue;
            }
            GameProfile profile = entry.getProfile();
            if (profile == null) {
                continue;
            }
            String name = profile.getName();
            if (PlayerNameUtil.isBlank(name)) {
                continue;
            }
            String faction = FactionParser.extractFaction(textOrEmpty(team.getSuffix()));
            buffer.add(observedFor(name, profile.getId(), localPlayerUuid, faction));
        }

        return buffer;
    }

    /** Human-readable team assignments, built only when debug logging asks for it. */
    public String describeTeams(ClientPlayNetworkHandler networkHandler) {
        StringBuilder description = new StringBuilder();
        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            GameProfile profile = entry.getProfile();
            if (profile == null || PlayerNameUtil.isBlank(profile.getName())) {
                continue;
            }
            var team = entry.getScoreboardTeam();
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(profile.getName())
                    .append('=')
                    .append(team == null ? "<no team>" : team.getName());
        }
        return description.toString();
    }

    public void reset() {
        buffer.clear();
        cache.clear();
    }

    private ObservedPlayer observedFor(String name, UUID uuid, UUID localPlayerUuid, String faction) {
        String displayFaction = PlayerNameUtil.display(faction);
        ObservedPlayer cached = cache.get(name);
        if (cached != null && cached.faction().equals(displayFaction)) {
            return cached;
        }
        if (cache.size() >= MAX_CACHED_NAMES) {
            cache.clear();
        }
        boolean local = localPlayerUuid != null && localPlayerUuid.equals(uuid);
        ObservedPlayer observed = ObservedPlayer.of(name, local, faction);
        cache.put(name, observed);
        return observed;
    }

    private static String textOrEmpty(Text text) {
        return text == null ? "" : text.getString();
    }
}
