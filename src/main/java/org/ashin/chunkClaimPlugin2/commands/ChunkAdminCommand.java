package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.gui.AdminSettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkAdminCommand implements CommandExecutor {
    private final AdminSettingsGUI gui;
    private final MessageManager messages;

    public ChunkAdminCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.gui = new AdminSettingsGUI(plugin, chunkManager, messages);
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("chunkclaim.admin")) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "admin-no-permission"));
            return true;
        }

        gui.openHome(player);
        return true;
    }
}
