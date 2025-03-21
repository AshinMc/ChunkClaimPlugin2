package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class CheckChunkCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;

    public CheckChunkCommand(JavaPlugin plugin, ChunkManager chunkManager) {
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

        UUID ownerUUID = chunkManager.getChunkOwner(chunk);

        if (ownerUUID == null) {
            player.sendMessage(ChatColor.YELLOW + "This chunk is not claimed by anyone.");
        } else {
            Player owner = Bukkit.getPlayer(ownerUUID);
            String ownerName = owner != null ? owner.getName() : Bukkit.getOfflinePlayer(ownerUUID).getName();

            if (ownerName == null) {
                ownerName = ownerUUID.toString();
            }

            if (ownerUUID.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "This chunk is claimed by you.");
            } else {
                player.sendMessage(ChatColor.RED + "This chunk is claimed by " + ownerName + ".");
            }
        }

        return true;
    }
}