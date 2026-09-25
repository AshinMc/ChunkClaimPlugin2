package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.gui.SettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkSettingsCommand implements CommandExecutor {
    private final SettingsGUI gui;
    private final MessageManager messages;

    public ChunkSettingsCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.gui = new SettingsGUI(plugin, chunkManager, messages, economyManager);
        this.messages = messages;
    }

    public ChunkSettingsCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this(plugin, chunkManager, messages, null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }
        gui.openHome(player);
        return true;
    }
}
