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
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public UnclaimChunkCommand(org.bukkit.plugin.java.JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
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

        String claimName;
        if (args.length > 0) {
            claimName = String.join(" ", args);
        } else {
            // No args: use the claim name of the chunk the player is standing on
            Chunk chunk = player.getLocation().getChunk();
            java.util.UUID owner = chunkManager.getChunkOwner(chunk);
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

        int count = chunkManager.unclaimByName(player.getUniqueId(), claimName);
        if (count > 0) {
            chunkManager.saveData();
            if (messages.isMessageEnabled("unclaim-success")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-unclaim-name-success",
                        "name", claimName, "count", String.valueOf(count)));
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
