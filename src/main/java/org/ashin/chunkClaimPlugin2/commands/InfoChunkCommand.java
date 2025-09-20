package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID; // Added missing import

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
        List<ChunkData> chunks = chunkManager.getPlayerChunks(playerId);

        if (chunks.isEmpty()) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "no-claims"));
            return true;
        }

        player.sendMessage(messages.getFor(player.getUniqueId(), "claims-header"));
        int count = 1;
        for (ChunkData chunk : chunks) {
            // Calculate block coordinates from chunk coordinates
            int blockX = chunk.getX() * 16;
            int blockZ = chunk.getZ() * 16;

        player.sendMessage(messages.getFor(player.getUniqueId(), "claim-entry",
            "index", String.valueOf(count),
            "world", chunk.getWorld(),
            "x", String.valueOf(blockX),
            "z", String.valueOf(blockZ))
        );
            count++;
        }

        return true;
    }
}