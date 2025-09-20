package org.ashin.chunkClaimPlugin2.gui;

import org.ashin.chunkClaimPlugin2.data.ChunkData;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI {
    public enum View { HOME, CLAIMS, VISUALIZE, DELETE, LANGUAGE, CLAIM_DETAILS, DELETE_CONFIRM }

    public static class SettingsHolder implements org.bukkit.inventory.InventoryHolder {
        public final View view;
        public SettingsHolder(View view) { this.view = view; }
        @Override
        public org.bukkit.inventory.Inventory getInventory() { return null; }
    }

    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public SettingsGUI(ChunkManager chunkManager, MessageManager messages) {
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    public void openHome(Player player) {
    String title = messages.getFor(player.getUniqueId(), "gui-title-home");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.HOME), 27, ChatColor.stripColor(title));
        inv.setItem(10, named(Material.GRASS_BLOCK, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-claims"))));
        inv.setItem(12, named(Material.SPYGLASS, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-visualize"))));
        inv.setItem(14, named(Material.BARRIER, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-delete"))));
        inv.setItem(16, named(Material.BOOK, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-language"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openClaims(Player player) {
        List<ChunkData> chunks = chunkManager.getPlayerChunks(player.getUniqueId());
        int size = invSizeFor(chunks.size() + 1);
    String title = messages.getFor(player.getUniqueId(), "gui-title-claims");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.CLAIMS), size, ChatColor.stripColor(title));
        int i = 0;
        for (ChunkData cd : chunks) {
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + cd.getWorld() + ChatColor.GRAY + " @ " + ChatColor.YELLOW + cd.getX() + ", " + cd.getZ());
            List<String> lore = new ArrayList<>();
            int blockX = cd.getX() * 16;
            int blockZ = cd.getZ() * 16;
            lore.add(ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-lore-block", "x", String.valueOf(blockX), "z", String.valueOf(blockZ))));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openVisualize(Player player) {
        // Same layout as claims, but clicking will trigger visualize for that single claim
        List<ChunkData> chunks = chunkManager.getPlayerChunks(player.getUniqueId());
        int size = invSizeFor(chunks.size() + 1);
    String title = messages.getFor(player.getUniqueId(), "gui-title-visualize");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.VISUALIZE), size, ChatColor.stripColor(title));
        int i = 0;
        for (ChunkData cd : chunks) {
            ItemStack item = new ItemStack(Material.SPYGLASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + cd.getWorld() + ChatColor.GRAY + " @ " + ChatColor.YELLOW + cd.getX() + ", " + cd.getZ());
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openDelete(Player player) {
        List<ChunkData> chunks = chunkManager.getPlayerChunks(player.getUniqueId());
        int size = invSizeFor(chunks.size() + 1);
    String title = messages.getFor(player.getUniqueId(), "gui-title-delete");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.DELETE), size, ChatColor.stripColor(title));
        int i = 0;
        for (ChunkData cd : chunks) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + cd.getWorld() + ChatColor.GRAY + " @ " + ChatColor.YELLOW + cd.getX() + ", " + cd.getZ());
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openLanguage(Player player) {
        List<String> locales = messages.getAvailableLocales();
        int size = invSizeFor(locales.size() + 1);
    String title = messages.getFor(player.getUniqueId(), "gui-title-language");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.LANGUAGE), size, ChatColor.stripColor(title));
        int i = 0;
        for (String loc : locales) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + loc);
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openClaimDetails(Player player, ChunkData cd) {
    String title = messages.getFor(player.getUniqueId(), "gui-title-claim-details");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.CLAIM_DETAILS), 27, ChatColor.stripColor(title));

        // Info item (paper)
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + cd.getWorld() + ChatColor.GRAY + " @ " + ChatColor.YELLOW + cd.getX() + ", " + cd.getZ());
        List<String> lore = new ArrayList<>();
        int blockX = cd.getX() * 16;
        int blockZ = cd.getZ() * 16;
        lore.add(ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-lore-block", "x", String.valueOf(blockX), "z", String.valueOf(blockZ))));
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(11, info);

        // Teleport item (ender pearl)
        inv.setItem(13, named(Material.ENDER_PEARL, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-teleport"))));

        // Back item
        inv.setItem(15, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));

        player.openInventory(inv);
        playClick(player);
    }

    public void openDeleteConfirm(Player player, ChunkData cd) {
    String title = messages.getFor(player.getUniqueId(), "gui-title-delete-confirm");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.DELETE_CONFIRM), 27, ChatColor.stripColor(title));

        // Info item in center
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.RED + cd.getWorld() + ChatColor.GRAY + " @ " + ChatColor.YELLOW + cd.getX() + ", " + cd.getZ());
        List<String> lore = new ArrayList<>();
        int blockX = cd.getX() * 16;
        int blockZ = cd.getZ() * 16;
        lore.add(ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-lore-block", "x", String.valueOf(blockX), "z", String.valueOf(blockZ))));
        lore.add(ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-lore-confirm")));
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(13, info);

        // Confirm/Cancel
        inv.setItem(11, named(Material.RED_CONCRETE, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-confirm"))));
        inv.setItem(15, named(Material.YELLOW_CONCRETE, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-cancel"))));

        player.openInventory(inv);
        playClick(player);
    }

    public void playClick(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        } catch (Throwable ignored) { }
    }

    private ItemStack named(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private int invSizeFor(int items) {
        int[] sizes = {9, 18, 27, 36, 45, 54};
        for (int s : sizes) if (items <= s) return s;
        return 54;
    }
}
