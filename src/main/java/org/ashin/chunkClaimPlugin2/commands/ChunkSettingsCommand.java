package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.gui.SettingsGUI;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChunkSettingsCommand implements CommandExecutor {
    private final SettingsGUI gui;
    private final MessageManager messages;

    public ChunkSettingsCommand(ChunkManager chunkManager, MessageManager messages) {
        this.gui = new SettingsGUI(chunkManager, messages);
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }
        Player player = (Player) sender;
        gui.openHome(player);
        return true;
    }
}
