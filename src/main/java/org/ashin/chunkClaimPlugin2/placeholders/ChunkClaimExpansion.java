package org.ashin.chunkClaimPlugin2.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ChunkClaimExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;

    public ChunkClaimExpansion(JavaPlugin plugin, ChunkManager chunkManager) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ccp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ashin";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.equalsIgnoreCase("total_claims")) {
            return String.valueOf(chunkManager.getTotalClaimedChunkCount());
        }

        if (offlinePlayer == null) return null;

        if (params.equalsIgnoreCase("claimed_chunks")) {
            return String.valueOf(chunkManager.getPlayerChunkCount(offlinePlayer.getUniqueId()));
        }

        if (params.equalsIgnoreCase("max_chunks")) {
            int defaultMax = plugin.getConfig().getInt("max-claims-per-player", 10);
            int limit = chunkManager.getPlayerLimit(offlinePlayer.getUniqueId(), defaultMax);
            return limit == 0 ? "Unlimited" : String.valueOf(limit);
        }

        if (offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                org.bukkit.Location loc = player.getLocation();
                if (loc.getWorld() == null) return null;
                String chunkKey = chunkManager.getChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
                UUID owner = chunkManager.getChunkOwner(chunkKey);

                if (params.equalsIgnoreCase("is_claimed")) {
                    return owner != null ? "true" : "false";
                }

                if (params.equalsIgnoreCase("chunk_owner")) {
                    if (owner == null) return "Unclaimed";
                    Player onlineOwner = Bukkit.getPlayer(owner);
                    String name = onlineOwner != null ? onlineOwner.getName() : Bukkit.getOfflinePlayer(owner).getName();
                    return name != null ? name : "Unknown";
                }

                if (params.equalsIgnoreCase("claim_name")) {
                    if (owner == null) return "None";
                    String name = chunkManager.getChunkClaimName(chunkKey);
                    return name != null ? name : "world";
                }

                if (params.equalsIgnoreCase("is_for_sale")) {
                    if (owner == null) return "false";
                    String name = chunkManager.getChunkClaimName(chunkKey);
                    return String.valueOf(name != null && chunkManager.isClaimForSale(owner, name));
                }

                if (params.equalsIgnoreCase("claim_price")) {
                    if (owner == null) return "0";
                    String name = chunkManager.getChunkClaimName(chunkKey);
                    if (name != null && chunkManager.isClaimForSale(owner, name)) {
                        Double price = chunkManager.getClaimPrice(owner, name);
                        return price != null ? String.valueOf(price) : "0";
                    }
                    return "0";
                }
            }
        }

        return null;
    }
}
