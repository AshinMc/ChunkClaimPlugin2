# ChunkClaimPlugin - Land Protection for Spigot/Paper

Protect your land by claiming chunks with named claim groups, per-claim trust management, and granular control over mob griefing, explosions, and PvP.

## 🚀 Features

- **Named Claim Groups** - Claim multiple chunks under one name and expand anytime
- **Dual Version Support** - Single JAR works on Minecraft 1.21.0-1.21.11 AND 26.1+ servers
- **GUI-Based Management** - No config file edits needed; use `/chunksettings` to manage everything
- **Per-Claim Trust System** - Grant other players building access for specific claim groups
- **Granular Protection Flags** - Toggle mob griefing, spawning, explosions, PvP, and block interactions per claim
- **Mob Protection** - Protect passive mobs (cows, pigs, etc.) from being damaged; hostile mobs always killable
- **Claim Teleportation** - Use `/chunktp <name>` to instantly teleport to your claims
- **Transfer Ownership** - Hand off claims to other players with all settings preserved
- **Message Customization** - Admins can suppress specific chat messages without code changes
- **Multi-Language Support** - 7 languages: English, Spanish, French, Chinese, German, Portuguese, Russian
- **WorldGuard Integration** - Prevents claiming over WorldGuard regions
- **Item-Based Claiming** - Right-click with a configurable item (default: Wooden Shovel) to claim chunks
- **Entry Titles** - Players see welcome titles when entering claimed areas

## ⚡ Quick Start

1. **Install**
   - Download `ChunkClaimPlugin.jar`
   - Drop into your `plugins/` folder
   - Restart your server

2. **Claim a Chunk**
   - Option A: Hold a Wooden Shovel and right-click in an unclaimed chunk
   - Option B: Use `/claimchunk <name>` or `/ccp claim <name>` command

3. **Manage Your Claims**
   - Type `/chunksettings` or `/ccp settings` to open the GUI
   - Invite trusted players, toggle protection flags, rename claims

4. **Teleport** (v0.6.0+)
   - Use `/chunktp <name>` or `/ccp tp <name>` to teleport to any of your claims

## 📋 Essential Commands

| Command | Alias | Purpose |
|---|---|---|
| `/claimchunk <name>` | `/ccp claim <name>` | Claim the chunk you're standing in |
| `/chunkexpand <name>` | `/ccp expand <name>` | Add current chunk to an existing claim |
| `/unclaimchunk [name]` | `/ccp unclaim [name]` | Unclaim a claim group |
| `/checkchunk` | `/ccp check` | See who owns the current chunk |
| `/infochunk` | `/ccp info` | List all your claim groups |
| `/chunksettings` | `/ccp settings` | Open player settings GUI |
| `/chunkadmin` | `/ccp admin` | Open admin settings GUI (OP only) |
| `/chunktp <name>` | `/ccp tp <name>` | Teleport to a claim (v0.6.0+) |
| `/visualizechunk [name]` | `/ccp visualize [name]` | Visualize claim boundaries |
| `/chunklang` | `/ccp lang` | Change your language (per-player) |

## 🛡️ Protection Flags

Each claim group has toggleable settings:

| Flag | Purpose | Default |
|---|---|---|
| Mob Griefing | Block creeper/enderman damage | ON |
| Mob Spawning | Prevent mob spawns | OFF |
| Mob Entry | Invisible wall keeps mobs out | OFF |
| Mob Protection | Protect passive mobs from players | ON |
| Explosions | Block TNT/Wither damage | ON |
| PvP | Prevent player combat | OFF |
| Block Interactions | Protect chests, furnaces, doors | ON |

## 🔑 Permissions

- `ccp.claim` - Claim chunks
- `ccp.unclaim` - Unclaim chunks
- `ccp.expand` - Expand claims
- `ccp.check` - Check chunk ownership
- `ccp.info` - List own claims
- `ccp.settings` - Open player GUI
- `ccp.visualize` - Visualize claims
- `ccp.lang` - Change personal language
- `ccp.teleport` - Teleport to claims (v0.6.0+)
- `ccp.transfer` - Transfer claim ownership (v0.6.0+)
- `chunkclaim.admin` - Admin GUI (OP only)
- `chunkclaimprotection.bypass` - Bypass all protection (OP only)

## 📚 Configuration

The `config.yml` file supports:

```yaml
locale: "en_US"                    # Default server language
max-claims-per-player: 10          # Claims per player
claim-item: "WOODEN_SHOVEL"        # Item for right-click claiming
mob-protection: true               # Protect passive mobs by default
message-toggles:                   # Admin control over chat messages
  deny-break: true
  deny-place: true
  deny-pvp: true
  claim-success: true
  # ... and more
```

For complete admin documentation, see the [**Server Owner Wiki**](WIKI.md).

## 🌍 Localization

Full support for:
- English (en_US)
- Spanish (es_ES)
- French (fr_FR)
- Chinese (zh_CN)
- German (de_DE)
- Portuguese (pt_BR)
- Russian (ru_RU)

Players can change their language per-account with `/ccp lang set <locale>`.

## 🔄 Trust System

Grant other players building access to specific claims:

1. Open `/ccp settings` → Claims → click a claim
2. Click "Trusted Players"
3. Click player heads to toggle trust (green = trusted)
4. Trust is claim-group specific

## ⚙️ Chunk Settings GUI (`/chunksettings`)

Everything you need to manage your claims in one place:

### 🏠 Home Screen
- **View Your Claims** - See all your claim groups and chunk counts
- **Visualize Claims** - View bounding boxes for your claims
- **Delete Claims** - Remove claim groups you no longer need
- **Particle Selection** - Choose your visualization particle (FLAME, REDSTONE, etc.)
- **Change Language** - Set your per-player language preference

### 📦 Claims Management
1. Navigate to **Claims** from the home screen
2. Select a claim group to see:
   - **Claim Details** - View chunk count, owner, creation info
   - **Teleport** (v0.6.0+) - Jump to the center of the claim
   - **Transfer Ownership** (v0.6.0+) - Hand off to another player
   - **Trusted Players** - Manage who can build here
   - **Claim Settings** - Toggle protection flags
   - **Rename Claim** - Change the claim group name via chat

### 🎯 Claim Settings (Per-Claim Flags)
Toggle protection for your specific claim:
- **Mob Griefing** - Block creeper/enderman damage
- **Mob Spawning** - Prevent natural mob spawns
- **Mob Entry** - Create invisible wall against mobs
- **Mob Protection** - Protect passive animals (cows, pigs)
- **Explosions** - Block TNT/Wither damage
- **PvP** - Allow/disable player combat
- **Chest Interactions** - Protect chests and containers
- **Furnace Interactions** - Protect furnaces
- **Utility Interactions** - Protect stonecutters and crafting blocks
- **Door Interactions** - Protect doors and trapdoors
- **Redstone Interactions** - Protect buttons and levers

(Green = Allowed, Red = Denied)

## 🎯 What's New in v0.7.0

- ✨ Multi-version support for Minecraft 1.19.x, 1.20.x, 1.21.x and 26.1+
- ✨ Individual Chunk Limits configuration for admins
- ✨ External Command integration for Claim/Unclaim events
- ✨ Indefinite Chunk Visualization support
- ✨ Crop Trampling protection fix in claimed chunks
- ✨ Java 17 support for older server instances

## 🎯 What's New in v0.6.0+

- ✨ Single JAR compatibility with Minecraft 1.21.x and 26.1+
- ✨ Claim ownership transfer directly from GUI
- ✨ `/chunktp <name>` teleport command
- ✨ Message customization system for admins
- ✨ Fixed player-to-player combat handling
- ✨ Mob protection toggle for passive mobs
- ✨ Enhanced GUI with Back buttons
- ✨ Full support for German, Portuguese, Russian

## 🐛 Bug Reports & Features

Found a bug or have a feature request?

- **Discord:** Join our community Discord
- **Issues:** Report on GitHub

## 📖 Advanced Setup

For detailed server configuration, claim management strategies, and admin tips, see the comprehensive [**Server Owner Wiki**](WIKI.md).

## 📜 License

See LICENSE file in repository.

---

**Protect your land. Organize your world. Claim your chunks.**
