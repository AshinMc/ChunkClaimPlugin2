# Current Issues & Engineering Roadmap (Target: v0.9.0)

This document lists the **32 open bugs, security/grief bypasses, economy loopholes, Spigot/Paper compatibility issues, and code quality issues** identified in the v0.8.5 codebase audit, scheduled to be fixed for **v0.9.0**.

---

## 1. Critical Economy, Limit & Data Integrity Bugs

### 1. GUI Claim Deletion Skips Economy Refunds & Event Commands
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (lines 362–378)
- **Issue:** Deleting a claim group through the `/chunksettings` GUI (`DELETE_CONFIRM`) calls `chunkManager.unclaimByName(...)` and saves data, but never calculates or deposits the unclaim refund (`economyManager.getUnclaimRefundForTracked(...)`), never checks `messages.isMessageEnabled("unclaim-success")`, and never dispatches `event-commands.unclaim-success`.
- **Task:** Mirror `UnclaimChunkCommand`'s tracked-cost refund calculation, message toggle check, and console `event-commands.unclaim-success` execution inside `SettingsGUIListener`.

### 2. GUI Ownership Transfer Bypasses Target Player's Max Claim Limit
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (lines 389–410)
- **Issue:** Unlike `/chunksettings transfer <claim> <player>`, the GUI `TRANSFER_CONFIRM` handler calls `chunkManager.transferClaim(...)` directly without checking whether the recipient would exceed `max-claims-per-player` (and without notifying the recipient).
- **Task:** Enforce target player claim-limit validation in `SettingsGUIListener` before transferring ownership and send the recipient notification message.

### 3. Payment Executed Before `ChunkTransferEvent` & Name Collision Merging on Transfer/Purchase
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimTradeCommand.java` (lines 252–265)
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java` (lines 260–292)
- **Issue:**
  1. `ClaimTradeCommand.handleBuy()` withdraws from the buyer and deposits to the seller **before** calling `chunkManager.transferClaim(...)`. If `ChunkTransferEvent` is cancelled by an external plugin (or `transferred == 0`), the buyer loses money and receives 0 chunks.
  2. If the buyer/recipient already owns a claim group with the same `claimName`, `transferClaim()` merges the transferred chunks into the buyer's existing group, overwrites the buyer's existing `claimFlags` with the seller's flags, removes the buyer's sale listing (`claimPrices.remove(newKey)`), and exposes the new chunks to the buyer's existing trusted players.
- **Task:** Transfer ownership (or verify non-cancelled `ChunkTransferEvent`) before finalizing/depositing payment (refunding buyer on cancellation), and prevent or disambiguate claim name collisions on transfer/purchase.

### 4. `NaN` and `Infinity` Price Exploit in `/claimtrade sell` & Chat Sell Prompt
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimTradeCommand.java` (lines 113–119)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChatRenameListener.java` (lines 100–112)
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java` (lines 443–450)
- **Issue:** `Double.parseDouble("NaN")` returns `Double.NaN` (where `price <= 0.0` evaluates to `false`), and `"Infinity"` returns `Double.POSITIVE_INFINITY`. Neither validator checks `Double.isFinite(price)`, allowing players to list claims for `NaN` or `Infinity` and corrupt `chunkclaims.yml`.
- **Task:** Validate `Double.isFinite(price) && price > 0.0` in `ClaimTradeCommand`, `ChatRenameListener`, and `ChunkManager.setClaimPrice()`.

### 5. Single-Chunk `/unclaimchunk` Leaves Orphaned Ghost Listings on `/claimtrade list`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java` (lines 189–215)
- **Issue:** Unlike `adminUnclaimChunk()`, `unclaimChunk(...)` does not check whether the removed chunk was the last remaining chunk in its claim group. Unclaiming the final chunk of a listed claim via `/unclaimchunk` (no args) leaves `claimPrices`, `trustedPlayers`, and `claimFlags` orphaned—allowing players to buy a 0-chunk ghost listing on `/claimtrade`.
- **Task:** Clean up `trustedPlayers`, `claimFlags`, and `claimPrices` in `unclaimChunk(...)` when `remaining == 0` for that `(owner, claimName)` group.

### 6. Free-Claim Tier Reset Exploit via Selective Single-Chunk Unclaiming
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/UnclaimChunkCommand.java` (lines 43–60)
  - `src/main/java/org/ashin/chunkClaimPlugin2/economy/EconomyManager.java` (lines 106–112)
- **Issue:** With `free-claims: 3`, a player can claim 3 free chunks (`paid-cost: 0.0`), claim a 4th paid chunk (`paid-cost: 10.0`), then unclaim the 3 free chunks (`$0` refund). Because their owned count drops back to `1 < 3`, they can claim 2 new free chunks while retaining `paid-cost: 10.0` on chunk #4, repeating to shift paid costs onto starter chunks.
- **Task:** Track/adjust free-chunk allocation or deduct unearned free-tier resets when calculating refunds/costs so selective unclaiming of free chunks cannot be abused for infinite cost-shifting.

---

## 2. Grief Protection & Exploit Bypasses

### 7. Cross-Border Dispenser Griefing (`BlockDispenseEvent` Missing)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** `BlockDispenseEvent` is not handled. Dispensers placed in the wilderness facing into a claim can dispense water/lava/powder snow buckets directly inside the claim, fire primed TNT, or place boats/minecarts/shulker boxes across the border.
- **Task:** Register `BlockDispenseEvent`, compute the target block `block.getRelative(facing)`, and cancel if the target chunk is claimed and does not share the dispenser chunk's owner.

### 8. Creeper/Wither Explosion Flag Mismatch & Hanging Entity Explosion Destruction
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 527–539, 652–670)
- **Issue:**
  1. `config.yml` documents `mob-griefing` as preventing Creeper explosions, yet `onEntityExplode` only checks `FLAG_EXPLOSIONS` and ignores `FLAG_MOB_GRIEFING` for living/monster explosions (`Creeper`, `Wither`, `WitherSkull`, `Fireball`).
  2. `onHangingBreak` only listens to `HangingBreakByEntityEvent` for `Player` removers. Explosions (`RemoveCause.EXPLOSION`) and mob projectiles still pop Item Frames, Glow Item Frames, Paintings, and Leash Hitches off walls inside claims.
- **Task:** Check `FLAG_MOB_GRIEFING` for mob-caused explosions in `onEntityExplode`, and register `HangingBreakEvent` to protect hanging entities from explosions and non-trusted entity breaks.

### 9. Mob & TNT-Minecart Explosions Killing Claimed Animals, Villagers & Armor Stands
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 246–270, 833–857)
- **Issue:** `onEntityDamageByEntity` exits early when `resolveAttacker()` is `null`, and `onIndirectExplosionDamage` only checks `BLOCK_EXPLOSION`, `EnderCrystal`, and `TNTPrimed`. Lured **Creepers** or **ExplosiveMinecarts (TNT Minecarts)** pushed into claims kill claimed animals, villagers, and `ArmorStand`s.
- **Task:** Extend `onIndirectExplosionDamage` (or `onEntityDamageByEntity`) to cover `ExplosiveMinecart` and `Creeper`/mob explosion damage to non-hostile entities and `ArmorStand`s when `mob-protection` or `explosions`/`mob-griefing` is active.

### 10. Projectile Griefing on Blocks (`EntityChangeBlockEvent` & `ProjectileHitEvent` / `TNTPrimeEvent`)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 597–605)
- **Issue:**
  1. `onEntityChangeBlock` returns immediately for `Player` (allowing burning players to melt `POWDER_SNOW`), and treats player-shot `Projectile`s under `FLAG_MOB_GRIEFING` instead of `canModifyChunk(shooter, chunk)` — allowing arrows/tridents/wind charges to break Chorus Flowers, Pointed Dripstone, and Decorated Pots when `mob-griefing` is disabled.
  2. `ProjectileHitEvent` and `TNTPrimeEvent` are not registered, allowing untrusted visitors to shoot Bells, Target blocks, Buttons, or ignite TNT/Campfires with Flame bows.
- **Task:** Resolve projectile shooters in `onEntityChangeBlock` via `canModifyChunk`, protect `POWDER_SNOW` from untrusted burning players, and register `ProjectileHitEvent` / `TNTPrimeEvent`.

### 11. Lectern Book Theft (`PlayerTakeLecternBookEvent` Missing)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 216–218)
- **Issue:** `PlayerTakeLecternBookEvent` is not registered. Any visitor who can open a lectern (e.g., if `interact-stonecutter` is disabled) can click **"Take Book"** to steal the book.
- **Task:** Register `PlayerTakeLecternBookEvent` and cancel if `!canModifyChunk(event.getPlayer(), event.getLectern().getChunk())`.

### 12. Right-Click Tool & Block Modification Gaps in `onPlayerInteract`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 176–235)
- **Issue:** Untrusted visitors can right-click inside claims to strip logs/copper/bamboo with axes, wax copper with honeycomb, till farmland/create dirt paths with hoes/shovels, harvest `BEEHIVE`/`BEE_NEST` with shears/bottles, toggle `REDSTONE_WIRE` dot/cross state, extinguish candles/campfires, set respawn in `_BED`s, or left-click `DRAGON_EGG`s when `interact-chest` is off.
- **Task:** Block untrusted block-modifying tool interactions, bed respawn/detonation, beehive harvesting, redstone wire toggling, and `DRAGON_EGG` punching inside claimed chunks.

### 13. Missing `PlayerBucketEntityEvent`, `PlayerShearEntityEvent` & `PlayerInteractAtEntityEvent`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** Visitors can scoop claimed Axolotls, Tadpoles, and Fish into buckets (`PlayerBucketEntityEvent`), shear claimed Sheep/Mooshrooms (`PlayerShearEntityEvent`), or interact with Armor Stands/entities via `PlayerInteractAtEntityEvent` (which has a separate Bukkit `HandlerList` from `PlayerInteractEntityEvent`).
- **Task:** Register all three events and enforce `canModifyChunk(...)`.

### 14. Cross-Border Sponge Water Absorption (`SpongeAbsorbEvent` Missing)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java`
- **Issue:** Placing a dry sponge in the wilderness adjacent to a claim drains water blocks up to 7 blocks inside the protected claim.
- **Task:** Register `SpongeAbsorbEvent` and filter `event.getBlocks()` to remove blocks inside claimed chunks that do not share the sponge block's owner.

### 15. Sticky Piston + Slime Block Retraction Into Claims (`shouldCancelPiston`)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 121–148)
- **Issue:** `shouldCancelPiston()` only checks destination blocks (`b.getRelative(direction)`) when `isExtend == true`. During `BlockPistonRetractEvent`, sticky pistons pulling slime/honey structures can pull blocks from the wilderness across the border **into** a claimed chunk.
- **Task:** Also check `b.getRelative(direction)` (the retraction destination) when `isExtend == false`.

---

## 3. Performance, Concurrency & Resource Issues

### 16. `mob-entry` Task Copies `chunkOwners` Every 0.5s, Loads Neighbor Chunks & Suffocates Mobs
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 704–804)
- **Issue:**
  1. `startMobEntryTask()` calls `chunkManager.getClaimedChunks()` (cloning `chunkOwners`) every 10 ticks even when zero claims have `mob-entry: true`.
  2. `pushOutOfChunk()` calls `dest.getChunk()` without checking `isChunkLoaded`, forcing synchronous chunk loads along borders.
  3. `dest` reuses the mob's current `Y` coordinate without checking for solid blocks, teleporting mobs into walls/cliffs.
- **Task:** Iterate `chunkOwners` directly (or maintain a set of `mob-entry`-enabled chunk keys), guard `dest` chunk loading, and find a safe non-solid `Y` at `(newX, newZ)`.

### 17. `O(N)` Full-Map Scans on Every `%ccp_claimed_chunks%` Placeholder Request
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/ChunkManager.java` (lines 520–526)
  - `src/main/java/org/ashin/chunkClaimPlugin2/placeholders/ChunkClaimExpansion.java` (lines 52–54)
- **Issue:** `getPlayerChunkCount(UUID)` iterates all `chunkOwners.values()` (`O(N)` across all server claims) on every placeholder evaluation, causing unnecessary CPU load on scoreboards/tablists.
- **Task:** Maintain an atomic/concurrent `Map<UUID, Integer>` (or `AtomicInteger`) counter updated on claim, unclaim, transfer, and load for `O(1)` lookups.

### 18. First-Join Locale Write Causes Main-Thread Disk I/O & Locks Players to Old Default Locale
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/PlayerJoinLocaleListener.java` (lines 20–26)
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/MessageManager.java` (lines 135–147)
- **Issue:** Writing `serverDefaultLocale` into `playerdata.yml` on first join triggers synchronous disk I/O on join and permanently pins players to the old default locale even if the server admin later changes `locale:` in `config.yml`.
- **Task:** Stop writing `serverDefaultLocale` to `playerdata.yml` on join; only persist when a player explicitly selects a language via `/chunklang` or the GUI (`getPlayerLocale()` already falls back to `serverDefaultLocale`).

### 19. Per-Particle Config & Enum Resolution Inside `VisualizeChunkCommand` Loop
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/VisualizeChunkCommand.java` (lines 29, 184–227)
- **Issue:** `spawnParticle()` resolves `messages.getPlayerParticle()`, `ConfigMigrator.getNonBlankString()`, and `AdminSettingsGUI.resolveParticle()` on every single particle step (hundreds of times per tick), and `activeVisualizations` uses a plain `HashMap` without cleanup on disable.
- **Task:** Resolve the player's `Particle` once per visualization tick before iterating coordinates, and switch `activeVisualizations` to `ConcurrentHashMap`.

### 20. Blocking `Bukkit.getOfflinePlayer(String)` Still Present in `ChunkSettingsCommand.handleTrust`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/commands/ChunkSettingsCommand.java` (line 212)
- **Issue:** `handleTrust()` still calls `Bukkit.getOfflinePlayer(targetPlayerName)` directly, allowing players to trigger blocking 2–5s Mojang API HTTP requests on the main thread with `/chunksettings trust <claim> <unknownName>`.
- **Task:** Replace with a local cached offline player lookup (`resolveCachedOfflinePlayer`) matching `ChunkAdminCommand`.

---

## 4. GUI, Command & Cross-Version Quality Issues

### 21. GUI Teleport Uses Wrong Permission (`chunkclaim.teleport` vs `ccp.teleport`) & Unsafe `findSafeY`
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (lines 328–353, 413–425)
- **Issue:** Clicking Teleport in `/chunksettings` checks `chunkclaim.teleport` (`default: op`) instead of `ccp.teleport` (`default: true`), and uses a private `findSafeY()` (`getHighestBlockYAt`) that teleports players onto the Nether bedrock ceiling (`Y=128`) or into lava instead of using `ChunkTeleportCommand`'s safe scanner.
- **Task:** Check `ccp.teleport` (or `chunkclaim.teleport`) and delegate to `ChunkTeleportCommand` / `player.performCommand("chunktp " + claimName)`, removing duplicate `findSafeY()`.

### 22. `ArrayIndexOutOfBoundsException` in `SettingsGUI` When Player Has $\ge 54$ Claims or $\ge 54$ Online Players
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/gui/SettingsGUI.java` (lines 62–131, 244–306)
- **Issue:** `invSizeFor(...)` caps inventory size at `54`, but loops in `openClaims`, `openVisualize`, `openDelete`, `openTrust`, and `openTransfer` do not stop at `size - 2` (`52`), causing `inv.setItem(54, ...)` to throw `ArrayIndexOutOfBoundsException` at $\ge 55$ entries.
- **Task:** Cap loop index at `size - 1` (reserving slot `size - 1` for the Back button) or add GUI pagination.

### 23. Particle GUI Click Failure on `DUST` / `CHERRY_LEAVES` Across 1.19.4–1.20.4
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/gui/SettingsGUI.java` (line 164)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (line 143)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/AdminSettingsGUIListener.java` (line 223)
- **Issue:** `SettingsGUI.openParticle()` uses `Material.valueOf(entry[1])` (crashing on 1.19.4 where `CHERRY_LEAVES` does not exist), and both `SettingsGUIListener` and `AdminSettingsGUIListener` validate particle clicks with raw `Particle.valueOf(name)` instead of `AdminSettingsGUI.resolveParticle(name)`, breaking `DUST` on 1.19.4–1.20.4.
- **Task:** Use `Material.matchMaterial(entry[1])` with `OAK_LEAVES` fallback in `SettingsGUI.openParticle()`, and validate particle clicks via `AdminSettingsGUI.resolveParticle(name)`.

### 24. Fragile Display-Name Slot Scraping in `SettingsGUIListener` (`CLAIM_DETAILS` & `DELETE_CONFIRM`)
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/gui/SettingsGUI.java` (lines 187, 382)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (lines 269–368)
- **Issue:** `openClaimDetails()` and `openDeleteConfirm()` pass `claimName == null` to `SettingsHolder`, forcing `SettingsGUIListener` to scrape `claimName` out of slot 11/13 display names.
- **Task:** Pass `claimName` directly into `new SettingsHolder(View.CLAIM_DETAILS, claimName)` and `new SettingsHolder(View.DELETE_CONFIRM, claimName)` and read `sh.claimName`.

### 25. Incomplete Fallback Array in `MessageManager.getAvailableLocales()` & Dead `WorldGuardHandler.java` Class
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/managers/MessageManager.java` (line 126)
  - `src/main/java/org/ashin/chunkClaimPlugin2/handlers/WorldGuardHandler.java`
- **Issue:**
  1. `MessageManager.getAvailableLocales()` omits `de_DE`, `pt_BR`, and `ru_RU` from its bundled JAR fallback array.
  2. `WorldGuardHandler.java` is dead unused code containing the legacy `__global__` bug and `e.printStackTrace()`.
- **Task:** Include all 7 bundled locales in `MessageManager.getAvailableLocales()`, and delete the unused `WorldGuardHandler.java` class.

---

## 5. Spigot & Paper Compatibility (1.19.4–1.21.x / 26.x)

### 26. `org.spigotmc.event.entity.EntityMountEvent` Removed in Paper/Spigot 1.21+
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 461–467)
- **Issue:** `onEntityMount` imports `org.spigotmc.event.entity.EntityMountEvent`, which moved to `org.bukkit.event.entity.EntityMountEvent` in 1.20.4 and was removed from `org.spigotmc` in newer Paper/Spigot builds. Because it sits inside `ChunkProtectionListener`, a missing class aborts `PluginManager.registerEvents(...)` with `NoClassDefFoundError` and disables **all** chunk protection listeners on startup.
- **Task:** Isolate mount handling into a dedicated dynamic listener that binds whichever `EntityMountEvent` class exists (`org.bukkit.event.entity.EntityMountEvent` or `org.spigotmc.event.entity.EntityMountEvent`) via `PluginManager.registerEvent(...)`, alongside standard Bukkit `VehicleEnterEvent`.

### 27. Async Thread-Safety Violations in `ChatRenameListener` (`AsyncPlayerChatEvent`)
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChatRenameListener.java` (lines 29–89)
- **Issue:** `AsyncPlayerChatEvent` runs off the main thread on Paper and Spigot, yet `onPlayerChat()` mutates Bukkit's unsynchronized `EntityMetadataStore` (`hasMetadata`/`getMetadata`/`removeMetadata`) and calls `messages.getFor(...)` (which accesses `MessageManager.playerData` and `PlaceholderAPI.setPlaceholders(...)` asynchronously).
- **Task:** Replace Bukkit entity metadata for rename/sell prompts with a thread-safe `ConcurrentHashMap<UUID, PendingPrompt>`, and dispatch all `messages.getFor(...)` / `player.sendMessage(...)` calls synchronously via `getScheduler().runTask(...)`.

### 28. `api-version: '1.13'` Forces Legacy Commodore Bytecode Rewriting on Paper/Spigot
- **Files:**
  - `build.gradle` (lines 60–61)
  - `src/main/resources/plugin.yml` (line 4)
- **Issue:** `build.gradle` expands `apiVersion: '1.13'` into `plugin.yml` despite targeting `1.19.4+`, forcing Paper and Spigot's legacy Commodore bytecode rewriter over every plugin class at startup.
- **Task:** Update `apiVersion` in `build.gradle` from `'1.13'` to `'1.19'` to match the `1.19.4+` baseline.

### 29. `ItemClaimListener` Missing `ignoreCancelled = true` & Doesn't Cancel Right-Click Tool Action
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ItemClaimListener.java` (lines 30–150)
- **Issue:** `onPlayerInteract` lacks `ignoreCancelled = true` (causing duplicate messages after `ChunkProtectionListener` cancels an interact) and never calls `event.setCancelled(true)` when claiming with the tool (allowing vanilla shovel path-flattening or container opening on the same click).
- **Task:** Add `ignoreCancelled = true` and call `event.setCancelled(true)` when the claim item handles a click.

### 30. Missing `ignoreCancelled = true` Across `ChunkProtectionListener` Event Handlers
- **File:** `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 56–696)
- **Issue:** Core protection handlers (`onBlockBreak`, `onBlockPlace`, `onBucketEmpty`, `onBucketFill`, `onPlayerInteract`, `onEntityDamageByEntity`, `onBlockBurn`, `onBlockIgnite`, `onBlockSpread`, `onVehicleDestroy`, `onPlayerInteractEntity`, `onArmorStandManipulate`, `onHangingBreak`, `onHangingPlace`, `onEntityChangeBlock`, `onEntityExplode`, `onBlockExplode`, `onCreatureSpawn`, and `onPvP`) omit `ignoreCancelled = true`, causing duplicate deny messages and redundant work when WorldGuard or another plugin already cancelled the event.
- **Task:** Add `ignoreCancelled = true` to all appropriate protection event handlers.

### 31. Paper Separate Entity-Chunk Loading & Async `OfflinePlayer` / `Location` Access
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/ChunkProtectionListener.java` (lines 732–735)
  - `src/main/java/org/ashin/chunkClaimPlugin2/placeholders/ChunkClaimExpansion.java` (lines 65–78)
- **Issue:**
  1. On Paper 1.17+, `world.isChunkLoaded(cx, cz)` can return `true` even when entity slices are not loaded/ticking, causing `startMobEntryTask()` to query `chunk.getEntities()` needlessly on non-ticking border chunks.
  2. `ChunkClaimExpansion.onRequest(...)` runs on async scoreboard/tablist threads while accessing `player.getLocation()` and `Bukkit.getOfflinePlayer(owner).getName()` directly.
- **Task:** Guard `mob-entry` entity scans with `chunk.isEntitiesLoaded()` (via reflection/compatibility check) and cache known owner `UUID -> name` mappings in `ChunkManager` for lock-free async placeholder lookups.

### 32. `Sound` Enum-to-Interface Change (1.21.3+) & Pure CraftBukkit ActionBar Fallback
- **Files:**
  - `src/main/java/org/ashin/chunkClaimPlugin2/gui/SettingsGUI.java` (lines 405–409)
  - `src/main/java/org/ashin/chunkClaimPlugin2/gui/AdminSettingsGUI.java` (lines 306–310)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/SettingsGUIListener.java` (line 349)
  - `src/main/java/org/ashin/chunkClaimPlugin2/commands/ClaimTradeCommand.java` (lines 273–275)
  - `src/main/java/org/ashin/chunkClaimPlugin2/listeners/PlayerMoveListener.java` (lines 150–156)
- **Issue:**
  1. `org.bukkit.Sound` became an `interface` in Spigot/Paper 1.21.3+; direct enum constant access relies on runtime bytecode rewriting and silently drops sounds via `catch (Throwable)` if compatibility fails.
  2. `PlayerMoveListener.sendActionBar()` relies on `player.spigot()` / Bungee `ChatMessageType`, which is unavailable on pure CraftBukkit (spamming chat instead).
- **Task:** Route GUI/teleport/trade sounds through a shared string/registry-safe helper (`playSound(loc, "minecraft:...", ...)`), and ensure action-bar delivery works cleanly across Spigot and Paper.
