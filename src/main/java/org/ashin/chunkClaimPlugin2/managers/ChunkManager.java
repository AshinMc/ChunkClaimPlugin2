package org.ashin.chunkClaimPlugin2.managers;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.handlers.WorldGuardHandler;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChunkManager {
    private final JavaPlugin plugin;
    private final Map<String, UUID> claimedChunks = new HashMap<>();
    private final File dataFile;
    private final FileConfiguration dataConfig;
    public final WorldGuardHandler worldGuardHandler;

    public ChunkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chunkclaims.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.worldGuardHandler = new WorldGuardHandler(plugin);
        loadData();
    }

    /**
     * Checks if a player can claim a chunk, considering both existing claims and WorldGuard regions
     * @param chunk The chunk to check
     * @param player The player attempting to claim
     * @return true if the chunk can be claimed, false otherwise
     */
    public boolean canClaimChunk(Chunk chunk, Player player) {
        String chunkKey = getChunkKey(chunk);

        // First check if chunk is already claimed
        if (claimedChunks.containsKey(chunkKey)) {
            return false;
        }

        // Then check WorldGuard permissions
        return worldGuardHandler.canClaimChunk(chunk, player);
    }

    /**
     * Returns a copy of all claimed chunks
     * @return Map of chunk keys to owner UUIDs
     */
    public Map<String, UUID> getClaimedChunks() {
        return new HashMap<>(claimedChunks);  // Return a copy to prevent external modification
    }

    public boolean claimChunk(Player player, Chunk chunk) {
        // Use the new canClaimChunk method
        if (!canClaimChunk(chunk, player)) {
            return false;
        }

        String chunkKey = getChunkKey(chunk);
        claimedChunks.put(chunkKey, player.getUniqueId());
        return true;
    }

    public boolean claimChunk(String chunkKey, UUID playerId) {
        if (claimedChunks.containsKey(chunkKey)) {
            return false;
        }

        claimedChunks.put(chunkKey, playerId);
        return true;
    }

    public boolean unclaimChunk(Player player, Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        UUID owner = claimedChunks.get(chunkKey);

        if (owner == null || !owner.equals(player.getUniqueId())) {
            return false;
        }

        claimedChunks.remove(chunkKey);
        return true;
    }

    public UUID getChunkOwner(Chunk chunk) {
        return claimedChunks.get(getChunkKey(chunk));
    }

    public List<ChunkData> getPlayerChunks(UUID playerId) {
        List<ChunkData> chunks = new ArrayList<>();

        for (Map.Entry<String, UUID> entry : claimedChunks.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                String key = entry.getKey();
                String[] parts = key.split(":");
                if (parts.length >= 3) {
                    String world = parts[0];
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        chunks.add(new ChunkData(world, x, z));
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid chunk coordinates in key: " + key);
                    }
                } else {
                    plugin.getLogger().warning("Invalid chunk key format: " + key);
                }
            }
        }

        return chunks;
    }

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public void loadData() {
        claimedChunks.clear();

        if (dataConfig.contains("claims")) {
            for (String key : dataConfig.getConfigurationSection("claims").getKeys(false)) {
                UUID owner = UUID.fromString(dataConfig.getString("claims." + key));
                claimedChunks.put(key, owner);
            }
        }
    }

    public void saveData() {
        // Clear existing data
        dataConfig.set("claims", null);

        // Save all claims
        for (Map.Entry<String, UUID> entry : claimedChunks.entrySet()) {
            dataConfig.set("claims." + entry.getKey(), entry.getValue().toString());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chunk claim data: " + e.getMessage());
        }
    }

}