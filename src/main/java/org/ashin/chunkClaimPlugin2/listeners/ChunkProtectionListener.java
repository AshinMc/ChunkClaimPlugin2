package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
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
        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-break"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canModifyChunk(event.getPlayer(), event.getBlock().getChunk())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-place"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (canModifyChunk(event.getPlayer(), event.getClickedBlock().getChunk())) {
            return;
        }

        // Only cancel interactions with containers, doors, etc.
        switch (event.getClickedBlock().getType().name()) {
            case "CHEST", "TRAPPED_CHEST", "BARREL", "FURNACE", "BLAST_FURNACE", "SMOKER",
                 "HOPPER", "DROPPER", "DISPENSER", "BREWING_STAND", "LEVER", "BUTTON",
                 "DOOR", "TRAPDOOR", "FENCE_GATE" -> {
                event.setCancelled(true);
                event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
            }
        }
    }

    // ── Entity protection ──

    /**
     * Prevents players from attacking/destroying entities (minecarts, animals, armor stands, etc.)
     * in chunks claimed by someone else.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player = resolveAttacker(event.getDamager());
        if (player == null) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (canModifyChunk(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage(messages.getFor(player.getUniqueId(), "deny-entity"));
    }

    /**
     * Prevents players from destroying vehicles (minecarts, boats) in claimed chunks.
     * This covers cases EntityDamageByEntityEvent may miss (e.g., empty minecart punch-destroy).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        if (canModifyChunk(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage(messages.getFor(player.getUniqueId(), "deny-entity"));
    }

    /**
     * Prevents players from interacting with entities (right-click on villagers, minecart chests,
     * storage minecarts, etc.) in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if (canModifyChunk(event.getPlayer(), chunk)) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
    }

    /**
     * Prevents players from taking/placing items in armor stands in claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if (canModifyChunk(event.getPlayer(), chunk)) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-interact"));
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
        player.sendMessage(messages.getFor(player.getUniqueId(), "deny-break"));
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
        event.getPlayer().sendMessage(messages.getFor(event.getPlayer().getUniqueId(), "deny-place"));
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
        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_GRIEFING)) {
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
        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }

    /**
     * Removes claimed-chunk blocks from entity explosion damage lists when explosions flag is enabled.
     * Covers: creepers, wither, ghast fireballs, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> chunkManager.isChunkFlagEnabled(block.getChunk(), ChunkManager.FLAG_EXPLOSIONS));
    }

    /**
     * Removes claimed-chunk blocks from block explosion damage lists when explosions flag is enabled.
     * Covers: TNT, respawn anchors, beds in nether, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> chunkManager.isChunkFlagEnabled(block.getChunk(), ChunkManager.FLAG_EXPLOSIONS));
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
            if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_SPAWNING)) {
                event.setCancelled(true);
            }
        }
    }

    // ── Mob entry prevention (periodic task, flag-gated) ──

    /**
     * Starts a periodic task that pushes all non-player mobs out of claimed chunks
     * that have the mob-entry flag enabled. Mobs are teleported to the nearest chunk
     * edge, creating an invisible barrier. Runs every 0.5 seconds for smooth behaviour.
     */
    private void startMobEntryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (entity instanceof Player) continue;
                        if (entity instanceof ArmorStand) continue;
                        Chunk chunk = entity.getLocation().getChunk();
                        if (chunkManager.isChunkFlagEnabled(chunk, ChunkManager.FLAG_MOB_ENTRY)) {
                            pushOutOfChunk(entity, chunk);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // every 0.5 seconds
    }

    /**
     * Teleports an entity to just outside the nearest edge of the given chunk.
     * If the destination chunk also has mob-entry protection, the entity is removed
     * (it is trapped between protected chunks with nowhere safe to go).
     */
    private void pushOutOfChunk(LivingEntity entity, Chunk chunk) {
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

        // If the destination chunk is also protected, the mob has nowhere to go
        if (chunkManager.isChunkFlagEnabled(dest.getChunk(), ChunkManager.FLAG_MOB_ENTRY)) {
            entity.remove();
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
            attacker.sendMessage(messages.getFor(attacker.getUniqueId(), "deny-pvp"));
        }
    }

    // ── Utility ──

    /**
     * Resolves the actual player attacker from a damage source.
     * Handles direct player hits and projectiles shot by players.
     */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
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