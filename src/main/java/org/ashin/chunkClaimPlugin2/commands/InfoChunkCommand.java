package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID; // Added missing import

public class InfoChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;

    public InfoChunkCommand(JavaPlugin plugin, ChunkManager chunkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        List<ChunkData> chunks = chunkManager.getPlayerChunks(playerId);

        if (chunks.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't have any claimed chunks.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Your claimed chunks:");
        int count = 1;
        for (ChunkData chunk : chunks) {
            // Calculate block coordinates from chunk coordinates
            int blockX = chunk.getX() * 16;
            int blockZ = chunk.getZ() * 16;

            player.sendMessage(ChatColor.GOLD + "" + count + ". " +
                    ChatColor.WHITE + "World: " + chunk.getWorld() +
                    ChatColor.WHITE + " Block coordinates: X: " + blockX + ", Z: " + blockZ);
            count++;
        }

        return true;
    }
}