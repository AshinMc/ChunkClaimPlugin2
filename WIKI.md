# CCP (Chunk Claim Plugin 2) Server Wiki

Welcome to the definitive guide on setting up and managing CCP on your Spigot/Paper server!

---

## 💻 1. Installation

1. Depending on your server version, download the correct \.jar\:
   - Download \ChunkClaimPlugin-26.1-X.X.X.jar\ for **Minecraft 26.1**.
   - Download \ChunkClaimPlugin-1.21.x-X.X.X.jar\ for **Minecraft 1.21**.
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
| \chunkclaimprotection.bypass\ | Admins bypass protection flags | OP |
| \chunkclaim.admin\ | Open \/chunkadmin\ | OP |
| \chunkclaim.teleport\ | Teleport to a claimed chunk | OP |

*Tip: Add \ccp.*\, to give players all normal commands.*

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
  mob-griefing: true    # Block creeper, enderman theft, snow trails
  mob-spawning: false   # Allow normal mob spawning
  mob-entry: false      # Allow mobs to walk in (if true, invisible wall active)
  explosions: true      # Block TNT, Wither, Creeper block damage
  pvp: false            # Disallow PVP by default
  greeting-title: true  # Pop Welcome Titles and Subtitles when visiting
  
  # Block Interactions (Set to true to PROTECT them from visitors)
  interact-chest: true
  interact-furnace: true
  interact-stonecutter: true
  interact-door: true
  interact-redstone: true
\\\

---

## 🚪 4. The GUI Menus Explained

### The Player Settings GUI (\/ccp settings\)
A deeply integrated GUI allowing chunk owners to customize their individual experiences.

- **Rename Claims**: Players can rename existing chunks seamlessly via Chat Sync prompt.
- **Granular Toggle Switches**: Interaction toggles inside the *"Claim Settings"* section. Every group (Furnaces, Chests, Utility Blocks, etc.) can be checked individually. *(Green Wool = Allowed for Visitors, Red Wool = Denied for Visitors)*.
- **Trusted Players**: Selecting a player head grants them total bypass access over all active flags within that specific claim.

### The Admin GUI (\/ccp admin\)
Allows quick Server Administration modification to variables like *Max Claims* and changing the server's master locale or restarting \config.yml\ directly from the game without file restarts!

---

## 🔧 5. Claiming Chunks Without Commands

As of v0.5.2, if **\claim-item\** is defined in \config.yml\ (e.g., \WOODEN_SHOVEL\), a player can **right-click** while holding that item inside an unclaimed chunk. A chat prompt will ask them to name the claim (or they can type \cancel\).

Once answered correctly, the property bounds will instantly draw and associate the real estate to that user.

**Have an awesome server setup!**
