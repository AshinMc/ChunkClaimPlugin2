package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.gui.AdminSettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkAdminCommand implements CommandExecutor {
    private final AdminSettingsGUI gui;
    private final MessageManager messages;
    private final ChunkManager chunkManager;

    public ChunkAdminCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.gui = new AdminSettingsGUI(plugin, chunkManager, messages);
        this.messages = messages;
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chunkclaim.admin")) {
            if (sender instanceof Player) {
                sender.sendMessage(messages.getFor(((Player) sender).getUniqueId(), "admin-no-permission"));
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setlimit")) {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            
            if (args.length == 3) {
                try {
                    int limit = Integer.parseInt(args[2]);
                    chunkManager.setPlayerLimit(target.getUniqueId(), limit);
                    chunkManager.saveData();
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s chunk limit to " + limit + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number format for limit.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /chunkadmin setlimit <player> <amount>");
            }
            return true;
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("removelimit")) {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            
            chunkManager.setPlayerLimit(target.getUniqueId(), null);
            chunkManager.saveData();
            sender.sendMessage(ChatColor.GREEN + "Removed individual chunk limit for " + target.getName() + ".");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to open the GUI. Use /chunkadmin setlimit <player> <amount> to set limits.");
            return true;
        }
        Player player = (Player) sender;

        gui.openHome(player);
        return true;
    }
}

