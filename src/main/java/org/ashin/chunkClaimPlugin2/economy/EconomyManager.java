package org.ashin.chunkClaimPlugin2.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages economy integrations (Vault, Physical Item, or None), pricing logic, and offline payout delivery.
 */
public class EconomyManager {
    private final JavaPlugin plugin;
    private EconomyProvider provider;
    private boolean enabled;
    private int freeClaims;
    private double costPerChunk;
    private double costIncreasePerChunk;
    private double refundPercentage;

    private final File pendingFile;
    private YamlConfiguration pendingConfig;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pendingFile = new File(plugin.getDataFolder(), "pending_economy.yml");
        loadPendingConfig();
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("economy.enabled", false);
        this.freeClaims = config.getInt("economy.free-claims", 0);
        this.costPerChunk = config.getDouble("economy.cost-per-chunk", 0.0);
        this.costIncreasePerChunk = config.getDouble("economy.cost-increase-per-chunk", 0.0);
        this.refundPercentage = config.getDouble("economy.unclaim-refund-percentage", 0.0);

        if (!enabled) {
            this.provider = new NoopEconomyProvider();
            return;
        }

        String mode = config.getString("economy.mode", "AUTO").toUpperCase();

        if ("VAULT".equals(mode) || "AUTO".equals(mode)) {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null && rsp.getProvider() != null) {
                    this.provider = new VaultEconomyProvider(rsp.getProvider());
                    plugin.getLogger().info("Economy hooked into Vault provider: " + rsp.getProvider().getName());
                    return;
                }
            }
            if ("VAULT".equals(mode)) {
                plugin.getLogger().warning("Vault economy mode requested, but no Vault provider was found. Disabling economy transactions until Vault is ready.");
                this.provider = new NoopEconomyProvider();
                return;
            }
        }

        // ITEM mode (or AUTO fallback when Vault isn't present)
        String matName = config.getString("economy.item.material", "GOLD_INGOT");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.GOLD_INGOT;

        String displayName = config.getString("economy.item.display-name", "Gold");
        boolean convertBlocks = config.getBoolean("economy.item.convert-blocks", true);

        this.provider = new ItemEconomyProvider(mat, displayName, convertBlocks, (offlinePlayer, amount) -> {
            addPendingPayment(offlinePlayer.getUniqueId(), amount, "Claim Sale");
        });
        plugin.getLogger().info("Using Physical Item economy with " + mat.name() + " (" + displayName + ")");
    }

    public boolean isEconomyEnabled() {
        return enabled && provider != null && provider.isAvailable();
    }

    public EconomyProvider getProvider() {
        return provider != null ? provider : new NoopEconomyProvider();
    }

    /**
     * Calculates the cost to claim or expand a chunk based on player's current count of owned chunks.
     */
    public double getClaimCost(int currentOwnedChunks) {
        if (!isEconomyEnabled()) return 0.0;
        if (currentOwnedChunks < freeClaims) return 0.0;
        int paidCount = currentOwnedChunks - freeClaims;
        double cost = costPerChunk + (paidCount * costIncreasePerChunk);
        return Math.max(0.0, cost);
    }

    /**
     * Calculates the refund amount for unclaiming a number of chunks.
     */
    public double getUnclaimRefund(int currentOwnedChunks, int chunksToUnclaim) {
        if (!isEconomyEnabled() || refundPercentage <= 0.0 || chunksToUnclaim <= 0) return 0.0;
        double totalRefund = 0.0;
        for (int i = 0; i < chunksToUnclaim; i++) {
            int chunkIndex = currentOwnedChunks - 1 - i;
            if (chunkIndex >= freeClaims) {
                int paidIndex = chunkIndex - freeClaims;
                double chunkCost = costPerChunk + (paidIndex * costIncreasePerChunk);
                totalRefund += chunkCost * (refundPercentage / 100.0);
            }
        }
        return Math.max(0.0, totalRefund);
    }

    // ── Pending payment storage for offline sales ──

    private void loadPendingConfig() {
        if (!pendingFile.exists()) {
            try {
                pendingFile.createNewFile();
            } catch (IOException ignored) {}
        }
        this.pendingConfig = YamlConfiguration.loadConfiguration(pendingFile);
    }

    public void addPendingPayment(UUID playerUUID, double amount, String source) {
        double current = pendingConfig.getDouble("pending." + playerUUID.toString() + ".amount", 0.0);
        pendingConfig.set("pending." + playerUUID.toString() + ".amount", current + amount);
        pendingConfig.set("pending." + playerUUID.toString() + ".source", source);
        savePendingConfig();
    }

    public void deliverPendingPayments(Player player) {
        if (player == null) return;
        String key = "pending." + player.getUniqueId().toString();
        if (!pendingConfig.contains(key + ".amount")) return;

        double amount = pendingConfig.getDouble(key + ".amount", 0.0);
        if (amount > 0) {
            if (provider instanceof ItemEconomyProvider itemProvider) {
                itemProvider.giveItems(player, (int) Math.round(amount));
            } else if (provider instanceof VaultEconomyProvider vaultProvider) {
                vaultProvider.deposit(player, amount);
            }
            player.sendMessage(org.bukkit.ChatColor.GREEN + "[CCP] You received " + org.bukkit.ChatColor.YELLOW +
                    provider.format(amount) + org.bukkit.ChatColor.GREEN + " from pending claim sales while you were offline!");
        }

        pendingConfig.set(key, null);
        savePendingConfig();
    }

    private void savePendingConfig() {
        try {
            pendingConfig.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save pending_economy.yml: " + e.getMessage());
        }
    }
}
