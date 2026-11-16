package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ChunkProtectionListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ChunkProtectionListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        startMobEntryTask();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (canModifyChunk(event.getPlayer(), event.getBlock().getChunk())) {
            return;
        }
        event.setCancelled(true);
        if (messages.isMessageEnabled("deny-break")) {
            event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-break"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkModifyOrCancel(event.getPlayer(), event.getBlock().getChunk(), event, "deny-place");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        checkModifyOrCancel(event.getPlayer(), event.getBlock().getChunk(), event, "deny-place");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        checkModifyOrCancel(event.getPlayer(), event.getBlock().getChunk(), event, "deny-break");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        org.bukkit.block.Block from = event.getBlock();
        org.bukkit.block.Block to = event.getToBlock();
        if ((from.getX() >> 4) == (to.getX() >> 4) && (from.getZ() >> 4) == (to.getZ() >> 4)) {
            return;
        }
        UUID toOwner = chunkManager.getChunkOwner(to.getChunk());
        if (toOwner == null) {
            return;
        }
        UUID fromOwner = chunkManager.getChunkOwner(from.getChunk());
        if (!toOwner.equals(fromOwner)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (shouldCancelPiston(event.getBlock(), event.getBlocks(), event.getDirection(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (shouldCancelPiston(event.getBlock(), event.getBlocks(), event.getDirection(), false)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelPiston(org.bukkit.block.Block pistonBlock, java.util.List<org.bukkit.block.Block> movedBlocks,
                                       org.bukkit.block.BlockFace direction, boolean isExtend) {
        UUID pistonOwner = chunkManager.getChunkOwner(pistonBlock.getChunk());

        // Check piston head reach into adjacent block when extending
        if (isExtend) {
            org.bukkit.block.Block headTarget = pistonBlock.getRelative(direction);
            UUID headOwner = chunkManager.getChunkOwner(headTarget.getChunk());
            if (headOwner != null && !headOwner.equals(pistonOwner)) {
                return true;
            }
        }

        for (org.bukkit.block.Block b : movedBlocks) {
            UUID blockOwner = chunkManager.getChunkOwner(b.getChunk());
            if (blockOwner != null && !blockOwner.equals(pistonOwner)) {
                return true;
            }
            if (isExtend) {
                org.bukkit.block.Block dest = b.getRelative(direction);
                UUID destOwner = chunkManager.getChunkOwner(dest.getChunk());
                if (destOwner != null && !destOwner.equals(pistonOwner)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        // Check boat/minecart placement before block interaction flags
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            String itemName = event.getItem().getType().name();
            if (isVehicleItem(itemName)) {
                Chunk clickedChunk = event.getClickedBlock().getChunk();
                Chunk placeChunk = event.getClickedBlock().getRelative(event.getBlockFace()).getChunk();
                if (!canModifyChunk(event.getPlayer(), clickedChunk) || !canModifyChunk(event.getPlayer(), placeChunk)) {
                    event.setCancelled(true);
                    if (messages.isMessageEnabled("deny-place")) {
                        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-place"));
                    }
                    return;
                }
            }
        }

        if (canModifyChunk(event.getPlayer(), event.getClickedBlock().getChunk())) {
            return;
        }

        Chunk chunk = event.getClickedBlock().getChunk();
        String name = event.getClickedBlock().getType().name();
        boolean isPhysical = event.getAction() == org.bukkit.event.block.Action.PHYSICAL;

        boolean blockIt = false;
        if (isPhysical) {
            if (name.equals("FARMLAND") || name.equals("TURTLE_EGG") || name.equals("SNIFFER_EGG")) {
                blockIt = true;
            } else if (name.endsWith("_PRESSURE_PLATE") || name.equals("TRIPWIRE")) {
                blockIt = chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_INTERACT_REDSTONE);
            }
        } else {
            String requiredFlag = resolveInteractionFlag(name);
            if (requiredFlag != null) {
                blockIt = chunkManager.isChunkFlagEnabled(chunk, requiredFlag);
            }
        }

        if (blockIt) {
            event.setCancelled(true);
            if (!isPhysical && messages.isMessageEnabled("deny-interact")) {
                event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
            }
        }
    }

    private boolean isVehicleItem(String itemName) {
        return itemName.endsWith("_BOAT") || itemName.endsWith("_RAFT") || itemName.endsWith("MINECART");
    }

    private String resolveInteractionFlag(String typeName) {
        return switch (typeName) {
            case "CHEST", "TRAPPED_CHEST", "BARREL", "ENDER_CHEST", "SHULKER_BOX",
                 "HOPPER", "DROPPER", "DISPENSER", "BREWING_STAND", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL",
                 "CAULDRON", "WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON",
                 "COMPOSTER", "CAKE", "SWEET_BERRY_BUSH", "DRAGON_EGG",
                 "CHISELED_BOOKSHELF", "DECORATED_POT", "JUKEBOX", "FLOWER_POT", "BEACON" ->
                ChunkManager.FLAG_INTERACT_CHEST;
            case "FURNACE", "BLAST_FURNACE", "SMOKER", "CAMPFIRE", "SOUL_CAMPFIRE" ->
                ChunkManager.FLAG_INTERACT_FURNACE;
            case "STONECUTTER", "CRAFTING_TABLE", "CARTOGRAPHY_TABLE", "SMITHING_TABLE",
                 "GRINDSTONE", "LOOM", "LECTERN", "ENCHANTING_TABLE", "CRAFTER" ->
                ChunkManager.FLAG_INTERACT_STONECUTTER;
            case "LEVER", "REPEATER", "COMPARATOR", "DAYLIGHT_DETECTOR",
                 "BELL", "RESPAWN_ANCHOR", "NOTE_BLOCK", "TARGET" ->
                ChunkManager.FLAG_INTERACT_REDSTONE;
            default -> {
                if (typeName.endsWith("_SHULKER_BOX") || typeName.endsWith("_CAKE") || typeName.startsWith("POTTED_")) {
                    yield ChunkManager.FLAG_INTERACT_CHEST;
                }
                if (typeName.endsWith("_DOOR") || typeName.endsWith("_TRAPDOOR") || typeName.endsWith("_FENCE_GATE")) {
                    yield ChunkManager.FLAG_INTERACT_DOOR;
                }
                if (typeName.endsWith("_BUTTON")) {
                    yield ChunkManager.FLAG_INTERACT_REDSTONE;
                }
                yield null;
            }
        };
    }

    // ── Entity protection ──

    /**
     * Prevents players from attacking/destroying entities (minecarts, animals, armor stands, etc.)
     * in chunks claimed by someone else when mob-protection flag is enabled.
     * Hostile monsters (zombies, creepers, skeletons, etc.) are explicitly excluded so players
     * can defend friends from hostile threats.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) return; // Handled in onPvP
        if (isHostileMob(event.getEntity())) return; // Allow defending players from monsters

        Player player = resolveAttacker(event.getDamager());
        if (player == null) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (canModifyChunk(player, chunk)) return;

        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_PROTECTION)) {
            event.setCancelled(true);
            if (messages.isMessageEnabled("deny-entity")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "deny-entity"));
            }
        }
    }

    private boolean isHostileMob(Entity entity) {
        if (entity instanceof org.bukkit.entity.Monster) return true;
        if (entity instanceof org.bukkit.entity.Slime) return true;
        if (entity instanceof org.bukkit.entity.Ghast) return true;
        if (entity instanceof org.bukkit.entity.Phantom) return true;
        if (entity instanceof org.bukkit.entity.Shulker) return true;
        if (entity instanceof org.bukkit.entity.Hoglin) return true;
        if (entity instanceof org.bukkit.entity.Boss) return true;

        // Check if neutral mob is currently aggressive/angry
        if (entity instanceof org.bukkit.entity.Bee bee && bee.getAnger() > 0) return true;
        if (entity instanceof org.bukkit.entity.PigZombie pz && pz.isAngry()) return true;
        if (entity instanceof org.bukkit.entity.Wolf wolf && wolf.isAngry()) return true;
        if (entity instanceof org.bukkit.entity.IronGolem golem && golem.getTarget() instanceof Player) return true;

        try {
            if (entity instanceof org.bukkit.entity.Enemy) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    // ── Fire spread protection (flag-gated) ──

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        if (chunkManager.isChunkClaimed(chunk) && chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        if (!chunkManager.isChunkClaimed(chunk)) {
            return;
        }

        // Player-driven ignition (flint & steel, fire charge, etc.) requires build trust regardless of fire-spread flag
        if (event.getPlayer() != null) {
            checkModifyOrCancel(event.getPlayer(), chunk, event, "deny-place");
            return;
        }

        if (event.getIgnitingEntity() != null) {
            Player attacker = resolveAttacker(event.getIgnitingEntity());
            if (attacker != null) {
                checkModifyOrCancel(attacker, chunk, event, "deny-place");
                return;
            }
        }

        org.bukkit.event.block.BlockIgniteEvent.IgniteCause cause = event.getCause();
        if (cause == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                || cause == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL) {
            // Block dispenser/fireball ignition originating from outside the claim
            org.bukkit.block.Block ignitingBlock = event.getIgnitingBlock();
            UUID targetOwner = chunkManager.getChunkOwner(chunk);
            UUID sourceOwner = ignitingBlock != null ? chunkManager.getChunkOwner(ignitingBlock.getChunk()) : null;
            if (!targetOwner.equals(sourceOwner)) {
                event.setCancelled(true);
                return;
            }
        }

        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_FIRE_SPREAD)
                && (cause == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.SPREAD
                        || cause == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.LAVA
                        || cause == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.LIGHTNING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        if (chunkManager.isChunkClaimed(chunk) && chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_FIRE_SPREAD)) {
            String type = event.getSource().getType().name();
            if (type.contains("FIRE")) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players from destroying vehicles (minecarts, boats) in claimed chunks.
     * This covers cases EntityDamageByEntityEvent may miss (e.g., empty minecart punch-destroy).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        checkModifyOrCancel(player, chunk, event, "deny-entity");
    }

    /**
     * Prevents untrusted visitors from mounting claimed animals or vehicles.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMount(org.spigotmc.event.entity.EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getMount() instanceof Player) return;

        Chunk chunk = event.getMount().getLocation().getChunk();
        checkModifyOrCancel(player, chunk, event, "deny-interact");
    }

    /**
     * Prevents players from interacting with entities (right-click on villagers, minecart chests,
     * storage minecarts, etc.) in claimed chunks.
     * Hostile monsters and angry mobs are explicitly permitted for custom weapons and defense.
     * Villager and Wandering Trader trading is governed by the interact-villagers claim flag.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Don't block interaction with other players
        if (event.getRightClicked() instanceof Player) return;

        // Allow interacting with hostile monsters (e.g. name tags, custom right-click combat systems)
        if (isHostileMob(event.getRightClicked())) return;

        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if (canModifyChunk(event.getPlayer(), chunk)) return;

        Entity clicked = event.getRightClicked();

        // Villager and Wandering Trader trading protection
        if (clicked instanceof org.bukkit.entity.AbstractVillager) {
            if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_INTERACT_VILLAGERS)) {
                event.setCancelled(true);
                if (messages.isMessageEnabled("deny-interact")) {
                    String msg = messages.getFor(event.getPlayer().getUniqueId(), "deny-villager-trade");
                    if (msg == null || msg.isEmpty() || msg.startsWith("Unknown message")) {
                        msg = messages.getFor(event.getPlayer().getUniqueId(), "deny-interact");
                    }
                    event.getPlayer().sendMessage(msg);
                }
            }
            return;
        }

        // Other entities (storage minecarts, boats, animals, etc.)
        event.setCancelled(true);
        if (messages.isMessageEnabled("deny-interact")) {
            event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
        }
    }

    /**
     * Prevents players from taking/placing items in armor stands in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if (canModifyChunk(event.getPlayer(), chunk)) return;

        event.setCancelled(true);
        if (messages.isMessageEnabled("deny-interact")) {
            event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
        }
    }

    /**
     * Prevents players from breaking hanging entities (item frames, paintings) in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = resolveAttacker(event.getRemover());
        if (player == null) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (canModifyChunk(player, chunk)) return;

        event.setCancelled(true);
        if (messages.isMessageEnabled("deny-break")) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "deny-break"));
        }
    }

    /**
     * Prevents players from placing hanging entities (item frames, paintings) in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() == null) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (canModifyChunk(event.getPlayer(), chunk)) return;

        event.setCancelled(true);
        if (messages.isMessageEnabled("deny-place")) {
            event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-place"));
        }
    }

    // ── Mob griefing protection (flag-gated) ──

    /**
     * Prevents mobs from forming blocks in claimed chunks when mob-griefing flag is enabled.
     * Covers: snow golem snow trails, frost walker ice, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (event.getEntity() instanceof Player) return;
        Chunk chunk = event.getBlock().getChunk();
        // Only block if chunk is claimed AND flag is enabled
        if (chunkManager.isChunkClaimed(chunk) && chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents mobs from changing blocks in claimed chunks when mob-griefing flag is enabled.
     * Covers: enderman pickup/place, silverfish, ravager, zombie door breaking, wither skulls.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) return;
        Chunk chunk = event.getBlock().getChunk();
        // Only block if chunk is claimed AND flag is enabled
        if (chunkManager.isChunkClaimed(chunk) && chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents trees or giant mushrooms grown outside or on the border of a claim from
     * replacing blocks inside claimed chunks owned by other players.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        UUID originOwner = chunkManager.getChunkOwner(event.getLocation().getChunk());
        Player player = event.getPlayer();

        event.getBlocks().removeIf(state -> {
            Chunk targetChunk = state.getLocation().getChunk();
            UUID targetOwner = chunkManager.getChunkOwner(targetChunk);
            if (targetOwner == null) {
                return false;
            }
            if (player != null) {
                return !canModifyChunk(player, targetChunk);
            }
            return !targetOwner.equals(originOwner);
        });
    }

    /**
     * Prevents cross-dimension Nether portal creation from replacing blocks inside claims
     * unless created by an owner or trusted player.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        Player player = event.getEntity() instanceof Player p ? p : null;
        for (org.bukkit.block.BlockState state : event.getBlocks()) {
            Chunk targetChunk = state.getLocation().getChunk();
            if (!chunkManager.isChunkClaimed(targetChunk)) {
                continue;
            }
            if (player == null || !canModifyChunk(player, targetChunk)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Removes claimed-chunk blocks from entity explosion damage lists when explosions flag is enabled.
     * Covers: creepers, wither, ghast fireballs, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Only filter if explosions flag is enabled on claimed chunks
        event.blockList().removeIf(block -> 
            chunkManager.isChunkClaimed(block.getChunk()) && 
            chunkManager.isChunkFlagEnabled(block.getChunk(), ChunkManager.FLAG_EXPLOSIONS));
    }

    /**
     * Removes claimed-chunk blocks from block explosion damage lists when explosions flag is enabled.
     * Covers: TNT, respawn anchors, beds in nether, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Only filter if explosions flag is enabled on claimed chunks
        event.blockList().removeIf(block -> 
            chunkManager.isChunkClaimed(block.getChunk()) && 
            chunkManager.isChunkFlagEnabled(block.getChunk(), ChunkManager.FLAG_EXPLOSIONS));
    }

    // ── Mob spawning prevention (flag-gated) ──

    /**
     * Prevents creature spawning in claimed chunks when mob-spawning flag is enabled.
     * Only blocks natural/spawner/chunk-gen spawns; spawn eggs and breeding still work.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Player) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER
                || reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                || reason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || reason == CreatureSpawnEvent.SpawnReason.JOCKEY
                || reason == CreatureSpawnEvent.SpawnReason.PATROL
                || reason == CreatureSpawnEvent.SpawnReason.RAID
                || reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            Chunk chunk = event.getEntity().getLocation().getChunk();
            // Only block if chunk is claimed AND flag is enabled
            if (chunkManager.isChunkClaimed(chunk) && chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_SPAWNING)) {
                event.setCancelled(true);
            }
        }
    }

    // ── Mob entry prevention (periodic task, flag-gated) ──

    /**
     * Starts a periodic task that pushes non-protected mobs out of loaded claimed chunks
     * that have the mob-entry flag enabled, avoiding global world.getLivingEntities() scans.
     */
    private void startMobEntryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (java.util.Map.Entry<String, UUID> entry : chunkManager.getClaimedChunks().entrySet()) {
                    String key = entry.getKey();
                    UUID owner = entry.getValue();
                    String claimName = chunkManager.getChunkClaimName(key);
                    if (!chunkManager.getClaimFlag(owner, claimName != null ? claimName : "world", ChunkManager.FLAG_MOB_ENTRY)) {
                        continue;
                    }

                    int lastColon = key.lastIndexOf(':');
                    int secondLastColon = lastColon > 0 ? key.lastIndexOf(':', lastColon - 1) : -1;
                    if (secondLastColon <= 0) continue;

                    String worldName = key.substring(0, secondLastColon);
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) continue;

                    int cx, cz;
                    try {
                        cx = Integer.parseInt(key.substring(secondLastColon + 1, lastColon));
                        cz = Integer.parseInt(key.substring(lastColon + 1));
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    if (!world.isChunkLoaded(cx, cz)) continue;

                    Chunk chunk = world.getChunkAt(cx, cz);
                    for (Entity entity : chunk.getEntities()) {
                        if (!(entity instanceof LivingEntity living)) continue;
                        if (living instanceof Player || living instanceof ArmorStand) continue;
                        if (isProtectedFromMobEntry(living)) continue;
                        pushOutOfChunk(living, chunk);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // every 0.5 seconds
    }

    private boolean isProtectedFromMobEntry(LivingEntity entity) {
        if (entity instanceof org.bukkit.entity.Tameable tame && tame.isTamed()) return true;
        if (entity.getCustomName() != null) return true;
        return entity instanceof org.bukkit.entity.IronGolem
                || entity instanceof org.bukkit.entity.AbstractVillager;
    }

    /**
     * Teleports an entity to just outside the nearest edge of the given chunk.
     * If the destination chunk also has mob-entry protection, removes non-protected mobs
     * while preserving tamed pets, custom-named entities, iron golems, and villagers.
     */
    private void pushOutOfChunk(LivingEntity entity, Chunk chunk) {
        if (isProtectedFromMobEntry(entity)) {
            return;
        }
        Location loc = entity.getLocation();
        double x = loc.getX();
        double z = loc.getZ();

        // Chunk boundaries in world coordinates
        double minX = chunk.getX() * 16.0;
        double maxX = minX + 16.0;
        double minZ = chunk.getZ() * 16.0;
        double maxZ = minZ + 16.0;

        // Distance to each edge
        double dMinX = x - minX;
        double dMaxX = maxX - x;
        double dMinZ = z - minZ;
        double dMaxZ = maxZ - z;

        double nearest = Math.min(Math.min(dMinX, dMaxX), Math.min(dMinZ, dMaxZ));

        double newX = x;
        double newZ = z;

        if (nearest == dMinX) {
            newX = minX - 0.5;
        } else if (nearest == dMaxX) {
            newX = maxX + 0.5;
        } else if (nearest == dMinZ) {
            newZ = minZ - 0.5;
        } else {
            newZ = maxZ + 0.5;
        }

        Location dest = new Location(loc.getWorld(), newX, loc.getY(), newZ, loc.getYaw(), loc.getPitch());

        // If the destination chunk is also protected, only remove non-protected mobs
        if (chunkManager.isChunkFlagEnabled(dest.getChunk(), ChunkManager.FLAG_MOB_ENTRY)) {
            if (!isProtectedFromMobEntry(entity)) {
                entity.remove();
            }
            return;
        }

        entity.teleport(dest);
    }

    // ── PvP prevention (flag-gated) ──

    /**
     * Prevents player-vs-player combat in claimed chunks when pvp flag is enabled.
     * Checked on the victim's chunk.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;
        if (attacker.equals(victim)) return; // self-damage is fine

        Chunk chunk = victim.getLocation().getChunk();
        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_PVP)) {
            event.setCancelled(true);
            if (messages.isMessageEnabled("deny-pvp")) {
                attacker.sendMessage(messages.getFor(attacker.getUniqueId(), "deny-pvp"));
            }
        }
    }

    /**
     * Blocks indirect explosion damage from End Crystals, Respawn Anchors, Beds, and player-less TNT
     * to players (when PvP protection is active) and non-hostile entities (when mob-protection is active).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIndirectExplosionDamage(EntityDamageEvent event) {
        boolean isBlockExplosion = event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        boolean isCrystalOrTnt = event instanceof EntityDamageByEntityEvent ebe
                && (ebe.getDamager() instanceof EnderCrystal || ebe.getDamager() instanceof org.bukkit.entity.TNTPrimed);
        if (!isBlockExplosion && !isCrystalOrTnt) {
            return;
        }

        Entity victim = event.getEntity();
        Chunk chunk = victim.getLocation().getChunk();
        if (!chunkManager.isChunkClaimed(chunk)) {
            return;
        }

        if (victim instanceof Player) {
            if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_PVP)) {
                event.setCancelled(true);
            }
        } else if (!isHostileMob(victim)) {
            if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_PROTECTION)
                    || chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_EXPLOSIONS)) {
                event.setCancelled(true);
            }
        }
    }

    // ── Utility ──

    /**
     * Resolves the actual player attacker from a damage source.
     * Handles direct player hits, projectiles, lingering potion clouds, and player-primed TNT.
     */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.AreaEffectCloud cloud && cloud.getSource() instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    private boolean checkModifyOrCancel(Player player, Chunk chunk, Cancellable event, String denyKey) {
        if (canModifyChunk(player, chunk)) {
            return true;
        }
        event.setCancelled(true);
        if (denyKey != null && messages.isMessageEnabled(denyKey)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), denyKey));
        }
        return false;
    }

    private boolean canModifyChunk(Player player, Chunk chunk) {
        UUID owner = chunkManager.getChunkOwner(chunk);

        // Not claimed or player is the owner
        if (owner == null || owner.equals(player.getUniqueId()) || player.hasPermission("chunkclaimprotection.bypass")) {
            return true;
        }

        // Check if the player is trusted on this claim group
        String claimName = chunkManager.getChunkClaimName(chunk);
        if (claimName != null && chunkManager.isTrusted(owner, claimName, player.getUniqueId())) {
            return true;
        }

        return false;
    }
}