# CCP (Chunk Claim Plugin 2) Server Wiki

Welcome to the definitive guide on setting up and managing CCP on your Spigot/Paper server!

---

## 💻 1. Installation

1. Download the universal JAR file:
   - Download \ChunkClaimPlugin.jar\ - Works with **Minecraft 1.21.0-1.21.11 and 26.1+**
2. Drop the plugin into your \plugins/\ folder.
3. Restart or reload your server.

**Integrations:** 
CCP natively supports **WorldGuard** and **WorldEdit** overlap protection to ensure players don't claim over existing server regions. They are treated as soft dependencies.

---

## 🛠️ 2. Permissions (LuckPerms)

The plugin has been refactored in v0.5.2 to use the much shorter \ccp.*\ permission node map, fully supporting server groups (like LuckPerms, Vault, etc.).

| Permission Node | Action Granted | Default |
|---|---|---|
| \ccp.claim\ | Claim a chunk (command/item) | Everyone |
| \ccp.expand\ | Add a chunk to an existing group | Everyone |
| \ccp.unclaim\ | Unclaim a chunk | Everyone |
| \ccp.check\ | Use \/checkchunk\ | Everyone |
| \ccp.info\ | Use \/infochunk\ | Everyone |
| \ccp.visualize\ | Use \/visualizechunk\ | Everyone |
| \ccp.settings\ | Open \/chunksettings\ GUI | Everyone |
| \ccp.lang\ | Change personal language | Everyone |
| \ccp.teleport\ | Use \/chunktp\ command | Everyone |
| \ccp.transfer\ | Transfer claim ownership | Everyone |
| \chunkclaimprotection.bypass\ | Admins bypass protection flags | OP |
| \chunkclaim.admin\ | Open \/chunkadmin\ | OP |

*Tip: Add \ccp.*\ to give players all normal commands.*

---

## ⚙️ 3. Configuration & Defaults

Here is the default \config.yml\ that will generate during the first boot. 
Admins can either edit this file and use \/chunkadmin\ GUI to reload the configuration, or alter the settings natively via the \/chunkadmin\ interface.

\\\yaml
# Default language out of the 7 provided (en_US, es_ES, fr_FR, zh_CN, de_DE, pt_BR, ru_RU)
locale: "en_US"

# The amount of chunk properties a regular player can possess
max-claims-per-player: 10

# Allow players to claim chunks using an item instead of the /ccp claim command
# To disable, set to false
claim-item: "WOODEN_SHOVEL"

# Visual border particle (Requires particle name)
visualization:
  particle-type: "FLAME"

# Default flags generated on newly created chunks
claim-flags:
  mob-griefing: true           # Block creeper, enderman theft, snow trails
  mob-spawning: false          # Allow normal mob spawning
  mob-entry: false             # Allow mobs to walk in (if true, invisible wall active)
  mob-protection: true         # Protect passive mobs (cows, pigs, etc.) from player damage
  explosions: true             # Block TNT, Wither, Creeper block damage
  pvp: false                   # Disallow PVP by default
  greeting-title: true         # Pop Welcome Titles and Subtitles when visiting
  
  # Block Interactions (Set to true to PROTECT them from visitors)
  interact-chest: true
  interact-furnace: true
  interact-stonecutter: true
  interact-door: true
  interact-redstone: true

# Message toggles: Admins can suppress specific messages without code changes
# Set to false to disable the message from appearing in chat
message-toggles:
  deny-break: true             # Show "cannot break blocks" message
  deny-place: true             # Show "cannot place blocks" message
  deny-interact: true          # Show "cannot interact" message
  deny-entity: true            # Show "cannot damage entities" message
  deny-pvp: true               # Show "cannot PVP" message
  claim-success: true          # Show successful claim message
  unclaim-success: true        # Show successful unclaim message
\\\

---

## 🚪 4. The GUI Menus Explained

### The Player Settings GUI (\/ccp settings\)
A deeply integrated GUI allowing chunk owners to customize their individual experiences.

- **Rename Claims**: Players can rename existing chunks seamlessly via Chat Sync prompt.
- **Claim Details**: Access detailed information about your claims including:
  - **Transfer Ownership**: Transfer claim ownership to another online player while preserving all settings, trusted players, and flags.
  - **Teleport**: Teleport to the center of your claim with safe landing height.
  - **Trusted Players**: Grant other players bypass access over all active flags within that specific claim.
  - **Claim Settings**: Toggle protection flags on/off for your specific claim group.
- **Granular Toggle Switches**: Interaction toggles inside the *"Claim Settings"* section. Every group (Furnaces, Chests, Utility Blocks, Mobs, etc.) can be checked individually. *(Green = Allowed, Red = Denied)*.

### The Admin GUI (\/ccp admin\)
Allows quick Server Administration modification to variables like *Max Claims* and changing the server's master locale or restarting \config.yml\ directly from the game without file restarts!

### Message Toggle Configuration
Admins can now control which protection denial messages appear to players by editing the \message-toggles\ section in \config.yml\. This allows you to:
- Hide repetitive "cannot break blocks" messages if you prefer a different approach
- Show only critical messages like PvP denials
- Suppress success messages to reduce chat spam
- Customize the player experience without code changes

*Example: Set \deny-break: false\ to prevent the "You cannot break blocks" message from appearing when players try to damage blocks in claimed chunks.*

---

## 🔧 5. Claiming Chunks Without Commands

As of v0.5.2, if **\claim-item\** is defined in \config.yml\ (e.g., \WOODEN_SHOVEL\), a player can **right-click** while holding that item inside an unclaimed chunk. A chat prompt will ask them to name the claim (or they can type \cancel\).

Once answered correctly, the property bounds will instantly draw and associate the real estate to that user.

---

## ✨ 6. Key Features Explained

### Mob Protection Flag
The \mob-protection\ flag controls whether players can damage **passive mobs** (cows, pigs, sheep, horses, etc.) within claimed chunks:
- **Enabled (true)**: Players CANNOT hurt passive mobs in the claim
- **Disabled (false)**: Players CAN hurt passive mobs
- **Hostile Mobs**: Zombies, creepers, and other hostile mobs can ALWAYS be damaged, regardless of this setting

This allows server admins to protect animal farms while still letting players defend against monsters.

### Teleport Command
Use \/chunktp <name>\ to quickly teleport to any of your claims:
- Teleports to the chunk center at a safe height
- Works across different worlds
- Respects player permissions (\ccp.teleport\)

### Transfer Ownership
Transfer claim ownership directly from the GUI:
1. Open \/chunksettings\ and navigate to a claim's details
2. Click the "Transfer Ownership" button (ENDER_EYE icon)
3. Select an online player
4. Confirm the transfer
5. All settings, trusted players, and flags are automatically preserved!

This is useful for:
- Handing off claims to clan members
- Transferring land to new players
- Management of multi-player bases

### Player vs Player Combat Fix
The PvP flag now correctly controls **only player-to-player combat**:
- **PvP Enabled**: Players can attack each other
- **PvP Disabled**: Players cannot attack each other
- This setting does NOT affect mob interactions or other entity damage

Previously, all entity damage was blocked uniformly, preventing even hostile mob combat.

---

## 💡 7. Tips for Server Admins

- Use \message-toggles\ to customize the player experience and reduce chat spam
- Set \mob-protection: false\ if your server focuses on player-run farms without interference
- Limit \max-claims-per-player\ on large servers to manage claim density
- Enable \greeting-title\ for immersive claim entry announcements
- Use the \/chunkadmin\ GUI to quickly adjust settings without file edits

**Have an awesome server setup!**
