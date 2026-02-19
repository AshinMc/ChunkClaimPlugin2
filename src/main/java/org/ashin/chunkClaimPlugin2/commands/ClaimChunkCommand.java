package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ClaimChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ClaimChunkCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
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

        // Determine claim name from args or generate default
        String claimName;
        if (args.length > 0) {
            claimName = String.join(" ", args);
            // Check if the player already has a claim group with this name
            if (chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-name-exists", "name", claimName));
                return true;
            }
        } else {
            // Generate default name: "Claim #N"
            int next = chunkManager.getPlayerClaimNames(player.getUniqueId()).size() + 1;
            claimName = "Claim #" + next;
        }

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

        // Check max claims limit
        int maxClaims = plugin.getConfig().getInt("max-claims-per-player", 10);
        if (maxClaims > 0) {
            int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
            if (currentCount >= maxClaims) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                        "max", String.valueOf(maxClaims)));
                return true;
            }
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return true;
        }

        // Actually claim the chunk with the name
        if (chunkManager.claimChunk(player, chunk, claimName)) {
            chunkManager.saveData();
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-success", "name", claimName));
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-fail"));
        }

        return true;
    }
}