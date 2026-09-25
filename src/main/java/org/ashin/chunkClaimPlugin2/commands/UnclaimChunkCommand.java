package org.ashin.chunkClaimPlugin2.commands;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class UnclaimChunkCommand implements CommandExecutor {
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final JavaPlugin plugin;
    private final EconomyManager economyManager;

    public UnclaimChunkCommand(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        String claimName;
        if (args.length > 0) {
            claimName = String.join(" ", args);
        } else {
            // No args: use the claim name of the chunk the player is standing on
            Chunk chunk = player.getLocation().getChunk();
            UUID owner = chunkManager.getChunkOwner(chunk);
            if (owner == null || !owner.equals(player.getUniqueId())) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
                return true;
            }
            claimName = chunkManager.getChunkClaimName(chunk);
            if (claimName == null) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-fail"));
                return true;
            }
        }

        int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
        int count = chunkManager.unclaimByName(player.getUniqueId(), claimName);
        if (count > 0) {
            chunkManager.saveData();
            if (messages.isMessageEnabled("unclaim-success")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-name-success",
                        "name", claimName, "count", String.valueOf(count)));
            }

            // Refund if economy enabled
            double refund = economyManager.getUnclaimRefund(currentCount, count);
            if (refund > 0.0) {
                economyManager.getProvider().deposit(player, refund);
                String formatted = economyManager.getProvider().format(refund);
                player.sendMessage(messages.getFor(player.getUniqueId(), "unclaim-refund-notice",
                        "amount", formatted, "count", String.valueOf(count)));
            }

            // Run success commands
            java.util.List<String> cmds = plugin.getConfig().getStringList("event-commands.unclaim-success");
            for (String cmd : cmds) {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                        cmd.replace("%player%", player.getName()).replace("%claim%", claimName));
            }
        } else {
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-name-fail", "name", claimName));
        }

        return true;
    }
}
