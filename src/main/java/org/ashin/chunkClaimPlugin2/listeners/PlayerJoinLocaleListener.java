package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinLocaleListener implements Listener {
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public PlayerJoinLocaleListener(MessageManager messages, EconomyManager economyManager) {
        this.messages = messages;
        this.economyManager = economyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        // If player has no stored locale, initialize with server default
        if (!messages.hasPlayerLocale(uuid)) {
            messages.setPlayerLocale(uuid, messages.getServerDefaultLocale());
        }

        // Deliver any pending claim sale earnings from when the player was offline
        if (economyManager != null && economyManager.isEconomyEnabled()) {
            economyManager.deliverPendingPayments(player);
        }
    }
}
