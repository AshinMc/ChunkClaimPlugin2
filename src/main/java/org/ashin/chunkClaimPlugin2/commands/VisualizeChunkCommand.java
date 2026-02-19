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

import java.util.List;
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

        if (args.length > 0) {
            String claimName = String.join(" ", args);

            // Check if the player has a claim with this name
            List<ChunkData> chunks = chunkManager.getChunksByName(playerId, claimName);
            if (chunks.isEmpty()) {
                player.sendMessage(messages.getFor(playerId, "expand-not-found", "name", claimName));
                return true;
            }

            player.sendMessage(messages.getFor(playerId, "visualize-start",
                    "count", String.valueOf(chunks.size()), "seconds", String.valueOf(VISUALIZATION_SECONDS)));

            // Calculate merged bounding box in block coordinates
            int minBlockX = Integer.MAX_VALUE, minBlockZ = Integer.MAX_VALUE;
            int maxBlockX = Integer.MIN_VALUE, maxBlockZ = Integer.MIN_VALUE;
            String worldName = chunks.get(0).getWorld();
            for (ChunkData cd : chunks) {
                int bx = cd.getX() << 4;
                int bz = cd.getZ() << 4;
                minBlockX = Math.min(minBlockX, bx);
                minBlockZ = Math.min(minBlockZ, bz);
                maxBlockX = Math.max(maxBlockX, bx + 16);
                maxBlockZ = Math.max(maxBlockZ, bz + 16);
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(messages.getFor(playerId, "chunk-claim-fail"));
                return true;
            }

            final int fMinX = minBlockX, fMinZ = minBlockZ, fMaxX = maxBlockX, fMaxZ = maxBlockZ;

            new BukkitRunnable() {
                int secondsLeft = VISUALIZATION_SECONDS;
                @Override
                public void run() {
                    if (secondsLeft <= 0) { cancel(); return; }
                    visualizeBoundingBox(player, fMinX, fMinZ, fMaxX, fMaxZ);
                    secondsLeft--;
                }
            }.runTaskTimer(plugin, 0L, 20L);

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
                    player.sendMessage(messages.getFor(playerId, "visualize-start",
                            "count", String.valueOf(standingChunks.size()), "seconds", String.valueOf(VISUALIZATION_SECONDS)));
                    int sMinX = Integer.MAX_VALUE, sMinZ = Integer.MAX_VALUE;
                    int sMaxX = Integer.MIN_VALUE, sMaxZ = Integer.MIN_VALUE;
                    String sWorldName = standingChunks.get(0).getWorld();
                    for (ChunkData cd : standingChunks) {
                        int bx = cd.getX() << 4;
                        int bz = cd.getZ() << 4;
                        sMinX = Math.min(sMinX, bx);
                        sMinZ = Math.min(sMinZ, bz);
                        sMaxX = Math.max(sMaxX, bx + 16);
                        sMaxZ = Math.max(sMaxZ, bz + 16);
                    }
                    World sWorld = Bukkit.getWorld(sWorldName);
                    if (sWorld != null) {
                        final int fsMinX = sMinX, fsMinZ = sMinZ, fsMaxX = sMaxX, fsMaxZ = sMaxZ;
                        new BukkitRunnable() {
                            int secondsLeft = VISUALIZATION_SECONDS;
                            @Override
                            public void run() {
                                if (secondsLeft <= 0) { cancel(); return; }
                                visualizeBoundingBox(player, fsMinX, fsMinZ, fsMaxX, fsMaxZ);
                                secondsLeft--;
                            }
                        }.runTaskTimer(plugin, 0L, 20L);
                    }
                    return true;
                }
            }
        }

        // Fallback: visualize all player claims (each group as a merged box)
        List<String> claimNames = chunkManager.getPlayerClaimNames(playerId);
        if (claimNames.isEmpty()) {
            player.sendMessage(messages.getFor(playerId, "no-claims"));
            return true;
        }

        int totalChunks = chunkManager.getPlayerChunkCount(playerId);
        player.sendMessage(messages.getFor(playerId, "visualize-start",
                "count", String.valueOf(totalChunks), "seconds", String.valueOf(VISUALIZATION_SECONDS)));

        new BukkitRunnable() {
            int secondsLeft = VISUALIZATION_SECONDS;

            @Override
            public void run() {
                if (secondsLeft <= 0) { cancel(); return; }

                for (String name : claimNames) {
                    List<ChunkData> chunks = chunkManager.getChunksByName(playerId, name);
                    if (chunks.isEmpty()) continue;
                    // Only visualize chunks in the player's current world
                    String worldName = chunks.get(0).getWorld();
                    if (!player.getWorld().getName().equals(worldName)) continue;

                    int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                    for (ChunkData cd : chunks) {
                        int bx = cd.getX() << 4;
                        int bz = cd.getZ() << 4;
                        minX = Math.min(minX, bx);
                        minZ = Math.min(minZ, bz);
                        maxX = Math.max(maxX, bx + 16);
                        maxZ = Math.max(maxZ, bz + 16);
                    }
                    visualizeBoundingBox(player, minX, minZ, maxX, maxZ);
                }
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    /**
     * Draws particle borders for a merged bounding box (block coordinates).
     */
    private void visualizeBoundingBox(Player player, int minX, int minZ, int maxX, int maxZ) {
        int playerY = player.getLocation().getBlockY();
        int worldMinY = player.getWorld().getMinHeight();
        int worldMaxY = player.getWorld().getMaxHeight();
        int minY = Math.max(worldMinY, playerY - 5);
        int maxY = Math.min(minY + PARTICLE_HEIGHT, worldMaxY);

        // Vertical corner pillars
        for (double y = minY; y <= maxY; y += PARTICLE_SPACING) {
            spawnParticle(player, minX, y, minZ);
            spawnParticle(player, maxX, y, minZ);
            spawnParticle(player, minX, y, maxZ);
            spawnParticle(player, maxX, y, maxZ);
        }

        // Horizontal edges at eye level
        double eyeY = playerY + 1.5;
        for (double x = minX; x <= maxX; x += PARTICLE_SPACING) {
            spawnParticle(player, x, eyeY, minZ);
            spawnParticle(player, x, eyeY, maxZ);
        }
        for (double z = minZ; z <= maxZ; z += PARTICLE_SPACING) {
            spawnParticle(player, minX, eyeY, z);
            spawnParticle(player, maxX, eyeY, z);
        }
    }

    private void spawnParticle(Player player, double x, double y, double z) {
        Location location = new Location(player.getWorld(), x, y, z);
        // Use per-player particle, falling back to server config, then FLAME
        String particleName = messages.getPlayerParticle(player.getUniqueId());
        if (particleName == null || particleName.isEmpty()) {
            particleName = plugin.getConfig().getString("visualization.particle-type", "FLAME");
        }
        Particle particle;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            particle = Particle.FLAME;
        }
        if (particle == Particle.DUST) {
            player.getWorld().spawnParticle(particle, location, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
        } else {
            player.getWorld().spawnParticle(particle, location, 1, 0, 0, 0, 0);
        }
    }
}