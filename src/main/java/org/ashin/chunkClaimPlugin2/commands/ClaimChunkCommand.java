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

import java.util.UUID;

public class ClaimChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public ClaimChunkCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
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
            claimName = "Claim_#" + next;
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
        int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        int maxClaims = chunkManager.getPlayerLimit(player.getUniqueId(), defaultMax);
        int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
        if (maxClaims > 0 && currentCount >= maxClaims) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                    "max", String.valueOf(maxClaims)));
            return true;
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return true;
        }

        // Check Economy Cost
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

        // Actually claim the chunk with the name
        if (chunkManager.claimChunk(player, chunk, claimName)) {
            chunkManager.saveData();
            if (messages.isMessageEnabled("claim-success")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-success", "name", claimName));
            }
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
            // Refund if paid
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