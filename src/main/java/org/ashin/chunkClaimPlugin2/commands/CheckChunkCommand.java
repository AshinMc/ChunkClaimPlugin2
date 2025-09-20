package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CheckChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;

    public CheckChunkCommand(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        UUID ownerUUID = chunkManager.getChunkOwner(chunk);

        if (ownerUUID == null) {
            player.sendMessage(Component.text("This chunk is not claimed by anyone.").color(NamedTextColor.YELLOW));
        } else {
            Player owner = Bukkit.getPlayer(ownerUUID);
            String ownerName = owner != null ? owner.getName() : Bukkit.getOfflinePlayer(ownerUUID).getName();

            if (ownerName == null) {
                ownerName = ownerUUID.toString();
            }

            if (ownerUUID.equals(player.getUniqueId())) {
                player.sendMessage(Component.text("This chunk is claimed by you.").color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("This chunk is claimed by " + ownerName + ".").color(NamedTextColor.RED));
            }
        }

        return true;
    }
}