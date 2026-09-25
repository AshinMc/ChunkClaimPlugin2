# 📘 ChunkClaimPlugin2 (CCP) — Server Owner Wiki & Guide

Welcome to the official documentation for **ChunkClaimPlugin2**. This guide covers installation, configuration, claim flags, permissions, admin commands, economy setup, and PlaceholderAPI integration for server administrators.

---

## 📋 Table of Contents
1. [Installation & Setup](#-installation--setup)
2. [Configuration Reference (`config.yml`)](#-configuration-reference-configyml)
3. [Economy & Real Estate Trading](#-economy--real-estate-trading)
   - [Mode Comparison: Vault vs Physical Item Economy](#mode-comparison-vault-vs-physical-item-economy)
   - [Gold Banks & Gringotts Support](#gold-banks--gringotts-support)
   - [Dynamic Pricing Curve & Step-by-Step Math](#dynamic-pricing-curve--step-by-step-math)
   - [Dynamic Price Drop on Unclaiming](#dynamic-price-drop-on-unclaiming)
   - [Economy Presets for Server Owners](#economy-presets-for-server-owners)
   - [Player-to-Player Marketplace (`/claimtrade`)](#player-to-player-marketplace-claimtrade)
4. [Claim Settings & Custom GUI Integration (DeluxeMenus)](#-claim-settings--custom-gui-integration)
   - [CLI Subcommands for `/chunksettings`](#cli-subcommands-for-chunksettings)
   - [DeluxeMenus & Custom GUI Example Configuration](#deluxemenus--custom-gui-example-configuration)
5. [Greeting Display & Action Bar System](#-greeting-display--action-bar-system)
6. [Admin Land Management & Force Unclaims](#-admin-land-management--force-unclaims)
7. [Claim Flags & Protection Reference](#-claim-flags--protection-reference)
8. [Permissions Reference](#-permissions-reference)
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

### Dynamic Pricing Curve & Step-by-Step Math
You can configure progressive claim costs so that acquiring larger territories becomes progressively more expensive. This acts as a natural economic regulator against land monopolization.

$$\text{Claim Cost} = \text{cost-per-chunk} + \Big((\text{CurrentOwnedChunks} - \text{free-claims}) \times \text{cost-increase-per-chunk}\Big)$$

#### Concrete Example Walkthrough:
Assume the following `config.yml` values:
```yaml
economy:
  enabled: true
  free-claims: 3                  # First 3 chunks are completely free
  cost-per-chunk: 10.0            # Base cost for the 1st paid chunk
  cost-increase-per-chunk: 5.0    # Each additional chunk increases by $5.00
  unclaim-refund-percentage: 50.0 # 50% refunded upon unclaiming
```

Here is the exact cost progression as a player claims land:

| Claimed Chunk | Total Owned Before Claim | Mathematical Breakdown | Cost Charged to Player |
|---|---|---|---|
| **1st Chunk** | 0 chunks | Within `free-claims` ($0 < 3$) | **$0.00** (Free) |
| **2nd Chunk** | 1 chunk | Within `free-claims` ($1 < 3$) | **$0.00** (Free) |
| **3rd Chunk** | 2 chunks | Within `free-claims` ($2 < 3$) | **$0.00** (Free) |
| **4th Chunk** | 3 chunks | $10.00 + (3 - 3) \times 5.00 = 10.00 + 0$ | **$10.00** |
| **5th Chunk** | 4 chunks | $10.00 + (4 - 3) \times 5.00 = 10.00 + 5.00$ | **$15.00** |
| **6th Chunk** | 5 chunks | $10.00 + (5 - 3) \times 5.00 = 10.00 + 10.00$ | **$20.00** |
| **7th Chunk** | 6 chunks | $10.00 + (6 - 3) \times 5.00 = 10.00 + 15.00$ | **$25.00** |
| **8th Chunk** | 7 chunks | $10.00 + (7 - 3) \times 5.00 = 10.00 + 20.00$ | **$30.00** |

---

### Dynamic Price Drop on Unclaiming
A common concern for players is: *"If I unclaim chunks I don't need anymore, will my next claim still be crazy expensive?"*

**No! The cost formula is strictly based on a player's CURRENT ACTIVE chunk count.**
When a player unclaims land, their chunk count decreases, and their next claim's price **immediately drops back down** to their new tier.

#### Walkthrough of an Unclaim and Price Drop:
1. **Player status:** Owns 6 chunks. Their next claim (Chunk #7) would cost **$25.00**.
2. **Action:** The player unclaims 1 chunk using `/unclaimchunk MyFarm`.
3. **Refund:** The plugin calculates the refund for the highest-tier chunk returned (Chunk #6, which cost $20.00). At `unclaim-refund-percentage: 50.0`, the player instantly receives **$10.00** deposited back into their balance.
4. **Current Status:** The player now owns 5 chunks.
5. **Next Claim Price:** When the player decides to claim a new chunk in the future, it is treated as Chunk #6 and costs **$20.00 again** (instead of $25.00).

> [!TIP]
> **Why this matters for server health:**  
> This mechanic encourages players to release abandoned builds and temporary resource outposts because they get a cash refund and actively reduce their expansion costs.

---

### Economy Presets for Server Owners

Depending on your server type, here are recommended setups:

#### Preset A: Standard Survival (Flat Fee per Chunk)
Every chunk beyond the starter allowance costs the exact same amount.
```yaml
economy:
  enabled: true
  free-claims: 5
  cost-per-chunk: 25.0
  cost-increase-per-chunk: 0.0     # Flat pricing (no increase)
  unclaim-refund-percentage: 50.0
```

#### Preset B: "Soft-Cap" Anti-Hoarding (No Hard Limit)
Instead of hard-capping players at 10 or 20 chunks, allow unlimited claims where the price escalation acts as the economic barrier.
```yaml
max-claims-per-player: 0           # 0 = unlimited claims
economy:
  enabled: true
  free-claims: 3
  cost-per-chunk: 10.0
  cost-increase-per-chunk: 10.0    # Aggressive curve ($10, $20, $30, $40, $50...)
  unclaim-refund-percentage: 75.0
```

#### Preset C: Native Physical Gold Economy (No Vault Needed)
No economy plugins needed! Chunks cost physical gold ingots right out of players' inventories. If a player holds Gold Blocks, they are automatically counted (1 Gold Block = 9 Gold Ingots).
```yaml
economy:
  enabled: true
  mode: ITEM
  item:
    material: "GOLD_INGOT"
    display-name: "Gold"
    convert-blocks: true
  free-claims: 2
  cost-per-chunk: 5.0              # 5 Gold Ingots
  cost-increase-per-chunk: 2.0     # +2 Gold Ingots per additional chunk
  unclaim-refund-percentage: 50.0
```

---

### Player-to-Player Marketplace (`/claimtrade`)
CCP features a dedicated Player-to-Player (P2P) real estate market where players can buy and sell established claims and bases directly to one another.

```
/claimtrade <buy|sell|cancel|list>  (Alias: /chunktrade)
```

| Command | Description | Example |
|---|---|---|
| `/claimtrade sell <claim> <price>` | List an owned claim group for sale to other players | `/claimtrade sell Castle 500` |
| `/claimtrade cancel <claim>` | Remove your claim group from the market | `/claimtrade cancel Castle` |
| `/claimtrade buy [claim]` | Purchase a listed claim (either standing inside it or by name) | `/claimtrade buy Castle` |
| `/claimtrade list` | View all player claims currently listed for sale across the server | `/claimtrade list` |

#### How Transactions Work:
1. **Listing:** When a seller runs `/claimtrade sell <claim> <price>`, the claim status updates to "For Sale". Visitors walking into the claim receive an action bar alert:  
   `Castle | Owner: Alex | For Sale: 500 Gold (/claimtrade buy Castle)`
2. **Purchasing:** A buyer runs `/claimtrade buy Castle`. The plugin validates:
   - Buyer has sufficient funds.
   - Buyer has enough available chunk quota to accommodate all chunks in the group.
3. **Atomic Handover:** Money is withdrawn from the buyer and deposited to the seller. Ownership of all chunks in the group is transferred to the buyer.
4. **Offline Sales Support:** If the seller is offline when their claim is bought:
   - In `mode: VAULT`, the offline deposit is executed directly.
   - In `mode: ITEM`, the payment is securely stored in `pending_economy.yml` and automatically delivered into the seller's inventory the moment they log in.

---

## 🛠️ Claim Settings & Custom GUI Integration

While CCP includes a built-in interactive chest GUI (`/chunksettings`), many server owners prefer building their own custom menus using plugins like **DeluxeMenus**, **ChestCommands**, or **CommandPanels**.

To support this, every setting and action in CCP is exposed via **CLI subcommands** with full tab-completion.

### CLI Subcommands for `/chunksettings`

| Subcommand | Description | Syntax Example |
|---|---|---|
| `flag` | Toggle or explicitly set a claim flag | `/chunksettings flag Castle pvp toggle`<br>`/chunksettings flag Castle mob-spawning true` |
| `trust` | Trust a player in a claim | `/chunksettings trust Castle Notch` |
| `untrust` | Remove trust from a player | `/chunksettings untrust Castle Notch` |
| `particle` | Change your border particle or reset | `/chunksettings particle FLAME`<br>`/chunksettings particle reset` |
| `rename` | Rename a claim group | `/chunksettings rename OldName NewName` |
| `transfer` | Transfer claim ownership to another player | `/chunksettings transfer Castle Alex` |
| `tp` | Teleport to a claim group | `/chunksettings tp Castle` |
| `visualize` | Visualize claim borders with particles | `/chunksettings visualize Castle` |
| `unclaim` | Unclaim an entire claim group | `/chunksettings unclaim Castle` |
| `lang` | Switch your personal display language | `/chunksettings lang es_ES` |

> [!NOTE]
> If a player is standing inside their own claim, the `<claim>` argument for `flag`, `trust`, and `untrust` is optional:
> `/chunksettings flag pvp toggle` will automatically apply to the claim they are standing in!

---

### DeluxeMenus & Custom GUI Example Configuration

Here is an example showing how easily server owners can bind DeluxeMenus buttons to CCP subcommands:

```yaml
# DeluxeMenus Claim Settings Menu Example
menu_title: '&8» &6Chunk Settings'
open_command: 'customclaims'
size: 27

items:
  'pvp_toggle':
    material: IRON_SWORD
    slot: 11
    display_name: '&c&lPvP Combat'
    lore:
      - '&7Current Claim: &e%ccp_claim_name%'
      - ''
      - '&eClick to toggle PvP in this claim!'
    left_click_commands:
      - '[player] chunksettings flag %ccp_claim_name% pvp toggle'

  'mob_griefing':
    material: SNOW_BLOCK
    slot: 13
    display_name: '&b&lMob Griefing Protection'
    lore:
      - '&7Blocks Creeper and Enderman damage.'
      - ''
      - '&eClick to toggle mob protection!'
    left_click_commands:
      - '[player] chunksettings flag %ccp_claim_name% mob-griefing toggle'

  'particle_flame':
    material: BLAZE_POWDER
    slot: 15
    display_name: '&6&lFlame Border'
    lore:
      - '&7Set your claim border particle to Flame.'
    left_click_commands:
      - '[player] chunksettings particle FLAME'

  'unclaim_button':
    material: BARRIER
    slot: 26
    display_name: '&4&lUnclaim Land'
    lore:
      - '&cClick to delete this claim group.'
    left_click_commands:
      - '[player] chunksettings unclaim %ccp_claim_name%'
```

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
- `ccp.trade` — Allows `/claimtrade` to buy, sell, cancel, and list claims on the market.
- `ccp.check` — Allows `/checkchunk`.
- `ccp.info` — Allows `/infochunk`.
- `ccp.visualize` — Allows `/visualizechunk`.
- `ccp.lang` — Allows `/chunklang`.
- `ccp.settings` — Allows `/chunksettings` (GUI and CLI subcommands).
- `ccp.teleport` — Allows `/chunktp`.
- `ccp.sell`, `ccp.buy` — Legacy permissions retained for backward compatibility with existing LuckPerms setups.

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
