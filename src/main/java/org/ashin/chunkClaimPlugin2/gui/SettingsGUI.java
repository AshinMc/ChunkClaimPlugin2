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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SettingsGUI {
    public enum View { HOME, CLAIMS, VISUALIZE, DELETE, LANGUAGE, PARTICLE, CLAIM_DETAILS, DELETE_CONFIRM, TRUST }

    public static class SettingsHolder implements org.bukkit.inventory.InventoryHolder {
        public final View view;
        public final String claimName; // for views that operate on a specific claim
        public SettingsHolder(View view) { this(view, null); }
        public SettingsHolder(View view, String claimName) { this.view = view; this.claimName = claimName; }
        @Override
        public org.bukkit.inventory.Inventory getInventory() { return null; }
    }

    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public SettingsGUI(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    public void openHome(Player player) {
    String title = messages.getFor(player.getUniqueId(), "gui-title-home");
    Inventory inv = Bukkit.createInventory(new SettingsHolder(View.HOME), 27, ChatColor.stripColor(title));
        inv.setItem(10, named(Material.GRASS_BLOCK, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-claims"))));
        inv.setItem(11, named(Material.SPYGLASS, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-visualize"))));
        inv.setItem(13, named(Material.BARRIER, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-delete"))));
        inv.setItem(15, named(Material.BLAZE_POWDER, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-particle"))));
        inv.setItem(16, named(Material.BOOK, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-language"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openClaims(Player player) {
        List<String> claimNames = chunkManager.getPlayerClaimNames(player.getUniqueId());
        int size = invSizeFor(claimNames.size() + 1);
        String title = messages.getFor(player.getUniqueId(), "gui-title-claims");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.CLAIMS), size, ChatColor.stripColor(title));
        int i = 0;
        for (String name : claimNames) {
            List<ChunkData> chunks = chunkManager.getChunksByName(player.getUniqueId(), name);
            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + name);
            List<String> lore = new ArrayList<>();
            lore.add("" + ChatColor.GRAY + chunks.size() + " chunk(s)");
            if (!chunks.isEmpty()) {
                ChunkData first = chunks.get(0);
                lore.add(ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-lore-block",
                        "x", String.valueOf(first.getX() * 16), "z", String.valueOf(first.getZ() * 16))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openVisualize(Player player) {
        List<String> claimNames = chunkManager.getPlayerClaimNames(player.getUniqueId());
        int size = invSizeFor(claimNames.size() + 1);
        String title = messages.getFor(player.getUniqueId(), "gui-title-visualize");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.VISUALIZE), size, ChatColor.stripColor(title));
        int i = 0;
        for (String name : claimNames) {
            List<ChunkData> chunks = chunkManager.getChunksByName(player.getUniqueId(), name);
            ItemStack item = new ItemStack(Material.SPYGLASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + name);
            List<String> lore = new ArrayList<>();
            lore.add("" + ChatColor.GRAY + chunks.size() + " chunk(s)");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openDelete(Player player) {
        List<String> claimNames = chunkManager.getPlayerClaimNames(player.getUniqueId());
        int size = invSizeFor(claimNames.size() + 1);
        String title = messages.getFor(player.getUniqueId(), "gui-title-delete");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.DELETE), size, ChatColor.stripColor(title));
        int i = 0;
        for (String name : claimNames) {
            List<ChunkData> chunks = chunkManager.getChunksByName(player.getUniqueId(), name);
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + name);
            List<String> lore = new ArrayList<>();
            lore.add("" + ChatColor.GRAY + chunks.size() + " chunk(s)");
            meta.setLore(lore);
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

    public void openParticle(Player player) {
        String title = messages.getFor(player.getUniqueId(), "gui-title-particle");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.PARTICLE), 27, ChatColor.stripColor(title));

        String currentParticle = messages.getPlayerParticle(player.getUniqueId());
        if (currentParticle == null || currentParticle.isEmpty()) {
            currentParticle = plugin.getConfig().getString("visualization.particle-type", "FLAME");
        }

        String[][] particles = AdminSettingsGUI.getAvailableParticles();
        int slot = 0;
        for (String[] entry : particles) {
            String particleName = entry[0];
            Material icon = Material.valueOf(entry[1]);
            boolean selected = particleName.equals(currentParticle);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.YELLOW) + particleName);
            if (selected) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "Currently selected");
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Reset to server default option
        inv.setItem(22, named(Material.NETHER_STAR, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-particle-reset"))));
        inv.setItem(26, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openClaimDetails(Player player, String claimName) {
        String title = messages.getFor(player.getUniqueId(), "gui-title-claim-details");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.CLAIM_DETAILS), 27, ChatColor.stripColor(title));

        List<ChunkData> chunks = chunkManager.getChunksByName(player.getUniqueId(), claimName);

        // Info item (paper)
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + claimName);
        List<String> lore = new ArrayList<>();
        lore.add("" + ChatColor.GRAY + chunks.size() + " chunk(s)");
        for (ChunkData cd : chunks) {
            lore.add(ChatColor.YELLOW + cd.getWorld() + " @ " + (cd.getX() * 16) + ", " + (cd.getZ() * 16));
        }
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(11, info);

        // Teleport item (ender pearl) - teleports to first chunk in group
        inv.setItem(13, named(Material.ENDER_PEARL, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-teleport"))));

        // Trusted players item (player head)
        inv.setItem(22, named(Material.PLAYER_HEAD, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-trust"))));

        // Back item
        inv.setItem(15, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));

        player.openInventory(inv);
        playClick(player);
    }

    public void openTrust(Player player, String claimName) {
        String title = messages.getFor(player.getUniqueId(), "gui-title-trust");
        java.util.Set<UUID> trusted = chunkManager.getTrustedPlayers(player.getUniqueId(), claimName);
        // Collect online players (excluding the owner)
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                candidates.add(online);
            }
        }
        int size = invSizeFor(candidates.size() + 1);
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.TRUST, claimName), size, ChatColor.stripColor(title));
        int i = 0;
        for (Player online : candidates) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(online);
            boolean isTrusted = trusted.contains(online.getUniqueId());
            sm.setDisplayName((isTrusted ? ChatColor.GREEN : ChatColor.GRAY) + online.getName());
            List<String> lore = new ArrayList<>();
            lore.add(isTrusted ? ChatColor.GREEN + "\u2714 Trusted" : ChatColor.RED + "\u2716 Not trusted");
            sm.setLore(lore);
            head.setItemMeta(sm);
            inv.setItem(i++, head);
        }
        if (candidates.isEmpty()) {
            player.sendMessage(messages.getFor(player.getUniqueId(), "trust-no-online"));
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openDeleteConfirm(Player player, String claimName) {
        String title = messages.getFor(player.getUniqueId(), "gui-title-delete-confirm");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(View.DELETE_CONFIRM), 27, ChatColor.stripColor(title));

        List<ChunkData> chunks = chunkManager.getChunksByName(player.getUniqueId(), claimName);

        // Info item in center
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.RED + claimName);
        List<String> lore = new ArrayList<>();
        lore.add("" + ChatColor.GRAY + chunks.size() + " chunk(s)");
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
