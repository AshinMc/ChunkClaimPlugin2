<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" width="128">

# CCP (Chunk Claim Plugin 2)

A lightweight chunk protection plugin for modern Spigot and Paper servers (1.19.4 through 26.x). Features named claim groups, multi-economy support (Vault and native physical gold/items), player land trading, action bar greetings, per-player localization, inventory GUIs, particle borders, and a developer API.

- **Target Platforms:** Spigot / Paper 1.19.4, 1.20.x, 1.21.x, and 26.x
- **Java Runtime:** Java 17+ (Java 21 supported)
- **Current Version:** 0.8.0

---

## Features

- **Named Claim Groups:** Claim single chunks or expand multi-chunk groups under custom names (e.g. `/claimchunk Base`, `/chunkexpand Base`).
- **Flexible Multi-Economy & Marketplace:** Optional land claiming costs and player-to-player claim trading (`/chunksell`, `/claimbuy`). Works with Vault (EssentialsX, CMI), Gold Banks (Gringotts), or native items (Gold Ingots, Diamonds) with zero external plugin dependencies required.
- **Action Bar Greetings:** Configurable claim entry alerts in the action bar, title bar, subtitle, or chat, plus wilderness transition notices.
- **Admin Land Management:** Remove griefed or abandoned land with `/chunkadmin unclaim`, `/chunkadmin unclaimplayer`, or via the GUI Chunk Inspector.
- **Multi-Language (i18n):** Per-player language settings (`/chunklang`). Supports English (`en_US`), Spanish (`es_ES`), French (`fr_FR`), German (`de_DE`), Portuguese (`pt_BR`), Russian (`ru_RU`), and Chinese (`zh_CN`).
- **PlaceholderAPI Integration:** Full PAPI expansion for claim stats, ownership, names, and marketplace status (`%ccp_claimed_chunks%`, `%ccp_max_chunks%`, `%ccp_chunk_owner%`, `%ccp_claim_name%`, `%ccp_is_for_sale%`, `%ccp_claim_price%`).
- **Protection Flags & Anti-Grief:** Per-claim toggles for Chests, Furnaces, Doors, Redstone, Mob Entry Wall, Mob Griefing, PvP, Passive Mob Protection, Explosions, and Fire Spread in `/chunksettings`.
- **Particle Border Visualizer:** Per-player custom particle effects (`FLAME`, `HEART`, `SOUL_FIRE_FLAME`, `CHERRY_LEAVES`, `SNOWFLAKE`, etc.) for visualizing boundaries.
- **Trust & Ownership Transfer:** Trust players per claim group or transfer ownership to other players.
- **Developer API:** Programmatic access via `ChunkClaimAPI` and cancellable Bukkit events (`ChunkClaimEvent`, `ChunkUnclaimEvent`, `ChunkRenameEvent`, `ChunkTransferEvent`).

---

## Documentation

- [Server Owner Wiki & Guide (WIKI.md)](WIKI.md) — Setup, configuration reference, economy setup, permissions, admin commands, and PlaceholderAPI.
- [Developer API Guide (DEVELOPER.md)](DEVELOPER.md) — API methods, events, and code examples.

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claim current chunk with an optional group name |
| `/chunkexpand <name>` | `ccp.expand` | Add current chunk to an existing claim group |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaim a claim group by name, or current chunk |
| `/chunksell <name> <price\|cancel>` | `ccp.sell` | Put a claim group up for sale or cancel listing |
| `/claimbuy [name]` | `ccp.buy` | Purchase a claim group currently for sale |
| `/checkchunk` | `ccp.check` | Check ownership and claim name of current chunk |
| `/infochunk` | `ccp.info` | View a list of your claim groups |
| `/visualizechunk [name]` | `ccp.visualize` | Visualize claim borders with particles |
| `/chunklang [locale]` | `ccp.lang` | View, list, or set your personal language |
| `/chunksettings` | `ccp.settings` | Open the chunk management GUI |
| `/chunktp <name>` | `ccp.teleport` | Teleport to a claim group by name |
| `/chunkadmin` | `chunkclaim.admin` | Open admin GUI control panel |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Forcefully unclaim the chunk you are standing in |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Forcefully unclaim a player's claim group or all chunks |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Set custom claim limit for a player |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Reset player claim limit to default |

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
3. Copy `build/libs/ChunkClaimPlugin-0.8.0.jar` into your server's `plugins/` directory and restart.

---

## License
Released under the MIT License.
