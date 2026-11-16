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

    private volatile boolean cachedShowWilderness;
    private volatile String cachedGreetingDisplay;
    private volatile int cachedFadeIn;
    private volatile int cachedStay;
    private volatile int cachedFadeOut;

    public PlayerMoveListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
        reloadConfigCache();
    }

    /**
     * Refreshes cached greeting and title duration settings from config.yml.
     */
    public void reloadConfigCache() {
        var cfg = plugin.getConfig();
        this.cachedShowWilderness = cfg.getBoolean("show-wilderness-greeting", true);
        this.cachedGreetingDisplay = org.ashin.chunkClaimPlugin2.config.ConfigMigrator
                .getNonBlankString(cfg, "greeting-display", "ACTION_BAR").toUpperCase();
        this.cachedFadeIn = Math.max(0, cfg.getInt("title-duration.fade-in", 10));
        this.cachedStay = Math.max(0, cfg.getInt("title-duration.stay", 60));
        this.cachedFadeOut = Math.max(0, cfg.getInt("title-duration.fade-out", 15));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)
                && from.getWorld() == to.getWorld()) {
            return; // completely within same chunk
        }

        Chunk fromChunk = from.getChunk();
        Chunk toChunk = to.getChunk();

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
            if (cachedShowWilderness) {
                String wildernessMsg = messages.getFor(player.getUniqueId(), "wilderness-actionbar");
                if (wildernessMsg == null || wildernessMsg.equals("wilderness-actionbar")) {
                    wildernessMsg = "§2~ Wilderness ~";
                }
                sendGreeting(player, cachedGreetingDisplay, wildernessMsg, "", wildernessMsg);
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

            sendGreeting(player, cachedGreetingDisplay, title, subtitle, actionBar);
        }
    }

    private void sendGreeting(Player player, String greetingDisplay, String title, String subtitle, String message) {
        switch (greetingDisplay) {
            case "ACTION_BAR" -> sendActionBar(player, message);
            case "TITLE" -> player.sendTitle(title, subtitle, cachedFadeIn, cachedStay, cachedFadeOut);
            case "SUBTITLE" -> {
                String sub = subtitle.isEmpty() ? title : subtitle;
                player.sendTitle("", sub, cachedFadeIn, cachedStay, cachedFadeOut);
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
