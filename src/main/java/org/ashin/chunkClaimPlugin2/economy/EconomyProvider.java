package org.ashin.chunkClaimPlugin2.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Common abstraction for economy providers (Vault, physical Item economy, or disabled/noop).
 */
public interface EconomyProvider {
    /** Name of the provider (e.g. "Vault (Essentials)", "Physical Item (GOLD_INGOT)"). */
    String getName();

    /** Whether this provider is currently available and ready to process transactions. */
    boolean isAvailable();

    /** Formats a numeric price into a human-readable string (e.g. "$150.00" or "10 Gold"). */
    String format(double amount);

    /** Checks if a player has enough funds. */
    boolean has(Player player, double amount);

    /** Withdraws funds from an online player. Returns true if successful. */
    boolean withdraw(Player player, double amount);

    /** Deposits funds to an online or offline player. Returns true if successful. */
    boolean deposit(OfflinePlayer player, double amount);
}
