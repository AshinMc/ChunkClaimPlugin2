package org.ashin.chunkClaimPlugin2.economy;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * Economy provider based on physical Minecraft items (e.g. Gold Ingots, Diamonds, Emeralds).
 * Requires NO external economy plugins. Supports block conversion (e.g. 1 Gold Block = 9 Gold Ingots).
 */
public class ItemEconomyProvider implements EconomyProvider {
    private final Material baseMaterial;
    private final Material blockMaterial;
    private final String displayName;
    private final boolean convertBlocks;
    private final BiConsumer<OfflinePlayer, Double> offlineDepositHandler;

    public ItemEconomyProvider(Material baseMaterial, String displayName, boolean convertBlocks,
                               BiConsumer<OfflinePlayer, Double> offlineDepositHandler) {
        this.baseMaterial = baseMaterial != null ? baseMaterial : Material.GOLD_INGOT;
        this.blockMaterial = findBlockEquivalent(this.baseMaterial);
        this.displayName = (displayName != null && !displayName.isEmpty()) ? displayName : formatDefaultName(this.baseMaterial);
        this.convertBlocks = convertBlocks;
        this.offlineDepositHandler = offlineDepositHandler;
    }

    private Material findBlockEquivalent(Material base) {
        return switch (base) {
            case GOLD_INGOT -> Material.GOLD_BLOCK;
            case IRON_INGOT -> Material.IRON_BLOCK;
            case COPPER_INGOT -> Material.COPPER_BLOCK;
            case DIAMOND -> Material.DIAMOND_BLOCK;
            case EMERALD -> Material.EMERALD_BLOCK;
            case NETHERITE_INGOT -> Material.NETHERITE_BLOCK;
            case REDSTONE -> Material.REDSTONE_BLOCK;
            case LAPIS_LAZULI -> Material.LAPIS_BLOCK;
            default -> null;
        };
    }

    private String formatDefaultName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        // Capitalize words
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public String getName() {
        return "Physical Item (" + baseMaterial.name() + ")";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String format(double amount) {
        int count = (int) Math.round(amount);
        return count + " " + displayName + (count == 1 ? "" : "s");
    }

    public int countItems(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() == baseMaterial) {
                total += item.getAmount();
            } else if (convertBlocks && blockMaterial != null && item.getType() == blockMaterial) {
                total += item.getAmount() * 9;
            }
        }
        return total;
    }

    @Override
    public boolean has(Player player, double amount) {
        int required = (int) Math.ceil(amount);
        if (required <= 0) return true;
        return countItems(player) >= required;
    }

    @Override
    public synchronized boolean withdraw(Player player, double amount) {
        int needed = (int) Math.ceil(amount);
        if (needed <= 0) return true;
        if (!has(player, amount)) return false;

        // First deduct base items
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != baseMaterial) continue;

            int take = Math.min(item.getAmount(), needed);
            item.setAmount(item.getAmount() - take);
            needed -= take;
            if (item.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            }
            if (needed <= 0) break;
        }

        // If still needed and convertBlocks is true, break down blocks
        if (needed > 0 && convertBlocks && blockMaterial != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() != blockMaterial) continue;

                while (item.getAmount() > 0 && needed > 0) {
                    item.setAmount(item.getAmount() - 1);
                    int provided = 9;
                    int take = Math.min(provided, needed);
                    needed -= take;
                    int change = provided - take;
                    if (change > 0) {
                        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(baseMaterial, change));
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                if (needed <= 0) break;
            }
        }

        player.updateInventory();
        return true;
    }

    @Override
    public synchronized boolean deposit(OfflinePlayer player, double amount) {
        int toGive = (int) Math.round(amount);
        if (toGive <= 0) return true;

        if (player.isOnline() && player.getPlayer() != null) {
            Player online = player.getPlayer();
            giveItems(online, toGive);
            return true;
        }

        // Offline: pass to offline deposit handler for pending delivery on join
        if (offlineDepositHandler != null) {
            offlineDepositHandler.accept(player, (double) toGive);
            return true;
        }
        return false;
    }

    public void giveItems(Player player, int total) {
        if (total <= 0) return;

        // Compact into blocks if convertBlocks is enabled
        if (convertBlocks && blockMaterial != null && total >= 9) {
            int blocks = total / 9;
            total = total % 9;
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(blockMaterial, blocks));
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        if (total > 0) {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(baseMaterial, total));
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        player.updateInventory();
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }
}
