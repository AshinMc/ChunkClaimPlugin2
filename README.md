<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" width="128">

# ChunkClaimPlugin2

Protect your land by claiming chunks with named claim groups, per-player trust, and a rich GUI.

- **Server:** Spigot / Paper 1.21.11
- **Java:** 21+
- **Version:** 0.5.1

---

## Highlights

### v0.5.1 - Per-Claim Settings, Entity Protection & Bugfixes
- **Per-claim settings GUI** - toggle flags per claim group: mob griefing, mob spawning, mob entry, explosions, PvP
- **Full entity protection** - minecarts, boats, armor stands, item frames, paintings, and all entities are now protected in claimed chunks
- **Mob griefing is now flag-gated** - snow golem trails, enderman theft, explosions controllable per claim
- **Mob spawning prevention** - blocks natural/spawner/raid spawns when enabled
- **Mob entry prevention** - invisible wall pushes all mobs out of protected chunks
- **PvP toggle** - disable player combat per claim group
- Bugfix: other players was able to destroy minecarts/vehicles in claimed chunks

### v0.5 - Named Claims, Trust & Chinese Locale
- **Named claim groups** - every claim has a name (`/claimchunk River`)
- **Expand existing claims** - `/chunkexpand River` adds adjacent chunks to a group
- **Merged bounding-box visualization** - each claim group renders as one outline
- **Current-chunk fallback** - running `/visualizechunk`, `/unclaimchunk`, or `/checkchunk` with no arguments uses the chunk you're standing on
- **Per-claim trust system** - trust other players on specific claim groups via the GUI; trusted players can build, break, and interact
- **Chinese (zh_CN) locale** added
- WorldGuard 7.0.15, WorldEdit 7.4.0


---

## Commands

| Command | Description |
|---|---|
| `/claimchunk <name>` | Claim the chunk you're standing in with a group name |
| `/chunkexpand <name>` | Add the current chunk to an existing claim group |
| `/unclaimchunk [name]` | Unclaim a claim group by name, or the group you're standing on |
| `/checkchunk` | See who owns the current chunk and its claim name |
| `/infochunk` | List all your claim groups with chunk counts |
| `/visualizechunk [name]` | Visualize a claim group, current chunk's group, or all claims |
| `/chunklang [get\|list\|set <locale>]` | View/list/set your language (per-player) |
| `/chunksettings` | Open the player settings GUI |
| `/chunkadmin` | Open the admin settings GUI (requires `chunkclaim.admin`) |

---

## GUI Overview

### `/chunksettings` - Player Settings
- **Home:** Claims, Visualize, Delete, Particle, Language
- **Claims:** Browse your claim groups -> Claim Details (teleport, trusted players, claim settings)
- **Claim Settings:** Toggle per-claim flags (mob griefing, mob spawning, mob entry, explosions, PvP)
- **Trusted Players:** Click player heads to toggle trust per claim group (green = trusted, gray = not)
- **Visualize:** Click a claim to visualize its merged bounding box
- **Delete:** Choose a claim -> Confirm Delete menu
- **Particle:** Pick your visualization particle or reset to server default
- **Language:** Pick a locale; saved per player

### `/chunkadmin` - Admin Settings
- **Max Claims:** Set the server-wide max claims per player
- **Default Language:** Set server default locale
- **Default Particle:** Set server default visualization particle
- **Reload Config:** Hot-reload `config.yml`

---

## Claim Settings (Flags)

Each claim group has toggleable protection flags. Access via `/chunksettings` -> Claims -> click a claim -> **Claim Settings**.

| Flag | Default | Description |
|---|---|---|
| Mob Griefing Protection | ON | Block snow trails, enderman theft, ravager crops, etc. |
| Block Mob Spawning | OFF | Prevent natural, spawner, and raid mob spawns |
| Block Mob Entry | OFF | Invisible wall prevents all mobs from entering the chunk, good for redstones |
| Explosion Protection | ON | Block creeper, TNT, wither, and all explosion damage |
| Block PvP | OFF | Prevent player vs player combat in the chunk |

Flags are saved per claim group and persist across restarts.

---

## Trust System

Trust lets you grant other players permission to build, break, and interact within a specific claim group.

1. Open `/chunksettings` -> **Claims** -> click a claim -> **Trusted Players**
2. Online players appear as player heads; click to toggle trust
3. Trusted players are shown in green, untrusted in gray
4. Trust is saved per claim group and persists across restarts

---

## Requirements
- Java 21+
- Spigot or Paper 1.21.11+
- **Optional:** WorldGuard 7.0.15+, WorldEdit 7.4.0+ (for region overlap checks)

## Build and Install
1. Clone the repository
2. Build: `.\gradlew.bat build` (Windows) or `./gradlew build` (Linux/Mac)
3. Output: `build/libs/ChunkClaimPlugin-0.5.1.jar`
4. Drop the jar into your server's `plugins/` folder and restart

## Configuration
- **config.yml** - `locale`, `max-claims-per-player`, `visualization.particle-type`, `claim-flags` defaults
- **lang/messages_*.yml** - en_US, es_ES, fr_FR, zh_CN

---

## Changelog

### v0.5.1
- Per-claim settings GUI with toggleable flags (mob griefing, mob spawning, mob entry, explosions, PvP)
- Full entity protection: minecarts, boats, armor stands, item frames, paintings
- Mob griefing, explosions, spawning, entry, and PvP now controlled per claim group
- Mob entry prevention: invisible wall teleports all mobs to the nearest chunk edge
- PvP combat toggle per claim
- Added `deny-entity`, `deny-pvp` and all flag-related messages to all 4 locales
- Default claim flags configurable in `config.yml`
- Bugfix: vehicles/minecarts could be destroyed by non-owners

### v0.5
- Named claim groups (`/claimchunk <name>`, `/chunkexpand <name>`)
- Merged bounding-box visualization per claim group
- Current-chunk fallback for `/visualizechunk`, `/unclaimchunk`, `/checkchunk`
- Per-claim trust system with GUI (player heads toggle)
- Trust check in chunk protection listener
- Chinese (zh_CN) locale
- Upgraded to WorldGuard 7.0.15, WorldEdit 7.4.0

### v0.4.1
- Upgraded to Minecraft 1.21.11 (Spigot API 1.21.11-R0.1-SNAPSHOT)
- Admin settings GUI (`/chunkadmin`) with max claims, language, particle, reload
- Per-player particle selection in `/chunksettings`
- Max claims enforcement (configurable)

### v0.4
- Upgraded to Minecraft 1.21.8 (api-version 1.21)
- Per-player i18n with `/chunklang`
- `/chunksettings` GUI with Claims/Visualize/Delete/Language
- Single-claim visualization from GUI and command
- Delete confirmation menu
- Safer teleports and UX improvements

### v0.3
- WorldGuard integration and general improvements

---

## Roadmap
- Pagination for large claim lists
- Offline player trust management
- Map integration
- More locales
- Force load chunks (customisable for admins)

Issues and feature requests welcome!
