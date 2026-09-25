# 📘 ChunkClaimPlugin2 (CCP) — Server Owner Wiki & Guide

Welcome to the official documentation for **ChunkClaimPlugin2**. This guide covers installation, configuration, claim flags, permissions, admin commands, economy setup, and PlaceholderAPI integration for server administrators.

---

## 📋 Table of Contents
1. [Installation & Setup](#-installation--setup)
2. [Configuration Reference (`config.yml`)](#-configuration-reference-configyml)
3. [Economy & Real Estate Trading](#-economy--real-estate-trading)
   - [Mode Comparison: Vault vs Physical Item Economy](#mode-comparison-vault-vs-physical-item-economy)
   - [Gold Banks & Gringotts Support](#gold-banks--gringotts-support)
   - [Pricing Curve & Refunds](#pricing-curve--refunds)
   - [Player-to-Player Marketplace](#player-to-player-marketplace)
4. [Greeting Display & Action Bar System](#-greeting-display--action-bar-system)
5. [Admin Land Management & Force Unclaims](#-admin-land-management--force-unclaims)
6. [Claim Flags & Protection Reference](#-claim-flags--protection-reference)
7. [Permissions Reference](#-permissions-reference)
8. [Admin Commands & Management](#-admin-commands--management)
9. [PlaceholderAPI Reference](#-placeholderapi-reference)
10. [Localization & Multi-Language (`lang/`)](#-localization--multi-language-lang)
11. [Data Migration & Upgrading from Previous Versions](#-data-migration--upgrading-from-previous-versions)

---

## 🛠️ Installation & Setup

1. Place `ChunkClaimPlugin.jar` into your server's `plugins/` folder.
2. (Optional) Install [Vault](https://www.spigotmc.org/resources/vault.34315/) or [VaultUnlocked](https://www.spigotmc.org/resources/vaultunlocked.108422/) if using a digital currency plugin (EssentialsX, CMI, TheNewEconomy, Gringotts).
3. (Optional) Install [WorldGuard](https://enginehub.org/worldguard/) to prevent players from claiming land inside existing WorldGuard regions.
4. (Optional) Install [PlaceholderAPI](https://mcbops.com/placeholderapi) to display claim stats in scoreboards, tablists, chat, or holograms.
5. Restart your server. The plugin will generate default configuration and language files under `plugins/ChunkClaimPlugin2/`.

---

## ⚙️ Configuration Reference (`config.yml`)

The primary configuration file is located at `plugins/ChunkClaimPlugin2/config.yml`.

```yaml
# Default server language file (matches files in plugins/ChunkClaimPlugin2/lang/)
# Options: en_US, es_ES, fr_FR, de_DE, pt_BR, ru_RU, zh_CN
locale: en_US

# Default maximum number of chunks a player can claim (0 = unlimited)
# Admins can override this per-player with /chunkadmin setlimit <player> <amount>
max-claims-per-player: 10

# Item used to claim chunks on right-click (Set to "NONE" to disable)
claim-item: "WOODEN_SHOVEL"

# How claim greetings appear when walking into a claim
# Options:
#   ACTION_BAR - Appears above player hotbar (unobtrusive, recommended)
#   TITLE      - Big banner centered on screen
#   SUBTITLE   - Smaller text centered on screen
#   CHAT       - Sent as a chat message
#   NONE       - Disabled completely
greeting-display: ACTION_BAR

# Send a subtle notice in the action bar when stepping out of claims into unclaimed wilderness
show-wilderness-greeting: true

# Title message display timing (20 ticks = 1 second, used when greeting-display is TITLE or SUBTITLE)
title-duration:
  fade-in: 10
  stay: 60
  fade-out: 15

# Economy and Land Trading System
economy:
  enabled: false

  # Mode:
  #   AUTO  - Automatically uses Vault if present, falls back to physical items
  #   VAULT - Strictly uses Vault (EssentialsX, CMI, Gringotts gold banks)
  #   ITEM  - Native physical item currency deducted from player inventories
  #   NONE  - Disables economy transactions
  mode: AUTO

  # Physical Item Currency settings (used in ITEM mode or AUTO fallback)
  item:
    material: "GOLD_INGOT"       # Any valid Minecraft material: GOLD_INGOT, DIAMOND, EMERALD, etc.
    display-name: "Gold"         # Currency label in messages (e.g. "10 Gold")
    convert-blocks: true         # Automatically count and convert 1 block = 9 ingots

  # Pricing and Refunds
  free-claims: 3                 # Number of chunks each player can claim for free
  cost-per-chunk: 10.0           # Base cost to claim or expand a chunk beyond free claims
  cost-increase-per-chunk: 2.0   # Additional cost per extra chunk owned (0.0 for flat pricing)
  unclaim-refund-percentage: 50.0# Percentage refunded when unclaiming (0.0 to disable refunds)

# Particle settings for chunk visualization (/visualizechunk)
visualization:
  duration-seconds: 10           # Duration in seconds (-1 for indefinite until toggled off)
  particle-height: 100           # Vertical beam height in blocks
  particle-spacing: 0.5          # Distance between particles along borders
  particle-type: FLAME           # Server default (FLAME, END_ROD, HEART, DUST, SNOWFLAKE, SOUL_FIRE_FLAME, CHERRY_LEAVES)

# General protection toggles
protection:
  prevent-block-break: true
  prevent-block-place: true
  prevent-interactions: true

# Message toggles - turn on/off specific warning messages for all players
message-toggles:
  deny-break: true
  deny-place: true
  deny-interact: true
  deny-entity: true
  deny-pvp: true
  claim-success: true
  unclaim-success: true

# Default claim flags applied to newly created claims
claim-flags:
  mob-griefing: true     # Block endermen, snow golems, silverfish
  mob-spawning: false    # Block natural mob spawning
  mob-entry: false       # Push mobs out of chunk (invisible wall)
  mob-protection: true   # Block non-owners from hurting passive animals
  explosions: true       # Block Creeper / TNT block damage
  pvp: false             # Block PvP combat
  fire-spread: true      # Block fire burn and spread
  greeting-title: true   # Show welcome greeting on entry
  interact-chest: true   # Protect chests, barrels, shulker boxes
  interact-furnace: true # Protect furnaces, smokers, blast furnaces
  interact-stonecutter: true # Protect crafting tables, stonecutters
  interact-door: true    # Protect doors, trapdoors, fence gates
  interact-redstone: true# Protect levers, buttons, repeaters

# Console commands executed on claim events (placeholders: %player%, %claim%)
event-commands:
  claim-success: []
  claim-fail: []
  unclaim-success: []
```

---

## 💰 Economy & Real Estate Trading

ChunkClaimPlugin2 provides an integrated land economy and claim marketplace designed to fit seamlessly into survival servers without forcing complex tax systems.

### Mode Comparison: Vault vs Physical Item Economy

| Feature | `mode: VAULT` | `mode: ITEM` |
|---|---|---|
| **Backing Engine** | Vault API | Native Bukkit inventory handler |
| **Compatible Plugins** | EssentialsX, CMI, TheNewEconomy, Gringotts | **None required** (100% standalone) |
| **Currency Type** | Virtual `$`, coins, or gold vault balances | Physical in-game items (`GOLD_INGOT`, `DIAMOND`, etc.) |
| **Denomination Math** | Handled by currency provider | 1 Block = 9 items automatically converted |
| **Offline Player Sales** | Deposited via Vault offline player method | Stored in `pending_economy.yml` and awarded upon login |

### Gold Banks & Gringotts Support
If your server runs **Gringotts** (the popular plugin where player money is physical gold ingots stored inside vaults and containers), simply set `economy.mode: VAULT` or `AUTO`. Because Gringotts registers itself as a standard Vault provider, CCP automatically draws gold from and deposits gold into physical player vaults without any special configuration.

### Pricing Curve & Refunds
You can configure progressive claim costs so that large empires cost more per chunk, preventing players from monopolizing the map:

$$\text{Cost} = \text{cost-per-chunk} + ((\text{OwnedChunks} - \text{free-claims}) \times \text{cost-increase-per-chunk})$$

- If a player owns fewer than `free-claims`, the cost is `$0.00`.
- When unclaiming chunks, players receive a percentage refund defined by `unclaim-refund-percentage`.

### Player-to-Player Marketplace
Players can list entire named claim groups for sale to other players:
- **Listing a claim:** `/chunksell <claimName> <price>` (or click the Emerald icon in `/chunksettings` -> **Claims** -> **Claim Details**).
- **Canceling a listing:** `/chunksell <claimName> cancel`.
- **Buying a claim:** Another player standing inside the claim runs `/claimbuy` (or `/claimbuy <claimName>`).
- **Safety checks:** Buyers must have sufficient funds and enough available claim quota to accommodate all chunks in the group. Ownership transfers atomically and earnings are delivered to the seller even if they are offline.

---

## 💬 Greeting Display & Action Bar System

When players travel across the world, CCP detects when they cross into a different claim group or enter the wilderness.

### Display Modes (`greeting-display`)
- **`ACTION_BAR` (Recommended):** Displays the claim name and owner right above the player's hotbar. This keeps the center of the screen clear for mining and combat.
- **`TITLE`:** Displays the claim name as a large title and owner as a subtitle.
- **`SUBTITLE`:** Displays the greeting as a subtitle only.
- **`CHAT`:** Prints the greeting in chat.
- **`NONE`:** Disables greetings entirely.

### Live GUI Switcher
Admins can cycle the greeting display mode directly inside the `/chunkadmin` GUI by clicking the **Greeting Display** sign in slot 14.

---

## 👑 Admin Land Management & Force Unclaims

Server operators and staff with the `chunkclaim.admin` permission have full control over claims:

### 1. In-Game Commands
- **`/chunkadmin unclaim`**  
  Unclaims the specific chunk the administrator is currently standing in, regardless of who owns it. Automatically notifies the owner if online and purges empty claim groups.
- **`/chunkadmin unclaimplayer <player> <claimName>`**  
  Deletes an entire claim group belonging to a player.
- **`/chunkadmin unclaimplayer <player> --all`**  
  Deletes all chunks owned by a player across all worlds and claim groups.
- **`/chunkadmin setlimit <player> <amount>`**  
  Sets a custom chunk limit for a specific player (0 = unlimited).
- **`/chunkadmin removelimit <player>`**  
  Resets a player's limit back to the server-wide `max-claims-per-player` default.
- **`/chunkadmin resetflag <flag> [remove|true|false]`**  
  Purges or overrides a flag across **all** existing player claims on the server. If `remove` is used, all claims have that flag deleted and revert to the server default (useful for resetting legacy flags like `mob-protection`). Also supports `true` or `false` to force an override server-wide.

### 2. GUI Chunk Inspector & Global Flag Manager
1. Type `/chunkadmin`.
2. **Chunk Inspector** (Compass icon):
   - Current chunk coordinates and world
   - Claim status and group name
   - Owner name and total chunks in that claim group
   - Force unclaim single chunk or entire group with confirmation.
3. **Global Flag Manager** (Repeater icon):
   - Left-click any flag to purge it from all server claims (reverts to server default).
   - Right-click to force DISABLE (`false`) across all claims.
   - Shift-click to force ENABLE (`true`) across all claims.

---

## 🛡️ Claim Flags & Protection Reference

Flags can be toggled per claim group in `/chunksettings`:

| Flag | Item Icon | Description | Default |
|---|---|---|---|
| `mob-griefing` | Snow Block | Blocks mob block modification (Endermen, Snow Golems) | `true` |
| `mob-spawning` | Spawner | Blocks natural mob spawning inside the claim | `false` |
| `mob-entry` | Iron Bars | Prevents mobs from entering claim (invisible wall) | `false` |
| `mob-protection` | Cow Spawn Egg | Prevents visitors from attacking passive animals & mobs | `true` |
| `explosions` | TNT | Prevents TNT, Creeper, and Wither explosions | `true` |
| `pvp` | Iron Sword | Prevents player vs player combat | `false` |
| `fire-spread` | Flint & Steel | Prevents fire spread and block burning | `true` |
| `greeting-title` | Oak Sign | Displays greeting when entering claim | `true` |
| `interact-chest` | Chest | Protects chests, barrels, ender chests, and shulkers | `true` |
| `interact-furnace` | Furnace | Protects furnaces, blast furnaces, and smokers | `true` |
| `interact-stonecutter` | Stonecutter | Protects stonecutters, crafting tables, and utilities | `true` |
| `interact-door` | Oak Door | Protects doors, trapdoors, and fence gates | `true` |
| `interact-redstone` | Redstone | Protects levers, buttons, repeaters, and comparators | `true` |

---

## 🔐 Permissions Reference

### Player Permissions (`default: true`)
- `ccp.claim` — Allows `/claimchunk` and item right-click claiming.
- `ccp.unclaim` — Allows `/unclaimchunk`.
- `ccp.expand` — Allows `/chunkexpand`.
- `ccp.sell` — Allows `/chunksell` to list claims for sale.
- `ccp.buy` — Allows `/claimbuy` to purchase claims.
- `ccp.check` — Allows `/checkchunk`.
- `ccp.info` — Allows `/infochunk`.
- `ccp.visualize` — Allows `/visualizechunk`.
- `ccp.lang` — Allows `/chunklang`.
- `ccp.settings` — Allows `/chunksettings`.
- `ccp.teleport` — Allows `/chunktp`.

### Flag Toggle Permissions (`default: true`)
- `ccp.flag.*` — Allows toggling all claim flags in the GUI.
- `ccp.flag.<flagname>` — Allows toggling a specific flag (e.g. `ccp.flag.pvp`, `ccp.flag.explosions`).

### Admin Permissions (`default: op`)
- `chunkclaim.admin` — Access to `/chunkadmin`, inspector, and force unclaim commands.
- `ccp.admin.unclaim` — Explicit permission for `/chunkadmin unclaim` and `/chunkadmin unclaimplayer`.
- `chunkclaimprotection.bypass` — Bypasses all chunk protection restrictions (building, breaking, interacting).

---

## 📊 PlaceholderAPI Reference

When PlaceholderAPI is installed, you can use these identifiers across TAB, Scoreboards, and Chat:

| Placeholder | Description | Example Output |
|---|---|---|
| `%ccp_claimed_chunks%` | Number of chunks claimed by player | `5` |
| `%ccp_max_chunks%` | Max chunk limit for player | `10` or `Unlimited` |
| `%ccp_is_claimed%` | Whether player's current chunk is claimed | `true` / `false` |
| `%ccp_chunk_owner%` | Owner name of current chunk | `Steve` or `Unclaimed` |
| `%ccp_claim_name%` | Claim group name of current chunk | `Base` or `None` |
| `%ccp_is_for_sale%` | Whether current chunk is listed for sale | `true` / `false` |
| `%ccp_claim_price%` | Price of current chunk if listed for sale | `250.0` or `0` |
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

---

## 📦 Data Migration & Upgrading from Previous Versions

- **Seamless Auto-Update:** When dropping in ChunkClaimPlugin2 v0.8.0 on an existing server, newly added configuration settings (`greeting-display`, `economy`, etc.) are automatically merged into your existing `config.yml` without overwriting your custom changes.
- **Data Safety:** Claim records in `chunkclaims.yml` retain 100% compatibility with older v0.5.x, v0.6.x, and v0.7.x schemas.
- **Language Updates:** Any missing language keys are automatically populated from the internal resource defaults.
