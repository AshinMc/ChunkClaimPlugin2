package org.ashin.chunkClaimPlugin2.handlers;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public interface WorldGuardBridge {
    boolean canClaimChunk(Chunk chunk, Player player);
}
