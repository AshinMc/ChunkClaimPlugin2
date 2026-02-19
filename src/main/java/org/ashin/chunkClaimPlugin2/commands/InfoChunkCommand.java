package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class InfoChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public InfoChunkCommand(ChunkManager chunkManager, MessageManager messages) {
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
        UUID playerId = player.getUniqueId();
        List<String> claimNames = chunkManager.getPlayerClaimNames(playerId);

        if (claimNames.isEmpty()) {
            player.sendMessage(messages.getFor(playerId, "no-claims"));
            return true;
        }

        player.sendMessage(messages.getFor(playerId, "claims-header"));
        int index = 1;
        for (String name : claimNames) {
            List<ChunkData> chunks = chunkManager.getChunksByName(playerId, name);
            // Show group header
            player.sendMessage(messages.getFor(playerId, "claim-group-entry",
                    "index", String.valueOf(index),
                    "name", name,
                    "count", String.valueOf(chunks.size())));
            // Show each chunk in the group
            for (ChunkData cd : chunks) {
                int blockX = cd.getX() * 16;
                int blockZ = cd.getZ() * 16;
                player.sendMessage(messages.getFor(playerId, "claim-chunk-entry",
                        "world", cd.getWorld(),
                        "x", String.valueOf(blockX),
                        "z", String.valueOf(blockZ)));
            }
            index++;
        }

        return true;
    }
}