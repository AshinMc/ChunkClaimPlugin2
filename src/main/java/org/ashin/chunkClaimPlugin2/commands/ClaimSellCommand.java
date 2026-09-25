package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClaimSellCommand implements CommandExecutor, TabCompleter {
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public ClaimSellCommand(ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
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

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <claimName> <price|cancel>");
            return true;
        }

        String claimName = args[0];
        if (!chunkManager.hasClaimName(player.getUniqueId(), claimName)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "expand-not-found", "name", claimName));
            return true;
        }

        String priceArg = args[1];
        if (priceArg.equalsIgnoreCase("cancel") || priceArg.equalsIgnoreCase("off") || priceArg.equals("0")) {
            chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
            chunkManager.saveData();
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-sale-cancelled", "name", claimName));
            return true;
        }

        try {
            double price = Double.parseDouble(priceArg);
            if (price <= 0.0) {
                player.sendMessage(ChatColor.RED + "Price must be greater than zero, or type 'cancel' to remove the listing.");
                return true;
            }

            chunkManager.setClaimPrice(player.getUniqueId(), claimName, price);
            chunkManager.saveData();
            String formattedPrice = economyManager.getProvider().format(price);
            player.sendMessage(messages.getFor(player.getUniqueId(), "claim-listed-for-sale",
                    "name", claimName, "price", formattedPrice));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number format for price.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> claims = chunkManager.getPlayerClaimNames(player.getUniqueId());
            List<String> res = new ArrayList<>();
            for (String c : claims) {
                if (c.toLowerCase().startsWith(args[0].toLowerCase())) res.add(c);
            }
            return res;
        }

        if (args.length == 2) {
            return List.of("100", "500", "1000", "cancel");
        }

        return Collections.emptyList();
    }
}
