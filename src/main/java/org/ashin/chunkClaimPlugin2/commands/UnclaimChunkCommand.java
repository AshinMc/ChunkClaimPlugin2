package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UnclaimChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;

    public UnclaimChunkCommand(JavaPlugin plugin, ChunkManager chunkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        if (chunkManager.unclaimChunk(player, chunk)) {
            player.sendMessage(ChatColor.GREEN + "You have successfully unclaimed this chunk!");
            chunkManager.saveData();
        } else {
            player.sendMessage(ChatColor.RED + "You don't own this chunk or it's not claimed!");
        }

        return true;
    }
}