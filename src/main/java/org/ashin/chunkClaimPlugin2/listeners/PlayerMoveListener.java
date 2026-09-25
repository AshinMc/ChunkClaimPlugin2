package org.ashin.chunkClaimPlugin2.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public PlayerMoveListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return; // completely within same chunk
        }

        Player player = event.getPlayer();
        UUID fromOwner = chunkManager.getChunkOwner(fromChunk);
        String fromName = fromOwner != null ? chunkManager.getChunkClaimName(fromChunk) : null;

        UUID toOwner = chunkManager.getChunkOwner(toChunk);
        String toName = toOwner != null ? chunkManager.getChunkClaimName(toChunk) : null;

        // Check if we entered a different claim group
        boolean sameOwner = (fromOwner != null && fromOwner.equals(toOwner));
        boolean sameName = (fromName != null && fromName.equals(toName));

        // ── Leaving a claim into the wilderness ──
        if (fromOwner != null && toOwner == null) {
            boolean showWilderness = plugin.getConfig().getBoolean("show-wilderness-greeting", true);
            if (showWilderness) {
                String wildernessMsg = messages.getFor(player.getUniqueId(), "wilderness-actionbar");
                if (wildernessMsg == null || wildernessMsg.equals("wilderness-actionbar")) {
                    wildernessMsg = "§2~ Wilderness ~";
                }
                String greetingDisplay = plugin.getConfig().getString("greeting-display", "ACTION_BAR").toUpperCase();
                sendGreeting(player, greetingDisplay, wildernessMsg, "", wildernessMsg);
            }
            return;
        }

        // ── Entering a new claim group ──
        if (toOwner != null && (!sameOwner || !sameName)) {
            // Check if greeting is enabled for this claim
            if (!chunkManager.getClaimFlag(toOwner, toName, ChunkManager.FLAG_GREETING_TITLE)) {
                return;
            }

            String ownerName = Bukkit.getOfflinePlayer(toOwner).getName();
            if (ownerName == null) ownerName = "Unknown";

            boolean forSale = chunkManager.isClaimForSale(toOwner, toName);
            Double price = forSale ? chunkManager.getClaimPrice(toOwner, toName) : null;
            String priceStr = price != null ? economyManager.getProvider().format(price) : "";

            String title = toName;
            String subtitle;
            String actionBar;

            if (forSale) {
                subtitle = messages.getFor(player.getUniqueId(), "greeting-subtitle-sale",
                        "player", ownerName, "price", priceStr);
                if (subtitle.equals("greeting-subtitle-sale")) {
                    subtitle = "§7Owned by " + ownerName + " §8| §aFor Sale: §6" + priceStr + " §7(/claimtrade buy)";
                }

                actionBar = messages.getFor(player.getUniqueId(), "greeting-actionbar-sale",
                        "claim", toName, "player", ownerName, "price", priceStr);
                if (actionBar.equals("greeting-actionbar-sale")) {
                    actionBar = "§e" + toName + " §8| §7Owner: §a" + ownerName + " §8| §aFor Sale: §6" + priceStr + " §7(/claimtrade buy)";
                }
            } else {
                subtitle = messages.getFor(player.getUniqueId(), "greeting-subtitle", "player", ownerName);
                if (subtitle.equals("greeting-subtitle")) {
                    subtitle = "Owned by " + ownerName;
                }

                actionBar = messages.getFor(player.getUniqueId(), "greeting-actionbar",
                        "claim", toName, "player", ownerName);
                if (actionBar.equals("greeting-actionbar")) {
                    actionBar = "§e" + toName + " §8| §7Owner: §a" + ownerName;
                }
            }

            String greetingDisplay = plugin.getConfig().getString("greeting-display", "ACTION_BAR").toUpperCase();
            sendGreeting(player, greetingDisplay, title, subtitle, actionBar);
        }
    }

    private void sendGreeting(Player player, String greetingDisplay, String title, String subtitle, String message) {
        switch (greetingDisplay) {
            case "ACTION_BAR" -> sendActionBar(player, message);
            case "TITLE" -> {
                int fadeIn = plugin.getConfig().getInt("title-duration.fade-in", 10);
                int stay = plugin.getConfig().getInt("title-duration.stay", 60);
                int fadeOut = plugin.getConfig().getInt("title-duration.fade-out", 15);
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
            case "SUBTITLE" -> {
                int fadeIn = plugin.getConfig().getInt("title-duration.fade-in", 10);
                int stay = plugin.getConfig().getInt("title-duration.stay", 60);
                int fadeOut = plugin.getConfig().getInt("title-duration.fade-out", 15);
                String sub = subtitle.isEmpty() ? title : subtitle;
                player.sendTitle("", sub, fadeIn, stay, fadeOut);
            }
            case "CHAT" -> player.sendMessage(message);
            case "NONE" -> {}
            default -> sendActionBar(player, message);
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        } catch (Throwable t) {
            player.sendMessage(message);
        }
    }
}
