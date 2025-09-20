package org.ashin.chunkClaimPlugin2.handlers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class WorldGuardHandler {
    private final Logger logger;
    private final boolean worldGuardEnabled;

    public WorldGuardHandler(JavaPlugin plugin) {
        this.logger = plugin.getLogger();

        // Check if WorldGuard is available
        this.worldGuardEnabled = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;

        if (worldGuardEnabled) {
            logger.info("WorldGuard detected - enabling integration (version 7.0.13)");
        } else {
            logger.info("WorldGuard not detected - integration disabled");
        }
    }

    /**
     * Checks if a chunk overlaps with any WorldGuard regions
     * @param chunk The chunk to check
     * @param player The player attempting to claim (for permission checks)
     * @return true if the player can claim this chunk, false if not
     */
    public boolean canClaimChunk(Chunk chunk, Player player) {
        if (!worldGuardEnabled) {
            return true; // WorldGuard not enabled, so no restrictions
        }

        try {
            // Get the WorldGuard region container
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(chunk.getWorld()));

            if (regions == null) {
                return true; // No region manager for this world
            }

            // Calculate chunk corners
            int chunkX = chunk.getX() << 4;
            int chunkZ = chunk.getZ() << 4;

            // Check the corners of the chunk
            Location loc1 = new Location(chunk.getWorld(), chunkX, 64, chunkZ);
            Location loc2 = new Location(chunk.getWorld(), chunkX + 15, 64, chunkZ + 15);

            // Get regions at these locations
            ApplicableRegionSet set1 = regions.getApplicableRegions(
                    BukkitAdapter.asBlockVector(loc1));
            ApplicableRegionSet set2 = regions.getApplicableRegions(
                    BukkitAdapter.asBlockVector(loc2));

            // If no regions at either point, the chunk is free to claim
            if (set1.size() == 0 && set2.size() == 0) {
                return true;
            }

            // Convert the player to a WorldGuard player - fixed approach
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            // Check if the player is an owner of all regions in both sets
            return set1.isOwnerOfAll(localPlayer) && set2.isOwnerOfAll(localPlayer);

        } catch (Exception e) {
            logger.warning("Error checking WorldGuard regions: " + e.getMessage());
            e.printStackTrace();
            return true; // Default to allowing claims if integration fails
        }
    }
}