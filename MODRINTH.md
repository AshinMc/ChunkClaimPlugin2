# ChunkClaimPlugin (CCP)

ChunkClaimPlugin is a lightweight, open-source land protection plugin for modern Spigot and Paper servers (1.19.4 through 26.x). It provides grid-aligned chunk claiming with named groups, granular protection flags, an optional survival-friendly economy layer, and per-player localization.

The plugin is designed to be intuitive for survival players and low-overhead for server operators. Instead of complex geometric polygons or arbitrary coordinate selection wands, claiming aligns with Minecraft's native chunk boundaries ($16 \times 16 \times \text{world height}$), keeping memory lookups fast and eliminating bounding-box calculations on block interactions.

---

## Highlights

- **Named Claim Groups:** Group multiple chunks under intuitive names (e.g., `/claimchunk Base`, `/chunkexpand Base`). Flags, permissions, and trusted players are configured per group rather than per block coordinate.
- **Flexible Economy Support:** Completely optional (`enabled: false` by default). Works with Vault for traditional virtual balances, gold-backed bank plugins like Gringotts, or built-in physical items (`GOLD_INGOT`, `DIAMOND`) with zero external dependencies. Supports player-to-player claim trading via `/claimtrade`. Includes progressive cost scaling that dynamically drops back down when players unclaim land. No decay mechanics or upkeep taxes that punish players for taking breaks.
- **Non-Intrusive Territory Alerts:** Territory entrance and wilderness notifications are delivered directly to the action bar by default, keeping the center of the screen clear during building and combat. Title, subtitle, and chat delivery modes are also supported.
- **Granular Protection Flags:** Per-group toggles for container access, furnaces, doors/trapdoors, redstone inputs, animal protection, creeper/enderman griefing, PvP combat, explosions, and fire spread. Accessible through `/chunksettings` GUI or CLI subcommands for custom menu integrations.
- **Admin Management Tools:** Server operators can inspect chunks, view ownership details, and forcefully remove claims via `/chunkadmin unclaim`, `/chunkadmin unclaimplayer`, or through an interactive chest GUI.
- **Per-Player Localization:** Players can set their own display language independently with `/chunklang`. Includes complete translations for English, Spanish, French, German, Portuguese, Russian, and Simplified Chinese.
- **Extensible API:** Programmatic access via `ChunkClaimAPI`, cancellable Bukkit events for external listeners, and a full PlaceholderAPI expansion.
- **WorldGuard Safe:** Automatically detects WorldGuard regions and prevents players from claiming over administrative regions.

---

## Technical Specifications & Compatibility

- **Server Software:** Paper, Purpur, or Spigot (Minecraft 1.19.4, 1.20.x, 1.21.x, and 26.x)
- **Java Runtime:** Java 17 or higher (Java 21 supported)
- **Optional Dependencies:**
  - **Vault:** Required only if using digital currency or Vault-backed bank accounts
  - **WorldGuard 7.x & WorldEdit 7.x:** Optional, for region collision prevention
  - **PlaceholderAPI:** Optional, for leaderboard and scoreboard tokens

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claims the chunk the player is currently standing in |
| `/chunkexpand <name>` | `ccp.expand` | Adds current chunk to an existing named claim group |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaims a specified claim group or the current chunk |
| `/claimtrade <buy\|sell\|cancel\|list>` | `ccp.trade` | P2P marketplace for buying, selling, or listing claims |
| `/checkchunk` | `ccp.check` | Checks ownership and claim status of current chunk |
| `/infochunk` | `ccp.info` | Lists all claim groups owned by the player |
| `/visualizechunk [name]` | `ccp.visualize` | Displays temporary particle borders around claim boundaries |
| `/chunktp <name>` | `ccp.teleport` | Teleports player to one of their claimed groups |
| `/chunksettings [subcommand]` | `ccp.settings` | Opens management GUI or executes CLI subcommands (flag, trust, particle, rename, transfer) |
| `/chunklang [locale]` | `ccp.lang` | Sets personal language preference |
| `/chunkadmin` | `chunkclaim.admin` | Opens administrator management dashboard |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Forcefully unclaims the current chunk |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Forcefully unclaims specific or all claims of a player |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Sets a custom claim limit override for a player |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Clears custom limit override and restores server default |
| `/chunkadmin resetflag <flag> [action]` | `chunkclaim.admin` | Purges or overrides a claim flag across all server claims |

---

## Protection Flags

The following flags can be toggled per claim group in `/chunksettings`:

| Flag | Description | Default |
|---|---|---|
| `mob-griefing` | Prevents Creeper explosions and Endermen from moving blocks | Enabled |
| `mob-spawning` | Disables hostile mob spawning within the claim | Disabled |
| `mob-entry` | Prevents hostile mobs from crossing the claim border | Disabled |
| `passive-mob-protection` | Prevents visitors from attacking or killing friendly animals | Enabled |
| `explosions` | Blocks TNT, minecart TNT, and Wither explosion damage | Enabled |
| `pvp` | Disables player-vs-player combat within the claim | Disabled |
| `fire-spread` | Blocks fire from igniting, burning, or spreading | Enabled |
| `interact-chest` | Restricts chest, barrel, and shulker box access to trusted members | Enabled |
| `interact-furnace` | Restricts furnace, smoker, and blast furnace access | Enabled |
| `interact-doors` | Restricts wooden/iron doors, trapdoors, and fence gates | Enabled |
| `interact-redstone` | Restricts levers, buttons, repeaters, and comparators | Enabled |

---

## Configuration Overview

Configuration options are managed in `plugins/ChunkClaimPlugin2/config.yml`. When updating between plugin releases, missing configuration keys are merged automatically without overwriting existing server values.

```yaml
# Default language file (matches plugins/ChunkClaimPlugin2/lang/)
locale: en_US

# Claim limits (0 = unlimited)
max-claims-per-player: 10

# Item used for right-click claiming (set to "NONE" to disable)
claim-item: "WOODEN_SHOVEL"

# Territory alert display style: ACTION_BAR, TITLE, SUBTITLE, CHAT, NONE
greeting-display: ACTION_BAR
show-wilderness-greeting: true

# Economy and trading configuration
economy:
  enabled: false
  mode: AUTO                       # AUTO, VAULT, ITEM, or NONE
  item:
    material: "GOLD_INGOT"
    display-name: "Gold"
    convert-blocks: true           # Counts 1 gold block as 9 ingots
  free-claims: 3                   # Claims granted before costs apply
  cost-per-chunk: 10.0             # Base claim cost
  cost-increase-per-chunk: 2.0     # Incremental cost curve per chunk owned
  unclaim-refund-percentage: 50.0  # Percentage refunded on unclaim
```

---

## Version History

### v0.8.0
- **Admin Land Management:** CLI and GUI tools to inspect and forcefully unclaim player land (`/chunkadmin unclaim`, `/chunkadmin unclaimplayer`).
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

## Documentation & Source

- [GitHub Repository](https://github.com/AshinMc/ChunkClaimPlugin2)
- [Server Owner Wiki & Guide](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/WIKI.md)
- [Developer API Reference](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/DEVELOPER.md)
- [Issue Tracker](https://github.com/AshinMc/ChunkClaimPlugin2/issues)

---

## License

ChunkClaimPlugin is open source software distributed under the terms of the MIT License.
