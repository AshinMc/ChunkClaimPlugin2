# 📘 ChunkClaimPlugin2 (CCP) — Server Owner Wiki & Guide

Welcome to the official documentation for **ChunkClaimPlugin2**. This guide covers installation, configuration, claim flags, permissions, admin commands, and PlaceholderAPI integration for server administrators.

---

## 📋 Table of Contents
1. [Installation & Setup](#-installation--setup)
2. [Configuration Reference (`config.yml`)](#-configuration-reference-configyml)
3. [Claim Flags & Interactive Protection](#-claim-flags--interactive-protection)
4. [Permissions Reference](#-permissions-reference)
5. [Admin Commands & Management](#-admin-commands--management)
6. [PlaceholderAPI Reference](#-placeholderapi-reference)
7. [Localization & Multi-Language (`lang/`)](#-localization--multi-language-lang)

---

## 🛠️ Installation & Setup

1. Download or compile `ChunkClaimPlugin.jar`.
2. Place `ChunkClaimPlugin.jar` into your server's `plugins/` directory.
3. (Optional) Install [WorldGuard](https://enginehub.org/worldguard/) if region protection checks are needed.
4. (Optional) Install [PlaceholderAPI](https://mcbops.com/placeholderapi) for placeholder support.
5. Restart your server to generate default configuration files.

---

## ⚙️ Configuration Reference (`config.yml`)

The primary configuration file is located at `plugins/ChunkClaimPlugin2/config.yml`.

```yaml
# Language / Locale (en_US, es_ES, fr_FR, de_DE, pt_BR, ru_RU, zh_CN)
locale: en_US

# Default maximum number of chunks a player can claim (0 = unlimited)
max-claims-per-player: 10

# Item used to claim chunks on right-click (Set to "NONE" to disable)
claim-item: "WOODEN_SHOVEL"

# Particle settings for chunk visualization (/visualizechunk)
visualization:
  duration-seconds: 10
  particle-height: 100
  particle-spacing: 0.5
  particle-type: FLAME # Options: FLAME, END_ROD, HEART, VILLAGER_HAPPY, REDSTONE, SNOWFLAKE, SOUL_FIRE_FLAME, CHERRY_LEAVES

# Title message display timing (20 ticks = 1 second)
title-duration:
  fade-in: 10
  stay: 70
  fade-out: 20

# Default claim flags for new claims (true = protection enabled / blocked)
claim-flags:
  mob-griefing: true     # Block endermen, snow golems, silverfish
  mob-spawning: false    # Block natural mob spawning
  mob-entry: false       # Push mobs out of chunk (invisible wall)
  mob-protection: true   # Block non-owners from hurting cows, pigs, villagers
  explosions: true       # Block Creeper / TNT block damage
  pvp: false             # Block PvP combat
  fire-spread: true      # Block fire burn and spread
  greeting-title: true   # Show welcome title on entry
  interact-chest: true   # Protect chests, barrels, shulker boxes
  interact-furnace: true # Protect furnaces, blast furnaces, smokers
  interact-stonecutter: true # Protect stonecutters, crafting tables
  interact-door: true    # Protect doors, trapdoors, fence gates
  interact-redstone: true# Protect levers, buttons, repeaters

# Console commands executed on claim events
event-commands:
  claim-success: []
  claim-fail: []
  unclaim-success: []
```

---

## 🛡️ Claim Flags & Interactive Protection

Players can customize flags per claim group via `/chunksettings` -> **Claims** -> **Claim Settings**.

| Flag | Item Icon | Description | Default |
|---|---|---|---|
| `mob-griefing` | Snow Block | Blocks mob block modification (Endermen, Snow Golems) | `true` |
| `mob-spawning` | Spawner | Blocks natural mob spawning inside the claim | `false` |
| `mob-entry` | Iron Bars | Prevents mobs from entering claim (invisible wall) | `false` |
| `mob-protection` | Cow Spawn Egg | Prevents visitors from attacking passive animals & mobs | `true` |
| `explosions` | TNT | Prevents TNT, Creeper, and Wither explosions | `true` |
| `pvp` | Iron Sword | Prevents player vs player combat | `false` |
| `fire-spread` | Flint & Steel | Prevents fire spread and block burning | `true` |
| `greeting-title` | Oak Sign | Displays title banner when entering claim | `true` |
| `interact-chest` | Chest | Protects chests, barrels, ender chests, and shulker boxes | `true` |
| `interact-furnace` | Furnace | Protects furnaces, blast furnaces, and smokers | `true` |
| `interact-stonecutter` | Stonecutter | Protects stonecutters, crafting tables, and utilities | `true` |
| `interact-door` | Oak Door | Protects doors, trapdoors, and fence gates | `true` |
| `interact-redstone` | Redstone | Protects levers, buttons, repeaters, and comparators | `true` |

---

## 🔐 Permissions Reference

Configure these permissions using a permissions plugin such as **LuckPerms**:

### Player Permissions (`default: true`)
- `ccp.claim` — Allows `/claimchunk` and item right-click claiming.
- `ccp.unclaim` — Allows `/unclaimchunk`.
- `ccp.check` — Allows `/checkchunk`.
- `ccp.info` — Allows `/infochunk`.
- `ccp.visualize` — Allows `/visualizechunk`.
- `ccp.expand` — Allows `/chunkexpand`.
- `ccp.lang` — Allows `/chunklang`.
- `ccp.settings` — Allows `/chunksettings`.
- `ccp.teleport` — Allows `/chunktp`.

### Flag Toggle Permissions (`default: true`)
- `ccp.flag.*` — Allows toggling all claim flags in the GUI.
- `ccp.flag.mob-griefing` — Allows toggling mob griefing flag.
- `ccp.flag.mob-spawning` — Allows toggling mob spawning flag.
- `ccp.flag.mob-entry` — Allows toggling mob entry flag.
- `ccp.flag.mob-protection` — Allows toggling passive mob protection flag.
- `ccp.flag.explosions` — Allows toggling explosions flag.
- `ccp.flag.pvp` — Allows toggling PvP flag.
- `ccp.flag.fire-spread` — Allows toggling fire spread flag.
- `ccp.flag.greeting-title` — Allows toggling greeting title flag.
- `ccp.flag.interact-chest` — Allows toggling chest protection flag.
- `ccp.flag.interact-furnace` — Allows toggling furnace protection flag.
- `ccp.flag.interact-stonecutter` — Allows toggling utility interaction flag.
- `ccp.flag.interact-door` — Allows toggling door protection flag.
- `ccp.flag.interact-redstone` — Allows toggling redstone protection flag.

### Admin Permissions (`default: op`)
- `chunkclaim.admin` — Access to `/chunkadmin` GUI and setting player limits.
- `chunkclaimprotection.bypass` — Bypasses all chunk protection restrictions.

---

## 👑 Admin Commands & Management

- **`/chunkadmin`** — Opens the admin control panel GUI.
- **`/chunkadmin setlimit <player> <amount>`** — Sets a custom chunk claim limit for a specific player.
- **`/chunkadmin removelimit <player>`** — Removes custom chunk claim limit for a player (reverts to config default).

---

## 📊 PlaceholderAPI Reference

When PlaceholderAPI is installed, you can use the `%ccp_*%` expansion in TAB, Scoreboards, Holograms, and Chat:

| Placeholder | Description | Example Output |
|---|---|---|
| `%ccp_claimed_chunks%` | Number of chunks claimed by player | `5` |
| `%ccp_max_chunks%` | Max chunk limit for player | `10` or `Unlimited` |
| `%ccp_is_claimed%` | Whether player's current chunk is claimed | `true` / `false` |
| `%ccp_chunk_owner%` | Owner name of current chunk | `Steve` or `Unclaimed` |
| `%ccp_claim_name%` | Claim group name of current chunk | `Base` or `None` |
| `%ccp_total_claims%` | Total claimed chunks across the server | `142` |

---

## 🌐 Localization & Multi-Language (`lang/`)

Plugin messages are stored in `plugins/ChunkClaimPlugin2/lang/`. Each player can independently select their preferred language using `/chunklang set <locale>`:

- `en_US` — English (US)
- `es_ES` — Spanish (Spain)
- `fr_FR` — French (France)
- `de_DE` — German (Germany)
- `pt_BR` — Portuguese (Brazil)
- `ru_RU` — Russian (Russia)
- `zh_CN` — Chinese (Simplified)
