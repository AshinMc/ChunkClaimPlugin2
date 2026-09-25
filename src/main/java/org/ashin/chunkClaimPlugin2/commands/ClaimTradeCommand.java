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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified player-to-player marketplace command: /claimtrade (alias: /chunktrade).
 * Handles:
 *  - /claimtrade buy [claim]
 *  - /claimtrade sell <claim> <price>
 *  - /claimtrade cancel <claim>
 *  - /claimtrade list
 */
public class ClaimTradeCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public ClaimTradeCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
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

        if (!player.hasPermission("ccp.trade") && !player.hasPermission("ccp.sell") && !player.hasPermission("ccp.buy")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use claim trade commands.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "sell" -> handleSell(player, label, args);
            case "cancel" -> handleCancel(player, label, args);
            case "buy" -> handleBuy(player, label, args);
            case "list" -> handleList(player, label);
            default -> sendHelp(player, label);
        }

        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "=== Claim Trade Market ===");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " buy [claim] " + ChatColor.GRAY + "- Buy a claim group currently for sale");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " sell <claim> <price> " + ChatColor.GRAY + "- Put your claim group up for sale");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " cancel <claim> " + ChatColor.GRAY + "- Remove your claim from the market");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " list " + ChatColor.GRAY + "- View all claims currently for sale");
    }

    private void handleSell(Player player, String label, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " sell <claimName> <price>");
            return;
        }

        String claimName = args[1];
        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return;
        }

        String priceArg = args[2];
        if (priceArg.equalsIgnoreCase("cancel") || priceArg.equalsIgnoreCase("off") || priceArg.equals("0")) {
            chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
            chunkManager.saveData();
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-sale-cancelled", "name", claimName));
            return;
        }

        try {
            double price = Double.parseDouble(priceArg);
            if (price <= 0.0) {
                player.sendMessage(ChatColor.RED + "Price must be greater than zero, or type 'cancel' to remove the listing.");
                return;
            }

            chunkManager.setClaimPrice(player.getUniqueId(), claimName, price);
            chunkManager.saveData();
            String formattedPrice = economyManager.getProvider().format(price);
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-listed-for-sale",
                    "name", claimName, "price", formattedPrice));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number format for price.");
        }
    }

    private void handleCancel(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " cancel <claimName>");
            return;
        }

        String claimName = args[1];
        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return;
        }

        if (!chunkManager.isClaimForSale(player.getUniqueId(), claimName)) {
            player.sendMessage(ChatColor.RED + "Claim '" + claimName + "' is not currently listed for sale.");
            return;
        }

        chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
        chunkManager.saveData();
        player.sendMessage(messages.getFor(player.getUniqueId(), "claim-sale-cancelled", "name", claimName));
    }

    private void handleBuy(Player player, String label, String[] args) {
        UUID sellerUUID = null;
        String claimName = null;

        if (args.length > 1) {
            String targetName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
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
                return;
            }
        } else {
            // Check chunk player is currently standing in
            Chunk currentChunk = player.getLocation().getChunk();
            sellerUUID = chunkManager.getChunkOwner(currentChunk);
            if (sellerUUID == null) {
                player.sendMessage(ChatColor.RED + "This chunk is not claimed. Stand inside a claim that is for sale or specify the claim name: /" + label + " buy <name>");
                return;
            }
            claimName = chunkManager.getChunkClaimName(currentChunk);
            if (claimName == null || !chunkManager.isClaimForSale(sellerUUID, claimName)) {
                player.sendMessage(ChatColor.RED + "This claim is not currently for sale.");
                return;
            }
        }

        if (sellerUUID.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot buy your own claim! Use /" + label + " cancel " + claimName + " to stop selling it.");
            return;
        }

        Double price = chunkManager.getClaimPrice(sellerUUID, claimName);
        if (price == null || price <= 0.0) {
            player.sendMessage(ChatColor.RED + "This claim is not currently for sale.");
            return;
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
                return;
            }
        }

        // Check funds
        if (!economyManager.getProvider().has(player, price)) {
            String formattedPrice = economyManager.getProvider().format(price);
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-insufficient-funds",
                    "cost", formattedPrice));
            return;
        }

        // Execute payment
        boolean paid = economyManager.getProvider().withdraw(player, price);
        if (!paid) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-payment-failed"));
            return;
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
    }

    private void handleList(Player player, String label) {
        Map<String, Double> allForSale = chunkManager.getAllClaimsForSale();
        if (allForSale.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are currently no claims listed for sale.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Claims Currently For Sale ===");
        for (Map.Entry<String, Double> entry : allForSale.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length != 2) continue;
            UUID sellerUUID = UUID.fromString(parts[0]);
            String name = parts[1];
            int chunksCount = chunkManager.getChunksByName(sellerUUID, name).size();
            String sellerName = Bukkit.getOfflinePlayer(sellerUUID).getName();
            if (sellerName == null) sellerName = "Unknown";
            String priceStr = economyManager.getProvider().format(entry.getValue());

            player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.AQUA + name +
                    ChatColor.GRAY + " (" + chunksCount + " chunks) by " +
                    ChatColor.WHITE + sellerName + ChatColor.GRAY + " - " +
                    ChatColor.GOLD + priceStr + ChatColor.DARK_GRAY + " [/" + label + " buy " + name + "]");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = List.of("buy", "sell", "cancel", "list", "help");
            List<String> res = new ArrayList<>();
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) res.add(s);
            }
            return res;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("sell")) {
                List<String> claims = chunkManager.getPlayerClaimNames(player.getUniqueId());
                List<String> res = new ArrayList<>();
                for (String c : claims) {
                    if (c.toLowerCase().startsWith(args[1].toLowerCase())) res.add(c);
                }
                return res;
            }
            if (sub.equals("cancel")) {
                List<String> res = new ArrayList<>();
                for (String c : chunkManager.getPlayerClaimNames(player.getUniqueId())) {
                    if (chunkManager.isClaimForSale(player.getUniqueId(), c) && c.toLowerCase().startsWith(args[1].toLowerCase())) {
                        res.add(c);
                    }
                }
                return res;
            }
            if (sub.equals("buy")) {
                List<String> res = new ArrayList<>();
                for (Map.Entry<String, Double> entry : chunkManager.getAllClaimsForSale().entrySet()) {
                    String[] parts = entry.getKey().split(":", 2);
                    if (parts.length == 2 && !parts[0].equals(player.getUniqueId().toString())) {
                        if (parts[1].toLowerCase().startsWith(args[1].toLowerCase())) {
                            res.add(parts[1]);
                        }
                    }
                }
                return res;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            return List.of("100", "500", "1000", "cancel");
        }

        return Collections.emptyList();
    }
}
