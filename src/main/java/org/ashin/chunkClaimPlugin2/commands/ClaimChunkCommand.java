package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;

    public ClaimChunkCommand(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

    Player player = (Player) sender;
    Chunk chunk = player.getLocation().getChunk();

        // Check if the chunk is already claimed by someone
        UUID existingOwner = chunkManager.getChunkOwner(chunk);
        if (existingOwner != null) {
            if (existingOwner.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "You have already claimed this chunk.");
            } else {
                player.sendMessage(ChatColor.RED + "This chunk is already claimed by another player.");
            }
            return true;
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(ChatColor.RED + "You cannot claim this chunk because it overlaps with a WorldGuard region you don't own.");
            return true;
        }

        // Actually claim the chunk
        if (chunkManager.claimChunk(player, chunk)) {
            chunkManager.saveData();
            player.sendMessage(ChatColor.GREEN + "You have successfully claimed this chunk!");
        } else {
            // This should rarely happen due to the checks above, but just in case
            player.sendMessage(ChatColor.RED + "Failed to claim this chunk.");
        }

        return true;
    }
}