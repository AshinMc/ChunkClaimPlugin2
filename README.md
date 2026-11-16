<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" alt="ChunkClaimPlugin Logo" width="128">

# CCP (Chunk Claim Plugin 2)

A lightweight chunk protection plugin for modern Spigot and Paper servers (1.19.4 through 26.x). Features named claim groups, multi-economy support (Vault and native physical gold/items), player land trading, action bar greetings, per-player localization, inventory GUIs, particle borders, and a developer API.

- **Target Platforms:** Spigot / Paper 1.19.4, 1.20.x, 1.21.x, and 26.x
- **Java Runtime:** Java 17+ (Java 21 supported)
- **Current Version:** 0.8.5

---

## Features

- **Named Claim Groups:** Claim single chunks or expand multi-chunk groups under custom names (e.g. `/claimchunk Base`, `/chunkexpand Base`).
- **Flexible Multi-Economy & Marketplace:** Optional progressive land claiming costs and player-to-player claim trading (`/claimtrade`). Works with Vault (EssentialsX, CMI), Gold Banks (Gringotts), or native items (Gold Ingots, Diamonds) with zero external plugin dependencies required.
- **Action Bar Greetings:** Configurable claim entry alerts in the action bar, title bar, subtitle, or chat, plus wilderness transition notices.
- **Admin Land Management:** Remove griefed or abandoned land with `/chunkadmin unclaim`, `/chunkadmin unclaimplayer`, or via the GUI Chunk Inspector.
- **Multi-Language (i18n):** Per-player language settings (`/chunklang`). Supports English (`en_US`), Spanish (`es_ES`), French (`fr_FR`), German (`de_DE`), Portuguese (`pt_BR`), Russian (`ru_RU`), and Chinese (`zh_CN`).
- **PlaceholderAPI Integration:** Full PAPI expansion for claim stats, ownership, names, and marketplace status (`%ccp_claimed_chunks%`, `%ccp_max_chunks%`, `%ccp_chunk_owner%`, `%ccp_claim_name%`, `%ccp_is_for_sale%`, `%ccp_claim_price%`).
- **Protection Flags & Anti-Grief:** Per-claim toggles for Chests, Furnaces, Doors, Redstone, Villager Trading, Mob Entry Wall, Mob Griefing, PvP, Passive Mob Protection, Explosions, and Fire Spread via GUI or CLI subcommands (`/chunksettings flag`). Full support for right-click weapon systems and self-defense against aggressive mobs.
- **Particle Border Visualizer:** Per-player custom particle effects (`FLAME`, `HEART`, `SOUL_FIRE_FLAME`, `CHERRY_LEAVES`, `SNOWFLAKE`, etc.) for visualizing boundaries.
- **Trust & Ownership Transfer:** Trust players per claim group or transfer ownership to other players (`/chunksettings trust`, `/chunksettings transfer`).
- **Developer API:** Programmatic access via `ChunkClaimAPI` and cancellable Bukkit events (`ChunkClaimEvent`, `ChunkUnclaimEvent`, `ChunkRenameEvent`, `ChunkTransferEvent`).

---

## Documentation

- [Server Owner Wiki & Guide (WIKI.md)](WIKI.md) — Setup, configuration reference, economy setup, permissions, admin commands, and PlaceholderAPI.
- [Developer API Guide (DEVELOPER.md)](DEVELOPER.md) — API methods, events, and code examples.
- [Engineering Roadmap & Task Backlog (ROADMAP.md)](ROADMAP.md) — Technical tasks, vulnerability audit findings, performance optimizations, and pending fixes.

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claim current chunk with an optional group name |
| `/chunkexpand <name>` | `ccp.expand` | Add current chunk to an existing claim group |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaim a claim group by name, or current chunk |
| `/claimtrade <buy\|sell\|cancel\|list\|claim>` | `ccp.trade` | Buy, sell, list, cancel listings, or collect pending payouts |
| `/checkchunk` | `ccp.check` | Check ownership and claim name of current chunk |
| `/infochunk [page]` | `ccp.info` | View paginated list of your claim groups |
| `/visualizechunk [name]` | `ccp.visualize` | Visualize claim borders with per-player particles |
| `/chunklang [locale]` | `ccp.lang` | View, list, or set your personal language |
| `/chunksettings [subcommand]` | `ccp.settings` | Open management GUI or run CLI subcommands (flag, trust, particle, rename, transfer) |
| `/chunktp <name>` | `ccp.teleport` | Safely teleport to a claim group by name |
| `/chunkadmin` | `chunkclaim.admin` | Open admin GUI control panel |
| `/chunkadmin reload` | `chunkclaim.admin` | Reload `config.yml`, localization files, caches, and economy engine |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Forcefully unclaim the chunk you are standing in |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Forcefully unclaim a player's claim group or all chunks |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Set custom claim limit for a player |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Reset player claim limit to default |
| `/chunkadmin resetflag <flag> [action]` | `chunkclaim.admin` | Purge or override a claim flag across all server claims |

---

## Version History

### v0.8.5 (Security, Performance & Reliability Hardening)
- **Grief & Exploit Prevention (17 Fixes):** Blocked cross-border bucket empty/fill, fluid/dragon-egg inflow (`BlockFromToEvent`), cross-chunk piston extend/retract, direct player/dispenser fire ignition, End Crystal & Respawn Anchor indirect explosion damage, lingering potion clouds, Chorus Fruit / Ender Pearl vault infiltration, Frost Walker water freezing, 1.20+ sign text editing, fishing-rod entity pulling, leash/unleash theft, Bad Omen raid triggering, vehicle placement & `EntityMountEvent`, entity farmland/turtle egg trampling, cross-border tree growth, Nether portal overwriting, and expanded interactive block protection (dyed shulkers, pressure plates, tripwire, anchors, cauldrons, composters, cakes, bells, lecterns, chiseled bookshelves, decorated pots). Decoupled `ArmorStand` break protection from `mob-protection`.
- **Data Integrity & Concurrency:** Added async snapshot + atomic temp-file saving (`chunkclaims.yml.tmp`), converted all `ChunkManager` collections to `ConcurrentHashMap`, eliminated async `.getChunk()` calls in PlaceholderAPI, enforced `^[a-zA-Z0-9_-]+$` (24-char max) claim names, cleared seller trust lists on claim transfer, expired stale chat rename/sell prompts after 60s, and eliminated blocking Mojang API lookups in `/chunkadmin`.
- **Economy & Marketplace Anti-Exploit:** Tracked historical per-chunk paid cost (zeroing refund on traded/transferred claims to eliminate progressive refund arbitrage), required seller disambiguation on duplicate `/claimtrade buy` names, protected custom-meta/RPG items from currency withdrawal, routed inventory overflow payouts to `pending_economy.yml` with `/claimtrade claim`, validated item materials (`!isAir && isItem`), and added `singular-name` / `plural-name` formatting.
- **Navigation, World & Config Polish:** Fixed Nether roof & cave `/chunktp` safety scanning, rendered exact L-shaped/disconnected per-chunk outer borders with per-player particles, fixed WorldGuard `__global__` bypass, added `disabled-worlds` blacklist and `/infochunk [page]` pagination, prevented config comment wiping on startup/migration/GUI saves, wired `particle-height` & `particle-spacing` + `REDSTONE`/`DUST` aliasing, added `/chunkadmin reload` (syncing economy, locales, and hot-path caches), fixed `block-pvp: true` config semantics, and guarded against blank YAML `NullPointerException`s.

### v0.8.0
- **Admin Land Management:** CLI and GUI tools to inspect and forcefully unclaim player land (`/chunkadmin unclaim`, `/chunkadmin unclaimplayer`).
- **Villager Trade Protection & Combat Upgrades:** Added dedicated `interact-villagers` claim flag to prevent trade exhaustion/griefing or create public malls, plus unblocked right-click combat interactions on hostile mobs and enabled self-defense against angry bees and neutral mobs.
- **Multi-Economy Support:** Flexible economy integration supporting Vault, Gringotts/gold banks, and native physical item currency (`GOLD_INGOT`, `DIAMOND`) with offline payment queues.
- **Dynamic Price Scaling & Drops:** Progressive claiming costs that dynamically drop back down when players unclaim land.
- **Player Claim Trading:** Unified marketplace under `/claimtrade` (`buy`, `sell`, `cancel`, `list`).
- **CLI Subcommands for Custom GUIs:** Full CLI subcommands in `/chunksettings` for flags, trust, particles, renames, and transfers to integrate with DeluxeMenus.
- **Action Bar Greetings:** Customizable territory entry and wilderness notifications delivered via the action bar.
- **Localization Completion:** 100% complete string parity across all 7 supported languages.
- **Developer API Expansion:** New administrative and marketplace query methods in `ChunkClaimAPI`.
- **PlaceholderAPI Additions:** `%ccp_is_for_sale%` and `%ccp_claim_price%`.

### v0.7.0
- **Developer API & Events:** Public `ChunkClaimAPI` singleton and cancellable Bukkit events (`ChunkClaimEvent`, `ChunkUnclaimEvent`, `ChunkRenameEvent`, `ChunkTransferEvent`).
- **PlaceholderAPI Integration:** Expansion providing claim counts, limits, ownership, and group details.
- **Fire Spread Protection:** Added `fire-spread` flag to protect claims against burning and ignite events.
- **Permission-Based Flags:** Granular `ccp.flag.*` permission nodes for GUI flag toggles.
- **Per-Player Limits:** Administrator command `/chunkadmin setlimit` for individual player overrides.
- **Event Command Triggers:** Console command execution hooks upon chunk claim and unclaim events.

### v0.6.0
- **Command Alias:** Added `/ccp` shortcut alias.
- **Item-Based Claiming:** Right-click claiming using a configurable tool (default: Wooden Shovel).
- **Chunk Entry Greetings:** Configurable welcome titles and subtitles upon entering claimed land.
- **Granular Interaction Settings:** Protection flags for chests, furnaces, utility blocks, doors, and redstone.
- **Passive Mob Protection:** Flag to prevent visitors from harming animals within claims.
- **Claim Renaming & Transfer:** Seamless claim renaming and ownership transfer directly from the GUI.
- **Claim Teleportation:** Teleport to claims by name with `/chunktp`.
- **New Languages:** Added German (`de_DE`), Portuguese (`pt_BR`), and Russian (`ru_RU`).

### v0.5.0 – v0.5.2
- **Admin Dashboard:** Initial `/chunkadmin` management GUI.
- **Named Claim Groups:** Replaced coordinate-only tracking with named claim groups (`/claimchunk <name>`, `/chunkexpand <name>`).
- **Trust System:** Per-claim trusted player management via GUI player heads.
- **Particle Selection:** Per-player customizable boundary visualizers in `/chunksettings`.
- **Chinese Language:** Added Simplified Chinese (`zh_CN`) localization.
- **Platform Support:** Minecraft 1.21.11 and 26.1 API compatibility.

### v0.2 – v0.4
- **Core Rewrite & Modernization:** Overhauled engine for modern Spigot/Paper architecture.
- **Interactive GUI:** Introduced player settings menu (`/chunksettings`) for claims, deletion, and languages.
- **Boundary Visualizer:** Particle-based chunk border rendering.
- **WorldGuard Integration:** Full-chunk region overlap validation.
- **Initial Localization:** English (`en_US`), Spanish (`es_ES`), and French (`fr_FR`).

---

## Build & Installation

1. Clone repository:
   ```bash
   git clone https://github.com/AshinMc/ChunkClaimPlugin2.git
   ```
2. Build with Gradle (Java 17 or 21):
   ```bash
   # Windows
   .\gradlew.bat build

   # Linux / macOS
   ./gradlew build
   ```
3. Copy `build/libs/ChunkClaimPlugin-0.8.5.jar` into your server's `plugins/` directory and restart.

---

## License
Released under the MIT License.
