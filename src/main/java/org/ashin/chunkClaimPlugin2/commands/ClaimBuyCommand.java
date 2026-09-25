package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class ClaimBuyCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public ClaimBuyCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
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

        if (!economyManager.isEconomyEnabled()) {
            player.sendMessage(ChatColor.RED + "Economy is currently disabled on this server.");
            return true;
        }

        UUID sellerUUID = null;
        String claimName = null;

        if (args.length > 0) {
            String targetName = String.join(" ", args);
            // Search all for-sale claims for one matching targetName
            for (Map.Entry<String, Double> entry : chunkManager.getAllClaimsForSale().entrySet()) {
                String[] parts = entry.getKey().split(":", 2);
                if (parts.length == 2 && parts[1].equalsIgnoreCase(targetName)) {
                    sellerUUID = UUID.fromString(parts[0]);
                    claimName = parts[1];
                    break;
                }
            }
            if (sellerUUID == null) {
                player.sendMessage(ChatColor.RED + "No claim named '" + targetName + "' was found for sale.");
                return true;
            }
        } else {
            // Check chunk player is currently standing in
            Chunk currentChunk = player.getLocation().getChunk();
            sellerUUID = chunkManager.getChunkOwner(currentChunk);
            if (sellerUUID == null) {
                player.sendMessage(ChatColor.RED + "This chunk is not claimed. Stand inside a claim that is for sale or specify the claim name: /" + label + " <name>");
                return true;
            }
            claimName = chunkManager.getChunkClaimName(currentChunk);
            if (claimName == null || !chunkManager.isClaimForSale(sellerUUID, claimName)) {
                player.sendMessage(ChatColor.RED + "This claim is not currently for sale.");
                return true;
            }
        }

        if (sellerUUID.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own claim! Use /chunksell " + claimName + " cancel to stop selling it.");
            return true;
        }

        Double price = chunkManager.getClaimPrice(sellerUUID, claimName);
        if (price == null || price <= 0.0) {
            player.sendMessage(ChatColor.RED + "This claim is not currently for sale.");
            return true;
        }

        // Check chunk limits
        int groupChunks = chunkManager.getChunksByName(sellerUUID, claimName).size();
        int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        int maxClaims = chunkManager.getPlayerLimit(player.getUniqueId(), defaultMax);
        if (maxClaims > 0) {
            int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
            if (currentCount + groupChunks > maxClaims) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                        "max", String.valueOf(maxClaims)));
                return true;
            }
        }

        // Check funds
        if (!economyManager.getProvider().has(player, price)) {
            String formattedPrice = economyManager.getProvider().format(price);
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-insufficient-funds",
                    "cost", formattedPrice));
            return true;
        }

        // Execute payment
        boolean paid = economyManager.getProvider().withdraw(player, price);
        if (!paid) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-payment-failed"));
            return true;
        }

        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerUUID);
        economyManager.getProvider().deposit(seller, price);

        // Transfer claim
        int transferred = chunkManager.transferClaim(sellerUUID, player.getUniqueId(), claimName);
        chunkManager.setClaimPrice(sellerUUID, claimName, null);
        chunkManager.saveData();

        String formattedPrice = economyManager.getProvider().format(price);
        String sellerName = seller.getName() != null ? seller.getName() : "Previous Owner";

        player.sendMessage(messages.getFor(player.getUniqueId(), "claim-buy-success",
                "name", claimName, "count", String.valueOf(transferred), "price", formattedPrice, "seller", sellerName));

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        } catch (Throwable ignored) {}

        if (seller.isOnline() && seller.getPlayer() != null) {
            seller.getPlayer().sendMessage(messages.getFor(seller.getUniqueId(), "claim-sold-notify",
                    "name", claimName, "buyer", player.getName(), "price", formattedPrice));
        }

        return true;
    }
}
