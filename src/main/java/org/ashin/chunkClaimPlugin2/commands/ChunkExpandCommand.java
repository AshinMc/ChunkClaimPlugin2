package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
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
    private final EconomyManager economyManager;

    public ChunkExpandCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

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
        int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        int maxClaims = chunkManager.getPlayerLimit(player.getUniqueId(), defaultMax);
        int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
        if (maxClaims > 0 && currentCount >= maxClaims) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                    "max", String.valueOf(maxClaims)));
            return true;
        }

        // Check WorldGuard
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return true;
        }

        // Check economy cost
        double cost = economyManager.getClaimCost(currentCount);
        if (cost > 0.0) {
            if (!economyManager.getProvider().has(player, cost)) {
                String formatted = economyManager.getProvider().format(cost);
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-insufficient-funds", "cost", formatted));
                return true;
            }
        }

        boolean paid = false;
        if (cost > 0.0) {
            paid = economyManager.getProvider().withdraw(player, cost);
            if (!paid) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-payment-failed"));
                return true;
            }
        }

        // Expand: add this chunk to the existing claim group
        if (chunkManager.claimChunk(player, chunk, claimName)) {
            chunkManager.saveData();
            int total = chunkManager.getChunksByName(player.getUniqueId(), claimName).size();
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-success",
                    "name", claimName, "total", String.valueOf(total)));
            if (cost > 0.0) {
                String formatted = economyManager.getProvider().format(cost);
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-cost-notice", "cost", formatted));
            }
            // Run success commands
            java.util.List<String> cmds = plugin.getConfig().getStringList("event-commands.claim-success");
            for (String cmd : cmds) {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                        cmd.replace("%player%", player.getName()).replace("%claim%", claimName));
            }
        } else {
            if (paid) {
                economyManager.getProvider().deposit(player, cost);
            }
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-fail"));
            // Run fail commands
            java.util.List<String> cmds = plugin.getConfig().getStringList("event-commands.claim-fail");
            for (String cmd : cmds) {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                        cmd.replace("%player%", player.getName()).replace("%claim%", claimName));
            }
        }

        return true;
    }
}
