<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" width="128">

# CCP (Chunk Claim Plugin 2)

Protect your land by claiming chunks with named claim groups, per-player trust, and a rich GUI.

- **Server:** Spigot / Paper 1.21.x & 26.1+
- **Java:** 21+
- **Version:** 0.6.0

---

## 🚀 Highlights in v0.6.0
- **Command Prefix Update:** The plugin alias is now officially \/ccp\ for faster typing!
- **Dual API Support:** Natively compiles two jars: one for \1.21.x\ and another for \26.1+\.
- **Item-Based Claiming:** Claim chunks by simply right-clicking with a configurable item (Default: Wooden Shovel).
- **Chunk Entry Titles:** See a Welcome Title and Subtitle when entering a claimed area.
- **Granular Permissions:** Full LuckPerms support with \ccp.claim\, \ccp.unclaim\, \ccp.check\, \ccp.info\, \ccp.visualize\, \ccp.expand\, \ccp.lang\, and \ccp.settings\.
- **GUI Enhancements:** Added 'Back' buttons for easier menu navigation.
- **Rename Claims:** Rename your claims seamlessly via an interactive chat prompt directly from the GUI.
- **Granular Interaction Settings:** Protect or allow access to specific blocks via the GUI:
  - Chests & Containers
  - Furnaces
  - Crafting/Utility blocks (Stonecutters, etc.)
  - Doors/Trapdoors
  - Redstone Inputs (Buttons/Levers)
- **New Languages:** Added full support for German (\de_DE\), Portuguese (\pt_BR\), and Russian (\
u_RU\).

For a full breakdown of features, permissions, and configuration, please see the [**Server Owner Wiki**](WIKI.md).

---

## 📜 Quick Commands

| Command | Alias | Description |
|---|---|---|
| \/claimchunk <name>\ | \/ccp claim <name>\ | Claim the chunk you're standing in with a group name |
| \/chunkexpand <name>\ | \/ccp expand <name>\ | Add the current chunk to an existing claim group |
| \/unclaimchunk [name]\ | \/ccp unclaim [name]\ | Unclaim a claim group by name, or the group you're standing on |
| \/checkchunk\ | \/ccp check\ | See who owns the current chunk and its claim name |
| \/infochunk\ | \/ccp info\ | List all your claim groups with chunk counts |
| \/visualizechunk [name]\| \/ccp visualize [name]\ | Visualize a claim group, current chunk's group, or all claims |
| \/chunklang\ | \/ccp lang\ | View/list/set your language (per-player) |
| \/chunksettings\ | \/ccp settings\ | Open the player settings GUI |
| \/chunkadmin\ | \/ccp admin\ | Open the admin settings GUI (requires \chunkclaim.admin\) |

---

## 🛠️ Build and Install
1. Clone the repository
2. Build the project:
   - Windows: `.\gradlew.bat build`
   - Linux/Mac: `./gradlew build`
3. The build script will automatically output two jars in `build/libs/`:
   - `ChunkClaimPlugin-26.1-0.6.0.jar` (For Spigot/Paper 26.1+)
   - `ChunkClaimPlugin-1.21.x-0.6.0.jar` (For older Spigot 1.21.x builds)
4. Drop the correct jar into your server's `plugins/` folder and restart!

---

## 🪄 Claiming Using an Item! (New in v0.6.0)
Don't want to type commands all day? Now you can claim chunks using your **Configured Claim Item**!
1. Grab your **Wooden Shovel** (or whatever you set in `config.yml`).
2. Stand inside an unclaimed chunk.
3. Simply **Right-Click** the air or a block!
4. The plugin will instantly pause and ask you in chat: *"What would you like to name this claim?"*
5. Type your desired name, and Boom! The borders burst into flames, and the land is yours. (Type `cancel` if you change your mind).

---

## 📖 Changelog

### v0.6.0
- **Major Architecture Update:** Brought the plugin to Minecraft 26.1 API native support.
- **Improved GUI & Experience:** Added Back Buttons, Rename Claim options, and 5 new modular item interaction toggles (Chests, Furnaces, Utilities, Doors, Redstone) right from the `/ccp settings` menu!
- **Added Dynamic Claim Item:** Right-click with your configured default item (Wooden Shovel) inside an unclaimed chunk to instantly grab it via a chat prompt.
- **Added Granular Permissions:** Fully migrated to `ccp.*` permission nodes for easier LuckPerms integration.
- **Added 3 New Languages:** `de_DE`, `pt_BR`, and `ru_RU` join our localization suite.
- **Dual APIs Built-In:** The build engine natively kicks out both `1.21.x` and `26.1` jars. Just drag and drop!
- **Welcome Titles:** Pop-up Titles and Subtitles now greet you (and visitors) automatically upon entering limits.

### v0.5.1
- Per-claim settings GUI with toggleable flags (mob griefing, mob spawning, mob entry, explosions, PvP)
- Full entity protection: minecarts, boats, armor stands, item frames, paintings
- Bugfix: vehicles/minecarts could be destroyed by non-owners

### v0.5
- Named claim groups (\/claimchunk <name>\, \/chunkexpand <name>\)
- Merged bounding-box visualization per claim group
- Current-chunk fallback for \/visualizechunk\, \/unclaimchunk\, \/checkchunk\
- Per-claim trust system with GUI (player heads toggle)

---

## 🗺️ Roadmap
- Pagination for large claim lists
- Offline player trust management
- Map integration
- Force load chunks (customisable for admins)

Issues and feature requests welcome!
