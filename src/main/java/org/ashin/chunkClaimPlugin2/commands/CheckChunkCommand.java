package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CheckChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public CheckChunkCommand(ChunkManager chunkManager, MessageManager messages) {
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        UUID ownerUUID = chunkManager.getChunkOwner(chunk);

        if (ownerUUID == null) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unowned"));
        } else {
            Player owner = Bukkit.getPlayer(ownerUUID);
            String ownerName = owner != null ? owner.getName() : Bukkit.getOfflinePlayer(ownerUUID).getName();

            if (ownerName == null) {
                ownerName = ownerUUID.toString();
            }

            if (ownerUUID.equals(player.getUniqueId())) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-owned-self"));
            } else {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-owned-other", "player", ownerName));
            }
        }

        return true;
    }
}