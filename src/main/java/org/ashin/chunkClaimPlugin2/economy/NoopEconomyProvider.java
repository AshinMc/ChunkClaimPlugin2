package org.ashin.chunkClaimPlugin2.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Fallback provider when economy is disabled or unconfigured.
 * Transactions always report success with 0 cost.
 */
public class NoopEconomyProvider implements EconomyProvider {

    @Override
    public String getName() {
        return "None";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public boolean has(Player player, double amount) {
        return true;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return true;
    }
}
