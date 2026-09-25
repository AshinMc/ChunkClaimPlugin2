package org.ashin.chunkClaimPlugin2.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Economy provider backed by the Vault API.
 * Automatically supports EssentialsX, CMI, TheNewEconomy, and physical gold banks like Gringotts.
 */
public class VaultEconomyProvider implements EconomyProvider {
    private final Economy economy;

    public VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getName() {
        return "Vault (" + (economy != null ? economy.getName() : "Unknown") + ")";
    }

    @Override
    public boolean isAvailable() {
        return economy != null && economy.isEnabled();
    }

    @Override
    public String format(double amount) {
        if (economy == null) return String.format("$%.2f", amount);
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.format("$%.2f", amount);
        }
    }

    @Override
    public boolean has(Player player, double amount) {
        if (economy == null || amount <= 0) return true;
        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount <= 0) return true;
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0) return true;
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }
}
