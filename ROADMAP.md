# Pending Technical Tasks & Engineering Roadmap

This document lists architectural bugs, security vulnerabilities, performance bottlenecks, and technical tasks identified during the codebase audit.

---

## 1. Data Integrity & Core Logic

### Fix Single-Chunk Unclaiming
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/UnclaimChunkCommand.java`
- **Issue:** Running `/unclaimchunk` without arguments calls `chunkManager.unclaimByName(...)`, unclaiming all chunks in the named claim group instead of just the chunk the player is standing in.
- **Task:** Update no-args logic to invoke `chunkManager.unclaimChunk(player, chunk)`.
- **Status:** ✅ **FIXED** — Split `onCommand` into two distinct paths. The no-args path now calls `chunkManager.unclaimChunk(player, chunk)` to remove exactly one chunk and issues a 1-chunk refund. The with-args path retains `unclaimByName` to unclaim entire named groups. Also pre-emptively stripped leading `/` from event-commands in both paths (see Console Event Command fix).

### Atomic & Asynchronous Data Persistence
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java`
- **Issue:** `saveData()` serializes the entire YAML map and executes blocking disk I/O on the main server thread on every claim, expand, rename, and GUI flag toggle. Direct file write risks file truncation to 0 bytes on server crash.
- **Task:** Offload serialization to an asynchronous worker. Write to `chunkclaims.yml.tmp` and replace atomically via `Files.move(..., StandardCopyOption.ATOMIC_MOVE)` or migrate data storage to SQLite.

### Clear Trust on Claim Transfer
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java`
- **Issue:** `transferClaim()` copies the previous owner's trusted player set to the new owner, granting the seller's friends access to the buyer's chests and blocks.
- **Task:** Clear `trustedPlayers` associated with the claim group upon ownership transfer.

### Chat Input Metadata Expiration
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChatRenameListener.java`
- **Issue:** The player metadata flags `ccp_renaming_claim` and `ccp_selling_claim` have no expiration. Unrelated chat messages sent hours later are intercepted as claim names or prices.
- **Task:** Store a millisecond timestamp in the metadata and ignore/remove it if older than 60 seconds.

### Claim Name Validation
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimChunkCommand.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkSettingsCommand.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChatRenameListener.java`
- **Issue:** Claim names accept arbitrary characters and unbounded lengths, which can cause YAML serialization anomalies and command argument parsing breaks.
- **Task:** Enforce length limit (maximum 24 characters) and regex pattern (`^[a-zA-Z0-9_-]+$`).

---

## 2. Grief Protection Engine

### Bucket Placement & Fluid Pickup
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `PlayerBucketEmptyEvent` and `PlayerBucketFillEvent` are not implemented. Visitors can empty lava/water buckets or pick up liquids inside claims.
- **Task:** Implement both events and cancel them if `!canModifyChunk(player, chunk)`.

### Cross-Border Fluid Inflow
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `BlockFromToEvent` is not implemented. Liquids poured outside a claim can flow across chunk boundaries, washing away redstone, crops, and burning wooden blocks.
- **Task:** Implement `BlockFromToEvent`. If destination block belongs to a claimed chunk and source chunk does not share the same owner, cancel the event.

### Piston Push & Pull Protection
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `BlockPistonExtendEvent` and `BlockPistonRetractEvent` are not handled. Pistons placed outside claims can push blocks in or pull blocks/containers out.
- **Task:** Implement both piston events. Inspect all blocks in `event.getBlocks()` and cancel if any block transitions into or out of a claimed chunk without matching ownership.

### Direct Fire Ignition
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `BlockIgniteEvent` only cancels `SPREAD`, `LAVA`, and `LIGHTNING`. Ignition from Flint & Steel (`IgniteCause.FLINT_AND_STEEL`) and Fire Charges (`IgniteCause.FIREBALL`) is not blocked.
- **Task:** Check `canModifyChunk(player, chunk)` for player-driven ignition causes.

### Indirect Damage & End Crystal Explosions
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `onPvP` resolves attackers via `resolveAttacker()`, which returns `null` for `EnderCrystal`, `RespawnAnchor`, and lingering potion clouds (`AreaEffectCloud`). End crystal detonation bypasses `pvp: false` inside claims.
- **Task:** Intercept damage events caused by crystals/anchors and cancel damage to players/animals inside claims where PvP or mob protection is enabled.

### Interactive Block Whitelist Coverage
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `onPlayerInteract` does not cover pressure plates (`Action.PHYSICAL` on `_PRESSURE_PLATE`), cauldrons, respawn anchors, composters, cakes, sweet berry bushes, dragon eggs, bells, and lecterns.
- **Task:** Add these materials to the interaction check under appropriate claim flags.

### Tree & Structure Growth
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `StructureGrowEvent` is not handled. Large trees (Dark Oak, Jungle) grown on borders replace blocks inside neighboring claims.
- **Task:** Implement `StructureGrowEvent` and strip out block changes falling inside chunks owned by other players.

### Vehicle & Mount Controls
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** Visitors can place boats or minecarts in claims via item interaction and use them to move or trap claimed villagers/mobs. Visitors can also mount claimed animals.
- **Task:** Block vehicle placement in `onPlayerInteract` and cancel `EntityMountEvent` for untrusted visitors on claimed entities.

### Nether Portal Block Replacement
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `PortalCreateEvent` is not handled. Linking a nether portal from the Nether can generate an obsidian portal structure inside an Overworld claim without the owner present.
- **Task:** Implement `PortalCreateEvent` and cancel portal generation if it intersects chunks not owned by the creating player.

---

## 3. Performance & Resource Optimization

### Remove Global Entity Task in `mob-entry`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `startMobEntryTask` queries `world.getLivingEntities()` across all worlds every 10 ticks, allocating new lists and iterating thousands of entities on the main thread. If a mob is between protected chunks, `entity.remove()` is called, permanently deleting player pets, horses, and named villagers.
- **Task:** Remove the global polling task. Switch to event-driven checks (e.g. tracking chunk crossings) or only scan entities within loaded chunks that have `mob-entry: true`. Add an explicit exclusion preventing `entity.remove()` on `Tameable`, named entities (`getCustomName() != null`), and iron golems.

### Optimize `PlayerMoveListener`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/PlayerMoveListener.java`
- **Issue:** `event.getFrom().getChunk()` and `event.getTo().getChunk()` are called on every micro-movement before checking if the chunk coordinates changed, creating continuous chunk cache lookups.
- **Task:** Add coordinate bitshift pre-check:
  ```java
  if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
      && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)
      && from.getWorld() == to.getWorld()) {
      return;
  }
  ```

### Per-Player Visualization Packets
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/VisualizeChunkCommand.java`
- **Issue:** `player.getWorld().spawnParticle(...)` broadcasts all border particles to all nearby players in the world, generating unnecessary network traffic and client rendering lag.
- **Task:** Change to `player.spawnParticle(...)` to send packets only to the player running the visualization.

### Concurrency & Thread-Safety in PlaceholderAPI Expansion
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/placeholders/ChunkClaimExpansion.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java`
- **Issue:** Placeholder expansions are evaluated asynchronously by scoreboard and tablist plugins. `ChunkManager` uses raw `HashMap` collections, causing `ConcurrentModificationException` and server deadlocks on concurrent read/write operations.
- **Task:** Convert `chunkOwners`, `chunkNames`, `trustedPlayers`, `claimFlags`, `playerLimits`, and `claimPrices` to `ConcurrentHashMap`. Use bitshifts for player location chunks instead of async `.getChunk()` calls.

---

## 4. Economy & Marketplace Engine

### Marketplace Listing Disambiguation
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimTradeCommand.java`
- **Issue:** `/claimtrade buy <claimName>` iterates `allClaimsForSale.entrySet()`. If two players sell claims with the same name (e.g. `"Base"`), it purchases whichever listing appears first in the map regardless of seller or price.
- **Task:** Enforce buying syntax by listing ID or `seller:claimName` (e.g. `/claimtrade buy <seller> <claim>`).

### Item Economy Meta Protection
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/economy/ItemEconomyProvider.java`
- **Issue:** `countItems()` and `withdraw()` only check `item.getType() == baseMaterial`. Custom RPG items, named items, enchanted tools, and items with PersistentDataContainer tags are consumed and destroyed as raw currency.
- **Task:** Verify that `item.hasItemMeta()` is false or that display name, lore, enchantments, and custom tags are empty before counting or withdrawing the item.

### Inventory Overflow Fallback
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/economy/ItemEconomyProvider.java`
- **Issue:** When giving change or refunding unclaims with a full inventory, items are dropped on the ground via `dropItemNaturally()`, risking loss in lava or theft.
- **Task:** Store overflow currency into `pending_economy.yml` for claim via login or command when inventory space is available.

### Unclaim Refund Exploitation (Infinite Money Exploit)
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/economy/EconomyManager.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimTradeCommand.java`
- **Issue:** `getUnclaimRefund()` calculates refunds based on theoretical server progressive price formulas rather than historical cost. Players can buy multi-chunk bases cheaply on `/claimtrade` and immediately unclaim them to generate free money from the server treasury.
- **Task:** Do not refund claims obtained via marketplace trading, or track actual paid cost per chunk.

---

## 5. Navigation & World Handling

### Safe `/chunktp` Positioning
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkTeleportCommand.java`
- **Issue:** Uses `world.getHighestBlockYAt(centerX, centerZ) + 1`. In the Nether, this targets Y=128 (above bedrock ceiling). In caves or underground bases, it teleports to the surface.
- **Task:** In Nether or below-ground environments, scan vertically for 2 contiguous air blocks above a solid, non-hazardous block (no lava or fire).

### Accurate Border Visualization
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/VisualizeChunkCommand.java`
- **Issue:** Calculates a single minimum and maximum bounding box across all chunks in a group. For L-shaped or disconnected chunks, it draws a rectangular perimeter enclosing unclaimed wilderness.
- **Task:** Iterate each chunk individually and render outer perimeter borders per-chunk.

### WorldGuard `__global__` Bypass
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/handlers/WG7WorldGuardBridge.java`
- **Issue:** `set.isOwnerOfAll(localPlayer)` returns `false` if `__global__` has any flags set, preventing all claiming server-wide because no player owns `__global__`.
- **Task:** Exclude regions with `getId().equalsIgnoreCase("__global__")` or test build flags directly via `set.testState(localPlayer, Flags.BUILD)`.

### World Blacklist Option
- **Files:**
  - `src/main/resources/config.yml`
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java`
- **Issue:** Claiming cannot be disabled in specific worlds, allowing players to claim around End portals, End gateways, or minigame arenas.
- **Task:** Add `disabled-worlds: []` list in `config.yml` and check `disabledWorlds.contains(chunk.getWorld().getName())` in `canClaimChunk()`.

### Pagination for `/infochunk`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/InfoChunkCommand.java`
- **Issue:** Dumps all owned chunks in chat sequentially without paging, flooding chat for players with large claims.
- **Task:** Implement `/infochunk [page]` with 6–8 claims per page.

---

## 6. Advanced Exploits & Interaction Defenses

### Sign Editing (1.20+) Defacement
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `SignChangeEvent` is not registered. Visitors can right-click any sign or hanging sign in a claim to edit the text on both sides.
- **Task:** Register `SignChangeEvent` and cancel if `!canModifyChunk(player, event.getBlock().getChunk())`.

### Fishing Rod Pulling / Entity Theft
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `PlayerFishEvent` is missing. Visitors can cast fishing rods onto claimed horses, farm animals, or armor stands from the wilderness and pull them out across boundaries.
- **Task:** Register `PlayerFishEvent` and cancel if `event.getCaught()` is in a protected chunk without modify permissions.

### Leash / Lead Breaking
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `PlayerLeashEntityEvent` and `PlayerUnleashEntityEvent` are missing. Visitors can attach leads to private animals or punch leads off fence posts.
- **Task:** Intercept both events and verify `canModifyChunk()`.

### Chorus Fruit & Pearl Vault Infiltration
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `PlayerTeleportEvent` is not handled. Eating Chorus Fruit outside enclosed claims teleports players randomly through walls into locked vaults.
- **Task:** Register `PlayerTeleportEvent` and block `TeleportCause.CHORUS_FRUIT` and `TeleportCause.ENDER_PEARL` into claims without modify access.

### Frost Walker Water Griefing
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `EntityBlockFormEvent` explicitly ignores players (`if (event.getEntity() instanceof Player) return;`), allowing visitors with Frost Walker boots to freeze water systems into frosted ice.
- **Task:** Check player modify permissions on the chunk before allowing Frost Walker ice formation.

### Decouple Armor Stand Damage from Mob Protection
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** Armor stands are guarded strictly by the `mob-protection` (animal protection) flag. If mob-protection is disabled for combat, visitors can punch and break armor stands, looting equipped gear.
- **Task:** Check container/block modification rules (`canModifyChunk`) for `ArmorStand` damage rather than mob protection flags.

### Bad Omen Village Raid Invasions
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `RaidTriggerEvent` is not handled. Visitors with Bad Omen can trigger raids in claimed villages.
- **Task:** Register `RaidTriggerEvent` and cancel if the triggering player is not trusted on the claim.

### Entity Crop & Egg Trampling
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `EntityInteractEvent` is missing. Mobs, horses, ravagers, and wandering traders can trample farmland and turtle eggs inside claims.
- **Task:** Register `EntityInteractEvent` and cancel `Action.PHYSICAL` on `FARMLAND` and `TURTLE_EGG` in claimed chunks.

---

## 7. Configuration, Reloading & Synchronization Deficiencies

### PvP Flag Semantic Inversion
- **Files:**
  - `src/main/resources/config.yml`
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `pvp: false` in `config.yml` is documented as `# Disable PvP combat inside the claim`, but in `onPvP`, `isChunkFlagEnabled(FLAG_PVP)` checks for `true` to cancel damage. By default, PvP is active in claims while the comment states it is disabled.
- **Task:** Invert check or rename to `block-pvp: true`.

### Comment Wiping in `saveConfig()` and `ConfigMigrator`
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/config/ConfigMigrator.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/AdminSettingsGUIListener.java`
- **Issue:** Using `plugin.saveConfig()` invokes standard SnakeYAML, which strips all YAML comments and section headers when saving or migrating.
- **Task:** Use Bukkit's comment-preserving YAML API (`parseComments` / `copyDefaults`) or modify keys directly without dumping SnakeYAML output over formatted files.

### Reload Desynchronization (`/chunkadmin reload`)
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkAdminCommand.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/AdminSettingsGUIListener.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/economy/EconomyManager.java`
- **Issue:** There is no console/CLI command for `/chunkadmin reload`. The GUI reload button reloads messages and config, but never invokes `economyManager.reload()`, leaving economy settings desynchronized until a server restart.
- **Task:** Add `/chunkadmin reload` CLI command and call `economyManager.reload()`.

### Cached `final serverDefaultLocale` Desynchronization
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/managers/MessageManager.java`
- **Issue:** `serverDefaultLocale` is declared `private final String` initialized on startup. Changing default language in `/chunkadmin` updates `config.yml`, but `serverDefaultLocale` remains unchanged in memory.
- **Task:** Make `serverDefaultLocale` non-final and update it during config reload.

### Console Event Command Leading Slash Failure
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimChunkCommand.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkExpandCommand.java`
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/UnclaimChunkCommand.java`
- **Issue:** `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)` fails if an admin writes `/give %player% diamond 1` in `config.yml` because the console command dispatcher does not accept leading slashes.
- **Task:** Strip leading `/` before dispatching console event commands.
- **Status:** ✅ **FIXED** — Added `String stripped = cmd.startsWith("/") ? cmd.substring(1) : cmd;` before every `Bukkit.dispatchCommand` call in `ClaimChunkCommand`, `ChunkExpandCommand`, `UnclaimChunkCommand`, and `ItemClaimListener`.

### Synchronous Mojang API Lookups in Admin Commands
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkAdminCommand.java`
- **Issue:** Calling `Bukkit.getOfflinePlayer(targetName)` with a username initiates a blocking synchronous HTTP request to Mojang API if the player profile is not cached, causing 2–5 second server freezes.
- **Task:** Resolve profiles asynchronously or verify from cached server offline player lists before falling back to network queries.

### Phantom Config Keys: `particle-height` & `particle-spacing`
- **Files:**
  - `src/main/resources/config.yml` (lines 46–48)
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/VisualizeChunkCommand.java` (lines 28–29, 227–246)
- **Issue:** `config.yml` provides `particle-height: 100` and `particle-spacing: 0.5`. However, `VisualizeChunkCommand` uses `private static final int PARTICLE_HEIGHT = 100` and `private static final double PARTICLE_SPACING = 0.5` without ever reading the configuration keys. Changes made by administrators are silently ignored.
- **Task:** Read `visualization.particle-height` and `visualization.particle-spacing` from `config.yml` with sensible bounds validation.

### Unconditional Startup Comment Wiping in `ConfigMigrator`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/config/ConfigMigrator.java` (lines 48–50)
- **Issue:** When `detectedVersion >= CURRENT_CONFIG_VERSION`, `ConfigMigrator` executes `plugin.getConfig().options().copyDefaults(true)` and `plugin.saveConfig()`. In Bukkit, calling `saveConfig()` without comment preservation erases all 7 section headers and inline comments from `config.yml` on every single server boot.
- **Task:** Remove unconditional `saveConfig()` on startup when no schema migration is required.

### NullPointerException Crashes on Blank Config Keys
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/PlayerMoveListener.java` (lines 61, 110)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/AdminSettingsGUIListener.java` (lines 89–90)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ItemClaimListener.java` (line 43)
- **Issue:** If an administrator leaves a scalar configuration key blank in YAML (e.g. `greeting-display:`, `claim-item:`, or `locale:`), Bukkit's `getString(path, def)` returns `null`. Directly invoking `.toUpperCase()` or `.equalsIgnoreCase()` triggers an uncaught `NullPointerException` on every player movement packet across chunk boundaries.
- **Task:** Null-check string returns before transforming cases, or fall back explicitly: `String raw = plugin.getConfig().getString("greeting-display"); String mode = (raw != null && !raw.isEmpty()) ? raw.toUpperCase() : "ACTION_BAR";`.

### Event Command Bypass on Right-Click Shovel Claiming
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ItemClaimListener.java` (lines 114–128)
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimChunkCommand.java` (lines 109–126)
- **Issue:** Claiming via `/claimchunk` dispatches `event-commands.claim-success` and `claim-fail`. Right-clicking with the claim tool (`claim-item: "WOODEN_SHOVEL"`) completely bypasses console event command dispatching, breaking third-party server integrations (e.g. permission upgrades, Discord webhooks).
- **Task:** Dispatch `event-commands.claim-success` and `claim-fail` inside `ItemClaimListener.java` matching command execution behavior.
- **Status:** ✅ **FIXED** — `ItemClaimListener` now dispatches `event-commands.claim-success` on successful item-claim and `event-commands.claim-fail` on failure, with leading-slash stripping applied.

### Message Toggle Inconsistency in `/chunkexpand`
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkExpandCommand.java` (line 97)
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimChunkCommand.java` (line 101)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ItemClaimListener.java` (line 116)
- **Issue:** `/claimchunk` and shovel claiming respect `message-toggles.claim-success: false`. `/chunkexpand` sends the success message unconditionally, ignoring the administrator's configured message toggle.
- **Task:** Wrap expand success message delivery with `if (messages.isMessageEnabled("claim-success"))`.

### Deprecated Particle Enum in Config Comments (`REDSTONE` vs `DUST`)
- **Files:**
  - `src/main/resources/config.yml` (line 49)
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/VisualizeChunkCommand.java` (lines 258–265)
  - `src/main/java/org/ashin/chunkClaimPlugin2/gui/AdminSettingsGUI.java` (line 275)
- **Issue:** `config.yml` lists `REDSTONE` in its comment recommendation. In modern Spigot/Paper 1.20+, `Particle.REDSTONE` was renamed to `Particle.DUST`. Setting `particle-type: REDSTONE` throws `IllegalArgumentException` on `Particle.valueOf()` and silently reverts to `FLAME`.
- **Task:** Update `config.yml` comments to recommend `DUST` and add an alias resolver mapping `REDSTONE` $\rightarrow$ `DUST`.

### Economy Currency Crash on `Material.AIR` and Non-Item Materials
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/economy/EconomyManager.java` (lines 71–73)
  - `src/main/java/org/ashin/chunkClaimPlugin2/economy/ItemEconomyProvider.java` (lines 24–25, 127)
- **Issue:** Setting `economy.item.material: "AIR"` or specifying a technical non-item block (`WATER`, `PISTON_HEAD`) resolves to a non-null `Material`. Calling `new ItemStack(mat)` subsequently throws `IllegalArgumentException` during economy transactions.
- **Task:** Validate `mat != null && mat.isItem() && !mat.isAir()`, logging a severe error and falling back to `GOLD_INGOT` if invalid.

### Naive Pluralization in Item Currency Display
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/economy/ItemEconomyProvider.java` (line 71)
- **Issue:** Currency formatting blindly appends `"s"` to `display-name` (e.g. `count + " " + displayName + (count == 1 ? "" : "s")`), generating grammatically invalid labels such as `"10 Golds"`.
- **Task:** Add `singular-name` and `plural-name` configuration fields in `config.yml` under `economy.item`.

### High-Frequency Dynamic Config Lookups on Hot Paths
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/PlayerMoveListener.java` (lines 61, 110, 119–127)
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/MessageManager.java` (line 230)
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java` (line 598)
- **Issue:** Dynamic string-path YAML lookups are executed on every chunk boundary crossing (`greeting-display`, `title-duration.*`), every chat message (`messages.*`), and every block/entity interaction (`claim-flags.*`) on the main server thread instead of being cached in memory fields.
- **Task:** Cache parsed configuration models (GreetingSettings, TitleDurationSettings, FlagDefaults) during startup and refresh them only on `/chunkadmin reload`.
