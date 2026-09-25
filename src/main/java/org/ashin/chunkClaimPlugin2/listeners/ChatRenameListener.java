package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.ChatColor;
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
    private final EconomyManager economyManager;

    public ChatRenameListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
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

            // Schedule renaming sync safely
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (chunkManager.renameClaim(player.getUniqueId(), oldName, newName)) {
                    chunkManager.saveData();
                    player.sendMessage(messages.getFor(player.getUniqueId(), "rename-success", "name", newName));
                } else {
                    player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-name-exists", "name", newName));
                }
            });
            return;
        }

        if (player.hasMetadata("ccp_selling_claim")) {
            event.setCancelled(true);
            String claimName = player.getMetadata("ccp_selling_claim").get(0).asString();
            String input = event.getMessage().trim();
            player.removeMetadata("ccp_selling_claim", plugin);

            if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("off") || input.equals("0")) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
                    chunkManager.saveData();
                    player.sendMessage(messages.getFor(player.getUniqueId(), "claim-sale-cancelled", "name", claimName));
                });
                return;
            }

            try {
                double price = Double.parseDouble(input);
                if (price <= 0.0) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        chunkManager.setClaimPrice(player.getUniqueId(), claimName, null);
                        chunkManager.saveData();
                        player.sendMessage(messages.getFor(player.getUniqueId(), "claim-sale-cancelled", "name", claimName));
                    });
                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    chunkManager.setClaimPrice(player.getUniqueId(), claimName, price);
                    chunkManager.saveData();
                    String formatted = economyManager.getProvider().format(price);
                    player.sendMessage(messages.getFor(player.getUniqueId(), "claim-listed-for-sale",
                            "name", claimName, "price", formatted));
                });
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number format for price.");
            }
        }
    }
}
