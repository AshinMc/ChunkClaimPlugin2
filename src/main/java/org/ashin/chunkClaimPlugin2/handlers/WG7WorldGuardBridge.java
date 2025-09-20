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

import java.util.UUID;

public class WG7WorldGuardBridge implements WorldGuardBridge {
    @Override
    public boolean canClaimChunk(Chunk chunk, Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(chunk.getWorld()));
        if (regions == null) return true;

        World w = chunk.getWorld();
        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        BlockVector3 min = BlockVector3.at(baseX, minY, baseZ);
        BlockVector3 max = BlockVector3.at(baseX + 15, maxY, baseZ + 15);
        ProtectedCuboidRegion cuboid = new ProtectedCuboidRegion("chunkclaim-" + UUID.randomUUID(), min, max);

        ApplicableRegionSet set = regions.getApplicableRegions(cuboid);
        if (set.size() == 0) return true;

        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return set.isOwnerOfAll(localPlayer);
    }
}
