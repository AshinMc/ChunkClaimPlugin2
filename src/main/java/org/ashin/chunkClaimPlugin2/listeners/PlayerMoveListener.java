package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public PlayerMoveListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();
        
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return; // completely within same chunk
        }
        
        Player player = event.getPlayer();
        UUID fromOwner = chunkManager.getChunkOwner(fromChunk);
        String fromName = fromOwner != null ? chunkManager.getChunkClaimName(fromChunk) : null;
        
        UUID toOwner = chunkManager.getChunkOwner(toChunk);
        String toName = toOwner != null ? chunkManager.getChunkClaimName(toChunk) : null;
        
        // Check if we entered a different claim group
        boolean sameOwner = (fromOwner != null && fromOwner.equals(toOwner));
        boolean sameName = (fromName != null && fromName.equals(toName));
        
        if (toOwner != null && (!sameOwner || !sameName)) {
            // We entered a new claim group
            // Check if greeting is enabled for this claim
            if (chunkManager.getClaimFlag(toOwner, toName, ChunkManager.FLAG_GREETING_TITLE)) {
                String ownerName = Bukkit.getOfflinePlayer(toOwner).getName();
                if (ownerName == null) ownerName = "Unknown";
                
                String title = toName;
                String subtitle = messages.getFor(player.getUniqueId(), "greeting-subtitle", "player", ownerName)
                    .replace("%player%", ownerName);
                
                // Fallback translations if missing
                if (subtitle.equals("greeting-subtitle")) {
                    subtitle = "Owned by " + ownerName;
                }
                
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
        }
    }
}
