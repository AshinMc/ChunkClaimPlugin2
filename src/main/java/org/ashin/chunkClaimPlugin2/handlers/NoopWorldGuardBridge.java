package org.ashin.chunkClaimPlugin2.handlers;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class NoopWorldGuardBridge implements WorldGuardBridge {
    @Override
    public boolean canClaimChunk(Chunk chunk, Player player) {
        return true; // allow claims when WG is not present
    }
}
