package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatRenameListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public ChatRenameListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("ccp_renaming_claim")) {
            event.setCancelled(true);
            String oldName = player.getMetadata("ccp_renaming_claim").get(0).asString();
            String newName = event.getMessage().trim();

            player.removeMetadata("ccp_renaming_claim", plugin);

            if (newName.equalsIgnoreCase("cancel")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "rename-cancel"));
                return;
            }

            // Schedule renaming sync safely since we talk with maps
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (chunkManager.renameClaim(player.getUniqueId(), oldName, newName)) {
                    chunkManager.saveData();
                    player.sendMessage(messages.getFor(player.getUniqueId(), "rename-success", "name", newName));
                } else {
                    player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-name-exists", "name", newName));
                }
            });
        }
    }
}
