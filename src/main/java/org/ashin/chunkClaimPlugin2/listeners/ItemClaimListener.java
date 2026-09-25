package org.ashin.chunkClaimPlugin2.listeners;

import org.ashin.chunkClaimPlugin2.economy.EconomyManager;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ItemClaimListener implements Listener {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;
    private final EconomyManager economyManager;

    public ItemClaimListener(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages, EconomyManager economyManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        if (!player.hasPermission("ccp.claim.item") && !player.hasPermission("ccp.claim")) {
            return;
        }

        String materialName = plugin.getConfig().getString("claim-item", "WOODEN_SHOVEL");
        if (materialName.equalsIgnoreCase("NONE")) {
            return; // Item claiming is disabled
        }

        Material configuredParam = Material.matchMaterial(materialName);
        if (configuredParam == null || event.getItem() == null || event.getItem().getType() != configuredParam) {
            return;
        }

        Chunk chunk = player.getLocation().getChunk();

        // Prevent spam clicking
        if (player.hasMetadata("ccp_item_claim_cooldown")) {
            long lastClaim = player.getMetadata("ccp_item_claim_cooldown").get(0).asLong();
            if (System.currentTimeMillis() - lastClaim < 1000) {
                return;
            }
        }
        player.setMetadata("ccp_item_claim_cooldown", new org.bukkit.metadata.FixedMetadataValue(plugin, System.currentTimeMillis()));

        // Generate default name: "Claim #N"
        int next = chunkManager.getPlayerClaimNames(player.getUniqueId()).size() + 1;
        String claimName = "Claim #" + next;

        // Check if the chunk is already claimed by someone
        UUID existingOwner = chunkManager.getChunkOwner(chunk);
        if (existingOwner != null) {
            if (existingOwner.equals(player.getUniqueId())) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-self"));
            } else {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-already-claimed-other"));
            }
            return;
        }

        // Check max claims limit
        int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        int maxClaims = chunkManager.getPlayerLimit(player.getUniqueId(), defaultMax);
        int currentCount = chunkManager.getPlayerChunkCount(player.getUniqueId());
        if (maxClaims > 0 && currentCount >= maxClaims) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "max-claims-reached",
                    "max", String.valueOf(maxClaims)));
            return;
        }

        // Check WorldGuard compatibility separately
        if (!chunkManager.worldGuardHandler.canClaimChunk(chunk, player)) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "worldguard-deny"));
            return;
        }

        // Check economy cost
        double cost = economyManager.getClaimCost(currentCount);
        if (cost > 0.0) {
            if (!economyManager.getProvider().has(player, cost)) {
                String formatted = economyManager.getProvider().format(cost);
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-insufficient-funds", "cost", formatted));
                return;
            }
        }

        boolean paid = false;
        if (cost > 0.0) {
            paid = economyManager.getProvider().withdraw(player, cost);
            if (!paid) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-payment-failed"));
                return;
            }
        }

        // Actually claim the chunk with the name
        if (chunkManager.claimChunk(player, chunk, claimName)) {
            chunkManager.saveData();
            if (messages.isMessageEnabled("claim-success")) {
                player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-success", "name", claimName));
            }
            if (cost > 0.0) {
                String formatted = economyManager.getProvider().format(cost);
                player.sendMessage(messages.getFor(player.getUniqueId(), "claim-cost-notice", "cost", formatted));
            }
        } else {
            if (paid) {
                economyManager.getProvider().deposit(player, cost);
            }
            player.sendMessage(messages.getFor(player.getUniqueId(), "chunk-claim-fail"));
        }
    }
}
