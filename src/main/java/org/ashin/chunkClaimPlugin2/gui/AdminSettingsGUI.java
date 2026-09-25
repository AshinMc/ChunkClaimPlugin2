package org.ashin.chunkClaimPlugin2.gui;

import org.ashin.chunkClaimPlugin2.managers.ChunkManager;
import org.ashin.chunkClaimPlugin2.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin GUI for server-wide settings: max claims, default language, default particle,
 * greeting display mode, chunk inspector / force unclaim, and config reload.
 */
public class AdminSettingsGUI {

    public enum View { ADMIN_HOME, ADMIN_MAX_CLAIMS, ADMIN_LANGUAGE, ADMIN_PARTICLE, ADMIN_CHUNK_INSPECT }

    public static class AdminHolder implements org.bukkit.inventory.InventoryHolder {
        public final View view;
        public AdminHolder(View view) { this.view = view; }
        @Override
        public Inventory getInventory() { return null; }
    }

    private final JavaPlugin plugin;
    private final ChunkManager chunkManager;
    private final MessageManager messages;

    public AdminSettingsGUI(JavaPlugin plugin, ChunkManager chunkManager, MessageManager messages) {
        this.plugin = plugin;
        this.chunkManager = chunkManager;
        this.messages = messages;
    }

    public void openHome(Player player) {
        String title = messages.getFor(player.getUniqueId(), "admin-gui-title-home");
        Inventory inv = Bukkit.createInventory(new AdminHolder(View.ADMIN_HOME), 27, ChatColor.stripColor(title));

        // Max Claims item
        int currentMax = plugin.getConfig().getInt("max-claims-per-player", 10);
        inv.setItem(10, namedWithLore(Material.CHEST,
                ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-max-claims")),
                ChatColor.GRAY + "Current: " + ChatColor.YELLOW + (currentMax == 0 ? "Unlimited" : currentMax)));

        // Default Language item
        String currentLocale = plugin.getConfig().getString("locale", "en_US");
        inv.setItem(11, namedWithLore(Material.BOOK,
                ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-language")),
                ChatColor.GRAY + "Current: " + ChatColor.YELLOW + currentLocale));

        // Default Particle item
        String currentParticle = plugin.getConfig().getString("visualization.particle-type", "FLAME");
        inv.setItem(12, namedWithLore(Material.BLAZE_POWDER,
                ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-particle")),
                ChatColor.GRAY + "Current: " + ChatColor.YELLOW + currentParticle));

        // Greeting Display Mode item (NEW)
        String currentGreeting = plugin.getConfig().getString("greeting-display", "ACTION_BAR").toUpperCase();
        inv.setItem(14, namedWithLore(Material.OAK_SIGN,
                ChatColor.GOLD + "Greeting Display",
                ChatColor.GRAY + "Mode: " + ChatColor.YELLOW + currentGreeting,
                ChatColor.DARK_GRAY + "Click to cycle: ACTION_BAR / TITLE / SUBTITLE / CHAT / NONE"));

        // Chunk Inspector / Admin Unclaim item (NEW)
        Chunk chunk = player.getLocation().getChunk();
        boolean isClaimed = chunkManager.isChunkClaimed(chunk);
        inv.setItem(15, namedWithLore(Material.COMPASS,
                ChatColor.AQUA + "Chunk Inspector",
                ChatColor.GRAY + "Chunk: " + ChatColor.WHITE + "X: " + (chunk.getX() * 16) + ", Z: " + (chunk.getZ() * 16),
                ChatColor.GRAY + "Status: " + (isClaimed ? ChatColor.GREEN + "Claimed" : ChatColor.YELLOW + "Unclaimed"),
                ChatColor.DARK_GRAY + "Click to inspect & manage claim"));

        // Reload config item
        inv.setItem(16, named(Material.REDSTONE,
                ChatColor.stripColor(messages.getFor(player.getUniqueId(), "admin-gui-item-reload"))));

        player.openInventory(inv);
        playClick(player);
    }

    public void openChunkInspector(Player player) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(View.ADMIN_CHUNK_INSPECT), 27, "Admin Chunk Inspector");
        Chunk chunk = player.getLocation().getChunk();
        UUID owner = chunkManager.getChunkOwner(chunk);

        if (owner == null) {
            inv.setItem(13, namedWithLore(Material.GRASS_BLOCK,
                    ChatColor.YELLOW + "Unclaimed Chunk",
                    ChatColor.GRAY + "Coordinates: " + ChatColor.WHITE + "X: " + (chunk.getX() * 16) + ", Z: " + (chunk.getZ() * 16),
                    ChatColor.GRAY + "World: " + ChatColor.WHITE + chunk.getWorld().getName(),
                    ChatColor.GRAY + "This chunk is in the wilderness."));
        } else {
            OfflinePlayer offOwner = Bukkit.getOfflinePlayer(owner);
            String ownerName = offOwner.getName() != null ? offOwner.getName() : owner.toString();
            String claimName = chunkManager.getChunkClaimName(chunk);
            if (claimName == null) claimName = "world";
            int groupSize = chunkManager.getChunksByName(owner, claimName).size();

            // Info center
            inv.setItem(13, namedWithLore(Material.GOLDEN_PICKAXE,
                    ChatColor.GOLD + claimName,
                    ChatColor.GRAY + "Owner: " + ChatColor.YELLOW + ownerName,
                    ChatColor.GRAY + "Coordinates: " + ChatColor.WHITE + "X: " + (chunk.getX() * 16) + ", Z: " + (chunk.getZ() * 16),
                    ChatColor.GRAY + "Group size: " + ChatColor.YELLOW + groupSize + " chunk(s)"));

            // Force unclaim this single chunk
            inv.setItem(11, namedWithLore(Material.RED_CONCRETE,
                    ChatColor.RED + "Force Unclaim Chunk",
                    ChatColor.GRAY + "Removes only this specific chunk",
                    ChatColor.DARK_RED + "Click to execute"));

            // Force unclaim entire group
            inv.setItem(15, namedWithLore(Material.TNT,
                    ChatColor.DARK_RED + "Force Unclaim Entire Group",
                    ChatColor.GRAY + "Removes all " + groupSize + " chunks in '" + claimName + "'",
                    ChatColor.DARK_RED + "Click to execute"));
        }

        inv.setItem(18, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openMaxClaims(Player player) {
        String title = messages.getFor(player.getUniqueId(), "admin-gui-title-max-claims");
        Inventory inv = Bukkit.createInventory(new AdminHolder(View.ADMIN_MAX_CLAIMS), 27, ChatColor.stripColor(title));

        int current = plugin.getConfig().getInt("max-claims-per-player", 10);

        // Show current value
        inv.setItem(4, namedWithLore(Material.PAPER,
                ChatColor.GOLD + "Current: " + (current == 0 ? "Unlimited" : String.valueOf(current)),
                ChatColor.GRAY + "Click items below to adjust"));

        // Decrease buttons
        inv.setItem(10, named(Material.RED_CONCRETE, ChatColor.RED + "-10"));
        inv.setItem(11, named(Material.RED_CONCRETE, ChatColor.RED + "-5"));
        inv.setItem(12, named(Material.RED_CONCRETE, ChatColor.RED + "-1"));

        // Increase buttons
        inv.setItem(14, named(Material.GREEN_CONCRETE, ChatColor.GREEN + "+1"));
        inv.setItem(15, named(Material.GREEN_CONCRETE, ChatColor.GREEN + "+5"));
        inv.setItem(16, named(Material.GREEN_CONCRETE, ChatColor.GREEN + "+10"));

        // Unlimited toggle
        inv.setItem(22, named(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Unlimited (0)"));

        // Back
        inv.setItem(18, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));

        player.openInventory(inv);
        playClick(player);
    }

    public void openLanguage(Player player) {
        List<String> locales = messages.getAvailableLocales();
        int size = invSizeFor(locales.size() + 1);
        String title = messages.getFor(player.getUniqueId(), "admin-gui-title-language");
        Inventory inv = Bukkit.createInventory(new AdminHolder(View.ADMIN_LANGUAGE), size, ChatColor.stripColor(title));

        String currentLocale = plugin.getConfig().getString("locale", "en_US");
        int i = 0;
        for (String loc : locales) {
            Material mat = loc.equals(currentLocale) ? Material.ENCHANTED_BOOK : Material.BOOK;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + loc);
            if (loc.equals(currentLocale)) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "Currently selected");
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }
        inv.setItem(size - 1, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public void openParticle(Player player) {
        String title = messages.getFor(player.getUniqueId(), "admin-gui-title-particle");
        Inventory inv = Bukkit.createInventory(new AdminHolder(View.ADMIN_PARTICLE), 27, ChatColor.stripColor(title));

        String current = plugin.getConfig().getString("visualization.particle-type", "FLAME");
        String[][] particles = getAvailableParticles();

        int slot = 0;
        for (String[] entry : particles) {
            String particleName = entry[0];
            Material icon = Material.matchMaterial(entry[1]);
            if (icon == null) icon = Material.OAK_LEAVES; // Fallback for versions lacking CHERRY_LEAVES
            
            boolean selected = particleName.equals(current);
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

        inv.setItem(26, named(Material.ARROW, ChatColor.stripColor(messages.getFor(player.getUniqueId(), "gui-item-back"))));
        player.openInventory(inv);
        playClick(player);
    }

    public static String[][] getAvailableParticles() {
        return new String[][]{
                {"FLAME", "BLAZE_POWDER"},
                {"END_ROD", "END_ROD"},
                {"HEART", "RED_DYE"},
                {"HAPPY_VILLAGER", "EMERALD"},
                {"DUST", "REDSTONE"},
                {"SNOWFLAKE", "SNOWBALL"},
                {"SOUL_FIRE_FLAME", "SOUL_LANTERN"},
                {"CHERRY_LEAVES", "CHERRY_LEAVES"},
        };
    }

    public static Particle resolveParticle(String name) {
        if (name == null || name.isEmpty()) return Particle.FLAME;
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
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

    private ItemStack namedWithLore(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(line);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int invSizeFor(int items) {
        int[] sizes = {9, 18, 27, 36, 45, 54};
        for (int s : sizes) if (items <= s) return s;
        return 54;
    }
}
