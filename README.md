# Palantir Client

A client-side Fabric mod for **Minecraft 1.18.2** that shows which players are in your shard and
notifies you when a tracked player arrives.

```
PALANTIR CLIENT
12 players detected
3 tracked

★ PlayerOne [Ikea]
★ FactionLeader
  NormalPlayer
  AnotherPlayer
```

## How it works

A player counts as "in the shard" when their tab-list entry has a non-null scoreboard team
(`entry.getScoreboardTeam() != null`). This requires the server to assign scoreboard teams — the
mod can't detect shard membership on servers that don't.

## Requirements

- Minecraft 1.18.2, Fabric Loader 0.14.0+, Java 17
- [Fabric API](https://modrinth.com/mod/fabric-api) and [Cloth Config](https://modrinth.com/mod/cloth-config) (required)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for the settings screen)

## Install

Drop the `palantir-*.jar` from [Releases](../../releases), Fabric API, and Cloth Config into your
`mods` folder.

## Configuring

Open via **Mods → Palantir Client → Configure** (Mod Menu), or edit `config/palantir.json`.

- **Tracked Players** — tick "Open editor" + Save for a dedicated Add/Remove screen; usernames get
  notified when they enter (case-insensitive). A separate "Open faction editor" list does the same
  for factions (parsed from the server's scoreboard team suffix, e.g. `[Ikea]`) — any member of a
  tracked faction is treated as tracked too, with a `*` wildcard supported (e.g. `Ikea*`)
- **Ignored Players** — same editor pattern; hides usernames or patterns entirely (HUD, count, and
  notifications), even if also tracked. Also has its own faction editor: any member of an ignored
  faction is hidden too, and ignoring always wins over tracking. Supports a `*` wildcard, e.g.
  `hoodcartel*` matches any name starting with it
- **HUD** — anchor, offset, independent horizontal/vertical scale, colors
- **Notifications** — corner, duration, sound, colors
- **Appearance** — tick "Open color wheel" + Save for an interactive hue/saturation wheel plus
  brightness/alpha sliders, instead of typing hex codes
- **Advanced** — tick "Test notification" + Save to preview alerts

Changes apply instantly, no restart needed.

Two keybinds (unbound by default — set them in **Options → Controls → Key Binds → Palantir Client**):

- **Edit HUD Position** — opens a drag-to-move editor for the HUD panel, with separate handles on
  the right and bottom edges to resize width and height independently
- **Toggle HUD** — hides/shows the panel for the session; while hidden, tab-list scanning is paused

## Privacy

Fully local — no network requests, telemetry, or analytics. The only file written is
`config/palantir.json`.

## Building

```bash
./gradlew build      # or .\gradlew.bat build on Windows
```

Jar output: `build/libs/`. Run `./gradlew test` for unit tests.

## License

MIT — see [LICENSE](LICENSE).
