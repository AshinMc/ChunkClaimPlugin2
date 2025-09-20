package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public UnclaimChunkCommand(ChunkManager chunkManager, MessageManager messages) {
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

        if (chunkManager.unclaimChunk(player, chunk)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-success"));
            chunkManager.saveData();
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
        }

        return true;
    }
}