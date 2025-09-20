package org.ashin.chunkClaimPlugin2.handlers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
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

            // Build a cuboid for the whole chunk, from world min to max height
            World w = chunk.getWorld();
            int minY = w.getMinHeight();
            int maxY = w.getMaxHeight();
            int baseX = chunk.getX() << 4;
            int baseZ = chunk.getZ() << 4;
            BlockVector3 min = BlockVector3.at(baseX, minY, baseZ);
            BlockVector3 max = BlockVector3.at(baseX + 15, maxY, baseZ + 15);
            // Dummy id; not added to manager, only used for intersection query
            ProtectedCuboidRegion cuboid = new ProtectedCuboidRegion("chunkclaim-" + UUID.randomUUID(), min, max);

            // Regions intersecting the whole chunk
            ApplicableRegionSet set = regions.getApplicableRegions(cuboid);
            if (set.size() == 0) {
                return true; // No regions intersecting this chunk
            }

            // Player must own all overlapping regions
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            return set.isOwnerOfAll(localPlayer);

        } catch (Exception e) {
            logger.warning("Error checking WorldGuard regions: " + e.getMessage());
            e.printStackTrace();
            return true; // Default to allowing claims if integration fails
        }
    }
}