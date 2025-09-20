package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class VisualizeChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private static final int VISUALIZATION_SECONDS = 10;
    private static final int PARTICLE_HEIGHT = 100;
    private static final double PARTICLE_SPACING = 0.5;

    public VisualizeChunkCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Single-claim visualization: /visualizechunk <world> <x> <z>
        if (args.length == 3) {
            String worldName = args[0];
            int x, z;
            try {
                x = Integer.parseInt(args[1]);
                z = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(messages.getFor(playerId, "chunk-unclaim-fail"));
                return true;
            }

            Map<String, UUID> claimedChunks = chunkManager.getClaimedChunks();
            String key = chunkManager.getChunkKey(worldName, x, z);
            UUID owner = claimedChunks.get(key);
            if (owner == null || !owner.equals(playerId)) {
                player.sendMessage(messages.getFor(playerId, "chunk-unclaim-fail"));
                return true;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(messages.getFor(playerId, "chunk-unclaim-fail"));
                return true;
            }

            Chunk chunk = world.getChunkAt(x, z);

            player.sendMessage(messages.getFor(playerId, "visualize-start", "count", "1", "seconds", String.valueOf(VISUALIZATION_SECONDS)));

            new BukkitRunnable() {
                int secondsLeft = VISUALIZATION_SECONDS;
                @Override
                public void run() {
                    if (secondsLeft <= 0) { cancel(); return; }
                    visualizeChunkBorders(player, chunk);
                    secondsLeft--;
                }
            }.runTaskTimer(plugin, 0L, 20L);

            return true;
        }

        // Get all chunks claimed by this player
        Map<String, UUID> claimedChunks = chunkManager.getClaimedChunks();
        if (claimedChunks.isEmpty()) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "no-claims"));
            return true;
        }

        // Count player's chunks and prepare for visualization
        int count = 0;
        for (Map.Entry<String, UUID> entry : claimedChunks.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                count++;
            }
        }

        if (count == 0) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "no-claims"));
            return true;
        }

    player.sendMessage(messages.getFor(player.getUniqueId(), "visualize-start", "count", String.valueOf(count), "seconds", String.valueOf(VISUALIZATION_SECONDS)));

        // Start visualization for all player claims
        new BukkitRunnable() {
            int secondsLeft = VISUALIZATION_SECONDS;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    this.cancel();
                    return;
                }

                // Visualize each claimed chunk
                for (Map.Entry<String, UUID> entry : claimedChunks.entrySet()) {
                    if (entry.getValue().equals(playerId)) {
                        Chunk chunk = getChunkFromKey(entry.getKey(), player.getWorld());
                        if (chunk != null) {
                            visualizeChunkBorders(player, chunk);
                        }
                    }
                }
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        return true;
    }

    private Chunk getChunkFromKey(String key, World world) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 3) {
                plugin.getLogger().info("Invalid chunk key format: " + key);
                return null;
            }
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            // Only visualize chunks in the player's current world
            if (!world.getName().equals(worldName)) {
                return null;
            }

            return world.getChunkAt(x, z);
        } catch (Exception e) {
            plugin.getLogger().info("Error parsing chunk key: " + key + " - " + e.getMessage());
            return null;
        }
    }

    private void visualizeChunkBorders(Player player, Chunk chunk) {
        // Get chunk corner coordinates
        int chunkX = chunk.getX() << 4; // Multiply by 16
        int chunkZ = chunk.getZ() << 4; // Multiply by 16
    int playerY = player.getLocation().getBlockY();
    int worldMinY = player.getWorld().getMinHeight();
    int worldMaxY = player.getWorld().getMaxHeight();
    int minY = Math.max(worldMinY, playerY - 5);
    int maxY = Math.min(minY + PARTICLE_HEIGHT, worldMaxY);

        // Create particles along vertical edges (corners going up)
        for (double y = minY; y <= maxY; y += PARTICLE_SPACING) {
            // Corner 1: minX, minZ
            spawnParticle(player, chunkX, y, chunkZ);

            // Corner 2: maxX, minZ
            spawnParticle(player, chunkX + 16, y, chunkZ);

            // Corner 3: minX, maxZ
            spawnParticle(player, chunkX, y, chunkZ + 16);

            // Corner 4: maxX, maxZ
            spawnParticle(player, chunkX + 16, y, chunkZ + 16);
        }

        // Create particles along horizontal edges at player's eye level
        double eyeY = playerY + 1.5;

        // Bottom edges (minY)
        for (double x = chunkX; x <= chunkX + 16; x += PARTICLE_SPACING) {
            spawnParticle(player, x, eyeY, chunkZ);
            spawnParticle(player, x, eyeY, chunkZ + 16);
        }

        for (double z = chunkZ; z <= chunkZ + 16; z += PARTICLE_SPACING) {
            spawnParticle(player, chunkX, eyeY, z);
            spawnParticle(player, chunkX + 16, eyeY, z);
        }
    }

    private void spawnParticle(Player player, double x, double y, double z) {
        Location location = new Location(player.getWorld(), x, y, z);
        // Use FLAME instead of END_ROD for better visibility
        player.getWorld().spawnParticle(Particle.FLAME, location, 1, 0, 0, 0, 0);
    }
}