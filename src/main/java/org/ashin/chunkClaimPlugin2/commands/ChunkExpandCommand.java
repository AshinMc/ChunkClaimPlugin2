package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkExpandCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ChunkExpandCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
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

        if (args.length == 0) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-usage"));
            return true;
        }

        String claimName = String.join(" ", args);
        Chunk chunk = player.getLocation().getChunk();

        // Check if the player has a claim group with this name
        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return true;
        }

        // Check if the current chunk is already claimed
        java.util.UUID existingOwner = chunkManager.getChunkOwner(chunk);
        if (existingOwner != null) {
            if (existingOwner.equals(player.getUniqueId())) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-self"));
            } else {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-other"));
            }
            return true;
        }

        // Check max claims limit (each individual chunk counts)
        int maxClaims = plugin.getConfig().getInt("max-claims-per-player", 10);
        if (maxClaims > 0) {
            int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
            if (currentCount >= maxClaims) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                        "max", String.valueOf(maxClaims)));
                return true;
            }
        }

        // Check WorldGuard
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return true;
        }

        // Expand: add this chunk to the existing claim group
        if (chunkManager.claimChunk(player, chunk, claimName)) {
            chunkManager.saveData();
            int total = chunkManager.getChunksByName(player.getUniqueId(), claimName).size();
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-success",
                    "name", claimName, "total", String.valueOf(total)));
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-fail"));
        }

        return true;
    }
}
