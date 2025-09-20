package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class ChunkProtectionListener implements Listener {
    private final ChunkManager chunkManager;

    public ChunkProtectionListener(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (canModifyChunk(event.getPlayer(), event.getBlock().getChunk())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "You cannot break blocks in a chunk claimed by someone else.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canModifyChunk(event.getPlayer(), event.getBlock().getChunk())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks in a chunk claimed by someone else.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (canModifyChunk(event.getPlayer(), event.getClickedBlock().getChunk())) {
            return;
        }

        // Only cancel interactions with containers, doors, etc.
        switch (event.getClickedBlock().getType().name()) {
            case "CHEST", "TRAPPED_CHEST", "BARREL", "FURNACE", "BLAST_FURNACE", "SMOKER",
                 "HOPPER", "DROPPER", "DISPENSER", "BREWING_STAND", "LEVER", "BUTTON",
                 "DOOR", "TRAPDOOR", "FENCE_GATE" -> {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot interact with objects in a chunk claimed by someone else.");
            }
        }
    }

    private boolean canModifyChunk(Player player, Chunk chunk) {
        UUID owner = chunkManager.getChunkOwner(chunk);

        // Not claimed or player is the owner
        return owner == null || owner.equals(player.getUniqueId()) || player.hasPermission("chunkclaimprotection.bypass");
    }
}