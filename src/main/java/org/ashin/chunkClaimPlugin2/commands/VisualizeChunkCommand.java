package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VisualizeChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    private final Map<UUID, BukkitTask> activeVisualizations = new HashMap<>();

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
        
        int durationSeconds = plugin.getConfig().getInt("visualization.duration-seconds", 10);

        // If player has an active visualization, toggle it off
        if (activeVisualizations.containsKey(playerId)) {
            activeVisualizations.get(playerId).cancel();
            activeVisualizations.remove(playerId);
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "Visualization stopped.");
            return true;
        }

        if (args.length > 0) {
            String claimName = String.join(" ", args);

            // Check if the player has a claim with this name
            List<ChunkData> chunks = chunkManager.getChunksByName(playerId, claimName);
            if (chunks.isEmpty()) {
                player.sendMessage(messages.getFor(playerId, "expand-not-found", "name", claimName));
                return true;
            }

            World world = Bukkit.getWorld(chunks.get(0).getWorld());
            if (world == null) {
                player.sendMessage(messages.getFor(playerId, "chunk-claim-fail"));
                return true;
            }

            player.sendMessage(messages.getFor(playerId, "visualize-start",
                    "count", String.valueOf(chunks.size()), "seconds", durationSeconds <= 0 ? "indefinite" : String.valueOf(durationSeconds)));
            startVisualization(player, durationSeconds, () -> visualizeChunkGroupPerimeter(player, chunks));
            return true;
        }

        // No args: use the claim the player is standing on, or all claims as fallback
        Chunk currentChunk = player.getLocation().getChunk();
        UUID standingOwner = chunkManager.getChunkOwner(currentChunk);
        if (standingOwner != null && standingOwner.equals(playerId)) {
            String standingName = chunkManager.getChunkClaimName(currentChunk);
            if (standingName != null) {
                List<ChunkData> standingChunks = chunkManager.getChunksByName(playerId, standingName);
                if (!standingChunks.isEmpty()) {
                    World sWorld = Bukkit.getWorld(standingChunks.get(0).getWorld());
                    if (sWorld != null) {
                        player.sendMessage(messages.getFor(playerId, "visualize-start",
                                "count", String.valueOf(standingChunks.size()), "seconds", durationSeconds <= 0 ? "indefinite" : String.valueOf(durationSeconds)));
                        startVisualization(player, durationSeconds, () -> visualizeChunkGroupPerimeter(player, standingChunks));
                    }
                    return true;
                }
            }
        }

        // Fallback: visualize all player claims accurately per group
        List<String> claimNames = chunkManager.getPlayerClaimNames(playerId);
        if (claimNames.isEmpty()) {
            player.sendMessage(messages.getFor(playerId, "no-claims"));
            return true;
        }

        int totalChunks = chunkManager.getPlayerChunkCount(playerId);
        player.sendMessage(messages.getFor(playerId, "visualize-start",
                "count", String.valueOf(totalChunks), "seconds", durationSeconds <= 0 ? "indefinite" : String.valueOf(durationSeconds)));

        startVisualization(player, durationSeconds, () -> {
            for (String name : claimNames) {
                List<ChunkData> chunks = chunkManager.getChunksByName(playerId, name);
                if (!chunks.isEmpty()) {
                    visualizeChunkGroupPerimeter(player, chunks);
                }
            }
        });

        return true;
    }

    private void startVisualization(Player player, int durationSeconds, Runnable drawAction) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = durationSeconds;

            @Override
            public void run() {
                if (durationSeconds > 0 && secondsLeft <= 0) {
                    activeVisualizations.remove(playerId);
                    cancel();
                    return;
                }
                if (!player.isOnline()) {
                    activeVisualizations.remove(playerId);
                    cancel();
                    return;
                }
                drawAction.run();
                if (durationSeconds > 0) secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeVisualizations.put(playerId, task);
    }

    /**
     * Renders exact outer perimeter particle borders per-chunk in the player's current world,
     * skipping shared interior edges so L-shaped and disconnected claims render accurately.
     */
    private void visualizeChunkGroupPerimeter(Player player, List<ChunkData> chunks) {
        String currentWorld = player.getWorld().getName();
        java.util.Set<String> groupCoords = new java.util.HashSet<>();
        for (ChunkData cd : chunks) {
            if (currentWorld.equals(cd.getWorld())) {
                groupCoords.add(cd.getX() + ":" + cd.getZ());
            }
        }
        if (groupCoords.isEmpty()) return;

        int particleHeight = Math.max(1, Math.min(384, plugin.getConfig().getInt("visualization.particle-height", 100)));
        double configuredSpacing = plugin.getConfig().getDouble("visualization.particle-spacing", 0.5);
        double particleSpacing = Math.max(0.1, Math.min(8.0, configuredSpacing));

        int playerY = player.getLocation().getBlockY();
        int worldMinY = player.getWorld().getMinHeight();
        int worldMaxY = player.getWorld().getMaxHeight();
        int minY = Math.max(worldMinY, playerY - 5);
        int maxY = Math.min(minY + particleHeight, worldMaxY);
        double eyeY = playerY + 1.5;

        for (ChunkData cd : chunks) {
            if (!currentWorld.equals(cd.getWorld())) continue;
            int cx = cd.getX();
            int cz = cd.getZ();
            int minX = cx << 4;
            int minZ = cz << 4;
            int maxX = minX + 16;
            int maxZ = minZ + 16;

            boolean hasNorth = groupCoords.contains(cx + ":" + (cz - 1));
            boolean hasSouth = groupCoords.contains(cx + ":" + (cz + 1));
            boolean hasWest = groupCoords.contains((cx - 1) + ":" + cz);
            boolean hasEast = groupCoords.contains((cx + 1) + ":" + cz);

            if (!hasNorth) {
                for (double x = minX; x <= maxX; x += particleSpacing) spawnParticle(player, x, eyeY, minZ);
            }
            if (!hasSouth) {
                for (double x = minX; x <= maxX; x += particleSpacing) spawnParticle(player, x, eyeY, maxZ);
            }
            if (!hasWest) {
                for (double z = minZ; z <= maxZ; z += particleSpacing) spawnParticle(player, minX, eyeY, z);
            }
            if (!hasEast) {
                for (double z = minZ; z <= maxZ; z += particleSpacing) spawnParticle(player, maxX, eyeY, z);
            }

            // Vertical corner pillars only at outer corners
            if (!hasNorth || !hasWest) drawVerticalPillar(player, minX, minZ, minY, maxY, particleSpacing);
            if (!hasNorth || !hasEast) drawVerticalPillar(player, maxX, minZ, minY, maxY, particleSpacing);
            if (!hasSouth || !hasWest) drawVerticalPillar(player, minX, maxZ, minY, maxY, particleSpacing);
            if (!hasSouth || !hasEast) drawVerticalPillar(player, maxX, maxZ, minY, maxY, particleSpacing);
        }
    }

    private void drawVerticalPillar(Player player, double x, double z, int minY, int maxY, double spacing) {
        for (double y = minY; y <= maxY; y += spacing) {
            spawnParticle(player, x, y, z);
        }
    }

    private void spawnParticle(Player player, double x, double y, double z) {
        Location location = new Location(player.getWorld(), x, y, z);
        // Use per-player particle, falling back to server config, then FLAME
        String particleName = messages.getPlayerParticle(player.getUniqueId());
        if (particleName == null || particleName.isEmpty()) {
            particleName = org.ashin.chunkClaimPlugin2.config.ConfigMigrator
                    .getNonBlankString(plugin.getConfig(), "visualization.particle-type", "FLAME");
        }
        Particle particle;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            particle = Particle.FLAME;
        }
        String name = particle.name();
        if (name.equals("DUST") || name.equals("REDSTONE")) {
            player.spawnParticle(particle, location, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
        } else {
            player.spawnParticle(particle, location, 1, 0, 0, 0, 0);
        }
    }
}