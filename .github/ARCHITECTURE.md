# All Goals Architecture

This document provides a concise technical overview of how All Goals works.

## Design Goals

- Track goal completion automatically and consistently.
- Keep gameplay decisions authoritative on the logical server.
- Support singleplayer, LAN, and dedicated servers from the same implementation.
- Preserve progress across deaths, disconnects, and server restarts.
- Keep the HUD and other display preferences local to each player.

## Goal Catalog

`GoalCatalog` loads the bundled 402-goal catalog and its icon manifest. `GoalDefinition` stores the display data and icon frames for each goal. The catalog is data-driven so the board, commands, counters, and completion services refer to the same source IDs.

## Progress Model

`PlayerGoalProgress` is an immutable value containing completed goal IDs, numeric counters, and observed unique values. Fabric data attachments persist and synchronise that state. `GoalProgressService` is the central path used to edit, save, announce, and synchronise completion.

Party members use one shared `PlayerGoalProgress` stored in `PartySavedData`. Leaving or disbanding a party preserves a snapshot so progress is not silently lost.

## Tracking

Tracking is divided by the event that can prove a goal was completed:

- `AutomaticGoalTracker` handles inventory, equipment, statistics, dimensions, biomes, effects, experience, movement, and nearby state.
- Dedicated trackers handle consumption, crafting, breeding, taming, deaths, combat, spyglass use, and interactions.
- Mixins are used for actions where Fabric or vanilla events do not expose the successful completion point precisely enough.

The client does not decide whether a gameplay goal is complete.

## Networking and Multiplayer

The server owns goal and party state. Fabric networking carries version checks, personal audio and announcement settings, and leaderboard snapshots. Incoming data is bounded and server-side permissions protect administrative actions.

All Goals is required on the server and every connecting client. The handshake compares the exact packaged version sourced from `gradle.properties`; every published build must increment it. `RELEASE_VERSION` is the separate player-facing goal-pool label. Party progress and timers are persistent, while HUD layout and display preferences remain client-local.

## User Interface

- `GoalsScreen` provides the advancement-style, categorised goal board.
- `AllGoalsOverlay` renders the progress bar, timer, and pinned goals.
- `LeaderboardScreen` uses Minecraft's social-interactions visual language for player and party rankings.
- Settings screens control the HUD palette, layout, pins, sounds, and announcements.

## Persistence and Performance

Progress and party data are saved with world/player data. Client settings use atomic file replacement. Inventory scans are revision-cached, slower repair/statistic checks run on an interval, party timers update once per second, and HUD values are cached until their immutable progress object changes.

## Attribution Boundary

The Java implementation in `src/main/java` and `src/client/java` is the original All Goals implementation. Draftout-derived goal information and artwork are identified separately in `THIRD_PARTY_NOTICES.md` and are excluded from the MIT code license.
