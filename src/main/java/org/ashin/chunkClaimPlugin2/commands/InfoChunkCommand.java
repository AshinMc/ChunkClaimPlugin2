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

        final int groupsPerPage = 6;
        int totalPages = Math.max(1, (int) Math.ceil((double) claimNames.size() / groupsPerPage));
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(org.bukkit.ChatColor.RED + "Invalid page number: " + args[0]);
                return true;
            }
        }
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * groupsPerPage;
        int end = Math.min(start + groupsPerPage, claimNames.size());

        player.sendMessage(messages.getFor(playerId, "claims-header")
                + org.bukkit.ChatColor.GRAY + " (" + page + "/" + totalPages + ")");
        int index = start + 1;
        for (int i = start; i < end; i++) {
            String name = claimNames.get(i);
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

        if (page < totalPages) {
            player.sendMessage(org.bukkit.ChatColor.GRAY + "Use /" + label + " " + (page + 1) + " for the next page.");
        }

        return true;
    }
}