<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" width="128">

# CCP (Chunk Claim Plugin 2)

A feature-rich, high-performance Minecraft chunk protection plugin designed for modern Spigot & Paper servers (1.19.x – 26.x). Features named claim groups, per-player localization, interactive inventory GUIs, PlaceholderAPI support, fire spread protection, particle border visualization, and a developer API.

- **Supported Server Engine:** Spigot / Paper 1.19.x, 1.20.x, 1.21.x, and 26.x
- **Java Runtime:** Java 17+ (Java 21 supported)
- **Plugin Version:** 0.7.0

---

## 🌟 Key Feature Overview

- 🏷️ **Named Claim Groups:** Claim individual chunks or group them together under custom names (e.g. `/claimchunk Base`, `/chunkexpand Base`).
- 🌐 **Multi-Language (i18n):** Per-player language settings (`/chunklang`). Supports English (`en_US`), Spanish (`es_ES`), French (`fr_FR`), German (`de_DE`), Portuguese (`pt_BR`), Russian (`ru_RU`), and Chinese (`zh_CN`).
- 📊 **PlaceholderAPI Integration:** Full support for PlaceholderAPI placeholders in player messages and custom placeholders (`%ccp_claimed_chunks%`, `%ccp_max_chunks%`, `%ccp_chunk_owner%`, `%ccp_claim_name%`, `%ccp_is_claimed%`).
- 🔥 **Fire Spread & Explosion Protection:** Prevent fire burning, fire spreading, and TNT/Creeper explosions inside claimed land.
- 🎨 **Particle Border Visualizer:** Per-player custom particle effects (`FLAME`, `HEART`, `SOUL_FIRE_FLAME`, `CHERRY_LEAVES`, `SNOWFLAKE`, etc.) for visualizing claim boundaries.
- ⚔️ **Granular Interaction & Flag Protection:** Toggle settings for Chests, Furnaces, Doors, Redstone, Mob Entry Wall, Mob Griefing, PvP, Passive Mob Protection, and Welcome Titles directly in `/chunksettings`.
- 🤝 **Player Trust & Ownership Transfer:** Easily trust friends or transfer claim ownership via the GUI.
- 💻 **Developer API & Custom Events:** Full API access (`ChunkClaimAPI`) and cancellable Bukkit events (`ChunkClaimEvent`, `ChunkUnclaimEvent`, `ChunkRenameEvent`, `ChunkTransferEvent`).

---

## 📚 Documentation Navigation

- 📘 [**Server Owner Wiki & Guide (WIKI.md)**](WIKI.md) — Complete guide for server administrators: installation, configuration, permissions, claim flags, admin commands, and PlaceholderAPI reference.
- 💻 [**Developer API Guide (DEVELOPER.md)**](DEVELOPER.md) — Complete guide for developers: Maven/Gradle setup, `ChunkClaimAPI` usage, listening to cancellable events, and code examples.

---

## 📜 Quick Command Reference

| Command | Permission | Description |
|---|---|---|
| `/claimchunk [name]` | `ccp.claim` | Claim the chunk you are standing in with an optional name |
| `/chunkexpand <name>` | `ccp.expand` | Expand an existing claim group by adding current chunk |
| `/unclaimchunk [name]` | `ccp.unclaim` | Unclaim a claim group by name or current chunk |
| `/checkchunk` | `ccp.check` | Check ownership and claim name of current chunk |
| `/infochunk` | `ccp.info` | View a list of your claim groups |
| `/visualizechunk [name]`| `ccp.visualize` | Visualize claim borders with particles |
| `/chunklang [locale]` | `ccp.lang` | View, list, or set your language |
| `/chunksettings` | `ccp.settings` | Open the player chunk management GUI |
| `/chunktp <name>` | `ccp.teleport` | Teleport to a claim group by name |
| `/chunkadmin` | `chunkclaim.admin` | Open the admin settings GUI & set player limits |

---

## 🛠️ Build & Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/AshinMc/ChunkClaimPlugin2.git
   ```
2. Build with Gradle:
   ```bash
   # Windows
   .\gradlew.bat build

   # Linux / macOS
   ./gradlew build
   ```
3. Copy `build/libs/ChunkClaimPlugin.jar` into your server's `plugins/` directory and restart your server!

---

## 📄 License
Released under the MIT License.
