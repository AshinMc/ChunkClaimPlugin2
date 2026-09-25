# ChunkClaimPlugin - Land Protection for Spigot/Paper

Protect your land by claiming chunks with named claim groups, per-claim trust management, multi-economy support, action bar greetings, and granular protection flags.

## 🚀 Features

- **Named Claim Groups** - Claim multiple chunks under one name and expand anytime
- **Multi-Version Support** - Single JAR runs on Minecraft 1.19.x, 1.20.x, 1.21.x and 26.x
- **Multi-Economy & Land Trading** - Supports Vault, physical Gold Banks (Gringotts), or native items (Gold Ingots, Diamonds) with zero extra plugins required. Buy and sell claims via `/chunksell` and `/claimbuy`
- **Action Bar Greetings** - Display welcome messages cleanly in the action bar, title, subtitle, or chat, plus wilderness transition notices
- **Admin Land Management** - Forcefully unclaim abandoned or griefed claims via `/chunkadmin unclaim`, `/chunkadmin unclaimplayer`, or GUI inspector
- **GUI-Based Management** - Use `/chunksettings` for player claims and `/chunkadmin` for server-wide administration
- **Per-Claim Trust System** - Grant specific players building access per claim group
- **Granular Protection Flags** - Toggle mob griefing, spawning, mob entry wall, explosions, PvP, passive mob protection, and container access
- **Claim Teleportation** - Use `/chunktp <name>` to teleport to your claims
- **Transfer Ownership** - Hand off claims to other players with all settings preserved
- **Multi-Language Support** - 7 languages: English, Spanish, French, German, Portuguese, Russian, Chinese
- **WorldGuard Integration** - Prevents claiming over WorldGuard regions
- **Item-Based Claiming** - Right-click with a configurable item (default: Wooden Shovel) to claim chunks
- **Developer API & PAPI** - Full access via `ChunkClaimAPI`, cancellable events, and PlaceholderAPI expansion

## 📋 Commands

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claim the chunk you're standing in |
| `/chunkexpand <name>` | `ccp.expand` | Add current chunk to an existing claim group |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaim a claim group or current chunk |
| `/chunksell <name> <price\|cancel>` | `ccp.sell` | Put a claim group up for sale or cancel listing |
| `/claimbuy [name]` | `ccp.buy` | Purchase a claim group currently for sale |
| `/checkchunk` | `ccp.check` | See who owns the current chunk |
| `/infochunk` | `ccp.info` | List all your claim groups |
| `/chunksettings` | `ccp.settings` | Open player settings GUI |
| `/chunktp <name>` | `ccp.teleport` | Teleport to a claim |
| `/visualizechunk [name]` | `ccp.visualize` | Visualize claim boundaries with particles |
| `/chunklang [locale]` | `ccp.lang` | Change your personal language |
| `/chunkadmin` | `chunkclaim.admin` | Open admin GUI |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Unclaim the chunk you are standing in |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Unclaim a player's claim group or all chunks |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Set custom chunk claim limit for a player |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Reset player claim limit to default |

For complete documentation, see the [**Server Owner Wiki**](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/WIKI.md) and [**Developer Guide**](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/DEVELOPER.md).

## 🛡️ Protection Flags

Toggle settings per claim group in `/chunksettings`:

| Flag | Purpose | Default |
|---|---|---|
| Mob Griefing | Block enderman and creeper block damage | ON |
| Mob Spawning | Prevent natural mob spawns | OFF |
| Mob Entry | Push mobs out of chunk at borders | OFF |
| Mob Protection | Prevent visitors from harming passive animals | ON |
| Explosions | Block TNT and Wither explosion damage | ON |
| PvP | Prevent player-vs-player combat | OFF |
| Fire Spread | Block fire burn and spread | ON |
| Containers | Protect chests, barrels, and shulkers | ON |
| Furnaces | Protect furnaces, smokers, blast furnaces | ON |
| Utilities | Protect crafting tables and stonecutters | ON |
| Doors | Protect doors, trapdoors, and fence gates | ON |
| Redstone | Protect levers, buttons, repeaters | ON |

## 🔑 Permissions

- `ccp.claim` - Claim chunks
- `ccp.unclaim` - Unclaim chunks
- `ccp.expand` - Expand claims
- `ccp.sell` - Put claims up for sale
- `ccp.buy` - Buy claims that are for sale
- `ccp.check` - Check chunk ownership
- `ccp.info` - List own claims
- `ccp.settings` - Open player GUI
- `ccp.visualize` - Visualize claims
- `ccp.lang` - Change personal language
- `ccp.teleport` - Teleport to claims
- `ccp.flag.*` - Toggle all claim flags in GUI
- `chunkclaim.admin` - Admin GUI and management commands
- `chunkclaimprotection.bypass` - Bypass all protection checks

## 📚 Configuration

The `config.yml` file supports:

```yaml
locale: "en_US"
max-claims-per-player: 10
claim-item: "WOODEN_SHOVEL"

greeting-display: ACTION_BAR       # Options: ACTION_BAR, TITLE, SUBTITLE, CHAT, NONE
show-wilderness-greeting: true

economy:
  enabled: false
  mode: AUTO                       # AUTO, VAULT, ITEM, NONE
  item:
    material: "GOLD_INGOT"
    display-name: "Gold"
    convert-blocks: true
  free-claims: 3
  cost-per-chunk: 10.0
  cost-increase-per-chunk: 2.0
  unclaim-refund-percentage: 50.0

visualization:
  duration-seconds: 10
  particle-height: 100
  particle-spacing: 0.5
  particle-type: FLAME
```

## 🌍 Localization

Supported languages (100% complete):
- English (`en_US`)
- Spanish (`es_ES`)
- French (`fr_FR`)
- German (`de_DE`)
- Portuguese (`pt_BR`)
- Russian (`ru_RU`)
- Chinese (`zh_CN`)

Players can change their language per-account with `/chunklang set <locale>`.

## 🎯 What's New in v0.8.0

- 💰 **Multi-Economy & Land Marketplace:** Support for Vault, physical Gold Banks (Gringotts), or native items (Gold Ingots/Diamonds). Players can list claims with `/chunksell` and buy with `/claimbuy` or through `/chunksettings`.
- 👑 **Admin Chunk Removal:** Force unclaim any chunk with `/chunkadmin unclaim`, remove player groups with `/chunkadmin unclaimplayer`, or use the GUI Chunk Inspector.
- 💬 **Action Bar Greetings:** Clean entry notices in the action bar instead of center-screen titles, plus wilderness exit notices.
- 🌐 **100% Localization Coverage:** Added missing translation strings across all 7 language files.
- 📦 **Backward Compatibility:** Automatic config and data migration when updating from older versions.

## 📜 License
Released under the MIT License.
