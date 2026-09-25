# ChunkClaimPlugin (CCP)

ChunkClaimPlugin is a lightweight, open-source land protection plugin for modern Spigot and Paper servers (1.19.4 through 26.x). It provides grid-aligned chunk claiming with named groups, granular protection flags, an optional survival-friendly economy layer, and per-player localization.

The plugin is designed to be intuitive for survival players and low-overhead for server operators. Instead of complex geometric polygons or arbitrary coordinate selection wands, claiming aligns with Minecraft's native chunk boundaries ($16 \times 16 \times \text{world height}$), keeping memory lookups fast and eliminating bounding-box calculations on block interactions.

---

## Highlights

- **Named Claim Groups:** Group multiple chunks under intuitive names (e.g., `/claimchunk Base`, `/chunkexpand Base`). Flags, permissions, and trusted players are configured per group rather than per block coordinate.
- **Flexible Economy Support:** Completely optional (`enabled: false` by default). Works with Vault for traditional virtual balances, gold-backed bank plugins like Gringotts, or built-in physical items (`GOLD_INGOT`, `DIAMOND`) with zero external dependencies. Supports player-to-player claim trading via `/chunksell` and `/claimbuy`. No decay mechanics or upkeep taxes that punish players for taking breaks.
- **Non-Intrusive Territory Alerts:** Territory entrance and wilderness notifications are delivered directly to the action bar by default, keeping the center of the screen clear during building and combat. Title, subtitle, and chat delivery modes are also supported.
- **Granular Protection Flags:** Per-group toggles for container access, furnaces, doors/trapdoors, redstone inputs, animal protection, creeper/enderman griefing, PvP combat, explosions, and fire spread.
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
| `/chunksell <name> <price\|cancel>` | `ccp.sell` | Lists a claim group on the market or cancels an active listing |
| `/claimbuy [name]` | `ccp.buy` | Purchases a claim group listed for sale |
| `/checkchunk` | `ccp.check` | Checks ownership and claim status of current chunk |
| `/infochunk` | `ccp.info` | Lists all claim groups owned by the player |
| `/visualizechunk [name]` | `ccp.visualize` | Displays temporary particle borders around claim boundaries |
| `/chunktp <name>` | `ccp.teleport` | Teleports player to one of their claimed groups |
| `/chunksettings` | `ccp.settings` | Opens the interactive player management GUI |
| `/chunklang [locale]` | `ccp.lang` | Sets personal language preference |
| `/chunkadmin` | `chunkclaim.admin` | Opens administrator management dashboard |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Forcefully unclaims the current chunk |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Forcefully unclaims specific or all claims of a player |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Sets a custom claim limit override for a player |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Clears custom limit override and restores server default |

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

## Documentation & Source

- [GitHub Repository](https://github.com/AshinMc/ChunkClaimPlugin2)
- [Server Owner Wiki & Guide](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/WIKI.md)
- [Developer API Reference](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/DEVELOPER.md)
- [Issue Tracker](https://github.com/AshinMc/ChunkClaimPlugin2/issues)

---

## License

ChunkClaimPlugin is open source software distributed under the terms of the MIT License.
