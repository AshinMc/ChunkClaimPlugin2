# ChunkClaimPlugin (CCP)

[![](https://img.shields.io/badge/Discord-Join%20Community-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/DfhaubcVdf)
[![](https://img.shields.io/badge/Ko--fi-Support%20Development-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/ashinmc)
[![](https://img.shields.io/badge/GitHub-Source%20Code-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/AshinMc/ChunkClaimPlugin2)
[![](https://img.shields.io/badge/Wiki-Documentation-blue?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/WIKI.md)

ChunkClaimPlugin is a lightweight, high-performance land protection plugin for modern Spigot and Paper servers (1.19.4 through 26.x). It provides grid-aligned chunk claiming with named territories, granular protection flags, an optional progressive economy engine, and per-player localization.

Designed from the ground up for survival servers, CCP replaces complex wand selection and arbitrary polygon math with native Minecraft chunk boundaries ($16 \times 16 \times \text{world height}$), guaranteeing zero tick overhead on block interactions.

---

## Command Reference

### Player Commands

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claims the chunk you are standing in under an optional group name |
| `/chunkexpand <name>` | `ccp.expand` | Adds the current chunk to an existing named claim group |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaims a specified claim group, or the chunk you are standing in |
| `/checkchunk` | `ccp.check` | Checks ownership and claim status of the current chunk |
| `/infochunk` | `ccp.info` | Lists all claim groups owned by you |
| `/visualizechunk [name]` | `ccp.visualize` | Displays temporary particle borders around claim boundaries |
| `/chunktp <name>` | `ccp.teleport` | Teleports you to one of your claimed groups |
| `/chunksettings [subcommand]` | `ccp.settings` | Opens the interactive settings GUI or runs CLI subcommands |
| `/chunklang [locale]` | `ccp.lang` | Sets your personal display language |

### Marketplace Commands

| Command | Permission | Description |
|---|---|---|
| `/claimtrade sell <claim> <price>` | `ccp.trade` | Lists an owned claim group for sale on the server market |
| `/claimtrade cancel <claim>` | `ccp.trade` | Removes your claim group from the market |
| `/claimtrade buy [claim]` | `ccp.trade` | Purchases a claim (standing inside it or by name) |
| `/claimtrade list` | `ccp.trade` | Displays all claims currently listed for sale across the server |

### Administrator Commands

| Command | Permission | Description |
|---|---|---|
| `/chunkadmin` | `chunkclaim.admin` | Opens administrator management dashboard |
| `/chunkadmin unclaim` | `chunkclaim.admin` | Forcefully unclaims the chunk you are standing in |
| `/chunkadmin unclaimplayer <player> [name\|--all]` | `chunkclaim.admin` | Forcefully unclaims specific or all claims of a player |
| `/chunkadmin setlimit <player> <amount>` | `chunkclaim.admin` | Sets a custom claim limit override for a player (0 = unlimited) |
| `/chunkadmin removelimit <player>` | `chunkclaim.admin` | Restores a player's claim limit back to server default |
| `/chunkadmin resetflag <flag> [action]` | `chunkclaim.admin` | Purges or overrides a claim flag across all server claims |

---

## Key Features

### Named Territory Groups
Players can organize their land into distinct named claim groups rather than isolated chunks (e.g., `/claimchunk Base`, `/chunkexpand Base`). Protection flags, trusted player permissions, and trading statuses apply to the entire group seamlessly.

### Progressive Economy & Dynamic Price Drops
Completely optional (`enabled: false` by default). Works with Vault virtual balances, Gringotts physical gold banks, or built-in item currency (`GOLD_INGOT`, `DIAMOND`) with zero external dependencies.
* **Escalating Price Curve:** Set free starter chunks, a base chunk cost, and progressive price increments per chunk owned to prevent map hoarding.
* **Dynamic Price Drop on Unclaim:** Cost formulas calculate strictly from a player's **current active chunk count**. When players unclaim land, their next claim's cost immediately decreases to match their new lower tier, while issuing a configurable refund.
* **P2P Marketplace (`/claimtrade`):** Players can buy and sell established bases directly. Offline sellers automatically receive their earnings upon login.

### Granular Protection Flags
Flags can be toggled per claim group in `/chunksettings` or via CLI commands:

| Flag | Description | Default |
|---|---|---|
| `mob-griefing` | Prevents Creeper explosions and Endermen block pickup | Enabled |
| `mob-spawning` | Disables natural hostile mob spawning within the claim | Disabled |
| `mob-entry` | Pushes hostile mobs away at the border (invisible barrier) | Disabled |
| `mob-protection` | Protects friendly farm animals from outside visitors | Enabled |
| `explosions` | Blocks TNT, minecart TNT, and Wither explosion damage | Enabled |
| `pvp` | Disables player-vs-player combat within the claim | Disabled |
| `fire-spread` | Blocks fire from igniting, burning, or spreading | Enabled |
| `interact-chest` | Restricts chests, barrels, and shulker boxes to trusted members | Enabled |
| `interact-furnace` | Restricts furnaces, blast furnaces, and smokers | Enabled |
| `interact-doors` | Restricts doors, trapdoors, and fence gates | Enabled |
| `interact-redstone` | Restricts levers, buttons, repeaters, and comparators | Enabled |

### Custom Menu Ready (DeluxeMenus / ChestCommands)
Every setting in `/chunksettings` features dedicated CLI subcommands (`flag`, `trust`, `untrust`, `particle`, `rename`, `transfer`, `unclaim`) with full tab-completion. Server owners can bind buttons in DeluxeMenus or custom GUI plugins directly to CCP subcommands.

### Non-Intrusive Territory Alerts
Territory entrance and wilderness notices are delivered directly above the hotbar in the **Action Bar** by default, keeping the center of the screen clear during building and combat. Title, subtitle, and chat modes are also supported.

### Native Per-Player Localization
Players can independently configure their display language using `/chunklang set <locale>`. Includes complete parity across:
* English (`en_US`), Spanish (`es_ES`), French (`fr_FR`), German (`de_DE`), Portuguese (`pt_BR`), Russian (`ru_RU`), and Chinese (`zh_CN`).

---

## Technical Specifications & Architecture

* **Spatial Lookup Complexity:** $O(1)$ memory lookup via chunk coordinate keys (`world:x:z`). No bounding-box geometric intersections or polygon parsing on block interact events.
* **Server Compatibility:** Paper, Purpur, and Spigot (Minecraft 1.19.4, 1.20.x, 1.21.x, and 26.x).
* **Java Runtime:** Java 17 or higher (Java 21 fully supported).
* **WorldGuard Compatible:** Automatically queries WorldGuard regions to prevent claiming over protected administrative territories.
* **PlaceholderAPI:** Full expansion for player claim statistics, limits, group names, and marketplace pricing (`%ccp_claimed_chunks%`, `%ccp_max_chunks%`, `%ccp_claim_name%`, `%ccp_is_for_sale%`, `%ccp_claim_price%`).
* **Developer API:** Programmatic singleton `ChunkClaimAPI` with cancellable Bukkit events (`ChunkClaimEvent`, `ChunkUnclaimEvent`, `ChunkRenameEvent`, `ChunkTransferEvent`).

---

## Documentation & Support

* [Server Owner Wiki & Setup Guide](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/WIKI.md)
* [Developer API Reference](https://github.com/AshinMc/ChunkClaimPlugin2/blob/main/DEVELOPER.md)
* [Discord Community Support](https://discord.gg/DfhaubcVdf)
* [Support on Ko-fi](https://ko-fi.com/ashinmc)
* [GitHub Repository & Issue Tracker](https://github.com/AshinMc/ChunkClaimPlugin2)

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

## License

ChunkClaimPlugin is open source software distributed under the terms of the MIT License.
