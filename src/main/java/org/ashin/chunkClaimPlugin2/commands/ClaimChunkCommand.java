package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ClaimChunkCommand(ChunkManager chunkManager, MessageManager messages) {
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
    Chunk chunk = player.getLocation().getChunk();

        // Check if the chunk is already claimed by someone
        UUID existingOwner = chunkManager.getChunkOwner(chunk);
        if (existingOwner != null) {
            if (existingOwner.equals(player.getUniqueId())) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-self"));
            } else {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-other"));
            }
            return true;
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return true;
        }

        // Actually claim the chunk
        if (chunkManager.claimChunk(player, chunk)) {
            chunkManager.saveData();
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-success"));
        } else {
            // This should rarely happen due to the checks above, but just in case
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-fail"));
        }

        return true;
    }
}